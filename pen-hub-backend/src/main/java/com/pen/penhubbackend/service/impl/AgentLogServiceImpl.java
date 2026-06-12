package com.pen.penhubbackend.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.pen.penhubbackend.mapper.AgentLogMapper;
import com.pen.penhubbackend.model.entity.AgentLog;
import com.pen.penhubbackend.model.vo.AgentExecutionStats;
import com.pen.penhubbackend.service.AgentLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体日志服务实现
 *
 * @author <a href="https://codefather.cn">编程导航学习圈</a>
 */
@Service
@Slf4j
public class AgentLogServiceImpl extends ServiceImpl<AgentLogMapper, AgentLog> implements AgentLogService {

    @Override
    @Async//异步保存日志
    public void saveLogAsync(AgentLog agentLog) {
        try {
            this.save(agentLog);
            log.info("智能体日志已保存, taskId={}, agentName={}, status={}, durationMs={}", 
                    agentLog.getTaskId(), agentLog.getAgentName(), agentLog.getStatus(), agentLog.getDurationMs());
        } catch (Exception e) {
            log.error("保存智能体日志失败, taskId={}, agentName={}", 
                    agentLog.getTaskId(), agentLog.getAgentName(), e);
        }
    }

    @Override
    public List<AgentLog> getLogsByTaskId(String taskId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("task_id", taskId)
                .orderBy("createTime", true);
        return this.list(queryWrapper);
    }

    @Override
    public AgentExecutionStats getExecutionStats(String taskId) {
        List<AgentLog> logs = getLogsByTaskId(taskId);
        
        if (logs == null || logs.isEmpty()) {
            return AgentExecutionStats.builder()
                    .taskId(taskId)
                    .agentCount(0)
                    .totalDurationMs(0)
                    .overallStatus("NOT_FOUND")
                    .build();
        }

        // 计算统计数据
        int totalDuration = 0;
        Map<String, Integer> agentDurations = new HashMap<>();
        String overallStatus = "SUCCESS";

        for (AgentLog log : logs) {
            // 累加总耗时
            if (log.getDurationMs() != null) {
                totalDuration += log.getDurationMs();
                agentDurations.put(log.getAgentName(), log.getDurationMs());
            }

            // 判断总体状态
            if ("FAILED".equals(log.getStatus())) {
                overallStatus = "FAILED";
            } else if ("RUNNING".equals(log.getStatus()) && !"FAILED".equals(overallStatus)) {
                overallStatus = "RUNNING";
            }
        }

        return AgentExecutionStats.builder()
                .taskId(taskId)
                .totalDurationMs(totalDuration)
                .agentCount(logs.size())
                .agentDurations(agentDurations)
                .overallStatus(overallStatus)
                .logs(logs)
                .build();
    }

    @Override
    public Map<String, Object> getGlobalAiCallStats() {
        Map<String, Object> stats = new HashMap<>();

        // 查询所有日志
        List<AgentLog> allLogs = this.list();
        int totalCalls = allLogs.size();
        int successCalls = 0;
        int failedCalls = 0;
        long totalDuration = 0;

        Map<String, Integer> agentCallCounts = new HashMap<>();

        for (AgentLog log : allLogs) {
            if ("SUCCESS".equals(log.getStatus())) {
                successCalls++;
            } else if ("FAILED".equals(log.getStatus())) {
                failedCalls++;
            }
            if (log.getDurationMs() != null) {
                totalDuration += log.getDurationMs();
            }
            agentCallCounts.merge(log.getAgentName(), 1, Integer::sum);
        }

        stats.put("totalCalls", totalCalls);
        stats.put("successCalls", successCalls);
        stats.put("failedCalls", failedCalls);
        stats.put("failureRate", totalCalls > 0 ? Math.round(failedCalls * 100.0 / totalCalls * 10) / 10.0 : 0);
        stats.put("avgDurationMs", totalCalls > 0 ? totalDuration / totalCalls : 0);
        stats.put("agentCallCounts", agentCallCounts);

        return stats;
    }
}
