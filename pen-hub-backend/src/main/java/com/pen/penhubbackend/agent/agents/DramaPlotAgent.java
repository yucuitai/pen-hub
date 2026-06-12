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
 * 剧情大纲 Agent
 * 根据角色设定规划剧情结构和幕次
 *
 * @author pen-hub
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DramaPlotAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_GENRE = "genre";
    public static final String INPUT_STYLE = "style";
    public static final String INPUT_CHARACTER_RESULT = "characterResult";
    public static final String OUTPUT_PLOT_RESULT = "plotResult";

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

        log.info("DramaPlotAgent 开始执行: topic={}, genre={}", topic, genre);

        String charactersJson = GsonUtils.toJson(characterResult.getCharacters());
        String prompt = PromptConstant.DRAMA_PLOT_PROMPT
                .replace("{topic}", topic)
                .replace("{genre}", genre)
                .replace("{style}", style)
                .replace("{characters}", charactersJson);

        Consumer<String> streamHandler = StreamHandlerContext.get();
        String content = callLlmWithStreaming(prompt, streamHandler);

        DramaState.DramaPlotResult plotResult = GsonUtils.fromJson(content, DramaState.DramaPlotResult.class);

        log.info("DramaPlotAgent 执行完成: 幂数={}", plotResult.getActs() != null ? plotResult.getActs().size() : 0);

        return Map.of(OUTPUT_PLOT_RESULT, plotResult);
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
                            streamHandler.accept(SseMessageTypeEnum.DRAMA_PLOT_COMPLETE.getStreamingPrefix() + chunk);
                        }
                    }
                })
                .doOnError(error -> log.error("DramaPlotAgent 流式调用失败", error))
                .blockLast();
        return contentBuilder.toString();
    }
}
