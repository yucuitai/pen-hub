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

import java.util.HashMap;
import java.util.Map;

/**
 * 直播台本合成 Agent - 合并为完整直播台本
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LiveMergerAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_PLATFORM = "platform";
    public static final String INPUT_DURATION = "duration";
    public static final String INPUT_LIVE_TYPE = "liveType";
    public static final String INPUT_SCRIPT_RESULT = "scriptResult";
    public static final String INPUT_INTERACTION_RESULT = "interactionResult";
    public static final String INPUT_EMERGENCY_RESULT = "emergencyResult";
    public static final String OUTPUT_SCRIPT_STRUCTURE = "scriptStructure";
    public static final String OUTPUT_MARKDOWN_CONTENT = "markdownContent";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String topic = state.value(INPUT_TOPIC).map(Object::toString).orElse("");
        String platform = state.value(INPUT_PLATFORM).map(Object::toString).orElse("douyin");
        String duration = state.value(INPUT_DURATION).map(Object::toString).orElse("2h");
        String liveType = state.value(INPUT_LIVE_TYPE).map(Object::toString).orElse("ecommerce");

        LiveScriptState.LiveScriptResult scriptResult = state.value(INPUT_SCRIPT_RESULT)
                .map(v -> {
                    if (v instanceof LiveScriptState.LiveScriptResult lsr) return lsr;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), LiveScriptState.LiveScriptResult.class);
                })
                .orElseThrow(() -> new IllegalArgumentException("缺少直播话术"));

        LiveScriptState.LiveInteractionResult interactionResult = state.value(INPUT_INTERACTION_RESULT)
                .map(v -> {
                    if (v instanceof LiveScriptState.LiveInteractionResult lir) return lir;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), LiveScriptState.LiveInteractionResult.class);
                })
                .orElse(null);

        LiveScriptState.LiveEmergencyResult emergencyResult = state.value(INPUT_EMERGENCY_RESULT)
                .map(v -> {
                    if (v instanceof LiveScriptState.LiveEmergencyResult ler) return ler;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), LiveScriptState.LiveEmergencyResult.class);
                })
                .orElse(null);

        log.info("LiveMergerAgent 开始执行: topic={}", topic);

        String segmentsScript = GsonUtils.toJson(scriptResult.getSegments());
        String interactions = interactionResult != null ? GsonUtils.toJson(interactionResult.getInteractions()) : "[]";
        String emergencyScripts = emergencyResult != null ? GsonUtils.toJson(emergencyResult.getEmergencyScripts()) : "[]";

        String prompt = PromptConstant.LIVE_MERGER_PROMPT
                .replace("{topic}", topic)
                .replace("{platform}", platform)
                .replace("{duration}", duration)
                .replace("{liveType}", liveType)
                .replace("{segmentsScript}", segmentsScript)
                .replace("{interactions}", interactions)
                .replace("{emergencyScripts}", emergencyScripts);

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String content = response.getResult().getOutput().getText();

        LiveScriptState.LiveMergeResult mergeResult = GsonUtils.fromJson(content, LiveScriptState.LiveMergeResult.class);

        log.info("LiveMergerAgent 执行完成: markdown长度={}", mergeResult.getMarkdownContent() != null ? mergeResult.getMarkdownContent().length() : 0);

        Map<String, Object> result = new HashMap<>();
        result.put(OUTPUT_SCRIPT_STRUCTURE, mergeResult.getScriptStructure());
        result.put(OUTPUT_MARKDOWN_CONTENT, mergeResult.getMarkdownContent());
        return result;
    }
}
