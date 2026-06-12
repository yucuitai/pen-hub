package com.pen.penhubbackend.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章反馈实体
 *
 * @author pen-hub
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "article_feedback")
public class ArticleFeedback implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 评分：1=满意, 0=不满意
     */
    private Integer rating;

    /**
     * 评论内容（可选）
     */
    private String comment;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
