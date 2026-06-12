package com.pen.penhubbackend.agent;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.pen.penhubbackend.agent.agents.*;
import com.pen.penhubbackend.agent.config.AgentConfig;
import com.pen.penhubbackend.agent.context.StreamHandlerContext;
import com.pen.penhubbackend.model.dto.script.LiveScriptState;
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
 * 直播台本智能体编排器
 * 流程：大纲规划 → 话术生成 → 互动设计 → 应急话术 → 台本合成
 */
@Service
@Slf4j
public class LiveScriptOrchestrator {

    @Resource
    private AgentConfig agentConfig;

    @Resource
    private LiveOutlineAgent liveOutlineAgent;

    @Resource
    private LiveScriptAgent liveScriptAgent;

    @Resource
    private LiveInteractionAgent liveInteractionAgent;

    @Resource
    private LiveEmergencyAgent liveEmergencyAgent;

    @Resource
    private LiveMergerAgent liveMergerAgent;

    private static final String KEY_TASK_ID = "taskId";
    private static final String KEY_TOPIC = "topic";
    private static final String KEY_PLATFORM = "platform";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_LIVE_TYPE = "liveType";
    private static final String KEY_PRODUCT_INFO = "productInfo";
    private static final String KEY_PARTICIPANT_COUNT = "participantCount";
    private static final String KEY_OUTLINE = "outline";
    private static final String KEY_SCRIPT_RESULT = "scriptResult";
    private static final String KEY_INTERACTION_RESULT = "interactionResult";
    private static final String KEY_EMERGENCY_RESULT = "emergencyResult";
    private static final String KEY_SCRIPT_STRUCTURE = "scriptStructure";
    private static final String KEY_MARKDOWN_CONTENT = "markdownContent";

    /**
     * 执行直播台本生成全流程
     */
    public void executeLiveScriptGeneration(LiveScriptState state, Consumer<String> streamHandler) {
        log.info("直播台本编排器：开始生成直播台本, taskId={}", state.getTaskId());

        StreamHandlerContext.set(streamHandler);

        try {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put(KEY_TASK_ID, state.getTaskId());
            inputs.put(KEY_TOPIC, state.getTopic());
            inputs.put(KEY_PLATFORM, state.getPlatform());
            inputs.put(KEY_DURATION, state.getDuration());
            inputs.put(KEY_LIVE_TYPE, state.getLiveType());
            inputs.put(KEY_PRODUCT_INFO, state.getProductInfo() != null ? state.getProductInfo() : "");
            inputs.put(KEY_PARTICIPANT_COUNT, state.getParticipantCount() != null ? state.getParticipantCount() : "1");

            StateGraph graph = buildLiveScriptGraph();
            CompiledGraph compiledGraph = graph.compile();

            Optional<OverAllState> result = compiledGraph.invoke(inputs);

            if (result.isPresent()) {
                OverAllState finalState = result.get();

                LiveScriptState.LiveOutlineResult outline = finalState.value(KEY_OUTLINE)
                        .map(v -> {
                            if (v instanceof LiveScriptState.LiveOutlineResult lor) return lor;
                            return GsonUtils.fromJson(GsonUtils.toJson(v), LiveScriptState.LiveOutlineResult.class);
                        })
                        .orElse(null);

                LiveScriptState.LiveScriptResult scriptResult = finalState.value(KEY_SCRIPT_RESULT)
                        .map(v -> {
                            if (v instanceof LiveScriptState.LiveScriptResult lsr) return lsr;
                            return GsonUtils.fromJson(GsonUtils.toJson(v), LiveScriptState.LiveScriptResult.class);
                        })
                        .orElse(null);

                LiveScriptState.LiveInteractionResult interactionResult = finalState.value(KEY_INTERACTION_RESULT)
                        .map(v -> {
                            if (v instanceof LiveScriptState.LiveInteractionResult lir) return lir;
                            return GsonUtils.fromJson(GsonUtils.toJson(v), LiveScriptState.LiveInteractionResult.class);
                        })
                        .orElse(null);

                LiveScriptState.LiveEmergencyResult emergencyResult = finalState.value(KEY_EMERGENCY_RESULT)
                        .map(v -> {
                            if (v instanceof LiveScriptState.LiveEmergencyResult ler) return ler;
                            return GsonUtils.fromJson(GsonUtils.toJson(v), LiveScriptState.LiveEmergencyResult.class);
                        })
                        .orElse(null);

                String scriptStructure = finalState.value(KEY_SCRIPT_STRUCTURE).map(Object::toString).orElse(null);
                String markdownContent = finalState.value(KEY_MARKDOWN_CONTENT).map(Object::toString).orElse(null);

                if (outline != null) {
                    state.setOutline(outline);
                    streamHandler.accept(SseMessageTypeEnum.LIVE_OUTLINE_COMPLETE.getValue());
                }
                if (scriptResult != null) {
                    state.setScriptResult(scriptResult);
                    streamHandler.accept(SseMessageTypeEnum.LIVE_SCRIPT_COMPLETE.getValue());
                }
                if (interactionResult != null) {
                    state.setInteractionResult(interactionResult);
                    streamHandler.accept(SseMessageTypeEnum.LIVE_INTERACTION_COMPLETE.getValue());
                }
                if (emergencyResult != null) {
                    state.setEmergencyResult(emergencyResult);
                    streamHandler.accept(SseMessageTypeEnum.LIVE_EMERGENCY_COMPLETE.getValue());
                }

                state.setScriptStructure(scriptStructure);
                state.setMarkdownContent(markdownContent);
                streamHandler.accept(SseMessageTypeEnum.LIVE_MERGE_COMPLETE.getValue());

                log.info("直播台本编排器：直播台本生成完成, taskId={}", state.getTaskId());
            } else {
                throw new RuntimeException("直播台本生成失败：执行结果为空");
            }
        } catch (Exception e) {
            log.error("直播台本编排器：直播台本生成失败, taskId={}", state.getTaskId(), e);
            throw new RuntimeException("直播台本生成失败: " + e.getMessage(), e);
        } finally {
            StreamHandlerContext.clear();
        }
    }

    private StateGraph buildLiveScriptGraph() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory();

        return new StateGraph(keyStrategyFactory)
                .addNode("outline_generator", node_async(liveOutlineAgent))
                .addNode("script_generator", node_async(liveScriptAgent))
                .addNode("interaction_designer", node_async(liveInteractionAgent))
                .addNode("emergency_generator", node_async(liveEmergencyAgent))
                .addNode("merger", node_async(liveMergerAgent))
                .addEdge(START, "outline_generator")
                .addEdge("outline_generator", "script_generator")
                .addEdge("script_generator", "interaction_designer")
                .addEdge("interaction_designer", "emergency_generator")
                .addEdge("emergency_generator", "merger")
                .addEdge("merger", END);
    }

    private KeyStrategyFactory createKeyStrategyFactory() {
        return () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put(KEY_TASK_ID, new ReplaceStrategy());
            strategies.put(KEY_TOPIC, new ReplaceStrategy());
            strategies.put(KEY_PLATFORM, new ReplaceStrategy());
            strategies.put(KEY_DURATION, new ReplaceStrategy());
            strategies.put(KEY_LIVE_TYPE, new ReplaceStrategy());
            strategies.put(KEY_PRODUCT_INFO, new ReplaceStrategy());
            strategies.put(KEY_PARTICIPANT_COUNT, new ReplaceStrategy());
            strategies.put(KEY_OUTLINE, new ReplaceStrategy());
            strategies.put(KEY_SCRIPT_RESULT, new ReplaceStrategy());
            strategies.put(KEY_INTERACTION_RESULT, new ReplaceStrategy());
            strategies.put(KEY_EMERGENCY_RESULT, new ReplaceStrategy());
            strategies.put(KEY_SCRIPT_STRUCTURE, new ReplaceStrategy());
            strategies.put(KEY_MARKDOWN_CONTENT, new ReplaceStrategy());
            return strategies;
        };
    }
}
