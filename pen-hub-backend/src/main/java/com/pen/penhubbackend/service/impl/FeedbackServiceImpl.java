package com.pen.penhubbackend.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.pen.penhubbackend.exception.BusinessException;
import com.pen.penhubbackend.exception.ErrorCode;
import com.pen.penhubbackend.mapper.ArticleFeedbackMapper;
import com.pen.penhubbackend.model.entity.Article;
import com.pen.penhubbackend.model.entity.ArticleFeedback;
import com.pen.penhubbackend.model.entity.User;
import com.pen.penhubbackend.service.ArticleService;
import com.pen.penhubbackend.service.FeedbackService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 文章反馈服务实现类
 *
 * @author pen-hub
 */
@Service
@Slf4j
public class FeedbackServiceImpl extends ServiceImpl<ArticleFeedbackMapper, ArticleFeedback> implements FeedbackService {

    @Resource
    private ArticleService articleService;

    @Override
    public boolean submitFeedback(String taskId, Integer rating, String comment, User loginUser) {
        // 校验文章存在
        Article article = articleService.getByTaskId(taskId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文章不存在");
        }

        // 校验是否已反馈
        ArticleFeedback existing = getUserFeedback(taskId, loginUser);
        if (existing != null) {
            // 更新已有反馈
            existing.setRating(rating);
            existing.setComment(comment);
            return this.updateById(existing);
        }

        // 创建新反馈
        ArticleFeedback feedback = ArticleFeedback.builder()
                .articleId(article.getId())
                .userId(loginUser.getId())
                .rating(rating)
                .comment(comment)
                .createTime(LocalDateTime.now())
                .build();

        boolean saved = this.save(feedback);
        if (saved) {
            log.info("用户反馈已提交, taskId={}, userId={}, rating={}", taskId, loginUser.getId(), rating);
        }
        return saved;
    }

    @Override
    public Map<String, Object> getFeedbackStats(String taskId) {
        Article article = articleService.getByTaskId(taskId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文章不存在");
        }

        long satisfied = this.count(QueryWrapper.create()
                .eq("article_id", article.getId())
                .eq("rating", 1));

        long unsatisfied = this.count(QueryWrapper.create()
                .eq("article_id", article.getId())
                .eq("rating", 0));

        Map<String, Object> stats = new HashMap<>();
        stats.put("satisfied", satisfied);
        stats.put("unsatisfied", unsatisfied);
        stats.put("total", satisfied + unsatisfied);
        return stats;
    }

    @Override
    public ArticleFeedback getUserFeedback(String taskId, User loginUser) {
        Article article = articleService.getByTaskId(taskId);
        if (article == null) {
            return null;
        }

        return this.getOne(QueryWrapper.create()
                .eq("article_id", article.getId())
                .eq("user_id", loginUser.getId())
                .limit(1));
    }
}
