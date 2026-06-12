package com.pen.penhubbackend.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.pen.penhubbackend.agent.context.StreamHandlerContext;
import com.pen.penhubbackend.constant.PromptConstant;
import com.pen.penhubbackend.model.dto.drama.DramaState;
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
 * 台词场景 Agent
 * 根据角色设定和剧情大纲生成详细台词和场景
 *
 * @author pen-hub
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DramaScriptAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_GENRE = "genre";
    public static final String INPUT_STYLE = "style";
    public static final String INPUT_CHARACTER_RESULT = "characterResult";
    public static final String INPUT_PLOT_RESULT = "plotResult";
    public static final String OUTPUT_SCRIPT_RESULT = "scriptResult";

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
                .orElseThrow(() -> new IllegalArgumentException("缺少角色设定结果"));

        DramaState.DramaPlotResult plotResult = state.value(INPUT_PLOT_RESULT)
                .map(v -> {
                    if (v instanceof DramaState.DramaPlotResult dpr) return dpr;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), DramaState.DramaPlotResult.class);
                })
                .orElseThrow(() -> new IllegalArgumentException("缺少剧情大纲结果"));

        log.info("DramaScriptAgent 开始执行: topic={}, genre={}", topic, genre);

        String charactersJson = GsonUtils.toJson(characterResult.getCharacters());
        String plotJson = GsonUtils.toJson(plotResult);
        String prompt = PromptConstant.DRAMA_SCRIPT_PROMPT
                .replace("{topic}", topic)
                .replace("{genre}", genre)
                .replace("{style}", style)
                .replace("{characters}", charactersJson)
                .replace("{plot}", plotJson);

        Consumer<String> streamHandler = StreamHandlerContext.get();
        String content = callLlmWithStreaming(prompt, streamHandler);

        DramaState.DramaScriptResult scriptResult = GsonUtils.fromJson(content, DramaState.DramaScriptResult.class);

        log.info("DramaScriptAgent 执行完成: 场景数={}", scriptResult.getScenes() != null ? scriptResult.getScenes().size() : 0);

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
                            streamHandler.accept(SseMessageTypeEnum.DRAMA_SCRIPT_STREAMING.getStreamingPrefix() + chunk);
                        }
                    }
                })
                .doOnError(error -> log.error("DramaScriptAgent 流式调用失败", error))
                .blockLast();
        return contentBuilder.toString();
    }
}
