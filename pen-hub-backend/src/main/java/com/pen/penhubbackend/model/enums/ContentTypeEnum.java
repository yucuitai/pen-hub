package com.pen.penhubbackend.model.enums;

import lombok.Getter;

/**
 * 内容类型枚举
 *
 * 设计原则：智能体是"能力"，不是"场景"
 * 4个智能体 × N种场景模式，通过枚举切换不同的 prompt 和策略
 *
 * @author pen-hub
 */
@Getter
public enum ContentTypeEnum {

    /**
     * 新媒体文案 - v1.0
     * 适用场景：公众号、小红书、抖音、微博等短文案
     */
    NEW_MEDIA("新媒体文案", "1.0", "短文案生成场景"),

    /**
     * 小说创作 - v2.0 (规划中)
     * 适用场景：短篇、中篇、长篇小说
     */
    NOVEL("小说创作", "2.0", "长文本创作场景"),

    /**
     * 剧本写作 - v3.0 (规划中)
     * 适用场景：影视剧本、舞台剧等
     */
    SCRIPT("剧本写作", "3.0", "剧本创作场景"),

    /**
     * 长文章 - v2.0 (规划中)
     * 适用场景：深度报道、长文分析等
     */
    ARTICLE("长文章", "2.0", "深度内容场景");

    /**
     * 场景描述
     */
    private final String description;

    /**
     * 引入版本
     */
    private final String version;

    /**
     * 详细说明
     */
    private final String detail;

    ContentTypeEnum(String description, String version, String detail) {
        this.description = description;
        this.version = version;
        this.detail = detail;
    }

    /**
     * 根据名称获取枚举（忽略大小写）
     *
     * @param name 枚举名称
     * @return 对应的枚举值，如果不存在则返回 null
     */
    public static ContentTypeEnum getByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
