-- 创建数据库

CREATE DATABASE IF NOT EXISTS pen_hub DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pen_hub;

-- 用户表
CREATE TABLE IF NOT EXISTS `user`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `user_account`  VARCHAR(256) NOT NULL COMMENT '账号',
    `user_password` VARCHAR(512) NOT NULL COMMENT '密码',
    `user_nickname` VARCHAR(256) NOT NULL DEFAULT '' COMMENT '用户昵称',
    `user_avatar`   VARCHAR(1024)         DEFAULT NULL COMMENT '用户头像URL',
    `user_profile`  VARCHAR(512)          DEFAULT NULL COMMENT '用户简介',
    `user_role`     VARCHAR(256)  NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
    `edit_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_account` (`user_account`),
    INDEX `idx_user_role` (`user_role`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户表';

-- 初始化测试数据（密码是 12345678，MD5 加密 + 盐值 yupi）
# INSERT INTO user (id, userAccount, userPassword, userName, userAvatar, userProfile, userRole) VALUES
# (1, 'admin', '10670d38ec32fa8102be6a37f8cb52bf', '管理员', 'https://www.codefather.cn/logo.png', '系统管理员', 'admin'),
# (2, 'user', '10670d38ec32fa8102be6a37f8cb52bf', '普通用户', 'https://www.codefather.cn/logo.png', '我是一个普通用户', 'user'),
# (3, 'test', '10670d38ec32fa8102be6a37f8cb52bf', '测试账号', 'https://www.codefather.cn/logo.png', '这是一个测试账号', 'user');

-- 文章表
CREATE TABLE IF NOT EXISTS `article`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文章ID',
    `task_id`        VARCHAR(64)  NOT NULL COMMENT '任务ID（UUID）',
    `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
    `topic`          VARCHAR(500) NOT NULL COMMENT '选题',
    `main_title`     VARCHAR(200)          DEFAULT NULL COMMENT '主标题',
    `sub_title`      VARCHAR(300)          DEFAULT NULL COMMENT '副标题',
    `outline`        JSON                  DEFAULT NULL COMMENT '大纲（JSON格式）',
    `content`        TEXT                  DEFAULT NULL COMMENT '正文（Markdown格式）',
    `full_content`   TEXT                  DEFAULT NULL COMMENT '完整图文（Markdown格式，含配图）',
    `cover_image`    VARCHAR(512)          DEFAULT NULL COMMENT '封面图URL',
    `images`         JSON                  DEFAULT NULL COMMENT '配图列表（JSON数组）',
    `status`         VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/COMPLETED/FAILED',
    `error_message`  TEXT                  DEFAULT NULL COMMENT '错误信息',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `completed_time` DATETIME              DEFAULT NULL COMMENT '完成时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_id` (`task_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_user_id_status` (`user_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='文章表';
