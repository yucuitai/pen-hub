package com.pen.penhubbackend.model.entity;

import com.mybatisflex.annotation.Column;
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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "article")
public class Article implements Serializable {

    @Serial// 标识该类中与序列化（Serialization）相关的成员，帮助编译器进行静态检查。
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 任务ID（UUID）
     */
    private String taskId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 选题
     */
    private String topic;

    /**
     * 用户补充描述
     */
    private String userDescription;

    /**
     * 允许的配图方式列表（JSON格式）
     */
    private String enabledImageMethods;

    /**
     * 文章风格：tech/emotional/educational/humorous，可为空
     */
    private String style;

    /**
     * 主标题
     */
    private String mainTitle;

    /**
     * 副标题
     */
    private String subTitle;

    /**
     * 标题方案列表（JSON格式）
     */
    private String titleOptions;

    /**
     * 大纲（JSON格式）
     */
    private String outline;

    /**
     * 正文（Markdown格式，不含图片）
     */
    private String content;

    /**
     * 完整图文（Markdown格式，含图片）
     */
    private String fullContent;

    /**
     * 封面图 URL
     */
    private String coverImage;

    /**
     * 配图列表（JSON数组，包含封面图 position=1）
     */
    private String images;

    /**
     * 状态：PENDING/PROCESSING/COMPLETED/FAILED
     */
    private String status;

    /**
     * 当前阶段：PENDING/TITLE_GENERATING/TITLE_SELECTING/OUTLINE_GENERATING/OUTLINE_EDITING/CONTENT_GENERATING
     */
    private String phase;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 状态快照（JSON，断点续传用）
     */
    private String stateSnapshot;

    /**
     * 收藏状态：0-未收藏 1-已收藏
     */
    private Integer isFavorited;

    /**
     * 文章标签（JSON数组）
     */
    private String tags;

    /**
     * 内容质量评分（0-100）
     */
    private Integer reviewScore;

    /**
     * 审核改进建议（JSON数组）
     */
    private String reviewSuggestions;

    /**
     * 内容类型：ARTICLE/SHORT_VIDEO_SCRIPT/LIVE_SCRIPT/INTERVIEW_SCRIPT/EVENT_SCRIPT/DRAMA_SCRIPT
     */
    private String contentType;

    /**
     * 脚本结构（JSON，存储分镜/环节/对话等）
     */
    private String scriptStructure;

    /**
     * 平台（脚本类型）：douyin/bilibili/weixin_video
     */
    private String platform;

    /**
     * 时长（脚本类型）：15s/30s/60s/3min/1h/2h/4h
     */
    private String duration;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 完成时间
     */
    private LocalDateTime completedTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @Column(value = "is_delete", isLogicDelete = true)
    private Integer isDelete;
}

