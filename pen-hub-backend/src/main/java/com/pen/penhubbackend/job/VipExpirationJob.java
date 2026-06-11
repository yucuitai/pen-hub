package com.pen.penhubbackend.job;

import com.pen.penhubbackend.service.RedemptionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 会员过期检查定时任务
 */
@Component
@EnableScheduling
@Slf4j
public class VipExpirationJob {

    @Resource
    private RedemptionService redemptionService;

    /**
     * 每小时检查一次会员是否过期
     * cron 表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkVipExpiration() {
        log.info("开始检查会员过期状态...");
        try {
            redemptionService.checkAndExpireVipMembers();
            log.info("会员过期检查完成");
        } catch (Exception e) {
            log.error("会员过期检查失败", e);
        }
    }
}
