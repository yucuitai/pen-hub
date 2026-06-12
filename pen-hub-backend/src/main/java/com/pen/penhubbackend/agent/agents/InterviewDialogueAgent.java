package com.pen.penhubbackend.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.pen.penhubbackend.agent.context.StreamHandlerContext;
import com.pen.penhubbackend.constant.PromptConstant;
import com.pen.penhubbackend.model.dto.script.InterviewState;
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
 * 访谈对话 Agent
 * 生成访谈对话内容
 *
 * @author pen-hub
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InterviewDialogueAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_PLATFORM = "platform";
    public static final String INPUT_STYLE = "style";
    public static final String INPUT_OUTLINE = "outline";
    public static final String OUTPUT_DIALOGUE_RESULT = "dialogueResult";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String topic = state.value(INPUT_TOPIC).map(Object::toString).orElse("");
        String platform = state.value(INPUT_PLATFORM).map(Object::toString).orElse("douyin");
        String style = state.value(INPUT_STYLE).map(Object::toString).orElse("formal");

        InterviewState.InterviewOutlineResult outline = state.value(INPUT_OUTLINE)
                .map(v -> {
                    if (v instanceof InterviewState.InterviewOutlineResult ior) return ior;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), InterviewState.InterviewOutlineResult.class);
                })
                .orElseThrow(() -> new IllegalArgumentException("缺少访谈提纲"));

        log.info("InterviewDialogueAgent 开始执行: topic={}", topic);

        String outlineJson = GsonUtils.toJson(outline.getSegments());
        String prompt = PromptConstant.INTERVIEW_DIALOGUE_PROMPT
                .replace("{topic}", topic)
                .replace("{platform}", platform)
                .replace("{style}", style)
                .replace("{outline}", outlineJson);

        Consumer<String> streamHandler = StreamHandlerContext.get();
        String content = callLlmWithStreaming(prompt, streamHandler);

        InterviewState.InterviewDialogueResult dialogueResult = GsonUtils.fromJson(content, InterviewState.InterviewDialogueResult.class);

        log.info("InterviewDialogueAgent 执行完成: 对话行数={}", dialogueResult.getDialogues() != null ? dialogueResult.getDialogues().size() : 0);

        return Map.of(OUTPUT_DIALOGUE_RESULT, dialogueResult);
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
                            streamHandler.accept(SseMessageTypeEnum.INTERVIEW_DIALOGUE_STREAMING.getStreamingPrefix() + chunk);
                        }
                    }
                })
                .doOnError(error -> log.error("InterviewDialogueAgent 流式调用失败", error))
                .blockLast();
        return contentBuilder.toString();
    }
}
