package com.pen.penhubbackend.model.dto.script;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 活动台本状态对象
 * 用于 StateGraph 中传递活动台本生成各阶段的数据
 *
 * @author pen-hub
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventScriptState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 活动主题
     */
    private String topic;

    /**
     * 平台：线下/线上/混合
     */
    private String platform;

    /**
     * 活动时长：1h/2h/3h/半天/全天
     */
    private String duration;

    /**
     * 活动类型：conference/launch/ceremony/etc
     */
    private String eventType;

    /**
     * 参与人数
     */
    private String participantCount;

    /**
     * 活动大纲
     */
    private EventOutlineResult outline;

    /**
     * 环节话术结果
     */
    private EventScriptResult scriptResult;

    /**
     * 提词设计结果
     */
    private EventCueResult cueResult;

    /**
     * 最终脚本结构（JSON）
     */
    private String scriptStructure;

    /**
     * Markdown 格式脚本
     */
    private String markdownContent;

    // region 内部类

    /**
     * 活动大纲结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventOutlineResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private List<EventSegment> segments;
        private Integer totalDuration;
        private List<String> keyMilestones;
    }

    /**
     * 活动环节信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventSegment implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer segmentIndex;
        private String startTime;
        private String endTime;
        private String segmentType;
        private String title;
        private String description;
        private List<String> keyPoints;
    }

    /**
     * 环节话术结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventScriptResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private List<EventSegmentScript> segments;
    }

    /**
     * 环节话术信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventSegmentScript implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer segmentIndex;
        private String script;
        private List<String> keySentences;
        private String tone;
    }

    /**
     * 提词设计结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventCueResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private List<EventCue> cues;
    }

    /**
     * 提词信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventCue implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer cueIndex;
        private String triggerTime;
        private String type;
        private String script;
        private String notes;
    }

    /**
     * 活动台本合并结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventMergeResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String scriptStructure;
        private String markdownContent;
    }

    // endregion
}
