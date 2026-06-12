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
 * 活动大纲 Agent - 规划活动流程和时间节点
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EventOutlineAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_PLATFORM = "platform";
    public static final String INPUT_DURATION = "duration";
    public static final String INPUT_EVENT_TYPE = "eventType";
    public static final String INPUT_PARTICIPANT_COUNT = "participantCount";
    public static final String OUTPUT_OUTLINE = "outline";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String topic = state.value(INPUT_TOPIC).map(Object::toString).orElse("");
        String platform = state.value(INPUT_PLATFORM).map(Object::toString).orElse("线下");
        String duration = state.value(INPUT_DURATION).map(Object::toString).orElse("2h");
        String eventType = state.value(INPUT_EVENT_TYPE).map(Object::toString).orElse("conference");
        String participantCount = state.value(INPUT_PARTICIPANT_COUNT).map(Object::toString).orElse("100");

        log.info("EventOutlineAgent 开始执行: topic={}, eventType={}", topic, eventType);

        String prompt = PromptConstant.EVENT_OUTLINE_PROMPT
                .replace("{topic}", topic)
                .replace("{platform}", platform)
                .replace("{duration}", duration)
                .replace("{eventType}", eventType)
                .replace("{participantCount}", participantCount);

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String content = response.getResult().getOutput().getText();

        EventScriptState.EventOutlineResult outline = GsonUtils.fromJson(content, EventScriptState.EventOutlineResult.class);

        log.info("EventOutlineAgent 执行完成: 环节数={}", outline.getSegments() != null ? outline.getSegments().size() : 0);

        return Map.of(OUTPUT_OUTLINE, outline);
    }
}
