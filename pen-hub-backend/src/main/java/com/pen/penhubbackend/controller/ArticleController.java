package com.pen.penhubbackend.controller;

import com.mybatisflex.core.paginate.Page;
import com.pen.penhubbackend.annotation.RateLimit;
import com.pen.penhubbackend.common.BaseResponse;
import com.pen.penhubbackend.common.DeleteRequest;
import com.pen.penhubbackend.common.ResultUtils;
import com.pen.penhubbackend.exception.BusinessException;
import com.pen.penhubbackend.exception.ErrorCode;
import com.pen.penhubbackend.exception.ThrowUtils;
import com.pen.penhubbackend.manager.SseEmitterManager;
import com.pen.penhubbackend.model.dto.article.*;
import com.pen.penhubbackend.model.entity.Article;
import com.pen.penhubbackend.model.entity.User;
import com.pen.penhubbackend.model.enums.ArticleStyleEnum;
import com.pen.penhubbackend.model.enums.ContentTypeEnum;
import com.pen.penhubbackend.model.vo.AgentExecutionStats;
import com.pen.penhubbackend.model.vo.ArticleVO;
import com.pen.penhubbackend.service.AgentLogService;
import com.pen.penhubbackend.service.ArticleAsyncService;
import com.pen.penhubbackend.service.ArticleService;
import com.pen.penhubbackend.service.ExportService;
import com.pen.penhubbackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 文章接口
 *
 */
@RestController
@RequestMapping("/article")
@Slf4j
public class ArticleController {

    @Resource
    private ArticleService articleService;

    @Resource
    private ArticleAsyncService articleAsyncService;

    @Resource
    private SseEmitterManager sseEmitterManager;

    @Resource
    private UserService userService;

    @Resource
    private AgentLogService agentLogService;

    @Resource
    private ExportService exportService;

    /**
     * 创建文章任务（仅创建记录，不启动异步任务）
    */
    @RateLimit(maxRequests = 5, windowSeconds = 60)
    @PostMapping("/create")
    @Operation(summary = "创建文章任务")
    public BaseResponse<String> createArticle(@RequestBody ArticleCreateRequest request, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTopic() == null || request.getTopic().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "选题不能为空");
        // 校验风格参数（允许为空）
        ThrowUtils.throwIf(!ArticleStyleEnum.isValid(request.getStyle()),
                ErrorCode.PARAMS_ERROR, "无效的文章风格");

        User loginUser = userService.getLoginUser(httpServletRequest);

        // 检查并消耗配额 + 创建文章任务（在同一事务中）
        String taskId = articleService.createArticleTaskWithQuotaCheck(
                request.getTopic(),
                request.getStyle(),
                request.getEnabledImageMethods(),
                loginUser
        );

        // 如果是脚本类型，更新额外字段
        String contentType = request.getContentType();
        if (contentType != null && !contentType.isEmpty() && !"ARTICLE".equals(contentType)) {
            Article article = articleService.getByTaskId(taskId);
            if (article != null) {
                article.setContentType(contentType);
                article.setPlatform(request.getPlatform());
                article.setDuration(request.getDuration());
                articleService.updateById(article);
            }
        }

        return ResultUtils.success(taskId);
    }

    /**
     * 启动文章任务（前端建立 SSE 连接后调用）
     */
    @PostMapping("/start/{taskId}")
    @Operation(summary = "启动文章生成任务")
    public BaseResponse<Boolean> startArticle(@PathVariable String taskId, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);

        // 校验任务存在且属于当前用户
        articleService.getArticleDetail(taskId, loginUser);

        // 获取文章实体（包含 style 字段）
        Article article = articleService.getByTaskId(taskId);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "文章不存在");

        // 根据内容类型分发到不同的生成流程
        String contentType = article.getContentType();
        ContentTypeEnum typeEnum = ContentTypeEnum.getEnumByValue(contentType);

        if (typeEnum == ContentTypeEnum.SHORT_VIDEO_SCRIPT) {
            // 短视频脚本生成
            articleAsyncService.executeScriptGeneration(
                    taskId,
                    article.getTopic(),
                    article.getPlatform() != null ? article.getPlatform() : "douyin",
                    article.getDuration() != null ? article.getDuration() : "30s",
                    article.getStyle() != null ? article.getStyle() : "funny"
            );
        } else if (typeEnum == ContentTypeEnum.LIVE_SCRIPT) {
            // 直播台本生成
            articleAsyncService.executeLiveScriptGeneration(
                    taskId,
                    article.getTopic(),
                    article.getPlatform() != null ? article.getPlatform() : "douyin",
                    article.getDuration() != null ? article.getDuration() : "2h",
                    "ecommerce",
                    null,
                    "1"
            );
        } else if (typeEnum == ContentTypeEnum.INTERVIEW_SCRIPT) {
            // 访谈脚本生成
            articleAsyncService.executeInterviewScriptGeneration(
                    taskId,
                    article.getTopic(),
                    article.getPlatform() != null ? article.getPlatform() : "podcast",
                    article.getDuration() != null ? article.getDuration() : "30min",
                    article.getStyle() != null ? article.getStyle() : "professional"
            );
        } else if (typeEnum == ContentTypeEnum.EVENT_SCRIPT) {
            // 活动台本生成
            articleAsyncService.executeEventScriptGeneration(
                    taskId,
                    article.getTopic(),
                    article.getPlatform() != null ? article.getPlatform() : "offline",
                    article.getDuration() != null ? article.getDuration() : "2h",
                    "conference",
                    "1"
            );
        } else if (typeEnum == ContentTypeEnum.DRAMA_SCRIPT) {
            // 剧本生成
            articleAsyncService.executeDramaScriptGeneration(
                    taskId,
                    article.getTopic(),
                    article.getPlatform() != null ? article.getPlatform() : "short_drama",
                    article.getDuration() != null ? article.getDuration() : "10min",
                    "drama",
                    article.getStyle() != null ? article.getStyle() : "emotional"
            );
        } else {
            // 普通文章生成（原有流程）
            articleAsyncService.executePhase1(
                    taskId,
                    article.getTopic(),
                    article.getStyle()
            );
        }

        return ResultUtils.success(true);
    }

    /**
     * 切换文章收藏状态
     */
    @PostMapping("/favorite/{taskId}")
    @Operation(summary = "切换文章收藏状态")
    public BaseResponse<Boolean> toggleFavorite(@PathVariable String taskId, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean favorited = articleService.toggleFavorite(taskId, loginUser);
        return ResultUtils.success(favorited);
    }

    /**
     * 更新文章标签
     */
    @PostMapping("/tags/{taskId}")
    @Operation(summary = "更新文章标签")
    public BaseResponse<Boolean> updateTags(@PathVariable String taskId,
                                             @RequestBody List<String> tags,
                                             HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        articleService.updateTags(taskId, tags, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 获取用户未完成的文章（断点续传）
     */
    @GetMapping("/unfinished")
    @Operation(summary = "获取用户未完成的文章")
    public BaseResponse<ArticleVO> getUnfinishedArticle(HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        ArticleVO articleVO = articleService.getUnfinishedArticle(loginUser);
        return ResultUtils.success(articleVO);
    }

    /**
     * SSE 进度推送
     */
    @GetMapping("/progress/{taskId}")
    @Operation(summary = "获取文章生成进度(SSE)")
    public SseEmitter getProgress(@PathVariable String taskId, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");

        // 校验权限（内部会检查任务是否存在以及用户是否有权限访问）
        User loginUser = userService.getLoginUser(httpServletRequest);
        articleService.getArticleDetail(taskId, loginUser);

        // 创建 SSE Emitter
        SseEmitter emitter = sseEmitterManager.createEmitter(taskId);
        
        log.info("SSE 连接已建立, taskId={}", taskId);
        return emitter;
    }

    /**
     * 获取文章详情
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "获取文章详情")
    public BaseResponse<ArticleVO> getArticle(@PathVariable String taskId, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(), 
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);
        ArticleVO articleVO = articleService.getArticleDetail(taskId, loginUser);

        return ResultUtils.success(articleVO);
    }

    /**
     * 分页查询文章列表
     */
    @PostMapping("/list")
    @Operation(summary = "分页查询文章列表")
    public BaseResponse<Page<ArticleVO>> listArticle(@RequestBody ArticleQueryRequest request,
                                                       HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        User ss = loginUser;
        Page<ArticleVO> articleVOPage = articleService.listArticleByPage(request, loginUser);
        
        return ResultUtils.success(articleVOPage);
    }

    /**
     * 分页查询收藏文章
     */
    @PostMapping("/listFavorite")
    @Operation(summary = "分页查询收藏文章")
    public BaseResponse<Page<ArticleVO>> listFavoriteArticle(@RequestBody ArticleQueryRequest request,
                                                             HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        Page<ArticleVO> articleVOPage = articleService.listFavoriteArticleByPage(
                request.getPageNum(), request.getPageSize(), loginUser);
        return ResultUtils.success(articleVOPage);
    }

    /**
     * 删除文章
     */
    @PostMapping("/delete")
    @Operation(summary = "删除文章")
    public BaseResponse<Boolean> deleteArticle(@RequestBody DeleteRequest deleteRequest,
                                                 HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null, 
                ErrorCode.PARAMS_ERROR);
        
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean result = articleService.deleteArticle(deleteRequest.getId(), loginUser);
        
        return ResultUtils.success(result);
    }

    /**
     * 确认标题并输入补充描述
     */
    @PostMapping("/confirm-title")
    @Operation(summary = "确认标题并输入补充描述")
    public BaseResponse<Void> confirmTitle(@RequestBody ArticleConfirmTitleRequest request,
                                            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTaskId() == null || request.getTaskId().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        ThrowUtils.throwIf(request.getSelectedMainTitle() == null || request.getSelectedMainTitle().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "主标题不能为空");
        ThrowUtils.throwIf(request.getSelectedSubTitle() == null || request.getSelectedSubTitle().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "副标题不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);

        // 确认标题
        articleService.confirmTitle(
                request.getTaskId(),
                request.getSelectedMainTitle(),
                request.getSelectedSubTitle(),
                request.getUserDescription(),
                loginUser
        );

        // 异步执行阶段2：生成大纲
        articleAsyncService.executePhase2(request.getTaskId());

        return ResultUtils.success(null);
    }

    /**
     * 确认大纲
     */
    @PostMapping("/confirm-outline")
    @Operation(summary = "确认大纲")
    public BaseResponse<Void> confirmOutline(@RequestBody ArticleConfirmOutlineRequest request,
                                              HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTaskId() == null || request.getTaskId().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        ThrowUtils.throwIf(request.getOutline() == null || request.getOutline().isEmpty(),
                ErrorCode.PARAMS_ERROR, "大纲不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);

        // 确认大纲
        articleService.confirmOutline(
                request.getTaskId(),
                request.getOutline(),
                loginUser
        );

        // 异步执行阶段3：生成正文+配图
        articleAsyncService.executePhase3(request.getTaskId());

        return ResultUtils.success(null);
    }

    /**
     * AI 修改大纲
     */
    @PostMapping("/ai-modify-outline")
    @Operation(summary = "AI 修改大纲")
    public BaseResponse<List<ArticleState.OutlineSection>> aiModifyOutline(
            @RequestBody ArticleAiModifyOutlineRequest request,
            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTaskId() == null || request.getTaskId().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        ThrowUtils.throwIf(request.getModifySuggestion() == null || request.getModifySuggestion().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "修改建议不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);

        // AI 修改大纲
        List<ArticleState.OutlineSection> modifiedOutline = articleService.aiModifyOutline(
                request.getTaskId(),
                request.getModifySuggestion(),
                loginUser
        );

        return ResultUtils.success(modifiedOutline);
    }

    /**
     * 获取任务执行日志
     */
    @GetMapping("/execution-logs/{taskId}")
    @Operation(summary = "获取任务执行日志")
    public BaseResponse<AgentExecutionStats> getExecutionLogs(@PathVariable String taskId) {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        // 获取任务执行统计信息
        AgentExecutionStats stats = agentLogService.getExecutionStats(taskId);
        return ResultUtils.success(stats);
    }

    /**
     * 更新文章内容（二次编辑）
     */
    @PutMapping("/content/{taskId}")
    @Operation(summary = "更新文章内容")
    public BaseResponse<Boolean> updateContent(@PathVariable String taskId,
                                                @RequestBody ArticleUpdateContentRequest request,
                                                HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        ThrowUtils.throwIf(request == null || request.getContent() == null,
                ErrorCode.PARAMS_ERROR, "内容不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);
        articleService.updateContent(taskId, request.getContent(), loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 导出文章（PDF/Word）
     */
    @RateLimit(maxRequests = 10, windowSeconds = 60)
    @GetMapping("/{taskId}/export")
    @Operation(summary = "导出文章")
    public void exportArticle(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "pdf") String format,
            HttpServletRequest httpServletRequest,
            jakarta.servlet.http.HttpServletResponse response) throws Exception {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        ThrowUtils.throwIf(!"pdf".equals(format) && !"docx".equals(format),
                ErrorCode.PARAMS_ERROR, "不支持的导出格式，仅支持 pdf 或 docx");

        User loginUser = userService.getLoginUser(httpServletRequest);
        Article article = articleService.getByTaskId(taskId);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "文章不存在");

        // 权限校验：仅本人或管理员可导出
        if (!article.getUserId().equals(loginUser.getId()) && !"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        byte[] data;
        String filename;
        String contentType;

        // 清理标题中的控制字符，防止 HTTP Header 注入
        String safeTitle = article.getMainTitle().replaceAll("[\\r\\n\\x00-\\x1f]", "_");

        if ("pdf".equals(format)) {
            data = exportService.exportToPdf(article);
            filename = safeTitle + ".pdf";
            contentType = "application/pdf";
        } else {
            data = exportService.exportToDocx(article);
            filename = safeTitle + ".docx";
            contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }

        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" +
                java.net.URLEncoder.encode(filename, "UTF-8"));
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    // TODO Phase 5.7 - 多平台一键发布
    // 需要平台资质后实现：
    // 1. POST /article/{taskId}/publish/wechat  — 发布到微信公众号（需要公众号开发者资质）
    // 2. POST /article/{taskId}/publish/xiaohongshu — 发布到小红书（需要开放平台接入）
    // 3. GET  /article/{taskId}/publish/status   — 查询发布状态
    // 涉及文件：
    // - 新建 service/PublishService.java + impl/PublishServiceImpl.java
    // - 新建 controller/PublishController.java
    // - 新建 model/entity/PublishRecord.java（发布记录表）
    // - 新建适配器：adapter/WechatPublisherAdapter.java, adapter/XiaohongshuPublisherAdapter.java
}
