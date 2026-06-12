package com.pen.penhubbackend.model.enums;

import lombok.Getter;

/**
 * SSE 消息类型枚举
 * 前后端都遵循的消息格式
 *
 */
@Getter
public enum SseMessageTypeEnum {

    /**
     * 智能体1完成（生成标题方案）
     */
    AGENT1_COMPLETE("AGENT1_COMPLETE", "标题方案生成完成"),
    
    /**
     * 标题方案生成完成（等待用户选择）
     */
    TITLES_GENERATED("TITLES_GENERATED", "标题方案已生成"),

    /**
     * 智能体2流式输出（大纲）
     */
    AGENT2_STREAMING("AGENT2_STREAMING", "大纲流式输出"),

    /**
     * 智能体2完成（生成大纲）
     */
    AGENT2_COMPLETE("AGENT2_COMPLETE", "大纲生成完成"),
    
    /**
     * 大纲生成完成（等待用户编辑）
     */
    OUTLINE_GENERATED("OUTLINE_GENERATED", "大纲已生成"),

    /**
     * 智能体3流式输出（正文）
     */
    AGENT3_STREAMING("AGENT3_STREAMING", "正文流式输出"),

    /**
     * 智能体3完成（生成正文）
     */
    AGENT3_COMPLETE("AGENT3_COMPLETE", "正文生成完成"),

    /**
     * Reviewer 完成（内容质量审核）
     */
    REVIEWER_COMPLETE("REVIEWER_COMPLETE", "内容审核完成"),

    /**
     * 智能体4完成（分析配图需求）
     */
    AGENT4_COMPLETE("AGENT4_COMPLETE", "配图需求分析完成"),

    /**
     * 单张配图完成
     */
    IMAGE_COMPLETE("IMAGE_COMPLETE", "单张配图完成"),

    /**
     * 智能体5完成（生成配图）
     */
    AGENT5_COMPLETE("AGENT5_COMPLETE", "配图生成完成"),

    /**
     * 图文合成完成
     */
    MERGE_COMPLETE("MERGE_COMPLETE", "图文合成完成"),

    /**
     * 全部完成
     */
    ALL_COMPLETE("ALL_COMPLETE", "全部完成"),

    // region 脚本相关消息类型

    /**
     * 脚本 Hook 生成完成
     */
    SCRIPT_HOOK_COMPLETE("SCRIPT_HOOK_COMPLETE", "脚本Hook生成完成"),

    /**
     * 脚本大纲流式输出
     */
    SCRIPT_OUTLINE_STREAMING("SCRIPT_OUTLINE_STREAMING", "脚本大纲流式输出"),

    /**
     * 脚本大纲生成完成
     */
    SCRIPT_OUTLINE_COMPLETE("SCRIPT_OUTLINE_COMPLETE", "脚本大纲生成完成"),

    /**
     * 脚本正文流式输出
     */
    SCRIPT_CONTENT_STREAMING("SCRIPT_CONTENT_STREAMING", "脚本正文流式输出"),

    /**
     * 脚本正文生成完成
     */
    SCRIPT_CONTENT_COMPLETE("SCRIPT_CONTENT_COMPLETE", "脚本正文生成完成"),

    /**
     * 脚本审核完成
     */
    SCRIPT_REVIEW_COMPLETE("SCRIPT_REVIEW_COMPLETE", "脚本审核完成"),

    /**
     * 脚本合成完成
     */
    SCRIPT_MERGE_COMPLETE("SCRIPT_MERGE_COMPLETE", "脚本合成完成"),

    // endregion

    // region 直播台本相关消息类型

    /**
     * 直播大纲生成完成
     */
    LIVE_OUTLINE_COMPLETE("LIVE_OUTLINE_COMPLETE", "直播大纲生成完成"),

    /**
     * 直播话术流式输出
     */
    LIVE_SCRIPT_STREAMING("LIVE_SCRIPT_STREAMING", "直播话术流式输出"),

    /**
     * 直播话术生成完成
     */
    LIVE_SCRIPT_COMPLETE("LIVE_SCRIPT_COMPLETE", "直播话术生成完成"),

    /**
     * 直播互动环节生成完成
     */
    LIVE_INTERACTION_COMPLETE("LIVE_INTERACTION_COMPLETE", "直播互动环节生成完成"),

    /**
     * 直播应急话术生成完成
     */
    LIVE_EMERGENCY_COMPLETE("LIVE_EMERGENCY_COMPLETE", "直播应急话术生成完成"),

    /**
     * 直播台本合成完成
     */
    LIVE_MERGE_COMPLETE("LIVE_MERGE_COMPLETE", "直播台本合成完成"),

    // endregion

    // region 访谈脚本相关消息类型

    INTERVIEW_OUTLINE_COMPLETE("INTERVIEW_OUTLINE_COMPLETE", "访谈提纲生成完成"),
    INTERVIEW_DIALOGUE_STREAMING("INTERVIEW_DIALOGUE_STREAMING", "对话内容流式输出"),
    INTERVIEW_DIALOGUE_COMPLETE("INTERVIEW_DIALOGUE_COMPLETE", "对话内容生成完成"),
    INTERVIEW_REVIEW_COMPLETE("INTERVIEW_REVIEW_COMPLETE", "访谈审核完成"),
    INTERVIEW_MERGE_COMPLETE("INTERVIEW_MERGE_COMPLETE", "访谈合成完成"),

    // endregion

    // region 活动台本相关消息类型

    EVENT_OUTLINE_COMPLETE("EVENT_OUTLINE_COMPLETE", "活动流程生成完成"),
    EVENT_SCRIPT_STREAMING("EVENT_SCRIPT_STREAMING", "环节话术流式输出"),
    EVENT_SCRIPT_COMPLETE("EVENT_SCRIPT_COMPLETE", "环节话术生成完成"),
    EVENT_CUE_COMPLETE("EVENT_CUE_COMPLETE", "提词设计生成完成"),
    EVENT_MERGE_COMPLETE("EVENT_MERGE_COMPLETE", "活动台本合成完成"),

    // endregion

    // region 剧本相关消息类型

    DRAMA_CHARACTER_COMPLETE("DRAMA_CHARACTER_COMPLETE", "角色设定生成完成"),
    DRAMA_PLOT_COMPLETE("DRAMA_PLOT_COMPLETE", "剧情大纲生成完成"),
    DRAMA_SCRIPT_STREAMING("DRAMA_SCRIPT_STREAMING", "台词场景流式输出"),
    DRAMA_SCRIPT_COMPLETE("DRAMA_SCRIPT_COMPLETE", "台词场景生成完成"),
    DRAMA_REVIEW_COMPLETE("DRAMA_REVIEW_COMPLETE", "剧本审核完成"),
    DRAMA_MERGE_COMPLETE("DRAMA_MERGE_COMPLETE", "剧本合成完成"),

    // endregion

    /**
     * 错误
     */
    ERROR("ERROR", "错误");

    /**
     * 消息类型值
     */
    private final String value;

    /**
     * 消息类型描述
     */
    private final String description;

    SseMessageTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 获取流式输出消息前缀
     * 用于构建带数据的流式消息，如 "AGENT2_STREAMING:内容"
     *
     * @return 消息前缀（带冒号）
     */
    public String getStreamingPrefix() {
        return this.value + ":";
    }
}
