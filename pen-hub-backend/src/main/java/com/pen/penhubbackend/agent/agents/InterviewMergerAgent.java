package com.pen.penhubbackend.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.pen.penhubbackend.constant.PromptConstant;
import com.pen.penhubbackend.model.dto.script.InterviewState;
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
 * 访谈合成 Agent
 * 合并为完整访谈脚本（结构化 + Markdown + 纯文本）
 *
 * @author pen-hub
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InterviewMergerAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_PLATFORM = "platform";
    public static final String INPUT_DURATION = "duration";
    public static final String INPUT_STYLE = "style";
    public static final String INPUT_DIALOGUE_RESULT = "dialogueResult";
    public static final String INPUT_REVIEW_RESULT = "reviewResult";
    public static final String OUTPUT_SCRIPT_STRUCTURE = "scriptStructure";
    public static final String OUTPUT_MARKDOWN_CONTENT = "markdownContent";
    public static final String OUTPUT_PLAIN_TEXT_CONTENT = "plainTextContent";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String topic = state.value(INPUT_TOPIC).map(Object::toString).orElse("");
        String platform = state.value(INPUT_PLATFORM).map(Object::toString).orElse("douyin");
        String duration = state.value(INPUT_DURATION).map(Object::toString).orElse("30min");
        String style = state.value(INPUT_STYLE).map(Object::toString).orElse("formal");

        InterviewState.InterviewDialogueResult dialogueResult = state.value(INPUT_DIALOGUE_RESULT)
                .map(v -> {
                    if (v instanceof InterviewState.InterviewDialogueResult idr) return idr;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), InterviewState.InterviewDialogueResult.class);
                })
                .orElseThrow(() -> new IllegalArgumentException("缺少访谈对话内容"));

        InterviewState.InterviewReviewResult reviewResult = state.value(INPUT_REVIEW_RESULT)
                .map(v -> {
                    if (v instanceof InterviewState.InterviewReviewResult irr) return irr;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), InterviewState.InterviewReviewResult.class);
                })
                .orElse(null);

        log.info("InterviewMergerAgent 开始执行: topic={}", topic);

        // 如果审核分数低且有重写内容，使用重写内容
        String dialoguesContent;
        if (reviewResult != null && reviewResult.getReviewScore() != null
                && reviewResult.getReviewScore() < 60
                && reviewResult.getRewrittenScript() != null
                && !reviewResult.getRewrittenScript().isBlank()) {
            log.info("使用审核重写后的对话内容, score={}", reviewResult.getReviewScore());
            dialoguesContent = reviewResult.getRewrittenScript();
        } else {
            dialoguesContent = GsonUtils.toJson(dialogueResult.getDialogues());
        }

        String reviewSuggestions = reviewResult != null && reviewResult.getSuggestions() != null
                ? String.join("; ", reviewResult.getSuggestions())
                : "无";

        String prompt = PromptConstant.INTERVIEW_MERGER_PROMPT
                .replace("{topic}", topic)
                .replace("{platform}", platform)
                .replace("{duration}", duration)
                .replace("{style}", style)
                .replace("{dialogues}", dialoguesContent)
                .replace("{reviewSuggestions}", reviewSuggestions);

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String content = response.getResult().getOutput().getText();

        // 解析合成结果中的各个字段
        // merger 返回的是包含 scriptStructure, markdownContent, plainTextContent 的 JSON
        com.google.gson.JsonObject jsonObj = GsonUtils.fromJson(content, com.google.gson.JsonObject.class);

        Map<String, Object> result = new HashMap<>();
        result.put(OUTPUT_SCRIPT_STRUCTURE, jsonObj.has("scriptStructure") ? jsonObj.get("scriptStructure").toString() : null);
        result.put(OUTPUT_MARKDOWN_CONTENT, jsonObj.has("markdownContent") ? jsonObj.get("markdownContent").getAsString() : null);
        result.put(OUTPUT_PLAIN_TEXT_CONTENT, jsonObj.has("plainTextContent") ? jsonObj.get("plainTextContent").getAsString() : null);

        log.info("InterviewMergerAgent 执行完成: markdown长度={}", result.get(OUTPUT_MARKDOWN_CONTENT) != null ? ((String) result.get(OUTPUT_MARKDOWN_CONTENT)).length() : 0);

        return result;
    }
}
