package com.pen.penhubbackend.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.pen.penhubbackend.constant.PromptConstant;
import com.pen.penhubbackend.model.dto.article.ArticleState;
import com.pen.penhubbackend.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 内容审核 Agent（Reviewer）
 * 评估文章质量，给出评分和改进建议
 * 评分维度：连贯性 / 准确性 / 风格一致性 / 平台适配度
 *
 * 评分策略：
 * - reviewScore < 60 → 自动使用 rewrittenContent
 * - reviewScore 60-80 → 保留原内容 + 记录 suggestions
 * - reviewScore > 80 → 直接通过
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReviewerAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_MAIN_TITLE = "mainTitle";
    public static final String INPUT_CONTENT = "content";
    public static final String INPUT_STYLE = "style";
    public static final String OUTPUT_REVIEW_RESULT = "reviewResult";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String mainTitle = state.value(INPUT_MAIN_TITLE)
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("缺少主标题参数"));

        String content = state.value(INPUT_CONTENT)
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("缺少正文内容参数"));

        String style = state.value(INPUT_STYLE)
                .map(Object::toString)
                .orElse("");

        log.info("ReviewerAgent 开始执行: mainTitle={}, 正文长度={}", mainTitle, content.length());

        // 构建 prompt
        String prompt = PromptConstant.REVIEWER_PROMPT
                .replace("{mainTitle}", mainTitle)
                .replace("{content}", content)
                .replace("{style}", style);

        // 调用 LLM（非流式，最多重试 3 次）
        String responseContent = callLlmWithRetry(prompt, 3);

        // 解析结果
        ArticleState.ReviewResult reviewResult = GsonUtils.fromJson(
                responseContent,
                ArticleState.ReviewResult.class
        );

        // 校验评分范围
        if (reviewResult.getReviewScore() < 0) {
            reviewResult.setReviewScore(0);
        }
        if (reviewResult.getReviewScore() > 100) {
            reviewResult.setReviewScore(100);
        }

        log.info("ReviewerAgent 执行完成: score={}, suggestions数量={}",
                reviewResult.getReviewScore(),
                reviewResult.getSuggestions() != null ? reviewResult.getSuggestions().size() : 0);

        return Map.of(OUTPUT_REVIEW_RESULT, reviewResult);
    }

    /**
     * 调用 LLM（带重试）
     */
    private String callLlmWithRetry(String prompt, int maxRetries) {
        Exception lastException = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
                String content = response.getResult().getOutput().getText();
                if (content != null && !content.isBlank()) {
                    return content;
                }
                log.warn("ReviewerAgent 第{}次调用返回空内容", i + 1);
            } catch (Exception e) {
                lastException = e;
                log.warn("ReviewerAgent 第{}次调用失败: {}", i + 1, e.getMessage());
            }
        }
        throw new RuntimeException("ReviewerAgent 调用失败，已重试" + maxRetries + "次",
                lastException);
    }
}
