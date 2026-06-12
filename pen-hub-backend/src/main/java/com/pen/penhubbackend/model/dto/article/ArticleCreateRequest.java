package com.pen.penhubbackend.model.dto.article;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建文章请求
 *
 */
@Data
public class ArticleCreateRequest implements Serializable {

    /**
     * 选题
     */
    private String topic;

    /**
     * 文章风格：tech/emotional/educational/humorous，可为空
     */
    private String style;

    /**
     * 允许的配图方式列表（为空或 null 表示支持所有方式）
     * 可选值：PEXELS, NANO_BANANA, MERMAID, ICONIFY, EMOJI_PACK, SVG_DIAGRAM
     */
    private List<String> enabledImageMethods;

    /**
     * 内容类型：ARTICLE / SHORT_VIDEO_SCRIPT / LIVE_SCRIPT
     * 默认为 ARTICLE
     */
    private String contentType;

    /**
     * 平台（脚本类型必填）：douyin/bilibili/weixin_video
     */
    private String platform;

    /**
     * 时长（脚本类型必填）：15s/30s/60s/3min（短视频）/ 1h/2h/4h（直播）
     */
    private String duration;

    /**
     * 直播类型（直播台本必填）：ecommerce/knowledge/event
     */
    private String liveType;

    /**
     * 产品信息（电商直播场景）
     */
    private String productInfo;

    /**
     * 参与人数（直播场景）
     */
    private String participantCount;

    private static final long serialVersionUID = 1L;
}
