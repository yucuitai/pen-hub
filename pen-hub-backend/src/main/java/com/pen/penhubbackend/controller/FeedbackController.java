package com.pen.penhubbackend.controller;

import com.pen.penhubbackend.annotation.RateLimit;
import com.pen.penhubbackend.common.BaseResponse;
import com.pen.penhubbackend.common.ResultUtils;
import com.pen.penhubbackend.exception.ErrorCode;
import com.pen.penhubbackend.exception.ThrowUtils;
import com.pen.penhubbackend.model.entity.ArticleFeedback;
import com.pen.penhubbackend.model.entity.User;
import com.pen.penhubbackend.service.FeedbackService;
import com.pen.penhubbackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 文章反馈接口
 *
 * @author pen-hub
 */
@RestController
@RequestMapping("/feedback")
@Slf4j
@Tag(name = "文章反馈", description = "文章反馈相关接口")
public class FeedbackController {

    @Resource
    private FeedbackService feedbackService;

    @Resource
    private UserService userService;

    /**
     * 提交反馈
     */
    @RateLimit(maxRequests = 10, windowSeconds = 60)
    @PostMapping("/submit")
    @Operation(summary = "提交文章反馈")
    public BaseResponse<Boolean> submitFeedback(@RequestBody Map<String, Object> request,
                                                 HttpServletRequest httpServletRequest) {
        String taskId = (String) request.get("taskId");
        Integer rating = (Integer) request.get("rating");
        String comment = (String) request.get("comment");

        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        ThrowUtils.throwIf(rating == null || (rating != 0 && rating != 1),
                ErrorCode.PARAMS_ERROR, "评分必须为 0（不满意）或 1（满意）");

        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean result = feedbackService.submitFeedback(taskId, rating, comment, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 获取反馈统计
     */
    @GetMapping("/stats/{taskId}")
    @Operation(summary = "获取文章反馈统计")
    public BaseResponse<Map<String, Object>> getFeedbackStats(@PathVariable String taskId) {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        Map<String, Object> stats = feedbackService.getFeedbackStats(taskId);
        return ResultUtils.success(stats);
    }

    /**
     * 获取当前用户的反馈
     */
    @GetMapping("/my/{taskId}")
    @Operation(summary = "获取当前用户对文章的反馈")
    public BaseResponse<ArticleFeedback> getMyFeedback(@PathVariable String taskId,
                                                        HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        User loginUser = userService.getLoginUser(httpServletRequest);
        ArticleFeedback feedback = feedbackService.getUserFeedback(taskId, loginUser);
        return ResultUtils.success(feedback);
    }
}
