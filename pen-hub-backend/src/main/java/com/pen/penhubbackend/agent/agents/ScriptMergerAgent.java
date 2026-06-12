package com.pen.penhubbackend.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.pen.penhubbackend.constant.PromptConstant;
import com.pen.penhubbackend.model.dto.script.ScriptState;
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
 * 脚本合成 Agent
 * 合并为完整短视频脚本（结构化 + Markdown + 纯文本）
 *
 * @author pen-hub
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ScriptMergerAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_PLATFORM = "platform";
    public static final String INPUT_DURATION = "duration";
    public static final String INPUT_STYLE = "style";
    public static final String INPUT_HOOK_RESULT = "hookResult";
    public static final String INPUT_CONTENT_RESULT = "contentResult";
    public static final String INPUT_REVIEW_RESULT = "reviewResult";
    public static final String OUTPUT_SCRIPT_STRUCTURE = "scriptStructure";
    public static final String OUTPUT_MARKDOWN_CONTENT = "markdownContent";
    public static final String OUTPUT_PLAIN_TEXT_CONTENT = "plainTextContent";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String topic = state.value(INPUT_TOPIC).map(Object::toString).orElse("");
        String platform = state.value(INPUT_PLATFORM).map(Object::toString).orElse("douyin");
        String duration = state.value(INPUT_DURATION).map(Object::toString).orElse("30s");
        String style = state.value(INPUT_STYLE).map(Object::toString).orElse("funny");

        ScriptState.HookResult hookResult = state.value(INPUT_HOOK_RESULT)
                .map(v -> {
                    if (v instanceof ScriptState.HookResult hr) return hr;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), ScriptState.HookResult.class);
                })
                .orElse(null);

        ScriptState.ScriptContentResult contentResult = state.value(INPUT_CONTENT_RESULT)
                .map(v -> {
                    if (v instanceof ScriptState.ScriptContentResult scr) return scr;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), ScriptState.ScriptContentResult.class);
                })
                .orElseThrow(() -> new IllegalArgumentException("缺少脚本内容"));

        ScriptState.ScriptReviewResult reviewResult = state.value(INPUT_REVIEW_RESULT)
                .map(v -> {
                    if (v instanceof ScriptState.ScriptReviewResult srr) return srr;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), ScriptState.ScriptReviewResult.class);
                })
                .orElse(null);

        log.info("ScriptMergerAgent 开始执行: topic={}", topic);

        // 如果审核分数低且有重写内容，使用重写内容
        String scenesContent;
        if (reviewResult != null && reviewResult.getReviewScore() != null
                && reviewResult.getReviewScore() < 60
                && reviewResult.getRewrittenScript() != null
                && !reviewResult.getRewrittenScript().isBlank()) {
            log.info("使用审核重写后的脚本内容, score={}", reviewResult.getReviewScore());
            scenesContent = reviewResult.getRewrittenScript();
        } else {
            scenesContent = GsonUtils.toJson(contentResult.getScenes());
        }

        String reviewSuggestions = reviewResult != null && reviewResult.getSuggestions() != null
                ? String.join("; ", reviewResult.getSuggestions())
                : "无";

        String prompt = PromptConstant.SCRIPT_MERGER_PROMPT
                .replace("{topic}", topic)
                .replace("{platform}", platform)
                .replace("{duration}", duration)
                .replace("{style}", style)
                .replace("{hookText}", hookResult != null ? hookResult.getHookText() : "")
                .replace("{scenesContent}", scenesContent)
                .replace("{reviewSuggestions}", reviewSuggestions);

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String content = response.getResult().getOutput().getText();

        ScriptState.ScriptMergeResult mergeResult = GsonUtils.fromJson(content, ScriptState.ScriptMergeResult.class);

        log.info("ScriptMergerAgent 执行完成: markdown长度={}", mergeResult.getMarkdownContent() != null ? mergeResult.getMarkdownContent().length() : 0);

        Map<String, Object> result = new HashMap<>();
        result.put(OUTPUT_SCRIPT_STRUCTURE, mergeResult.getScriptStructure());
        result.put(OUTPUT_MARKDOWN_CONTENT, mergeResult.getMarkdownContent());
        result.put(OUTPUT_PLAIN_TEXT_CONTENT, mergeResult.getPlainTextContent());
        return result;
    }
}
