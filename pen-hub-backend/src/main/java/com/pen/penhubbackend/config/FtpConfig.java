package com.pen.penhubbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * FTP 上传配置
 * 所有配置从 application.yml 中读取，不硬编码敏感信息
 */
@Data
@Component
@ConfigurationProperties(prefix = "ftp")
public class FtpConfig {

    /**
     * FTP 服务器地址
     */
    private String host;

    /**
     * FTP 端口
     */
    private int port;

    /**
     * FTP 用户名
     */
    private String username;

    /**
     * FTP 密码
     */
    private String password;

    /**
     * 是否使用被动模式
     */
    private boolean passiveMode;

    /**
     * 服务器上的基础路径
     */
    private String basePath;

    /**
     * 访问 URL 基础地址
     */
    private String baseUrl;
}
