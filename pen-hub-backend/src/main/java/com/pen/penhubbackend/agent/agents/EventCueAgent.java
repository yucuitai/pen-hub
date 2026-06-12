package com.pen.penhubbackend.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.pen.penhubbackend.constant.PromptConstant;
import com.pen.penhubbackend.model.dto.script.EventScriptState;
import com.pen.penhubbackend.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 活动提词 Agent - 设计提词节点和提示内容
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EventCueAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_EVENT_TYPE = "eventType";
    public static final String INPUT_DURATION = "duration";
    public static final String INPUT_SEGMENTS = "segments";
    public static final String OUTPUT_CUE_RESULT = "cueResult";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String topic = state.value(INPUT_TOPIC).map(Object::toString).orElse("");
        String eventType = state.value(INPUT_EVENT_TYPE).map(Object::toString).orElse("conference");
        String duration = state.value(INPUT_DURATION).map(Object::toString).orElse("2h");

        EventScriptState.EventOutlineResult outline = state.value("outline")
                .map(v -> {
                    if (v instanceof EventScriptState.EventOutlineResult eor) return eor;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), EventScriptState.EventOutlineResult.class);
                })
                .orElseThrow(() -> new IllegalArgumentException("缺少活动大纲"));

        log.info("EventCueAgent 开始执行: topic={}", topic);

        String segments = GsonUtils.toJson(outline.getSegments());
        String prompt = PromptConstant.EVENT_CUE_PROMPT
                .replace("{topic}", topic)
                .replace("{eventType}", eventType)
                .replace("{duration}", duration)
                .replace("{segments}", segments);

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String content = response.getResult().getOutput().getText();

        EventScriptState.EventCueResult cueResult = GsonUtils.fromJson(content, EventScriptState.EventCueResult.class);

        log.info("EventCueAgent 执行完成: 提词环节数={}", cueResult.getCues() != null ? cueResult.getCues().size() : 0);

        return Map.of(OUTPUT_CUE_RESULT, cueResult);
    }
}
