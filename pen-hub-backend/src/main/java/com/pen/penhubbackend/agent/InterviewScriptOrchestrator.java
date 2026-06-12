package com.pen.penhubbackend.agent;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.pen.penhubbackend.agent.agents.*;
import com.pen.penhubbackend.agent.config.AgentConfig;
import com.pen.penhubbackend.agent.context.StreamHandlerContext;
import com.pen.penhubbackend.model.dto.script.InterviewState;
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
 * 访谈脚本智能体编排器
 * 使用 StateGraph 编排 4 个访谈 Agent
 *
 * 流程：提纲生成 → 对话生成 → 脚本审核 → 脚本合成
 *
 * @author pen-hub
 */
@Service
@Slf4j
public class InterviewScriptOrchestrator {

    @Resource
    private AgentConfig agentConfig;

    @Resource
    private InterviewOutlineAgent interviewOutlineAgent;

    @Resource
    private InterviewDialogueAgent interviewDialogueAgent;

    @Resource
    private InterviewReviewAgent interviewReviewAgent;

    @Resource
    private InterviewMergerAgent interviewMergerAgent;

    // region 状态键常量

    private static final String KEY_TASK_ID = "taskId";
    private static final String KEY_TOPIC = "topic";
    private static final String KEY_PLATFORM = "platform";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_STYLE = "style";
    private static final String KEY_OUTLINE = "outline";
    private static final String KEY_DIALOGUE_RESULT = "dialogueResult";
    private static final String KEY_REVIEW_RESULT = "reviewResult";
    private static final String KEY_SCRIPT_STRUCTURE = "scriptStructure";
    private static final String KEY_MARKDOWN_CONTENT = "markdownContent";
    private static final String KEY_PLAIN_TEXT_CONTENT = "plainTextContent";

    // endregion

    /**
     * 执行访谈脚本生成全流程
     *
     * @param state         访谈状态
     * @param streamHandler 流式输出处理器
     */
    public void executeInterviewScriptGeneration(InterviewState state, Consumer<String> streamHandler) {
        log.info("访谈编排器：开始生成访谈脚本, taskId={}", state.getTaskId());

        StreamHandlerContext.set(streamHandler);

        try {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put(KEY_TASK_ID, state.getTaskId());
            inputs.put(KEY_TOPIC, state.getTopic());
            inputs.put(KEY_PLATFORM, state.getPlatform());
            inputs.put(KEY_DURATION, state.getDuration());
            inputs.put(KEY_STYLE, state.getStyle());

            StateGraph graph = buildInterviewGraph();
            CompiledGraph compiledGraph = graph.compile();

            Optional<OverAllState> result = compiledGraph.invoke(inputs);

            if (result.isPresent()) {
                OverAllState finalState = result.get();

                InterviewState.InterviewOutlineResult outline = finalState.value(KEY_OUTLINE)
                        .map(v -> {
                            if (v instanceof InterviewState.InterviewOutlineResult ior) return ior;
                            return GsonUtils.fromJson(GsonUtils.toJson(v), InterviewState.InterviewOutlineResult.class);
                        })
                        .orElse(null);

                InterviewState.InterviewDialogueResult dialogueResult = finalState.value(KEY_DIALOGUE_RESULT)
                        .map(v -> {
                            if (v instanceof InterviewState.InterviewDialogueResult idr) return idr;
                            return GsonUtils.fromJson(GsonUtils.toJson(v), InterviewState.InterviewDialogueResult.class);
                        })
                        .orElse(null);

                InterviewState.InterviewReviewResult reviewResult = finalState.value(KEY_REVIEW_RESULT)
                        .map(v -> {
                            if (v instanceof InterviewState.InterviewReviewResult irr) return irr;
                            return GsonUtils.fromJson(GsonUtils.toJson(v), InterviewState.InterviewReviewResult.class);
                        })
                        .orElse(null);

                String scriptStructure = finalState.value(KEY_SCRIPT_STRUCTURE)
                        .map(Object::toString).orElse(null);
                String markdownContent = finalState.value(KEY_MARKDOWN_CONTENT)
                        .map(Object::toString).orElse(null);
                String plainTextContent = finalState.value(KEY_PLAIN_TEXT_CONTENT)
                        .map(Object::toString).orElse(null);

                // 更新状态并发送 SSE 消息
                if (outline != null) {
                    state.setOutline(outline);
                    streamHandler.accept(SseMessageTypeEnum.INTERVIEW_OUTLINE_COMPLETE.getValue());
                }

                if (dialogueResult != null) {
                    state.setDialogueResult(dialogueResult);
                    streamHandler.accept(SseMessageTypeEnum.INTERVIEW_DIALOGUE_COMPLETE.getValue());
                }

                if (reviewResult != null) {
                    state.setReviewResult(reviewResult);
                    streamHandler.accept(SseMessageTypeEnum.INTERVIEW_REVIEW_COMPLETE.getValue());
                }

                state.setScriptStructure(scriptStructure);
                state.setMarkdownContent(markdownContent);
                state.setPlainTextContent(plainTextContent);
                streamHandler.accept(SseMessageTypeEnum.INTERVIEW_MERGE_COMPLETE.getValue());

                log.info("访谈编排器：访谈脚本生成完成, taskId={}", state.getTaskId());
            } else {
                throw new RuntimeException("访谈脚本生成失败：执行结果为空");
            }
        } catch (Exception e) {
            log.error("访谈编排器：访谈脚本生成失败, taskId={}", state.getTaskId(), e);
            throw new RuntimeException("访谈脚本生成失败: " + e.getMessage(), e);
        } finally {
            StreamHandlerContext.clear();
        }
    }

    /**
     * 构建访谈脚本生成图：提纲 → 对话 → 审核 → 合成
     */
    private StateGraph buildInterviewGraph() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory();

        return new StateGraph(keyStrategyFactory)
                .addNode("outline_generator", node_async(interviewOutlineAgent))
                .addNode("dialogue_generator", node_async(interviewDialogueAgent))
                .addNode("reviewer", node_async(interviewReviewAgent))
                .addNode("merger", node_async(interviewMergerAgent))
                .addEdge(START, "outline_generator")
                .addEdge("outline_generator", "dialogue_generator")
                .addEdge("dialogue_generator", "reviewer")
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
            strategies.put(KEY_OUTLINE, new ReplaceStrategy());
            strategies.put(KEY_DIALOGUE_RESULT, new ReplaceStrategy());
            strategies.put(KEY_REVIEW_RESULT, new ReplaceStrategy());
            strategies.put(KEY_SCRIPT_STRUCTURE, new ReplaceStrategy());
            strategies.put(KEY_MARKDOWN_CONTENT, new ReplaceStrategy());
            strategies.put(KEY_PLAIN_TEXT_CONTENT, new ReplaceStrategy());
            return strategies;
        };
    }
}
