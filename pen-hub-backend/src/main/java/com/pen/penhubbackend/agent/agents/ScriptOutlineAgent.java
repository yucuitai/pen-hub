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
 * 脚本大纲 Agent
 * 规划短视频分镜和节奏
 *
 * @author pen-hub
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ScriptOutlineAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_PLATFORM = "platform";
    public static final String INPUT_DURATION = "duration";
    public static final String INPUT_STYLE = "style";
    public static final String INPUT_HOOK_RESULT = "hookResult";
    public static final String OUTPUT_OUTLINE = "outline";

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
                .orElseThrow(() -> new IllegalArgumentException("缺少Hook结果"));

        log.info("ScriptOutlineAgent 开始执行: topic={}", topic);

        String prompt = PromptConstant.SCRIPT_OUTLINE_PROMPT
                .replace("{topic}", topic)
                .replace("{platform}", platform)
                .replace("{duration}", duration)
                .replace("{style}", style)
                .replace("{hookText}", hookResult.getHookText());

        Consumer<String> streamHandler = StreamHandlerContext.get();
        String content = callLlmWithStreaming(prompt, streamHandler);

        ScriptState.ScriptOutlineResult outline = GsonUtils.fromJson(content, ScriptState.ScriptOutlineResult.class);

        log.info("ScriptOutlineAgent 执行完成: 分镜数={}", outline.getScenes() != null ? outline.getScenes().size() : 0);

        return Map.of(OUTPUT_OUTLINE, outline);
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
                            streamHandler.accept(SseMessageTypeEnum.SCRIPT_OUTLINE_STREAMING.getStreamingPrefix() + chunk);
                        }
                    }
                })
                .doOnError(error -> log.error("ScriptOutlineAgent 流式调用失败", error))
                .blockLast();
        return contentBuilder.toString();
    }
}
