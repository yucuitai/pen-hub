package com.pen.penhubbackend.model.enums;

import lombok.Getter;

/**
 * 文章阶段枚举
 * 对应数据表字段phase
 *
 */
@Getter
public enum ArticlePhaseEnum {

    PENDING("PENDING", "等待处理"),
    TITLE_GENERATING("TITLE_GENERATING", "生成标题中"),
    TITLE_SELECTING("TITLE_SELECTING", "等待选择标题"),
    OUTLINE_GENERATING("OUTLINE_GENERATING", "生成大纲中"),
    OUTLINE_EDITING("OUTLINE_EDITING", "等待编辑大纲"),
    CONTENT_GENERATING("CONTENT_GENERATING", "生成正文中"),

    // 短视频脚本阶段
    SCRIPT_HOOK_GENERATING("SCRIPT_HOOK_GENERATING", "生成Hook中"),
    SCRIPT_OUTLINE_GENERATING("SCRIPT_OUTLINE_GENERATING", "生成脚本大纲中"),
    SCRIPT_CONTENT_GENERATING("SCRIPT_CONTENT_GENERATING", "生成脚本内容中"),
    SCRIPT_REVIEWING("SCRIPT_REVIEWING", "脚本审核中"),
    SCRIPT_MERGING("SCRIPT_MERGING", "脚本合成中"),

    // 直播台本阶段
    LIVE_OUTLINE_GENERATING("LIVE_OUTLINE_GENERATING", "生成直播大纲中"),
    LIVE_SCRIPT_GENERATING("LIVE_SCRIPT_GENERATING", "生成直播话术中"),
    LIVE_INTERACTION_GENERATING("LIVE_INTERACTION_GENERATING", "生成互动环节中"),
    LIVE_EMERGENCY_GENERATING("LIVE_EMERGENCY_GENERATING", "生成应急话术中"),
    LIVE_MERGING("LIVE_MERGING", "直播台本合成中"),

    // 访谈脚本阶段
    INTERVIEW_OUTLINE_GENERATING("INTERVIEW_OUTLINE_GENERATING", "生成访谈提纲中"),
    INTERVIEW_DIALOGUE_GENERATING("INTERVIEW_DIALOGUE_GENERATING", "生成对话内容中"),
    INTERVIEW_REVIEWING("INTERVIEW_REVIEWING", "访谈审核中"),
    INTERVIEW_MERGING("INTERVIEW_MERGING", "访谈合成中"),

    // 活动台本阶段
    EVENT_OUTLINE_GENERATING("EVENT_OUTLINE_GENERATING", "生成活动流程中"),
    EVENT_SCRIPT_GENERATING("EVENT_SCRIPT_GENERATING", "生成环节话术中"),
    EVENT_CUE_GENERATING("EVENT_CUE_GENERATING", "生成提词设计中"),
    EVENT_MERGING("EVENT_MERGING", "活动台本合成中"),

    // 剧本阶段
    DRAMA_CHARACTER_GENERATING("DRAMA_CHARACTER_GENERATING", "生成角色设定中"),
    DRAMA_PLOT_GENERATING("DRAMA_PLOT_GENERATING", "生成剧情大纲中"),
    DRAMA_SCRIPT_GENERATING("DRAMA_SCRIPT_GENERATING", "生成台词场景中"),
    DRAMA_REVIEWING("DRAMA_REVIEWING", "剧本审核中"),
    DRAMA_MERGING("DRAMA_MERGING", "剧本合成中");

    /**
     * 阶段值
     */
    private final String value;

    /**
     * 阶段描述
     */
    private final String description;

    ArticlePhaseEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 根据值获取枚举
     *
     * @param value 阶段值
     * @return 枚举实例
     */
    public static ArticlePhaseEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ArticlePhaseEnum phaseEnum : values()) {
            if (phaseEnum.getValue().equals(value)) {
                return phaseEnum;
            }
        }
        return null;
    }

    /**
     * 校验是否可以转换到目标阶段
     *
     * @param targetPhase 目标阶段
     * @return 是否可以转换
     */
    public boolean canTransitionTo(ArticlePhaseEnum targetPhase) {
        if (targetPhase == null) {
            return false;
        }
        
        // 定义合法的状态转换
        return switch (this) {
            case PENDING -> targetPhase == TITLE_GENERATING
                    || targetPhase == SCRIPT_HOOK_GENERATING
                    || targetPhase == LIVE_OUTLINE_GENERATING
                    || targetPhase == INTERVIEW_OUTLINE_GENERATING
                    || targetPhase == EVENT_OUTLINE_GENERATING
                    || targetPhase == DRAMA_CHARACTER_GENERATING;
            case TITLE_GENERATING -> targetPhase == TITLE_SELECTING;
            case TITLE_SELECTING -> targetPhase == OUTLINE_GENERATING;
            case OUTLINE_GENERATING -> targetPhase == OUTLINE_EDITING;
            case OUTLINE_EDITING -> targetPhase == CONTENT_GENERATING;
            case CONTENT_GENERATING -> false;

            // 短视频脚本流程
            case SCRIPT_HOOK_GENERATING -> targetPhase == SCRIPT_OUTLINE_GENERATING;
            case SCRIPT_OUTLINE_GENERATING -> targetPhase == SCRIPT_CONTENT_GENERATING;
            case SCRIPT_CONTENT_GENERATING -> targetPhase == SCRIPT_REVIEWING;
            case SCRIPT_REVIEWING -> targetPhase == SCRIPT_MERGING;
            case SCRIPT_MERGING -> false;

            // 直播台本流程
            case LIVE_OUTLINE_GENERATING -> targetPhase == LIVE_SCRIPT_GENERATING;
            case LIVE_SCRIPT_GENERATING -> targetPhase == LIVE_INTERACTION_GENERATING;
            case LIVE_INTERACTION_GENERATING -> targetPhase == LIVE_EMERGENCY_GENERATING;
            case LIVE_EMERGENCY_GENERATING -> targetPhase == LIVE_MERGING;
            case LIVE_MERGING -> false;

            // 访谈脚本流程
            case INTERVIEW_OUTLINE_GENERATING -> targetPhase == INTERVIEW_DIALOGUE_GENERATING;
            case INTERVIEW_DIALOGUE_GENERATING -> targetPhase == INTERVIEW_REVIEWING;
            case INTERVIEW_REVIEWING -> targetPhase == INTERVIEW_MERGING;
            case INTERVIEW_MERGING -> false;

            // 活动台本流程
            case EVENT_OUTLINE_GENERATING -> targetPhase == EVENT_SCRIPT_GENERATING;
            case EVENT_SCRIPT_GENERATING -> targetPhase == EVENT_CUE_GENERATING;
            case EVENT_CUE_GENERATING -> targetPhase == EVENT_MERGING;
            case EVENT_MERGING -> false;

            // 剧本流程
            case DRAMA_CHARACTER_GENERATING -> targetPhase == DRAMA_PLOT_GENERATING;
            case DRAMA_PLOT_GENERATING -> targetPhase == DRAMA_SCRIPT_GENERATING;
            case DRAMA_SCRIPT_GENERATING -> targetPhase == DRAMA_REVIEWING;
            case DRAMA_REVIEWING -> targetPhase == DRAMA_MERGING;
            case DRAMA_MERGING -> false;
        };
    }
}
