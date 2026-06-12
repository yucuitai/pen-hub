package com.pen.penhubbackend.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.pen.penhubbackend.constant.PromptConstant;
import com.pen.penhubbackend.model.dto.script.LiveScriptState;
import com.pen.penhubbackend.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 直播大纲 Agent - 规划直播流程和时间节点
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LiveOutlineAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_PLATFORM = "platform";
    public static final String INPUT_DURATION = "duration";
    public static final String INPUT_LIVE_TYPE = "liveType";
    public static final String INPUT_PRODUCT_INFO = "productInfo";
    public static final String INPUT_PARTICIPANT_COUNT = "participantCount";
    public static final String OUTPUT_OUTLINE = "outline";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String topic = state.value(INPUT_TOPIC).map(Object::toString).orElse("");
        String platform = state.value(INPUT_PLATFORM).map(Object::toString).orElse("douyin");
        String duration = state.value(INPUT_DURATION).map(Object::toString).orElse("2h");
        String liveType = state.value(INPUT_LIVE_TYPE).map(Object::toString).orElse("ecommerce");
        String productInfo = state.value(INPUT_PRODUCT_INFO).map(Object::toString).orElse("");
        String participantCount = state.value(INPUT_PARTICIPANT_COUNT).map(Object::toString).orElse("1");

        log.info("LiveOutlineAgent 开始执行: topic={}, liveType={}", topic, liveType);

        String prompt = PromptConstant.LIVE_OUTLINE_PROMPT
                .replace("{topic}", topic)
                .replace("{platform}", platform)
                .replace("{duration}", duration)
                .replace("{liveType}", liveType)
                .replace("{productInfo}", productInfo.isEmpty() ? "无" : productInfo)
                .replace("{participantCount}", participantCount);

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String content = response.getResult().getOutput().getText();

        LiveScriptState.LiveOutlineResult outline = GsonUtils.fromJson(content, LiveScriptState.LiveOutlineResult.class);

        log.info("LiveOutlineAgent 执行完成: 环节数={}", outline.getSegments() != null ? outline.getSegments().size() : 0);

        return Map.of(OUTPUT_OUTLINE, outline);
    }
}
