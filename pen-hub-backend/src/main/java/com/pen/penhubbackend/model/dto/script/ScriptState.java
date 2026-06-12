package com.pen.penhubbackend.model.dto.script;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 短视频脚本状态对象
 * 用于 StateGraph 中传递脚本生成各阶段的数据
 *
 * @author pen-hub
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptState implements Serializable {

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
     * 时长：15s/30s/60s/3min
     */
    private String duration;

    /**
     * 风格：funny/knowledge/emotional/product
     */
    private String style;

    /**
     * Hook 生成结果
     */
    private HookResult hookResult;

    /**
     * 脚本大纲（分镜规划）
     */
    private ScriptOutlineResult outline;

    /**
     * 脚本正文（各分镜详细内容）
     */
    private ScriptContentResult contentResult;

    /**
     * 脚本审核结果
     */
    private ScriptReviewResult reviewResult;

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
     * Hook 结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HookResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String hookText;
        private String hookType;
        private Integer durationSeconds;
        private String visualDescription;
    }

    /**
     * 脚本大纲结果（分镜规划）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScriptOutlineResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private List<SceneInfo> scenes;
        private Integer totalDuration;
        private String pacing;
    }

    /**
     * 分镜信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SceneInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer sceneIndex;
        private String startTime;
        private String endTime;
        private Integer duration;
        private String sceneType;
        private String description;
        private String visualNotes;
    }

    /**
     * 脚本正文结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScriptContentResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private List<SceneDetail> scenes;
        private String fullScript;
    }

    /**
     * 分镜详细内容
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SceneDetail implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer sceneIndex;
        private String startTime;
        private String endTime;
        private String script;
        private String visualDescription;
        private String subtitle;
        private String transition;
    }

    /**
     * 脚本审核结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScriptReviewResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer reviewScore;
        private List<String> suggestions;
        private String rewrittenScript;
        private ScriptReviewDimensions dimensions;
    }

    /**
     * 脚本审核维度
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScriptReviewDimensions implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer attraction;
        private Integer pacing;
        private Integer density;
        private Integer completeness;
        private Integer platformFit;
    }

    /**
     * 脚本合成最终结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScriptMergeResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String scriptStructure;
        private String markdownContent;
        private String plainTextContent;
    }

    // endregion
}
