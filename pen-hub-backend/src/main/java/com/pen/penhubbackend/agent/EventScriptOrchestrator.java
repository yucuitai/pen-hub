package com.pen.penhubbackend.agent;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.pen.penhubbackend.agent.agents.*;
import com.pen.penhubbackend.agent.config.AgentConfig;
import com.pen.penhubbackend.agent.context.StreamHandlerContext;
import com.pen.penhubbackend.model.dto.script.EventScriptState;
import com.pen.penhubbackend.model.enums.SseMessageTypeEnum;
import com.pen.penhubbackend.utils.GsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 活动台本智能体编排器
 * 流程：大纲规划 → 话术生成 → 提词设计 → 台本合成
 */
@Service
@Slf4j
public class EventScriptOrchestrator {

    @Resource
    private AgentConfig agentConfig;

    @Resource
    private EventOutlineAgent eventOutlineAgent;

    @Resource
    private EventScriptAgent eventScriptAgent;

    @Resource
    private EventCueAgent eventCueAgent;

    @Resource
    private EventMergerAgent eventMergerAgent;

    private static final String KEY_TASK_ID = "taskId";
    private static final String KEY_TOPIC = "topic";
    private static final String KEY_PLATFORM = "platform";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_EVENT_TYPE = "eventType";
    private static final String KEY_PARTICIPANT_COUNT = "participantCount";
    private static final String KEY_OUTLINE = "outline";
    private static final String KEY_SCRIPT_RESULT = "scriptResult";
    private static final String KEY_CUE_RESULT = "cueResult";
    private static final String KEY_SCRIPT_STRUCTURE = "scriptStructure";
    private static final String KEY_MARKDOWN_CONTENT = "markdownContent";

    /**
     * 执行活动台本生成全流程
     */
    public void executeEventScriptGeneration(EventScriptState state, Consumer<String> streamHandler) {
        log.info("活动台本编排器：开始生成活动台本, taskId={}", state.getTaskId());

        StreamHandlerContext.set(streamHandler);

        try {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put(KEY_TASK_ID, state.getTaskId());
            inputs.put(KEY_TOPIC, state.getTopic());
            inputs.put(KEY_PLATFORM, state.getPlatform());
            inputs.put(KEY_DURATION, state.getDuration());
            inputs.put(KEY_EVENT_TYPE, state.getEventType());
            inputs.put(KEY_PARTICIPANT_COUNT, state.getParticipantCount() != null ? state.getParticipantCount() : "100");

            StateGraph graph = buildEventScriptGraph();
            CompiledGraph compiledGraph = graph.compile();

            Optional<OverAllState> result = compiledGraph.invoke(inputs);

            if (result.isPresent()) {
                OverAllState finalState = result.get();

                EventScriptState.EventOutlineResult outline = finalState.value(KEY_OUTLINE)
                        .map(v -> {
                            if (v instanceof EventScriptState.EventOutlineResult eor) return eor;
                            return GsonUtils.fromJson(GsonUtils.toJson(v), EventScriptState.EventOutlineResult.class);
                        })
                        .orElse(null);

                EventScriptState.EventScriptResult scriptResult = finalState.value(KEY_SCRIPT_RESULT)
                        .map(v -> {
                            if (v instanceof EventScriptState.EventScriptResult esr) return esr;
                            return GsonUtils.fromJson(GsonUtils.toJson(v), EventScriptState.EventScriptResult.class);
                        })
                        .orElse(null);

                EventScriptState.EventCueResult cueResult = finalState.value(KEY_CUE_RESULT)
                        .map(v -> {
                            if (v instanceof EventScriptState.EventCueResult ecr) return ecr;
                            return GsonUtils.fromJson(GsonUtils.toJson(v), EventScriptState.EventCueResult.class);
                        })
                        .orElse(null);

                String scriptStructure = finalState.value(KEY_SCRIPT_STRUCTURE).map(Object::toString).orElse(null);
                String markdownContent = finalState.value(KEY_MARKDOWN_CONTENT).map(Object::toString).orElse(null);

                if (outline != null) {
                    state.setOutline(outline);
                    streamHandler.accept(SseMessageTypeEnum.EVENT_OUTLINE_COMPLETE.getValue());
                }
                if (scriptResult != null) {
                    state.setScriptResult(scriptResult);
                    streamHandler.accept(SseMessageTypeEnum.EVENT_SCRIPT_COMPLETE.getValue());
                }
                if (cueResult != null) {
                    state.setCueResult(cueResult);
                    streamHandler.accept(SseMessageTypeEnum.EVENT_CUE_COMPLETE.getValue());
                }

                state.setScriptStructure(scriptStructure);
                state.setMarkdownContent(markdownContent);
                streamHandler.accept(SseMessageTypeEnum.EVENT_MERGE_COMPLETE.getValue());

                log.info("活动台本编排器：活动台本生成完成, taskId={}", state.getTaskId());
            } else {
                throw new RuntimeException("活动台本生成失败：执行结果为空");
            }
        } catch (Exception e) {
            log.error("活动台本编排器：活动台本生成失败, taskId={}", state.getTaskId(), e);
            throw new RuntimeException("活动台本生成失败: " + e.getMessage(), e);
        } finally {
            StreamHandlerContext.clear();
        }
    }

    private StateGraph buildEventScriptGraph() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory();

        return new StateGraph(keyStrategyFactory)
                .addNode("outline_generator", node_async(eventOutlineAgent))
                .addNode("script_generator", node_async(eventScriptAgent))
                .addNode("cue_designer", node_async(eventCueAgent))
                .addNode("merger", node_async(eventMergerAgent))
                .addEdge(START, "outline_generator")
                .addEdge("outline_generator", "script_generator")
                .addEdge("script_generator", "cue_designer")
                .addEdge("cue_designer", "merger")
                .addEdge("merger", END);
    }

    private KeyStrategyFactory createKeyStrategyFactory() {
        return () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put(KEY_TASK_ID, new ReplaceStrategy());
            strategies.put(KEY_TOPIC, new ReplaceStrategy());
            strategies.put(KEY_PLATFORM, new ReplaceStrategy());
            strategies.put(KEY_DURATION, new ReplaceStrategy());
            strategies.put(KEY_EVENT_TYPE, new ReplaceStrategy());
            strategies.put(KEY_PARTICIPANT_COUNT, new ReplaceStrategy());
            strategies.put(KEY_OUTLINE, new ReplaceStrategy());
            strategies.put(KEY_SCRIPT_RESULT, new ReplaceStrategy());
            strategies.put(KEY_CUE_RESULT, new ReplaceStrategy());
            strategies.put(KEY_SCRIPT_STRUCTURE, new ReplaceStrategy());
            strategies.put(KEY_MARKDOWN_CONTENT, new ReplaceStrategy());
            return strategies;
        };
    }
}
