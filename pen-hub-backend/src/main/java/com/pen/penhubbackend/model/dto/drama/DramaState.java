package com.pen.penhubbackend.model.dto.drama;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 剧本状态对象
 * 用于 StateGraph 中传递剧本生成各阶段的数据
 *
 * @author pen-hub
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DramaState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 选题/主题
     */
    private String topic;

    /**
     * 平台
     */
    private String platform;

    /**
     * 时长
     */
    private String duration;

    /**
     * 类型：romance/comedy/thriller/etc
     */
    private String genre;

    /**
     * 风格
     */
    private String style;

    /**
     * 角色设定结果
     */
    private DramaCharacterResult characterResult;

    /**
     * 剧情大纲结果
     */
    private DramaPlotResult plotResult;

    /**
     * 台词场景结果
     */
    private DramaScriptResult scriptResult;

    /**
     * 剧本审核结果
     */
    private DramaReviewResult reviewResult;

    /**
     * 最终剧本结构（JSON）
     */
    private String scriptStructure;

    /**
     * Markdown 格式剧本
     */
    private String markdownContent;

    /**
     * 纯文本格式剧本
     */
    private String plainTextContent;

    // region 内部类

    /**
     * 角色设定结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DramaCharacterResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private List<Character> characters;
    }

    /**
     * 角色信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Character implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String name;
        private String role;
        private String personality;
        private String background;
    }

    /**
     * 剧情大纲结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DramaPlotResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private List<Act> acts;
    }

    /**
     * 幕/章节信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Act implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer actIndex;
        private String title;
        private List<Scene> scenes;
        private List<String> conflictPoints;
    }

    /**
     * 场景信息（大纲级别）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Scene implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String description;
        private String location;
        private String time;
    }

    /**
     * 台词场景结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DramaScriptResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private List<ScriptScene> scenes;
        private String fullScript;
    }

    /**
     * 台词场景详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScriptScene implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer sceneIndex;
        private String location;
        private String time;
        private List<String> characters;
        private List<Dialogue> dialogue;
    }

    /**
     * 对话信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dialogue implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String speaker;
        private String line;
        private String action;
    }

    /**
     * 剧本审核结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DramaReviewResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer reviewScore;
        private List<String> suggestions;
        private String rewrittenScript;
        private DramaReviewDimensions dimensions;
    }

    /**
     * 剧本审核维度
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DramaReviewDimensions implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer plotCoherence;
        private Integer characterConsistency;
        private Integer dialogueNaturalism;
        private Integer pacing;
        private Integer emotionalImpact;
    }

    // endregion
}
