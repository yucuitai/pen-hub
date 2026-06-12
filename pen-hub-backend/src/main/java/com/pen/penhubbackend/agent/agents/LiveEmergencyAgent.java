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
 * 直播应急 Agent - 生成应急话术库
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LiveEmergencyAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_LIVE_TYPE = "liveType";
    public static final String OUTPUT_EMERGENCY_RESULT = "emergencyResult";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String topic = state.value(INPUT_TOPIC).map(Object::toString).orElse("");
        String liveType = state.value(INPUT_LIVE_TYPE).map(Object::toString).orElse("ecommerce");

        log.info("LiveEmergencyAgent 开始执行: topic={}", topic);

        String prompt = PromptConstant.LIVE_EMERGENCY_PROMPT
                .replace("{topic}", topic)
                .replace("{liveType}", liveType);

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String content = response.getResult().getOutput().getText();

        LiveScriptState.LiveEmergencyResult emergencyResult = GsonUtils.fromJson(content, LiveScriptState.LiveEmergencyResult.class);

        log.info("LiveEmergencyAgent 执行完成: 应急场景数={}", emergencyResult.getEmergencyScripts() != null ? emergencyResult.getEmergencyScripts().size() : 0);

        return Map.of(OUTPUT_EMERGENCY_RESULT, emergencyResult);
    }
}
