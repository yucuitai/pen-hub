package com.pen.penhubbackend.agent;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.pen.penhubbackend.agent.agents.*;
import com.pen.penhubbackend.agent.config.AgentConfig;
import com.pen.penhubbackend.agent.context.StreamHandlerContext;
import com.pen.penhubbackend.model.dto.drama.DramaState;
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
 * 剧本智能体编排器
 * 使用 StateGraph 编排 5 个剧本 Agent
 *
 * 流程：角色生成 → 剧情大纲 → 台词场景 → 剧本审核 → 剧本合成
 *
 * @author pen-hub
 */
@Service
@Slf4j
public class DramaScriptOrchestrator {

    @Resource
    private AgentConfig agentConfig;

    @Resource
    private DramaCharacterAgent dramaCharacterAgent;

    @Resource
    private DramaPlotAgent dramaPlotAgent;

    @Resource
    private DramaScriptAgent dramaScriptAgent;

    @Resource
    private DramaReviewAgent dramaReviewAgent;

    @Resource
    private DramaMergerAgent dramaMergerAgent;

    // region 状态键常量

    private static final String KEY_TASK_ID = "taskId";
    private static final String KEY_TOPIC = "topic";
    private static final String KEY_PLATFORM = "platform";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_GENRE = "genre";
    private static final String KEY_STYLE = "style";
    private static final String KEY_CHARACTER_RESULT = "characterResult";
    private static final String KEY_PLOT_RESULT = "plotResult";
    private static final String KEY_SCRIPT_RESULT = "scriptResult";
    private static final String KEY_REVIEW_RESULT = "reviewResult";
    private static final String KEY_SCRIPT_STRUCTURE = "scriptStructure";
    private static final String KEY_MARKDOWN_CONTENT = "markdownContent";
    private static final String KEY_PLAIN_TEXT_CONTENT = "plainTextContent";

    // endregion

    /**
     * 执行剧本生成全流程
     *
     * @param state         剧本状态
     * @param streamHandler 流式输出处理器
     */
    public void executeDramaScriptGeneration(DramaState state, Consumer<String> streamHandler) {
        log.info("剧本编排器：开始生成剧本, taskId={}", state.getTaskId());

        StreamHandlerContext.set(streamHandler);

        try {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put(KEY_TASK_ID, state.getTaskId());
            inputs.put(KEY_TOPIC, state.getTopic());
            inputs.put(KEY_PLATFORM, state.getPlatform());
            inputs.put(KEY_DURATION, state.getDuration());
            inputs.put(KEY_GENRE, state.getGenre());
            inputs.put(KEY_STYLE, state.getStyle());

            StateGraph graph = buildDramaGraph();
            CompiledGraph compiledGraph = graph.compile();

            Optional<OverAllState> result = compiledGraph.invoke(inputs);

            if (result.isPresent()) {
                OverAllState finalState = result.get();

                DramaState.DramaCharacterResult characterResult = finalState.value(KEY_CHARACTER_RESULT)
                        .map(v -> {
                            if (v instanceof DramaState.DramaCharacterResult dcr) return dcr;
                            return GsonUtils.fromJson(GsonUtils.toJson(v), DramaState.DramaCharacterResult.class);
                        })
                        .orElse(null);

                DramaState.DramaPlotResult plotResult = finalState.value(KEY_PLOT_RESULT)
                        .map(v -> {
                            if (v instanceof DramaState.DramaPlotResult dpr) return dpr;
                            return GsonUtils.fromJson(GsonUtils.toJson(v), DramaState.DramaPlotResult.class);
                        })
                        .orElse(null);

                DramaState.DramaScriptResult scriptResult = finalState.value(KEY_SCRIPT_RESULT)
                        .map(v -> {
                            if (v instanceof DramaState.DramaScriptResult dsr) return dsr;
                            return GsonUtils.fromJson(GsonUtils.toJson(v), DramaState.DramaScriptResult.class);
                        })
                        .orElse(null);

                DramaState.DramaReviewResult reviewResult = finalState.value(KEY_REVIEW_RESULT)
                        .map(v -> {
                            if (v instanceof DramaState.DramaReviewResult drr) return drr;
                            return GsonUtils.fromJson(GsonUtils.toJson(v), DramaState.DramaReviewResult.class);
                        })
                        .orElse(null);

                String scriptStructure = finalState.value(KEY_SCRIPT_STRUCTURE)
                        .map(Object::toString).orElse(null);
                String markdownContent = finalState.value(KEY_MARKDOWN_CONTENT)
                        .map(Object::toString).orElse(null);
                String plainTextContent = finalState.value(KEY_PLAIN_TEXT_CONTENT)
                        .map(Object::toString).orElse(null);

                // 更新状态并发送 SSE 消息
                state.setCharacterResult(characterResult);
                streamHandler.accept(SseMessageTypeEnum.DRAMA_CHARACTER_COMPLETE.getValue());

                if (plotResult != null) {
                    state.setPlotResult(plotResult);
                    streamHandler.accept(SseMessageTypeEnum.DRAMA_PLOT_COMPLETE.getValue());
                }

                if (scriptResult != null) {
                    state.setScriptResult(scriptResult);
                    streamHandler.accept(SseMessageTypeEnum.DRAMA_SCRIPT_COMPLETE.getValue());
                }

                if (reviewResult != null) {
                    state.setReviewResult(reviewResult);
                    streamHandler.accept(SseMessageTypeEnum.DRAMA_REVIEW_COMPLETE.getValue());
                }

                state.setScriptStructure(scriptStructure);
                state.setMarkdownContent(markdownContent);
                state.setPlainTextContent(plainTextContent);
                streamHandler.accept(SseMessageTypeEnum.DRAMA_MERGE_COMPLETE.getValue());

                log.info("剧本编排器：剧本生成完成, taskId={}", state.getTaskId());
            } else {
                throw new RuntimeException("剧本生成失败：执行结果为空");
            }
        } catch (Exception e) {
            log.error("剧本编排器：剧本生成失败, taskId={}", state.getTaskId(), e);
            throw new RuntimeException("剧本生成失败: " + e.getMessage(), e);
        } finally {
            StreamHandlerContext.clear();
        }
    }

    /**
     * 构建剧本生成图：角色 → 剧情 → 台词 → 审核 → 合成
     */
    private StateGraph buildDramaGraph() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory();

        return new StateGraph(keyStrategyFactory)
                .addNode("character_generator", node_async(dramaCharacterAgent))
                .addNode("plot_generator", node_async(dramaPlotAgent))
                .addNode("script_generator", node_async(dramaScriptAgent))
                .addNode("reviewer", node_async(dramaReviewAgent))
                .addNode("merger", node_async(dramaMergerAgent))
                .addEdge(START, "character_generator")
                .addEdge("character_generator", "plot_generator")
                .addEdge("plot_generator", "script_generator")
                .addEdge("script_generator", "reviewer")
                .addEdge("reviewer", "merger")
                .addEdge("merger", END);
    }

    private KeyStrategyFactory createKeyStrategyFactory() {
        return () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put(KEY_TASK_ID, new ReplaceStrategy());
            strategies.put(KEY_TOPIC, new ReplaceStrategy());
            strategies.put(KEY_PLATFORM, new ReplaceStrategy());
            strategies.put(KEY_DURATION, new ReplaceStrategy());
            strategies.put(KEY_GENRE, new ReplaceStrategy());
            strategies.put(KEY_STYLE, new ReplaceStrategy());
            strategies.put(KEY_CHARACTER_RESULT, new ReplaceStrategy());
            strategies.put(KEY_PLOT_RESULT, new ReplaceStrategy());
            strategies.put(KEY_SCRIPT_RESULT, new ReplaceStrategy());
            strategies.put(KEY_REVIEW_RESULT, new ReplaceStrategy());
            strategies.put(KEY_SCRIPT_STRUCTURE, new ReplaceStrategy());
            strategies.put(KEY_MARKDOWN_CONTENT, new ReplaceStrategy());
            strategies.put(KEY_PLAIN_TEXT_CONTENT, new ReplaceStrategy());
            return strategies;
        };
    }
}
