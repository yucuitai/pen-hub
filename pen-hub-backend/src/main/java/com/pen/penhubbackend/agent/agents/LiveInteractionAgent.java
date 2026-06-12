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
 * 直播互动 Agent - 设计互动节点和福利环节
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LiveInteractionAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_LIVE_TYPE = "liveType";
    public static final String INPUT_DURATION = "duration";
    public static final String INPUT_OUTLINE = "outline";
    public static final String OUTPUT_INTERACTION_RESULT = "interactionResult";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String topic = state.value(INPUT_TOPIC).map(Object::toString).orElse("");
        String liveType = state.value(INPUT_LIVE_TYPE).map(Object::toString).orElse("ecommerce");
        String duration = state.value(INPUT_DURATION).map(Object::toString).orElse("2h");

        LiveScriptState.LiveOutlineResult outline = state.value(INPUT_OUTLINE)
                .map(v -> {
                    if (v instanceof LiveScriptState.LiveOutlineResult lor) return lor;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), LiveScriptState.LiveOutlineResult.class);
                })
                .orElseThrow(() -> new IllegalArgumentException("缺少直播大纲"));

        log.info("LiveInteractionAgent 开始执行: topic={}", topic);

        String segments = GsonUtils.toJson(outline.getSegments());
        String prompt = PromptConstant.LIVE_INTERACTION_PROMPT
                .replace("{topic}", topic)
                .replace("{liveType}", liveType)
                .replace("{duration}", duration)
                .replace("{segments}", segments);

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String content = response.getResult().getOutput().getText();

        LiveScriptState.LiveInteractionResult interactionResult = GsonUtils.fromJson(content, LiveScriptState.LiveInteractionResult.class);

        log.info("LiveInteractionAgent 执行完成: 互动环节数={}", interactionResult.getInteractions() != null ? interactionResult.getInteractions().size() : 0);

        return Map.of(OUTPUT_INTERACTION_RESULT, interactionResult);
    }
}
