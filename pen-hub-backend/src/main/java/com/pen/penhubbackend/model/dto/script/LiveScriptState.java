package com.pen.penhubbackend.model.dto.script;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 直播台本状态对象
 * 用于 StateGraph 中传递直播台本生成各阶段的数据
 *
 * @author pen-hub
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveScriptState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String taskId;
    private String topic;
    private String platform;
    private String duration;
    private String liveType;
    private String productInfo;
    private String participantCount;

    private LiveOutlineResult outline;
    private LiveScriptResult scriptResult;
    private LiveInteractionResult interactionResult;
    private LiveEmergencyResult emergencyResult;
    private String scriptStructure;
    private String markdownContent;

    // region 内部类

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveOutlineResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private List<SegmentInfo> segments;
        private Integer totalDuration;
        private List<String> interactionPoints;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentInfo implements Serializable {
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveScriptResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private List<SegmentScript> segments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentScript implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer segmentIndex;
        private String script;
        private List<String> keySentences;
        private String tone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveInteractionResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private List<InteractionInfo> interactions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InteractionInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer interactionIndex;
        private String triggerTime;
        private String type;
        private String title;
        private String script;
        private Integer duration;
        private String reward;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveEmergencyResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private List<EmergencyScript> emergencyScripts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmergencyScript implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String scenario;
        private String trigger;
        private List<String> scripts;
        private String tips;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveMergeResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String scriptStructure;
        private String markdownContent;
    }

    // endregion
}
