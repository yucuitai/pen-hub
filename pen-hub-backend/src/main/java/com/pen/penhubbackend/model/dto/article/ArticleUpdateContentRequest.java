package com.pen.penhubbackend.model.dto.article;

import lombok.Data;

import java.io.Serializable;

/**
 * 文章内容更新请求
 */
@Data
public class ArticleUpdateContentRequest implements Serializable {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 新的 Markdown 内容
     */
    private String content;

    private static final long serialVersionUID = 1L;
}
