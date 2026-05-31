package com.pen.penhubbackend.model.dto.article;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 文章生成状态（智能体间共享的状态对象）
 * ArticleState的作用:
 * 5个智能体是串行执行的,后一个智能体需要用到前一个的输出结果。
 * 需要一个”状态容器”在它们之间传递数据
 *
 * 整体结构：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  外层字段：文章基础信息                                        │
 * │    taskId, topic, style, userDescription, phase              │
 * ├─────────────────────────────────────────────────────────────┤
 * │  中间字段：各智能体的输出结果（按执行顺序）                       │
 * │    titleOptions (智能体1) → title (用户选择后)                 │
 * │    outline (智能体2)                                          │
 * │    content (智能体3) → imageRequirements (智能体4)             │
 * │    images (智能体5)                                           │
 * ├─────────────────────────────────────────────────────────────┤
 * │  最终字段：合成后的完整内容                                     │
 * │    fullContent (图文合成后)                                    │
 * ├─────────────────────────────────────────────────────────────┤
 * │  内部类：定义各组件的数据结构                                    │
 * │    TitleOption, TitleResult, OutlineResult, OutlineSection    │
 * │    ImageRequirement, ImageResult, Agent4Result                │
 * └─────────────────────────────────────────────────────────────┘
 */
@Data
public class ArticleState implements Serializable {

    // ==================== 外层字段：文章基础信息 ====================

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 选题
     */
    private String topic;

    /**
     * 用户补充描述
     */
    private String userDescription;

    /**
     * 文章风格
     */
    private String style;

    /**
     * 当前阶段
     */
    private String phase;

    // ==================== 中间字段：各智能体的输出结果 ====================

    /**
     * 标题方案列表（智能体1输出）
     * 用户输入选题后，智能体1生成3-5个标题方案供用户选择
     */
    private List<TitleOption> titleOptions;

    /**
     * 标题结果（用户从 titleOptions 中选择后设置）
     * 包含主标题和副标题
     */
    private TitleResult title;

    /**
     * 大纲结果（智能体2输出）
     * 智能体2根据用户选择的标题生成文章大纲
     */
    private OutlineResult outline;

    /**
     * 正文内容（智能体3输出）
     * 智能体3根据大纲生成 Markdown 格式的正文
     */
    private String content;

    /**
     * 配图需求列表（智能体4输出）
     * 智能体4分析正文内容，确定需要配图的位置和类型
     * 同时会在正文中插入占位符 {{IMAGE_PLACEHOLDER_N}}
     */
    private List<ImageRequirement> imageRequirements;

    /**
     * 封面图 URL（单独存储，同时 images 列表中的 position=1 也是封面图）
     */
    private String coverImage;

    /**
     * 配图结果列表（智能体5输出）
     * 智能体5根据 imageRequirements 生成实际配图，并上传到 COS
     */
    private List<ImageResult> images;

    /**
     * 允许的配图方式列表（为空表示支持所有方式）
     * 用于限制用户可选的配图方式，如：["PEXELS", "NANO_BANANA"]
     */
    private List<String> enabledImageMethods;

    // ==================== 内部类：定义各组件的数据结构 ====================

    /**
     * 标题方案（智能体1生成的选项）
     * 用户输入选题后，智能体1会生成3-5个标题方案供用户选择
     */
    @Data
    public static class TitleOption implements Serializable {
        private String mainTitle;
        private String subTitle;
    }

    /**
     * 标题结果（用户选择后的最终标题）
     * 用户从 titleOptions 中选择一个后，设置到此字段
     */
    @Data
    public static class TitleResult implements Serializable {
        private String mainTitle;
        private String subTitle;
    }

    /**
     * 大纲结果（智能体2生成）
     * 包含多个章节，每个章节有标题和要点列表
     */
    @Data
    public static class OutlineResult implements Serializable {
        private List<OutlineSection> sections;
    }

    /**
     * 大纲章节（大纲的组成部分）
     * 例如：{section: 1, title: "机器学习基础", points: ["定义", "发展历史", "应用领域"]}
     */
    @Data
    public static class OutlineSection implements Serializable {
        private Integer section;
        private String title;
        private List<String> points;
    }

    /**
     * 图片需求信息（智能体4生成）
     * 描述文章中需要配图的具体要求，包括位置、类型、来源等信息
     * 用于指导智能体5生成实际配图
     */
    @Data
    public static class ImageRequirement implements Serializable {
        /**
         * 图片在文章中的位置序号
         */
        private Integer position;

        /**
         * 图片类型
         */
        private String type;

        /**
         * 所属章节标题
         */
        private String sectionTitle;

        /**
         * 图片检索关键词
         */
        private String keywords;

        /**
         * 图片来源：PEXELS（图库检索）或 NANO_BANANA（AI 生图）
         */
        private String imageSource;

        /**
         * AI 生图提示词（当 imageSource 为 NANO_BANANA 时使用）
         */
        private String prompt;

        /**
         * 占位符ID，用于在正文中定位插入位置，格式：{{IMAGE_PLACEHOLDER_N}}
         */
        private String placeholderId;
    }

    /**
     * 配图结果（智能体5生成）
     * 存储实际生成的图片信息和插入位置
     * 用于图文合成时将图片插入到正文的正确位置
     */
    @Data
    public static class ImageResult implements Serializable {
        /**
         * 图片在文章中的位置序号
         */
        private Integer position;

        /**
         * 图片URL地址
         */
        private String url;

        /**
         * 配图方法
         */
        private String method;

        /**
         * 使用的关键词
         */
        private String keywords;

        /**
         * 所属章节标题
         */
        private String sectionTitle;

        /**
         * 图片描述信息
         */
        private String description;

        /**
         * 占位符ID，用于在正文中定位插入位置
         */
        private String placeholderId;
    }

    /**
     * 智能体4返回结果（包含带占位符的正文和配图需求列表）
     * 智能体4分析正文后，会：
     * 1. 在正文中插入占位符 {{IMAGE_PLACEHOLDER_N}}
     * 2. 生成配图需求列表
     */
    @Data
    public static class Agent4Result implements Serializable {
        /**
         * 包含占位符的正文内容
         */
        private String contentWithPlaceholders;
        /**
         * 配图需求列表
         */
        private List<ImageRequirement> imageRequirements;
    }

    // ==================== 最终字段：合成后的完整内容 ====================

    /**
     * 完整图文内容（图文合成后）
     * 将 content 中的占位符替换为实际图片 Markdown 后的最终结果
     */
    private String fullContent;

    private static final long serialVersionUID = 1L;
}

