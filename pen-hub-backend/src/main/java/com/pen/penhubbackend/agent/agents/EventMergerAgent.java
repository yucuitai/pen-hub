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

import java.util.HashMap;
import java.util.Map;

/**
 * 活动台本合成 Agent - 合并为完整活动台本
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EventMergerAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_PLATFORM = "platform";
    public static final String INPUT_DURATION = "duration";
    public static final String INPUT_EVENT_TYPE = "eventType";
    public static final String INPUT_SCRIPT_RESULT = "scriptResult";
    public static final String INPUT_CUE_RESULT = "cueResult";
    public static final String OUTPUT_SCRIPT_STRUCTURE = "scriptStructure";
    public static final String OUTPUT_MARKDOWN_CONTENT = "markdownContent";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String topic = state.value(INPUT_TOPIC).map(Object::toString).orElse("");
        String platform = state.value(INPUT_PLATFORM).map(Object::toString).orElse("线下");
        String duration = state.value(INPUT_DURATION).map(Object::toString).orElse("2h");
        String eventType = state.value(INPUT_EVENT_TYPE).map(Object::toString).orElse("conference");

        EventScriptState.EventScriptResult scriptResult = state.value(INPUT_SCRIPT_RESULT)
                .map(v -> {
                    if (v instanceof EventScriptState.EventScriptResult esr) return esr;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), EventScriptState.EventScriptResult.class);
                })
                .orElseThrow(() -> new IllegalArgumentException("缺少环节话术"));

        EventScriptState.EventCueResult cueResult = state.value(INPUT_CUE_RESULT)
                .map(v -> {
                    if (v instanceof EventScriptState.EventCueResult ecr) return ecr;
                    return GsonUtils.fromJson(GsonUtils.toJson(v), EventScriptState.EventCueResult.class);
                })
                .orElse(null);

        log.info("EventMergerAgent 开始执行: topic={}", topic);

        String segmentsScript = GsonUtils.toJson(scriptResult.getSegments());
        String cues = cueResult != null ? GsonUtils.toJson(cueResult.getCues()) : "[]";

        String prompt = PromptConstant.EVENT_MERGER_PROMPT
                .replace("{topic}", topic)
                .replace("{platform}", platform)
                .replace("{duration}", duration)
                .replace("{eventType}", eventType)
                .replace("{segmentsScript}", segmentsScript)
                .replace("{cues}", cues);

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String content = response.getResult().getOutput().getText();

        EventScriptState.EventMergeResult mergeResult = GsonUtils.fromJson(content, EventScriptState.EventMergeResult.class);

        log.info("EventMergerAgent 执行完成: markdown长度={}", mergeResult.getMarkdownContent() != null ? mergeResult.getMarkdownContent().length() : 0);

        Map<String, Object> result = new HashMap<>();
        result.put(OUTPUT_SCRIPT_STRUCTURE, mergeResult.getScriptStructure());
        result.put(OUTPUT_MARKDOWN_CONTENT, mergeResult.getMarkdownContent());
        return result;
    }
}
