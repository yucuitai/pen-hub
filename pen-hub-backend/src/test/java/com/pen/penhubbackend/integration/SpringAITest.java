package com.pen.penhubbackend.integration;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spring AI 大模型集成测试
 * 测试 DashScope (通义千问) 和 DeepSeek 的调用
 */
@Slf4j
@SpringBootTest
@DisplayName("Spring AI 大模型集成测试")
class SpringAITest {

    @Resource
    private DashScopeChatModel dashScopeChatModel;

    @Resource
    private ChatModel openAiChatModel;

    @Test
    @DisplayName("测试 DashScope 同步调用")
    void testDashScopeSync() {
        // given
        String question = "你好，请用一句话介绍自己";

        // when
        String response = dashScopeChatModel.call(question);

        // then
        log.info("DashScope 同步响应: {}", response);
        assertNotNull(response, "响应不应为空");
        assertFalse(response.isEmpty(), "响应内容不应为空");
    }

    @Test
    @DisplayName("测试 DashScope 流式调用")
    void testDashScopeStream() throws InterruptedException {
        // given
        String question = "用三句话描述春天";
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<StringBuilder> fullResponse = new AtomicReference<>(new StringBuilder());

        // when
        Flux<ChatResponse> stream = dashScopeChatModel.stream(new Prompt(question));
        stream
            .doOnComplete(latch::countDown)
            .subscribe(
                chunk -> {
                    String text = chunk.getResult().getOutput().getText();
                    if (text != null) {
                        fullResponse.get().append(text);
                    }
                },
                error -> {
                    log.error("流式调用出错", error);
                    latch.countDown();
                }
            );

        // then
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "流式调用应在 30 秒内完成");
        String response = fullResponse.get().toString();
        log.info("DashScope 流式响应: {}", response);
        assertFalse(response.isEmpty(), "流式响应内容不应为空");
    }

    @Test
    @DisplayName("测试 DeepSeek 同步调用")
    void testDeepSeekSync() {
        // given
        String question = "你好，请用一句话介绍自己";

        // when
        String response = openAiChatModel.call(question);

        // then
        log.info("DeepSeek 同步响应: {}", response);
        assertNotNull(response, "响应不应为空");
        assertFalse(response.isEmpty(), "响应内容不应为空");
    }

    @Test
    @DisplayName("测试 DeepSeek 流式调用")
    void testDeepSeekStream() throws InterruptedException {
        // given
        String question = "用三句话描述夏天";
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<StringBuilder> fullResponse = new AtomicReference<>(new StringBuilder());

        // when
        Flux<ChatResponse> stream = openAiChatModel.stream(new Prompt(question));
        stream
            .doOnComplete(latch::countDown)
            .subscribe(
                chunk -> {
                    String text = chunk.getResult().getOutput().getText();
                    if (text != null) {
                        fullResponse.get().append(text);
                    }
                },
                error -> {
                    log.error("流式调用出错", error);
                    latch.countDown();
                }
            );

        // then
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "流式调用应在 30 秒内完成");
        String response = fullResponse.get().toString();
        log.info("DeepSeek 流式响应: {}", response);
        assertFalse(response.isEmpty(), "流式响应内容不应为空");
    }

    @Test
    @DisplayName("测试 DashScope 带 Prompt 对象调用")
    void testDashScopeWithPrompt() {
        // given
        Prompt prompt = new Prompt("请用 JSON 格式返回一个包含姓名和年龄的用户信息");

        // when
        ChatResponse response = dashScopeChatModel.call(prompt);

        // then
        log.info("DashScope Prompt 响应: {}", response.getResult().getOutput().getText());
        assertNotNull(response, "响应不应为空");
        assertNotNull(response.getResult(), "结果不应为空");
        assertNotNull(response.getResult().getOutput(), "输出不应为空");
    }

    @Test
    @DisplayName("测试响应时间")
    void testResponseTime() {
        // given
        String question = "你好";
        long startTime = System.currentTimeMillis();

        // when
        dashScopeChatModel.call(question);

        // then
        long duration = System.currentTimeMillis() - startTime;
        log.info("DashScope 响应耗时: {}ms", duration);
        assertTrue(duration < 10000, "响应时间应小于 10 秒");
    }
}
