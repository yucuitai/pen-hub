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

import java.util.Map;

/**
 * 访谈审核 Agent
 * 检查访谈脚本质量和对话流畅度
 *
 * @author pen-hub
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InterviewReviewAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_PLATFORM = "platform";
    public static final String INPUT_STYLE = "style";
    public static final String INPUT_DIALOGUE_RESULT = "dialogueResult";
    public static final String OUTPUT_REVIEW_RESULT = "reviewResult";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String topic = state.value(INPUT_TOPIC).map(Object::toString).orElse("");
        String platform = state.value(INPUT_PLATFORM).map(Object::toString).orElse("douyin");
        String style = state.value(INPUT_STYLE).map(Object::toString).orElse("formal");

        InterviewState.InterviewDialogueResult dialogueResult = state.value(INPUT_DIALOGUE_RESULT)
                .map(v -> {
                    if (v instanceof InterviewState.InterviewDialogueResult idr) return idr;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), InterviewState.InterviewDialogueResult.class);
                })
                .orElseThrow(() -> new IllegalArgumentException("缺少访谈对话内容"));

        log.info("InterviewReviewAgent 开始执行: topic={}", topic);

        String dialogueContent = GsonUtils.toJson(dialogueResult);
        String prompt = PromptConstant.INTERVIEW_REVIEW_PROMPT
                .replace("{topic}", topic)
                .replace("{platform}", platform)
                .replace("{style}", style)
                .replace("{dialogueContent}", dialogueContent);

        String responseContent = callLlmWithRetry(prompt, 3);
        InterviewState.InterviewReviewResult reviewResult = GsonUtils.fromJson(responseContent, InterviewState.InterviewReviewResult.class);

        if (reviewResult.getReviewScore() != null) {
            reviewResult.setReviewScore(Math.max(0, Math.min(100, reviewResult.getReviewScore())));
        }

        log.info("InterviewReviewAgent 执行完成: score={}", reviewResult.getReviewScore());

        return Map.of(OUTPUT_REVIEW_RESULT, reviewResult);
    }

    private String callLlmWithRetry(String prompt, int maxRetries) {
        Exception lastException = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
                String content = response.getResult().getOutput().getText();
                if (content != null && !content.isBlank()) {
                    return content;
                }
                log.warn("InterviewReviewAgent 第{}次调用返回空内容", i + 1);
            } catch (Exception e) {
                lastException = e;
                log.warn("InterviewReviewAgent 第{}次调用失败: {}", i + 1, e.getMessage());
            }
        }
        throw new RuntimeException("InterviewReviewAgent 调用失败，已重试" + maxRetries + "次", lastException);
    }
}
