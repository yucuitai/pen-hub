package com.pen.penhubbackend.controller;

import com.pen.penhubbackend.annotation.AuthCheck;
import com.pen.penhubbackend.common.BaseResponse;
import com.pen.penhubbackend.common.ResultUtils;
import com.pen.penhubbackend.constant.UserConstant;
import com.pen.penhubbackend.model.vo.StatisticsVO;
import com.pen.penhubbackend.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计分析控制器
 *
 */
@RestController
@RequestMapping("/statistics")
@Slf4j
@Tag(name = "StatisticsController", description = "统计分析接口")
public class StatisticsController {

    @Resource
    private StatisticsService statisticsService;

    @Resource
    private com.pen.penhubbackend.service.AgentLogService agentLogService;

    /**
     * 获取系统统计数据（仅管理员）
     */
    @GetMapping("/overview")
    @Operation(summary = "获取系统统计数据")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<StatisticsVO> getStatistics() {
        StatisticsVO statistics = statisticsService.getStatistics();
        return ResultUtils.success(statistics);
    }

    /**
     * 获取 AI 调用统计（仅管理员）
     */
    @GetMapping("/ai-call-stats")
    @Operation(summary = "获取 AI 调用统计")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<java.util.Map<String, Object>> getAiCallStats() {
        java.util.Map<String, Object> stats = agentLogService.getGlobalAiCallStats();
        return ResultUtils.success(stats);
    }
}
