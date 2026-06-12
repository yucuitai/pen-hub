package com.pen.penhubbackend.model.enums;

import lombok.Getter;

/**
 * 内容类型枚举
 *
 * Phase 4 v2.0 内容类型扩展：支持短视频脚本和直播台本
 *
 * @author pen-hub
 */
@Getter
public enum ContentTypeEnum {

    /**
     * 普通文章 - v1.0
     * 适用场景：公众号、小红书、抖音、微博等新媒体文案
     */
    ARTICLE("文章", "1.0", "新媒体文案生成"),

    /**
     * 短视频脚本 - v2.0
     * 适用场景：抖音/B站/视频号短视频脚本，含分镜、台词、画面描述
     */
    SHORT_VIDEO_SCRIPT("短视频脚本", "2.0", "短视频脚本生成"),

    /**
     * 直播台本 - v2.0
     * 适用场景：电商带货/知识分享/活动直播，含环节、话术、互动节点
     */
    LIVE_SCRIPT("直播台本", "2.0", "直播台本生成"),

    /**
     * 访谈/对话脚本 - v2.1 (规划中)
     * 适用场景：播客、访谈节目
     */
    INTERVIEW_SCRIPT("访谈脚本", "2.1", "访谈对话脚本生成"),

    /**
     * 活动/会议台本 - v2.1 (规划中)
     * 适用场景：线下活动、发布会
     */
    EVENT_SCRIPT("活动台本", "2.1", "活动会议台本生成"),

    /**
     * 剧本/故事脚本 - v2.2 (规划中)
     * 适用场景：短剧、微电影
     */
    DRAMA_SCRIPT("剧本", "2.2", "剧本故事脚本生成");

    /**
     * 类型描述
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
     * 根据值获取枚举（忽略大小写）
     *
     * @param value 枚举值
     * @return 对应的枚举值，如果不存在则返回 null
     */
    public static ContentTypeEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 判断是否为脚本类型（非普通文章）
     */
    public boolean isScriptType() {
        return this != ARTICLE;
    }

    /**
     * 判断是否为短视频脚本
     */
    public boolean isShortVideoScript() {
        return this == SHORT_VIDEO_SCRIPT;
    }

    /**
     * 判断是否为直播台本
     */
    public boolean isLiveScript() {
        return this == LIVE_SCRIPT;
    }
}
