package com.pen.penhubbackend.service;

import com.pen.penhubbackend.model.entity.Article;

/**
 * 文章导出服务接口
 */
public interface ExportService {

    /**
     * 导出为 PDF
     *
     * @param article 文章实体
     * @return PDF 字节数组
     */
    byte[] exportToPdf(Article article);

    /**
     * 导出为 Word (docx)
     *
     * @param article 文章实体
     * @return Word 文档字节数组
     */
    byte[] exportToDocx(Article article);
}
