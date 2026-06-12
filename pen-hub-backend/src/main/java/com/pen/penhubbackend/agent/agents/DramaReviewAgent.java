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

import java.util.Map;

/**
 * 剧本审核 Agent
 * 检查剧本的剧情连贯性、角色一致性、对话自然度等
 *
 * @author pen-hub
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DramaReviewAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_GENRE = "genre";
    public static final String INPUT_STYLE = "style";
    public static final String INPUT_SCRIPT_RESULT = "scriptResult";
    public static final String OUTPUT_REVIEW_RESULT = "reviewResult";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String topic = state.value(INPUT_TOPIC).map(Object::toString).orElse("");
        String genre = state.value(INPUT_GENRE).map(Object::toString).orElse("drama");
        String style = state.value(INPUT_STYLE).map(Object::toString).orElse("natural");

        DramaState.DramaScriptResult scriptResult = state.value(INPUT_SCRIPT_RESULT)
                .map(v -> {
                    if (v instanceof DramaState.DramaScriptResult dsr) return dsr;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), DramaState.DramaScriptResult.class);
                })
                .orElseThrow(() -> new IllegalArgumentException("缺少台词场景结果"));

        log.info("DramaReviewAgent 开始执行: topic={}, genre={}", topic, genre);

        String scriptContent = GsonUtils.toJson(scriptResult);
        String prompt = PromptConstant.DRAMA_REVIEW_PROMPT
                .replace("{topic}", topic)
                .replace("{genre}", genre)
                .replace("{style}", style)
                .replace("{scriptContent}", scriptContent);

        String responseContent = callLlmWithRetry(prompt, 3);
        DramaState.DramaReviewResult reviewResult = GsonUtils.fromJson(responseContent, DramaState.DramaReviewResult.class);

        if (reviewResult.getReviewScore() != null) {
            reviewResult.setReviewScore(Math.max(0, Math.min(100, reviewResult.getReviewScore())));
        }

        log.info("DramaReviewAgent 执行完成: score={}", reviewResult.getReviewScore());

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
                log.warn("DramaReviewAgent 第{}次调用返回空内容", i + 1);
            } catch (Exception e) {
                lastException = e;
                log.warn("DramaReviewAgent 第{}次调用失败: {}", i + 1, e.getMessage());
            }
        }
        throw new RuntimeException("DramaReviewAgent 调用失败，已重试" + maxRetries + "次", lastException);
    }
}
