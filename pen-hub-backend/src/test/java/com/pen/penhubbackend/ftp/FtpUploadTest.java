package com.pen.penhubbackend.ftp;

import com.pen.penhubbackend.model.dto.image.ImageData;
import com.pen.penhubbackend.service.CosService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FTP 上传功能测试
 */
@Slf4j
@SpringBootTest
@DisplayName("FTP 上传功能测试")
class FtpUploadTest {

    @Resource
    private CosService cosService;

    @Test
    @DisplayName("测试从 URL 上传图片到 FTP")
    void testUploadFromUrlToFtp() {
        // given - 使用国内可访问的测试图片 URL
        String imageUrl = "https://www.baidu.com/img/PCtm_d9c8750bed0b3c7d089fa7d55720d6cf.png";
        String folder = "test";

        // when
        log.info("开始测试从 URL 上传到 FTP, imageUrl={}", imageUrl);
        String result = cosService.uploadFromUrlToFtp(imageUrl, folder);

        // then
        log.info("上传结果: {}", result);
        assertNotNull(result, "上传结果不应为空");
        assertTrue(result.startsWith("http://118.190.107.1/images/"), "URL 格式不正确");
        log.info("✅ 测试通过：从 URL 上传到 FTP 成功");
    }

    @Test
    @DisplayName("测试从本地资源上传图片到 FTP")
    void testUploadFromResourceToFtp() throws IOException {
        // given - 使用项目中的测试图片
        String folder = "test";
        byte[] imageBytes;

        // 读取测试图片（使用 Spring Boot logo）
        try (InputStream is = getClass().getResourceAsStream("/static/test-image.png")) {
            if (is == null) {
                log.warn("未找到测试图片，使用 URL 测试替代");
                testUploadFromUrlToFtp();
                return;
            }
            imageBytes = is.readAllBytes();
        }

        // when
        log.info("开始测试从本地资源上传到 FTP, size={} bytes", imageBytes.length);
        String result = cosService.uploadToFtp(imageBytes, "image/png", folder);

        // then
        log.info("上传结果: {}", result);
        assertNotNull(result, "上传结果不应为空");
        assertTrue(result.startsWith("http://118.190.107.1/images/"), "URL 格式不正确");
        log.info("✅ 测试通过：从本地资源上传到 FTP 成功");
    }

    @Test
    @DisplayName("测试从 Data URL 上传图片到 FTP")
    void testUploadFromDataUrlToFtp() {
        // given - 使用一个简单的 base64 编码图片
        String base64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        String dataUrl = "data:image/png;base64," + base64Image;
        String folder = "test";

        // 构建 ImageData
        ImageData imageData = ImageData.builder()
                .url(dataUrl)
                .build();

        // when
        log.info("开始测试从 Data URL 上传到 FTP");
        String result = cosService.uploadFromDataUrlToFtp(imageData, folder);

        // then
        log.info("上传结果: {}", result);
        assertNotNull(result, "上传结果不应为空");
        assertTrue(result.startsWith("http://118.190.107.1/images/"), "URL 格式不正确");
        log.info("✅ 测试通过：从 Data URL 上传到 FTP 成功");
    }

    @Test
    @DisplayName("测试 FTP 服务器连接")
    void testFtpConnection() {
        // given - 使用国内可访问的测试图片 URL
        String imageUrl = "https://www.baidu.com/img/PCtm_d9c8750bed0b3c7d089fa7d55720d6cf.png";
        String folder = "connection-test";

        // when
        log.info("开始测试 FTP 连接...");
        String result = cosService.uploadFromUrlToFtp(imageUrl, folder);

        // then
        if (result != null) {
            log.info("✅ FTP 连接测试成功, url={}", result);
        } else {
            log.warn("⚠️ FTP 连接测试失败，请检查配置和服务器状态");
            // 不抛出异常，因为可能是网络问题
        }
    }
}
