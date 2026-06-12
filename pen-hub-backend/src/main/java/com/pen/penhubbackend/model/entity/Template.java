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

/**
 * 模板实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "template")
public class Template implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 模板名称 */
    private String name;

    /** 分类：platform/scene/style */
    private String category;

    /** 目标平台：wechat/xiaohongshu/douyin/weibo */
    private String platform;

    /** 推荐风格 */
    private String style;

    /** 示例选题 */
    private String topicExample;

    /** 推荐配图方式（JSON数组） */
    private String recommendedImageMethods;

    /** 模板描述 */
    private String description;

    /** 排序权重 */
    private Integer sortOrder;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Column(value = "is_delete", isLogicDelete = true)
    private Integer isDelete;
}
