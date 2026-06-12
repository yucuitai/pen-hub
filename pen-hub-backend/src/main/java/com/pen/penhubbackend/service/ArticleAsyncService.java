package com.pen.penhubbackend.service;

import com.google.gson.reflect.TypeToken;
import com.pen.penhubbackend.agent.ArticleAgentOrchestrator;
import com.pen.penhubbackend.agent.ScriptAgentOrchestrator;
import com.pen.penhubbackend.agent.LiveScriptOrchestrator;
import com.pen.penhubbackend.agent.EventScriptOrchestrator;
import com.pen.penhubbackend.agent.InterviewScriptOrchestrator;
import com.pen.penhubbackend.agent.DramaScriptOrchestrator;
import com.pen.penhubbackend.agent.config.AgentConfig;
import com.pen.penhubbackend.manager.SseEmitterManager;
import com.pen.penhubbackend.model.dto.article.ArticleState;
import com.pen.penhubbackend.model.entity.Article;
import com.pen.penhubbackend.model.enums.ArticlePhaseEnum;
import com.pen.penhubbackend.model.enums.ArticleStatusEnum;
import com.pen.penhubbackend.model.enums.SseMessageTypeEnum;
import com.pen.penhubbackend.utils.GsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章异步任务服务
 * 
 * 支持两种执行模式：
 * 1. 多智能体编排模式（通过 article.agent.orchestrator.enabled=true 启用）
 * 2. 原有模式（默认或 article.agent.orchestrator.enabled=false）
 *
 */
@Service
@Slf4j
public class ArticleAsyncService {

    @Resource
    private ArticleAgentService articleAgentService;

    @Resource
    private ArticleAgentOrchestrator articleAgentOrchestrator;

    @Resource
    private ScriptAgentOrchestrator scriptAgentOrchestrator;

    @Resource
    private LiveScriptOrchestrator liveScriptOrchestrator;

    @Resource
    private InterviewScriptOrchestrator interviewScriptOrchestrator;

    @Resource
    private EventScriptOrchestrator eventScriptOrchestrator;

    @Resource
    private DramaScriptOrchestrator dramaScriptOrchestrator;

    @Resource
    private AgentConfig agentConfig;

    @Resource
    private SseEmitterManager sseEmitterManager;

    @Resource
    private ArticleService articleService;

    /**
     * 阶段1：异步生成标题方案
     *
     * @param taskId 任务ID
     * @param topic  选题
     * @param style  文章风格（可为空）
     */
    @Async("articleExecutor")
    public void executePhase1(String taskId, String topic, String style) {
        //是否启用多智能体编排器
        boolean useOrchestrator = agentConfig.isOrchestratorEnabled();
        log.info("阶段1异步任务开始, taskId={}, topic={}, style={}, 使用多智能体编排={}", 
                taskId, topic, style, useOrchestrator);
        
        try {
            // 更新状态和阶段
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.PROCESSING, null);
            articleService.updatePhase(taskId, ArticlePhaseEnum.TITLE_GENERATING);
            
            // 创建状态对象
            ArticleState state = new ArticleState();
            state.setTaskId(taskId);
            state.setTopic(topic);
            state.setStyle(style);
            
            // 执行阶段1：生成标题方案（根据配置选择执行方式）
            if (useOrchestrator) {
                articleAgentOrchestrator.executePhase1_GenerateTitles(state, message -> {
                    handleAgentMessage(taskId, message, state);
                });
            } else {
                articleAgentService.executePhase1_GenerateTitles(state, message -> {
                    handleAgentMessage(taskId, message, state);
                });
            }
            
            // 保存标题方案到数据库
            articleService.saveTitleOptions(taskId, state.getTitleOptions());
            
            // 更新阶段为等待选择标题
            articleService.updatePhase(taskId, ArticlePhaseEnum.TITLE_SELECTING);
            
            // 推送标题方案生成完成消息
            Map<String, Object> data = new HashMap<>();
            data.put("titleOptions", state.getTitleOptions());
            sendSseMessage(taskId, SseMessageTypeEnum.TITLES_GENERATED, data);
            
            log.info("阶段1异步任务完成, taskId={}", taskId);
        } catch (Exception e) {
            log.error("阶段1异步任务失败, taskId={}", taskId, e);
            
            // 更新状态为失败
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.FAILED, e.getMessage());
            
            // 推送错误消息
            sendSseMessage(taskId, SseMessageTypeEnum.ERROR, Map.of("message", e.getMessage()));
            
            // 完成 SSE 连接
            sseEmitterManager.complete(taskId);
        }
    }

    /**
     * 阶段2：异步生成大纲（用户确认标题后调用）
     *
     * @param taskId 任务ID
     */
    @Async("articleExecutor")
    public void executePhase2(String taskId) {
        boolean useOrchestrator = agentConfig.isOrchestratorEnabled();
        log.info("阶段2异步任务开始, taskId={}, 使用多智能体编排={}", taskId, useOrchestrator);
        
        try {
            // 获取文章信息
            Article article = articleService.getByTaskId(taskId);
            if (article == null) {
                throw new RuntimeException("文章不存在");
            }
            
            // 创建状态对象
            ArticleState state = new ArticleState();
            state.setTaskId(taskId);
            state.setStyle(article.getStyle());
            state.setUserDescription(article.getUserDescription());
            
            // 设置标题
            ArticleState.TitleResult title = new ArticleState.TitleResult();
            title.setMainTitle(article.getMainTitle());
            title.setSubTitle(article.getSubTitle());
            state.setTitle(title);
            
            // 执行阶段2：生成大纲（根据配置选择执行方式）
            if (useOrchestrator) {
                articleAgentOrchestrator.executePhase2_GenerateOutline(state, message -> {
                    handleAgentMessage(taskId, message, state);
                });
            } else {
                articleAgentService.executePhase2_GenerateOutline(state, message -> {
                    handleAgentMessage(taskId, message, state);
                });
            }
            
            // 保存大纲到数据库
            Article articleToUpdate = articleService.getByTaskId(taskId);
            articleToUpdate.setOutline(GsonUtils.toJson(state.getOutline().getSections()));
            articleService.updateById(articleToUpdate);
            
            // 更新阶段为等待编辑大纲
            articleService.updatePhase(taskId, ArticlePhaseEnum.OUTLINE_EDITING);
            
            // 推送大纲生成完成消息
            Map<String, Object> data = new HashMap<>();
            data.put("outline", state.getOutline().getSections());
            sendSseMessage(taskId, SseMessageTypeEnum.OUTLINE_GENERATED, data);
            
            log.info("阶段2异步任务完成, taskId={}", taskId);
        } catch (Exception e) {
            log.error("阶段2异步任务失败, taskId={}", taskId, e);
            
            // 更新状态为失败
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.FAILED, e.getMessage());
            
            // 推送错误消息
            sendSseMessage(taskId, SseMessageTypeEnum.ERROR, Map.of("message", e.getMessage()));
            
            // 完成 SSE 连接
            sseEmitterManager.complete(taskId);
        }
    }

    /**
     * 阶段3：异步生成正文+配图（用户确认大纲后调用）
     *
     * @param taskId 任务ID
     */
    @Async("articleExecutor")
    public void executePhase3(String taskId) {
        boolean useOrchestrator = agentConfig.isOrchestratorEnabled();
        log.info("阶段3异步任务开始, taskId={}, 使用多智能体编排={}", taskId, useOrchestrator);

        // 更新阶段为生成正文中
        articleService.updatePhase(taskId, ArticlePhaseEnum.CONTENT_GENERATING);
        
        try {
            // 获取文章信息
            Article article = articleService.getByTaskId(taskId);
            if (article == null) {
                throw new RuntimeException("文章不存在");
            }
            
            // 创建状态对象
            ArticleState state = new ArticleState();
            state.setTaskId(taskId);
            state.setStyle(article.getStyle());
            
            // 从数据库获取允许的配图方式
            List<String> enabledMethods = null;
            if (article.getEnabledImageMethods() != null) {
                enabledMethods = GsonUtils.fromJson(
                        article.getEnabledImageMethods(),
                        new TypeToken<List<String>>(){}
                );
            }
            state.setEnabledImageMethods(enabledMethods);
            
            // 设置标题
            ArticleState.TitleResult title = new ArticleState.TitleResult();
            title.setMainTitle(article.getMainTitle());
            title.setSubTitle(article.getSubTitle());
            state.setTitle(title);
            
            // 设置大纲
            List<ArticleState.OutlineSection> outlineSections = GsonUtils.fromJson(
                    article.getOutline(),
                    new TypeToken<List<ArticleState.OutlineSection>>(){}
            );
            ArticleState.OutlineResult outlineResult = new ArticleState.OutlineResult();
            outlineResult.setSections(outlineSections);
            state.setOutline(outlineResult);
            
            // 执行阶段3：生成正文+配图（根据配置选择执行方式）
            // 多智能体编排模式支持配图并行生成
            if (useOrchestrator) {
                articleAgentOrchestrator.executePhase3_GenerateContent(state, message -> {
                    handleAgentMessage(taskId, message, state);
                });
            } else {
                articleAgentService.executePhase3_GenerateContent(state, message -> {
                    handleAgentMessage(taskId, message, state);
                });
            }
            
            // 保存完整文章到数据库
            articleService.saveArticleContent(taskId, state);
            
            // 更新状态为已完成
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.COMPLETED, null);
            
            // 推送完成消息
            sendSseMessage(taskId, SseMessageTypeEnum.ALL_COMPLETE, Map.of("taskId", taskId));
            
            // 完成 SSE 连接
            sseEmitterManager.complete(taskId);
            
            log.info("阶段3异步任务完成, taskId={}", taskId);
        } catch (Exception e) {
            log.error("阶段3异步任务失败, taskId={}", taskId, e);
            
            // 更新状态为失败
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.FAILED, e.getMessage());
            
            // 推送错误消息
            sendSseMessage(taskId, SseMessageTypeEnum.ERROR, Map.of("message", e.getMessage()));
            
            // 完成 SSE 连接
            sseEmitterManager.complete(taskId);
        }
    }

    /**
     * 处理智能体消息并推送
     */
    private void handleAgentMessage(String taskId, String message, ArticleState state) {
        Map<String, Object> data = buildMessageData(message, state);
        if (data != null) {
            sseEmitterManager.send(taskId, GsonUtils.toJson(data));
        }
    }

    /**
     * 构建消息数据
     * 
     * @param message 原始消息
     * @param state   文章状态
     * @return 消息数据，如果消息无效返回 null
     */
    private Map<String, Object> buildMessageData(String message, ArticleState state) {
        // 处理流式消息（带冒号分隔符）
        String streamingPrefix2 = SseMessageTypeEnum.AGENT2_STREAMING.getStreamingPrefix();
        String streamingPrefix3 = SseMessageTypeEnum.AGENT3_STREAMING.getStreamingPrefix();
        String imageCompletePrefix = SseMessageTypeEnum.IMAGE_COMPLETE.getStreamingPrefix();
        
        if (message.startsWith(streamingPrefix2)) {
            return buildStreamingData(SseMessageTypeEnum.AGENT2_STREAMING, message.substring(streamingPrefix2.length()));
        }
        
        if (message.startsWith(streamingPrefix3)) {
            return buildStreamingData(SseMessageTypeEnum.AGENT3_STREAMING, message.substring(streamingPrefix3.length()));
        }
        
        if (message.startsWith(imageCompletePrefix)) {
            String imageJson = message.substring(imageCompletePrefix.length());
            return buildImageCompleteData(imageJson);
        }
        
        // 处理完成消息（枚举值）
        return buildCompleteMessageData(message, state);
    }

    /**
     * 构建流式输出数据
     */
    private Map<String, Object> buildStreamingData(SseMessageTypeEnum type, String content) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", type.getValue());
        data.put("content", content);
        return data;
    }

    /**
     * 构建图片完成数据
     */
    private Map<String, Object> buildImageCompleteData(String imageJson) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", SseMessageTypeEnum.IMAGE_COMPLETE.getValue());
        data.put("image", GsonUtils.fromJson(imageJson, ArticleState.ImageResult.class));
        return data;
    }

    /**
     * 构建完成消息数据
     */
    private Map<String, Object> buildCompleteMessageData(String message, ArticleState state) {
        Map<String, Object> data = new HashMap<>();
        
        // 使用枚举值匹配
        if (SseMessageTypeEnum.AGENT1_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT1_COMPLETE.getValue());
            data.put("title", state.getTitle());
        } else if (SseMessageTypeEnum.AGENT2_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT2_COMPLETE.getValue());
            data.put("outline", state.getOutline().getSections());
        } else if (SseMessageTypeEnum.AGENT3_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT3_COMPLETE.getValue());
        } else if (SseMessageTypeEnum.REVIEWER_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.REVIEWER_COMPLETE.getValue());
            data.put("reviewResult", state.getReviewResult());
        } else if (SseMessageTypeEnum.AGENT4_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT4_COMPLETE.getValue());
            data.put("imageRequirements", state.getImageRequirements());
        } else if (SseMessageTypeEnum.AGENT5_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT5_COMPLETE.getValue());
            data.put("images", state.getImages());
        } else if (SseMessageTypeEnum.MERGE_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.MERGE_COMPLETE.getValue());
            data.put("fullContent", state.getFullContent());
        } else {
            return null;
        }
        
        return data;
    }

    /**
     * 发送 SSE 消息
     */
    private void sendSseMessage(String taskId, SseMessageTypeEnum type, Map<String, Object> additionalData) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", type.getValue());
        data.putAll(additionalData);
        sseEmitterManager.send(taskId, GsonUtils.toJson(data));
    }

    // region 脚本生成

    /**
     * 异步执行短视频脚本生成
     *
     * @param taskId   任务ID
     * @param topic    选题
     * @param platform 平台
     * @param duration 时长
     * @param style    风格
     */
    @Async("articleExecutor")
    public void executeScriptGeneration(String taskId, String topic, String platform, String duration, String style) {
        log.info("短视频脚本异步任务开始, taskId={}, topic={}, platform={}", taskId, topic, platform);

        try {
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.PROCESSING, null);
            articleService.updatePhase(taskId, ArticlePhaseEnum.SCRIPT_HOOK_GENERATING);

            com.pen.penhubbackend.model.dto.script.ScriptState state = com.pen.penhubbackend.model.dto.script.ScriptState.builder()
                    .taskId(taskId)
                    .topic(topic)
                    .platform(platform)
                    .duration(duration)
                    .style(style)
                    .build();

            scriptAgentOrchestrator.executeScriptGeneration(state, message -> {
                handleScriptMessage(taskId, message, state);
            });

            // 保存脚本内容到数据库
            Article article = articleService.getByTaskId(taskId);
            if (article != null) {
                article.setContent(state.getMarkdownContent());
                article.setFullContent(state.getMarkdownContent());
                article.setScriptStructure(state.getScriptStructure());
                articleService.updateById(article);
            }

            articleService.updateArticleStatus(taskId, ArticleStatusEnum.COMPLETED, null);
            sendSseMessage(taskId, SseMessageTypeEnum.ALL_COMPLETE, Map.of("taskId", taskId));
            sseEmitterManager.complete(taskId);

            log.info("短视频脚本异步任务完成, taskId={}", taskId);
        } catch (Exception e) {
            log.error("短视频脚本异步任务失败, taskId={}", taskId, e);
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.FAILED, e.getMessage());
            sendSseMessage(taskId, SseMessageTypeEnum.ERROR, Map.of("message", e.getMessage()));
            sseEmitterManager.complete(taskId);
        }
    }

    /**
     * 异步执行直播台本生成
     *
     * @param taskId          任务ID
     * @param topic           主题
     * @param platform        平台
     * @param duration        时长
     * @param liveType        直播类型
     * @param productInfo     产品信息
     * @param participantCount 参与人数
     */
    @Async("articleExecutor")
    public void executeLiveScriptGeneration(String taskId, String topic, String platform, String duration,
                                            String liveType, String productInfo, String participantCount) {
        log.info("直播台本异步任务开始, taskId={}, topic={}, liveType={}", taskId, topic, liveType);

        try {
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.PROCESSING, null);
            articleService.updatePhase(taskId, ArticlePhaseEnum.LIVE_OUTLINE_GENERATING);

            com.pen.penhubbackend.model.dto.script.LiveScriptState state = com.pen.penhubbackend.model.dto.script.LiveScriptState.builder()
                    .taskId(taskId)
                    .topic(topic)
                    .platform(platform)
                    .duration(duration)
                    .liveType(liveType)
                    .productInfo(productInfo)
                    .participantCount(participantCount)
                    .build();

            liveScriptOrchestrator.executeLiveScriptGeneration(state, message -> {
                handleLiveScriptMessage(taskId, message, state);
            });

            // 保存直播台本到数据库
            Article article = articleService.getByTaskId(taskId);
            if (article != null) {
                article.setContent(state.getMarkdownContent());
                article.setFullContent(state.getMarkdownContent());
                article.setScriptStructure(state.getScriptStructure());
                articleService.updateById(article);
            }

            articleService.updateArticleStatus(taskId, ArticleStatusEnum.COMPLETED, null);
            sendSseMessage(taskId, SseMessageTypeEnum.ALL_COMPLETE, Map.of("taskId", taskId));
            sseEmitterManager.complete(taskId);

            log.info("直播台本异步任务完成, taskId={}", taskId);
        } catch (Exception e) {
            log.error("直播台本异步任务失败, taskId={}", taskId, e);
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.FAILED, e.getMessage());
            sendSseMessage(taskId, SseMessageTypeEnum.ERROR, Map.of("message", e.getMessage()));
            sseEmitterManager.complete(taskId);
        }
    }

    /**
     * 处理脚本 Agent 消息
     */
    private void handleScriptMessage(String taskId, String message, com.pen.penhubbackend.model.dto.script.ScriptState state) {
        Map<String, Object> data = buildScriptMessageData(message, state);
        if (data != null) {
            sseEmitterManager.send(taskId, GsonUtils.toJson(data));
        }
    }

    /**
     * 处理直播台本 Agent 消息
     */
    private void handleLiveScriptMessage(String taskId, String message, com.pen.penhubbackend.model.dto.script.LiveScriptState state) {
        Map<String, Object> data = buildLiveScriptMessageData(message, state);
        if (data != null) {
            sseEmitterManager.send(taskId, GsonUtils.toJson(data));
        }
    }

    /**
     * 构建脚本消息数据
     */
    private Map<String, Object> buildScriptMessageData(String message, com.pen.penhubbackend.model.dto.script.ScriptState state) {
        Map<String, Object> data = new HashMap<>();

        // 处理流式消息
        String outlineStreamingPrefix = SseMessageTypeEnum.SCRIPT_OUTLINE_STREAMING.getStreamingPrefix();
        String contentStreamingPrefix = SseMessageTypeEnum.SCRIPT_CONTENT_STREAMING.getStreamingPrefix();

        if (message.startsWith(outlineStreamingPrefix)) {
            data.put("type", SseMessageTypeEnum.SCRIPT_OUTLINE_STREAMING.getValue());
            data.put("content", message.substring(outlineStreamingPrefix.length()));
            return data;
        }
        if (message.startsWith(contentStreamingPrefix)) {
            data.put("type", SseMessageTypeEnum.SCRIPT_CONTENT_STREAMING.getValue());
            data.put("content", message.substring(contentStreamingPrefix.length()));
            return data;
        }

        // 处理完成消息
        if (SseMessageTypeEnum.SCRIPT_HOOK_COMPLETE.getValue().equals(message)) {
            data.put("type", message);
            data.put("hookResult", state.getHookResult());
        } else if (SseMessageTypeEnum.SCRIPT_OUTLINE_COMPLETE.getValue().equals(message)) {
            data.put("type", message);
            data.put("outline", state.getOutline());
        } else if (SseMessageTypeEnum.SCRIPT_CONTENT_COMPLETE.getValue().equals(message)) {
            data.put("type", message);
            data.put("contentResult", state.getContentResult());
        } else if (SseMessageTypeEnum.SCRIPT_REVIEW_COMPLETE.getValue().equals(message)) {
            data.put("type", message);
            data.put("reviewResult", state.getReviewResult());
        } else if (SseMessageTypeEnum.SCRIPT_MERGE_COMPLETE.getValue().equals(message)) {
            data.put("type", message);
            data.put("scriptStructure", state.getScriptStructure());
            data.put("markdownContent", state.getMarkdownContent());
            data.put("plainTextContent", state.getPlainTextContent());
        } else {
            return null;
        }
        return data;
    }

    /**
     * 构建直播台本消息数据
     */
    private Map<String, Object> buildLiveScriptMessageData(String message, com.pen.penhubbackend.model.dto.script.LiveScriptState state) {
        Map<String, Object> data = new HashMap<>();

        // 处理流式消息
        String scriptStreamingPrefix = SseMessageTypeEnum.LIVE_SCRIPT_STREAMING.getStreamingPrefix();
        if (message.startsWith(scriptStreamingPrefix)) {
            data.put("type", SseMessageTypeEnum.LIVE_SCRIPT_STREAMING.getValue());
            data.put("content", message.substring(scriptStreamingPrefix.length()));
            return data;
        }

        // 处理完成消息
        if (SseMessageTypeEnum.LIVE_OUTLINE_COMPLETE.getValue().equals(message)) {
            data.put("type", message);
            data.put("outline", state.getOutline());
        } else if (SseMessageTypeEnum.LIVE_SCRIPT_COMPLETE.getValue().equals(message)) {
            data.put("type", message);
            data.put("scriptResult", state.getScriptResult());
        } else if (SseMessageTypeEnum.LIVE_INTERACTION_COMPLETE.getValue().equals(message)) {
            data.put("type", message);
            data.put("interactionResult", state.getInteractionResult());
        } else if (SseMessageTypeEnum.LIVE_EMERGENCY_COMPLETE.getValue().equals(message)) {
            data.put("type", message);
            data.put("emergencyResult", state.getEmergencyResult());
        } else if (SseMessageTypeEnum.LIVE_MERGE_COMPLETE.getValue().equals(message)) {
            data.put("type", message);
            data.put("scriptStructure", state.getScriptStructure());
            data.put("markdownContent", state.getMarkdownContent());
        } else {
            return null;
        }
        return data;
    }

    // endregion

    // region 访谈脚本生成

    @Async("articleExecutor")
    public void executeInterviewScriptGeneration(String taskId, String topic, String platform, String duration, String style) {
        log.info("访谈脚本异步任务开始, taskId={}, topic={}", taskId, topic);
        try {
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.PROCESSING, null);
            articleService.updatePhase(taskId, ArticlePhaseEnum.INTERVIEW_OUTLINE_GENERATING);

            com.pen.penhubbackend.model.dto.script.InterviewState state =
                    com.pen.penhubbackend.model.dto.script.InterviewState.builder()
                            .taskId(taskId).topic(topic).platform(platform).duration(duration).style(style).build();

            interviewScriptOrchestrator.executeInterviewScriptGeneration(state, message -> handleGenericScriptMessage(taskId, message));

            Article article = articleService.getByTaskId(taskId);
            if (article != null) {
                article.setContent(state.getMarkdownContent());
                article.setFullContent(state.getMarkdownContent());
                article.setScriptStructure(state.getScriptStructure());
                articleService.updateById(article);
            }

            articleService.updateArticleStatus(taskId, ArticleStatusEnum.COMPLETED, null);
            sendSseMessage(taskId, SseMessageTypeEnum.ALL_COMPLETE, Map.of("taskId", taskId));
            sseEmitterManager.complete(taskId);
        } catch (Exception e) {
            log.error("访谈脚本异步任务失败, taskId={}", taskId, e);
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.FAILED, e.getMessage());
            sendSseMessage(taskId, SseMessageTypeEnum.ERROR, Map.of("message", e.getMessage()));
            sseEmitterManager.complete(taskId);
        }
    }

    // endregion

    // region 活动台本生成

    @Async("articleExecutor")
    public void executeEventScriptGeneration(String taskId, String topic, String platform, String duration,
                                              String eventType, String participantCount) {
        log.info("活动台本异步任务开始, taskId={}, topic={}", taskId, topic);
        try {
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.PROCESSING, null);
            articleService.updatePhase(taskId, ArticlePhaseEnum.EVENT_OUTLINE_GENERATING);

            com.pen.penhubbackend.model.dto.script.EventScriptState state =
                    com.pen.penhubbackend.model.dto.script.EventScriptState.builder()
                            .taskId(taskId).topic(topic).platform(platform).duration(duration)
                            .eventType(eventType).participantCount(participantCount).build();

            eventScriptOrchestrator.executeEventScriptGeneration(state, message -> handleGenericScriptMessage(taskId, message));

            Article article = articleService.getByTaskId(taskId);
            if (article != null) {
                article.setContent(state.getMarkdownContent());
                article.setFullContent(state.getMarkdownContent());
                article.setScriptStructure(state.getScriptStructure());
                articleService.updateById(article);
            }

            articleService.updateArticleStatus(taskId, ArticleStatusEnum.COMPLETED, null);
            sendSseMessage(taskId, SseMessageTypeEnum.ALL_COMPLETE, Map.of("taskId", taskId));
            sseEmitterManager.complete(taskId);
        } catch (Exception e) {
            log.error("活动台本异步任务失败, taskId={}", taskId, e);
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.FAILED, e.getMessage());
            sendSseMessage(taskId, SseMessageTypeEnum.ERROR, Map.of("message", e.getMessage()));
            sseEmitterManager.complete(taskId);
        }
    }

    // endregion

    // region 剧本生成

    @Async("articleExecutor")
    public void executeDramaScriptGeneration(String taskId, String topic, String platform, String duration,
                                              String genre, String style) {
        log.info("剧本异步任务开始, taskId={}, topic={}", taskId, topic);
        try {
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.PROCESSING, null);
            articleService.updatePhase(taskId, ArticlePhaseEnum.DRAMA_CHARACTER_GENERATING);

            com.pen.penhubbackend.model.dto.drama.DramaState state =
                    com.pen.penhubbackend.model.dto.drama.DramaState.builder()
                            .taskId(taskId).topic(topic).platform(platform).duration(duration)
                            .genre(genre).style(style).build();

            dramaScriptOrchestrator.executeDramaScriptGeneration(state, message -> handleGenericScriptMessage(taskId, message));

            Article article = articleService.getByTaskId(taskId);
            if (article != null) {
                article.setContent(state.getMarkdownContent());
                article.setFullContent(state.getMarkdownContent());
                article.setScriptStructure(state.getScriptStructure());
                articleService.updateById(article);
            }

            articleService.updateArticleStatus(taskId, ArticleStatusEnum.COMPLETED, null);
            sendSseMessage(taskId, SseMessageTypeEnum.ALL_COMPLETE, Map.of("taskId", taskId));
            sseEmitterManager.complete(taskId);
        } catch (Exception e) {
            log.error("剧本异步任务失败, taskId={}", taskId, e);
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.FAILED, e.getMessage());
            sendSseMessage(taskId, SseMessageTypeEnum.ERROR, Map.of("message", e.getMessage()));
            sseEmitterManager.complete(taskId);
        }
    }

    // endregion

    /**
     * 通用脚本消息处理（转发 SSE 消息）
     */
    private void handleGenericScriptMessage(String taskId, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", message);
        sseEmitterManager.send(taskId, GsonUtils.toJson(data));
    }
}
