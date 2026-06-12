package com.pen.penhubbackend.model.dto.script;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 访谈脚本状态对象
 * 用于 StateGraph 中传递访谈脚本生成各阶段的数据
 *
 * @author pen-hub
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 选题
     */
    private String topic;

    /**
     * 平台：douyin/bilibili/weixin_video
     */
    private String platform;

    /**
     * 时长：15min/30min/60min
     */
    private String duration;

    /**
     * 风格：formal/casual/tech/emotional
     */
    private String style;

    /**
     * 访谈提纲
     */
    private InterviewOutlineResult outline;

    /**
     * 访谈对话结果
     */
    private InterviewDialogueResult dialogueResult;

    /**
     * 访谈审核结果
     */
    private InterviewReviewResult reviewResult;

    /**
     * 最终脚本结构（JSON）
     */
    private String scriptStructure;

    /**
     * Markdown 格式脚本
     */
    private String markdownContent;

    /**
     * 纯文本格式脚本
     */
    private String plainTextContent;

    // region 内部类

    /**
     * 访谈提纲结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewOutlineResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private List<InterviewSegment> segments;
        private Integer totalDuration;
    }

    /**
     * 访谈段落信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewSegment implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer segmentIndex;
        private String startTime;
        private String endTime;
        private String segmentType;
        private String title;
        private String description;
        private List<String> keyQuestions;
    }

    /**
     * 访谈对话结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewDialogueResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private List<InterviewDialogueLine> dialogues;
        private String fullScript;
    }

    /**
     * 访谈对话行
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewDialogueLine implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer segmentIndex;
        private String role;
        private String speaker;
        private String content;
        private String tone;
    }

    /**
     * 访谈审核结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewReviewResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer reviewScore;
        private List<String> suggestions;
        private String rewrittenScript;
        private InterviewReviewDimensions dimensions;
    }

    /**
     * 访谈审核维度
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewReviewDimensions implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer flow;
        private Integer depth;
        private Integer interaction;
        private Integer styleConsistency;
        private Integer platformFit;
    }

    // endregion
}
