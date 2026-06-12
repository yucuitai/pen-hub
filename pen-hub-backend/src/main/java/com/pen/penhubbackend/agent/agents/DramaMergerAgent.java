package com.pen.penhubbackend.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.pen.penhubbackend.constant.PromptConstant;
import com.pen.penhubbackend.model.dto.drama.DramaState;
import com.pen.penhubbackend.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 剧本合成 Agent
 * 合并为完整剧本（结构化 + Markdown + 纯文本）
 *
 * @author pen-hub
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DramaMergerAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_GENRE = "genre";
    public static final String INPUT_STYLE = "style";
    public static final String INPUT_CHARACTER_RESULT = "characterResult";
    public static final String INPUT_PLOT_RESULT = "plotResult";
    public static final String INPUT_SCRIPT_RESULT = "scriptResult";
    public static final String INPUT_REVIEW_RESULT = "reviewResult";
    public static final String OUTPUT_SCRIPT_STRUCTURE = "scriptStructure";
    public static final String OUTPUT_MARKDOWN_CONTENT = "markdownContent";
    public static final String OUTPUT_PLAIN_TEXT_CONTENT = "plainTextContent";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String topic = state.value(INPUT_TOPIC).map(Object::toString).orElse("");
        String genre = state.value(INPUT_GENRE).map(Object::toString).orElse("drama");
        String style = state.value(INPUT_STYLE).map(Object::toString).orElse("natural");

        DramaState.DramaCharacterResult characterResult = state.value(INPUT_CHARACTER_RESULT)
                .map(v -> {
                    if (v instanceof DramaState.DramaCharacterResult dcr) return dcr;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), DramaState.DramaCharacterResult.class);
                })
                .orElse(null);

        DramaState.DramaPlotResult plotResult = state.value(INPUT_PLOT_RESULT)
                .map(v -> {
                    if (v instanceof DramaState.DramaPlotResult dpr) return dpr;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), DramaState.DramaPlotResult.class);
                })
                .orElse(null);

        DramaState.DramaScriptResult scriptResult = state.value(INPUT_SCRIPT_RESULT)
                .map(v -> {
                    if (v instanceof DramaState.DramaScriptResult dsr) return dsr;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), DramaState.DramaScriptResult.class);
                })
                .orElseThrow(() -> new IllegalArgumentException("缺少台词场景结果"));

        DramaState.DramaReviewResult reviewResult = state.value(INPUT_REVIEW_RESULT)
                .map(v -> {
                    if (v instanceof DramaState.DramaReviewResult drr) return drr;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), DramaState.DramaReviewResult.class);
                })
                .orElse(null);

        log.info("DramaMergerAgent 开始执行: topic={}, genre={}", topic, genre);

        // 如果审核分数低且有重写内容，使用重写内容
        String scriptContent;
        if (reviewResult != null && reviewResult.getReviewScore() != null
                && reviewResult.getReviewScore() < 60
                && reviewResult.getRewrittenScript() != null
                && !reviewResult.getRewrittenScript().isBlank()) {
            log.info("使用审核重写后的剧本内容, score={}", reviewResult.getReviewScore());
            scriptContent = reviewResult.getRewrittenScript();
        } else {
            scriptContent = GsonUtils.toJson(scriptResult);
        }

        String charactersJson = characterResult != null ? GsonUtils.toJson(characterResult.getCharacters()) : "[]";
        String plotJson = plotResult != null ? GsonUtils.toJson(plotResult) : "{}";
        String reviewSuggestions = reviewResult != null && reviewResult.getSuggestions() != null
                ? String.join("; ", reviewResult.getSuggestions())
                : "无";

        String prompt = PromptConstant.DRAMA_MERGER_PROMPT
                .replace("{topic}", topic)
                .replace("{genre}", genre)
                .replace("{style}", style)
                .replace("{characters}", charactersJson)
                .replace("{plot}", plotJson)
                .replace("{scriptContent}", scriptContent)
                .replace("{reviewSuggestions}", reviewSuggestions);

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String content = response.getResult().getOutput().getText();

        DramaState.DramaScriptResult mergeResult = GsonUtils.fromJson(content, DramaState.DramaScriptResult.class);

        log.info("DramaMergerAgent 执行完成: fullScript长度={}",
                mergeResult.getFullScript() != null ? mergeResult.getFullScript().length() : 0);

        Map<String, Object> result = new HashMap<>();
        result.put(OUTPUT_SCRIPT_STRUCTURE, GsonUtils.toJson(mergeResult));
        result.put(OUTPUT_MARKDOWN_CONTENT, mergeResult.getFullScript());
        result.put(OUTPUT_PLAIN_TEXT_CONTENT, mergeResult.getFullScript());
        return result;
    }
}
