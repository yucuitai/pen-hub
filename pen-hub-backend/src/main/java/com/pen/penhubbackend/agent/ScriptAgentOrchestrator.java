package com.pen.penhubbackend.agent;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.pen.penhubbackend.agent.agents.*;
import com.pen.penhubbackend.agent.config.AgentConfig;
import com.pen.penhubbackend.agent.context.StreamHandlerContext;
import com.pen.penhubbackend.model.dto.script.ScriptState;
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
 * 短视频脚本智能体编排器
 * 使用 StateGraph 编排 5 个脚本 Agent
 *
 * 流程：Hook生成 → 分镜规划 → 内容生成 → 脚本审核 → 脚本合成
 *
 * @author pen-hub
 */
@Service
@Slf4j
public class ScriptAgentOrchestrator {

    @Resource
    private AgentConfig agentConfig;

    @Resource
    private ScriptHookAgent scriptHookAgent;

    @Resource
    private ScriptOutlineAgent scriptOutlineAgent;

    @Resource
    private ScriptContentAgent scriptContentAgent;

    @Resource
    private ScriptReviewAgent scriptReviewAgent;

    @Resource
    private ScriptMergerAgent scriptMergerAgent;

    // region 状态键常量

    private static final String KEY_TASK_ID = "taskId";
    private static final String KEY_TOPIC = "topic";
    private static final String KEY_PLATFORM = "platform";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_STYLE = "style";
    private static final String KEY_HOOK_RESULT = "hookResult";
    private static final String KEY_OUTLINE = "outline";
    private static final String KEY_CONTENT_RESULT = "contentResult";
    private static final String KEY_REVIEW_RESULT = "reviewResult";
    private static final String KEY_SCRIPT_STRUCTURE = "scriptStructure";
    private static final String KEY_MARKDOWN_CONTENT = "markdownContent";
    private static final String KEY_PLAIN_TEXT_CONTENT = "plainTextContent";

    // endregion

    /**
     * 执行短视频脚本生成全流程
     *
     * @param state         脚本状态
     * @param streamHandler 流式输出处理器
     */
    public void executeScriptGeneration(ScriptState state, Consumer<String> streamHandler) {
        log.info("脚本编排器：开始生成短视频脚本, taskId={}", state.getTaskId());

        StreamHandlerContext.set(streamHandler);

        try {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put(KEY_TASK_ID, state.getTaskId());
            inputs.put(KEY_TOPIC, state.getTopic());
            inputs.put(KEY_PLATFORM, state.getPlatform());
            inputs.put(KEY_DURATION, state.getDuration());
            inputs.put(KEY_STYLE, state.getStyle());

            StateGraph graph = buildScriptGraph();
            CompiledGraph compiledGraph = graph.compile();

            Optional<OverAllState> result = compiledGraph.invoke(inputs);

            if (result.isPresent()) {
                OverAllState finalState = result.get();

                ScriptState.HookResult hookResult = finalState.value(KEY_HOOK_RESULT)
                        .map(v -> {
                            if (v instanceof ScriptState.HookResult hr) return hr;
                            return GsonUtils.fromJson(GsonUtils.toJson(v), ScriptState.HookResult.class);
                        })
                        .orElse(null);

                ScriptState.ScriptOutlineResult outline = finalState.value(KEY_OUTLINE)
                        .map(v -> {
                            if (v instanceof ScriptState.ScriptOutlineResult sor) return sor;
                            return GsonUtils.fromJson(GsonUtils.toJson(v), ScriptState.ScriptOutlineResult.class);
                        })
                        .orElse(null);

                ScriptState.ScriptContentResult contentResult = finalState.value(KEY_CONTENT_RESULT)
                        .map(v -> {
                            if (v instanceof ScriptState.ScriptContentResult scr) return scr;
                            return GsonUtils.fromJson(GsonUtils.toJson(v), ScriptState.ScriptContentResult.class);
                        })
                        .orElse(null);

                ScriptState.ScriptReviewResult reviewResult = finalState.value(KEY_REVIEW_RESULT)
                        .map(v -> {
                            if (v instanceof ScriptState.ScriptReviewResult srr) return srr;
                            return GsonUtils.fromJson(GsonUtils.toJson(v), ScriptState.ScriptReviewResult.class);
                        })
                        .orElse(null);

                String scriptStructure = finalState.value(KEY_SCRIPT_STRUCTURE)
                        .map(Object::toString).orElse(null);
                String markdownContent = finalState.value(KEY_MARKDOWN_CONTENT)
                        .map(Object::toString).orElse(null);
                String plainTextContent = finalState.value(KEY_PLAIN_TEXT_CONTENT)
                        .map(Object::toString).orElse(null);

                // 更新状态并发送 SSE 消息
                state.setHookResult(hookResult);
                streamHandler.accept(SseMessageTypeEnum.SCRIPT_HOOK_COMPLETE.getValue());

                if (outline != null) {
                    state.setOutline(outline);
                    streamHandler.accept(SseMessageTypeEnum.SCRIPT_OUTLINE_COMPLETE.getValue());
                }

                if (contentResult != null) {
                    state.setContentResult(contentResult);
                    streamHandler.accept(SseMessageTypeEnum.SCRIPT_CONTENT_COMPLETE.getValue());
                }

                if (reviewResult != null) {
                    state.setReviewResult(reviewResult);
                    streamHandler.accept(SseMessageTypeEnum.SCRIPT_REVIEW_COMPLETE.getValue());
                }

                state.setScriptStructure(scriptStructure);
                state.setMarkdownContent(markdownContent);
                state.setPlainTextContent(plainTextContent);
                streamHandler.accept(SseMessageTypeEnum.SCRIPT_MERGE_COMPLETE.getValue());

                log.info("脚本编排器：短视频脚本生成完成, taskId={}", state.getTaskId());
            } else {
                throw new RuntimeException("脚本生成失败：执行结果为空");
            }
        } catch (Exception e) {
            log.error("脚本编排器：脚本生成失败, taskId={}", state.getTaskId(), e);
            throw new RuntimeException("脚本生成失败: " + e.getMessage(), e);
        } finally {
            StreamHandlerContext.clear();
        }
    }

    /**
     * 构建脚本生成图：Hook → 大纲 → 内容 → 审核 → 合成
     */
    private StateGraph buildScriptGraph() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory();

        return new StateGraph(keyStrategyFactory)
                .addNode("hook_generator", node_async(scriptHookAgent))
                .addNode("outline_generator", node_async(scriptOutlineAgent))
                .addNode("content_generator", node_async(scriptContentAgent))
                .addNode("reviewer", node_async(scriptReviewAgent))
                .addNode("merger", node_async(scriptMergerAgent))
                .addEdge(START, "hook_generator")
                .addEdge("hook_generator", "outline_generator")
                .addEdge("outline_generator", "content_generator")
                .addEdge("content_generator", "reviewer")
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
            strategies.put(KEY_STYLE, new ReplaceStrategy());
            strategies.put(KEY_HOOK_RESULT, new ReplaceStrategy());
            strategies.put(KEY_OUTLINE, new ReplaceStrategy());
            strategies.put(KEY_CONTENT_RESULT, new ReplaceStrategy());
            strategies.put(KEY_REVIEW_RESULT, new ReplaceStrategy());
            strategies.put(KEY_SCRIPT_STRUCTURE, new ReplaceStrategy());
            strategies.put(KEY_MARKDOWN_CONTENT, new ReplaceStrategy());
            strategies.put(KEY_PLAIN_TEXT_CONTENT, new ReplaceStrategy());
            return strategies;
        };
    }
}
