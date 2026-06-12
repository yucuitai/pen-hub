package com.pen.penhubbackend.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.pen.penhubbackend.agent.context.StreamHandlerContext;
import com.pen.penhubbackend.constant.PromptConstant;
import com.pen.penhubbackend.model.dto.script.ScriptState;
import com.pen.penhubbackend.model.enums.SseMessageTypeEnum;
import com.pen.penhubbackend.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 脚本内容 Agent
 * 生成每个分镜的文案/台词/画面描述
 *
 * @author pen-hub
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ScriptContentAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_PLATFORM = "platform";
    public static final String INPUT_STYLE = "style";
    public static final String INPUT_HOOK_RESULT = "hookResult";
    public static final String INPUT_OUTLINE = "outline";
    public static final String OUTPUT_CONTENT_RESULT = "contentResult";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String topic = state.value(INPUT_TOPIC).map(Object::toString).orElse("");
        String platform = state.value(INPUT_PLATFORM).map(Object::toString).orElse("douyin");
        String style = state.value(INPUT_STYLE).map(Object::toString).orElse("funny");

        ScriptState.HookResult hookResult = state.value(INPUT_HOOK_RESULT)
                .map(v -> {
                    if (v instanceof ScriptState.HookResult hr) return hr;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), ScriptState.HookResult.class);
                })
                .orElseThrow(() -> new IllegalArgumentException("缺少Hook结果"));

        ScriptState.ScriptOutlineResult outline = state.value(INPUT_OUTLINE)
                .map(v -> {
                    if (v instanceof ScriptState.ScriptOutlineResult sor) return sor;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), ScriptState.ScriptOutlineResult.class);
                })
                .orElseThrow(() -> new IllegalArgumentException("缺少脚本大纲"));

        log.info("ScriptContentAgent 开始执行: topic={}", topic);

        String scenesOutline = GsonUtils.toJson(outline.getScenes());
        String prompt = PromptConstant.SCRIPT_CONTENT_PROMPT
                .replace("{topic}", topic)
                .replace("{platform}", platform)
                .replace("{style}", style)
                .replace("{hookText}", hookResult.getHookText())
                .replace("{scenesOutline}", scenesOutline);

        Consumer<String> streamHandler = StreamHandlerContext.get();
        String content = callLlmWithStreaming(prompt, streamHandler);

        ScriptState.ScriptContentResult contentResult = GsonUtils.fromJson(content, ScriptState.ScriptContentResult.class);

        log.info("ScriptContentAgent 执行完成: 分镜数={}", contentResult.getScenes() != null ? contentResult.getScenes().size() : 0);

        return Map.of(OUTPUT_CONTENT_RESULT, contentResult);
    }

    private String callLlmWithStreaming(String prompt, Consumer<String> streamHandler) {
        StringBuilder contentBuilder = new StringBuilder();
        Flux<ChatResponse> streamResponse = chatModel.stream(new Prompt(new UserMessage(prompt)));
        streamResponse
                .doOnNext(response -> {
                    String chunk = response.getResult().getOutput().getText();
                    if (chunk != null && !chunk.isEmpty()) {
                        contentBuilder.append(chunk);
                        if (streamHandler != null) {
                            streamHandler.accept(SseMessageTypeEnum.SCRIPT_CONTENT_STREAMING.getStreamingPrefix() + chunk);
                        }
                    }
                })
                .doOnError(error -> log.error("ScriptContentAgent 流式调用失败", error))
                .blockLast();
        return contentBuilder.toString();
    }
}
