package com.pen.penhubbackend.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.pen.penhubbackend.agent.context.StreamHandlerContext;
import com.pen.penhubbackend.constant.PromptConstant;
import com.pen.penhubbackend.model.dto.script.EventScriptState;
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
 * 活动话术 Agent - 生成各环节话术
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EventScriptAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_EVENT_TYPE = "eventType";
    public static final String INPUT_OUTLINE = "outline";
    public static final String OUTPUT_SCRIPT_RESULT = "scriptResult";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String topic = state.value(INPUT_TOPIC).map(Object::toString).orElse("");
        String eventType = state.value(INPUT_EVENT_TYPE).map(Object::toString).orElse("conference");

        EventScriptState.EventOutlineResult outline = state.value(INPUT_OUTLINE)
                .map(v -> {
                    if (v instanceof EventScriptState.EventOutlineResult eor) return eor;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), EventScriptState.EventOutlineResult.class);
                })
                .orElseThrow(() -> new IllegalArgumentException("缺少活动大纲"));

        log.info("EventScriptAgent 开始执行: topic={}", topic);

        String segmentsOutline = GsonUtils.toJson(outline.getSegments());
        String prompt = PromptConstant.EVENT_SCRIPT_PROMPT
                .replace("{topic}", topic)
                .replace("{eventType}", eventType)
                .replace("{segmentsOutline}", segmentsOutline);

        Consumer<String> streamHandler = StreamHandlerContext.get();
        String content = callLlmWithStreaming(prompt, streamHandler);

        EventScriptState.EventScriptResult scriptResult = GsonUtils.fromJson(content, EventScriptState.EventScriptResult.class);

        log.info("EventScriptAgent 执行完成: 环节数={}", scriptResult.getSegments() != null ? scriptResult.getSegments().size() : 0);

        return Map.of(OUTPUT_SCRIPT_RESULT, scriptResult);
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
                            streamHandler.accept(SseMessageTypeEnum.EVENT_SCRIPT_STREAMING.getStreamingPrefix() + chunk);
                        }
                    }
                })
                .doOnError(error -> log.error("EventScriptAgent 流式调用失败", error))
                .blockLast();
        return contentBuilder.toString();
    }
}
