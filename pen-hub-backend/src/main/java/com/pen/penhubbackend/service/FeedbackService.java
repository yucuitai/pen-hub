package com.pen.penhubbackend.service;

import com.mybatisflex.core.service.IService;
import com.pen.penhubbackend.model.entity.ArticleFeedback;
import com.pen.penhubbackend.model.entity.User;

import java.util.Map;

/**
 * 文章反馈服务接口
 *
 * @author pen-hub
 */
public interface FeedbackService extends IService<ArticleFeedback> {

    /**
     * 提交反馈
     *
     * @param taskId    文章任务ID
     * @param rating    评分（1=满意, 0=不满意）
     * @param comment   评论（可选）
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean submitFeedback(String taskId, Integer rating, String comment, User loginUser);

    /**
     * 获取文章反馈统计
     *
     * @param taskId 文章任务ID
     * @return 统计数据（satisfied, unsatisfied, total）
     */
    Map<String, Object> getFeedbackStats(String taskId);

    /**
     * 获取当前用户对某文章的反馈
     *
     * @param taskId    文章任务ID
     * @param loginUser 当前登录用户
     * @return 反馈实体（可能为 null）
     */
    ArticleFeedback getUserFeedback(String taskId, User loginUser);
}
