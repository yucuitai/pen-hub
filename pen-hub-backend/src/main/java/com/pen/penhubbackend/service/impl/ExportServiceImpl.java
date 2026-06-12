package com.pen.penhubbackend.service.impl;

import com.pen.penhubbackend.exception.BusinessException;
import com.pen.penhubbackend.exception.ErrorCode;
import com.pen.penhubbackend.model.entity.Article;
import com.pen.penhubbackend.service.ExportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 文章导出服务实现
 */
@Service
@Slf4j
public class ExportServiceImpl implements ExportService {

    @Override
    public byte[] exportToPdf(Article article) {
        try {
            String htmlContent = buildHtmlContent(article);
            // 使用 OpenHTMLToPDF 生成 PDF
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                org.xhtmlrenderer.pdf.ITextRenderer renderer = new org.xhtmlrenderer.pdf.ITextRenderer();
                renderer.setDocumentFromString(htmlContent);
                renderer.layout();
                renderer.createPDF(os);
                return os.toByteArray();
            }
        } catch (Exception e) {
            log.error("PDF 导出失败: taskId={}", article.getTaskId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "PDF 导出失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] exportToDocx(Article article) {
        try (XWPFDocument document = new XWPFDocument()) {
            // 添加标题
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText(article.getMainTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(22);

            // 添加副标题
            if (article.getSubTitle() != null && !article.getSubTitle().isEmpty()) {
                XWPFParagraph subtitleParagraph = document.createParagraph();
                subtitleParagraph.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun subtitleRun = subtitleParagraph.createRun();
                subtitleRun.setText(article.getSubTitle());
                subtitleRun.setColor("666666");
                subtitleRun.setFontSize(14);
            }

            // 添加分隔线
            XWPFParagraph separator = document.createParagraph();
            separator.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun separatorRun = separator.createRun();
            separatorRun.setText("————————————————————");

            // 添加正文内容
            String content = article.getFullContent() != null ? article.getFullContent() : article.getContent();
            if (content != null && !content.isEmpty()) {
                addMarkdownContent(document, content);
            }

            // 写入字节数组
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                document.write(os);
                return os.toByteArray();
            }
        } catch (IOException e) {
            log.error("Word 导出失败: taskId={}", article.getTaskId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Word 导出失败: " + e.getMessage());
        }
    }

    /**
     * 构建 HTML 内容（用于 PDF 导出）
     */
    private String buildHtmlContent(Article article) {
        String content = article.getFullContent() != null ? article.getFullContent() : article.getContent();
        String htmlBody = markdownToHtml(content != null ? content : "");

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8"/>
                    <style>
                        @page { margin: 2cm; }
                        body {
                            font-family: 'Noto Sans SC', 'Microsoft YaHei', sans-serif;
                            font-size: 12pt;
                            line-height: 1.8;
                            color: #333;
                        }
                        h1 { font-size: 22pt; text-align: center; margin-bottom: 8pt; }
                        h2 { font-size: 16pt; margin: 18pt 0 10pt; border-bottom: 1px solid #eee; padding-bottom: 6pt; }
                        h3 { font-size: 14pt; margin: 14pt 0 8pt; }
                        p { margin-bottom: 10pt; text-indent: 0; }
                        img { max-width: 100%%; display: block; margin: 12pt auto; }
                        blockquote { border-left: 3px solid #D97706; padding-left: 10pt; color: #666; margin: 12pt 0; }
                        ul, ol { padding-left: 2em; margin-bottom: 10pt; }
                        li { margin-bottom: 4pt; }
                        code { background: #f5f5f5; padding: 1pt 4pt; border-radius: 2pt; font-size: 10pt; }
                        pre { background: #1e1e1e; color: #d4d4d4; padding: 10pt; border-radius: 4pt; overflow-x: auto; }
                        pre code { background: transparent; color: inherit; }
                        .subtitle { text-align: center; color: #666; font-size: 14pt; margin-bottom: 16pt; }
                    </style>
                </head>
                <body>
                    <h1>%s</h1>
                    <p class="subtitle">%s</p>
                    %s
                </body>
                </html>
                """.formatted(
                escapeHtml(article.getMainTitle()),
                escapeHtml(article.getSubTitle() != null ? article.getSubTitle() : ""),
                htmlBody
        );
    }

    /**
     * 简单的 Markdown 转 HTML
     */
    private String markdownToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }

        String html = markdown
                // 标题
                .replaceAll("^### (.+)$", "<h3>$1</h3>")
                .replaceAll("^## (.+)$", "<h2>$1</h2>")
                .replaceAll("^# (.+)$", "<h1>$1</h1>")
                // 粗体和斜体
                .replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>")
                .replaceAll("\\*(.+?)\\*", "<em>$1</em>")
                // 图片
                .replaceAll("!\\[(.+?)\\]\\((.+?)\\)", "<img src=\"$2\" alt=\"$1\"/>")
                // 链接
                .replaceAll("\\[(.+?)\\]\\((.+?)\\)", "<a href=\"$2\">$1</a>")
                // 代码块
                .replaceAll("```[\\s\\S]*?```", "<pre><code>$0</code></pre>")
                // 行内代码
                .replaceAll("`(.+?)`", "<code>$1</code>")
                // 引用
                .replaceAll("^> (.+)$", "<blockquote>$1</blockquote>")
                // 无序列表
                .replaceAll("^[-*+] (.+)$", "<li>$1</li>")
                // 段落（换行）
                .replaceAll("\n\n", "</p><p>")
                .replaceAll("\n", "<br/>");

        return "<p>" + html + "</p>";
    }

    /**
     * HTML 转义
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * 将 Markdown 内容添加到 Word 文档
     */
    private void addMarkdownContent(XWPFDocument document, String markdown) {
        String[] lines = markdown.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("### ")) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(line.substring(4));
                run.setBold(true);
                run.setFontSize(13);
            } else if (line.startsWith("## ")) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(line.substring(3));
                run.setBold(true);
                run.setFontSize(15);
            } else if (line.startsWith("# ")) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(line.substring(2));
                run.setBold(true);
                run.setFontSize(18);
            } else if (line.startsWith("> ")) {
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.setIndentationLeft(720); // 1 inch indent
                XWPFRun run = paragraph.createRun();
                run.setText(line.substring(2));
                run.setColor("666666");
                run.setItalic(true);
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText("• " + line.substring(2));
            } else if (line.matches("^\\d+\\. .+$")) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(line);
            } else if (line.startsWith("![") && line.contains("](")) {
                // 图片占位符
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun run = paragraph.createRun();
                String altText = line.replaceAll("!\\[(.+?)\\]\\(.+?\\)", "$1");
                run.setText("[图片: " + altText + "]");
                run.setColor("999999");
                run.setItalic(true);
            } else {
                // 普通段落
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                // 移除 Markdown 格式
                String plainText = line
                        .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                        .replaceAll("\\*(.+?)\\*", "$1")
                        .replaceAll("`(.+?)`", "$1")
                        .replaceAll("\\[(.+?)\\]\\(.+?\\)", "$1");
                run.setText(plainText);
                run.setFontSize(11);
            }
        }
    }
}
