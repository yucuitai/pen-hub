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

-- 为 article 表添加 style 字段（文章风格）
ALTER TABLE article
    ADD COLUMN style VARCHAR(20) NULL COMMENT '文章风格：tech/emotional/educational/humorous' AFTER topic;

# 添加阶段相关字段
# @author <a href="https://codefather.cn">编程导航学习圈</a>

-- 为 article 表添加阶段相关字段
ALTER TABLE article
    ADD COLUMN phase VARCHAR(50) DEFAULT 'PENDING' COMMENT '当前阶段：PENDING/TITLE_GENERATING/TITLE_SELECTING/OUTLINE_GENERATING/OUTLINE_EDITING/CONTENT_GENERATING' AFTER status,
    ADD COLUMN title_options JSON NULL COMMENT '标题方案列表（3-5个方案）' AFTER sub_title,
    ADD COLUMN user_description TEXT NULL COMMENT '用户补充描述' AFTER topic,
    ADD COLUMN enabled_image_methods JSON NULL COMMENT '允许的配图方式列表' AFTER user_description;


-- 1. 扩展 user 表，添加会员相关字段
ALTER TABLE user
    ADD COLUMN vip_time DATETIME NULL COMMENT '成为会员时间';

-- 2. 创建支付记录表
CREATE TABLE IF NOT EXISTS payment_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    stripe_session_id VARCHAR(128) COMMENT 'Stripe Checkout Session ID',
    stripe_payment_intent_id VARCHAR(128) COMMENT 'Stripe 支付意向ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '金额（美元）',
    currency VARCHAR(8) DEFAULT 'usd' COMMENT '货币',
    status VARCHAR(32) NOT NULL COMMENT '状态：PENDING/SUCCEEDED/FAILED/REFUNDED',
    product_type VARCHAR(32) NOT NULL COMMENT '产品类型：VIP_PERMANENT',
    description VARCHAR(256) COMMENT '描述',
    refund_time DATETIME NULL COMMENT '退款时间',
    refund_reason VARCHAR(512) NULL COMMENT '退款原因',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_user_id (user_id),
    INDEX idx_stripe_session_id (stripe_session_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) COMMENT '支付记录表' COLLATE = utf8mb4_unicode_ci;

-- 3. 为 user 表添加 vip_type 字段
ALTER TABLE user
    ADD COLUMN vip_type VARCHAR(32) NULL COMMENT '会员类型：VIP_PERMANENT/VIP_MONTHLY/VIP_YEARLY' AFTER vip_time;

-- 4. 创建兑换码表
CREATE TABLE IF NOT EXISTS redemption_code (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '兑换码',
    product_type VARCHAR(32) NOT NULL COMMENT '产品类型：VIP_MONTHLY/VIP_YEARLY',
    max_uses INT DEFAULT 1 COMMENT '最大使用次数',
    used_count INT DEFAULT 0 COMMENT '已使用次数',
    status VARCHAR(32) NOT NULL COMMENT '状态：ACTIVE/DISABLED/EXHAUSTED',
    expire_time DATETIME NULL COMMENT '过期时间，NULL 表示永不过期',
    description VARCHAR(256) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE INDEX uk_code (code),
    INDEX idx_status (status),
    INDEX idx_product_type (product_type)
) COMMENT '兑换码表' COLLATE = utf8mb4_unicode_ci;

-- 5. 创建兑换记录表
CREATE TABLE IF NOT EXISTS redemption_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    code_id BIGINT NOT NULL COMMENT '兑换码ID',
    code VARCHAR(64) NOT NULL COMMENT '兑换码（冗余便于查询）',
    product_type VARCHAR(32) NOT NULL COMMENT '产品类型',
    status VARCHAR(32) NOT NULL COMMENT '状态：SUCCESS/FAILED',
    expire_time DATETIME NULL COMMENT '会员到期时间',
    description VARCHAR(256) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_user_id (user_id),
    INDEX idx_code_id (code_id),
    INDEX idx_status (status),
    INDEX idx_expire_time (expire_time)
) COMMENT '兑换记录表' COLLATE = utf8mb4_unicode_ci;

-- 添加 quota 字段
ALTER TABLE user ADD COLUMN quota int default 5 not null comment '剩余配额' AFTER user_role;

-- 为已有用户设置默认配额
UPDATE user SET quota = 5 WHERE quota IS NULL;


-- 智能体执行日志表
create table if not exists agent_log
(
    id              bigint auto_increment comment 'id' primary key,
    task_id          varchar(64)                        not null comment '任务ID',
    agent_name       varchar(50)                        not null comment '智能体名称',
    start_time       datetime                           not null comment '开始时间',
    end_time         datetime                           null comment '结束时间',
    duration_ms      int                                null comment '耗时（毫秒）',
    status          varchar(20)                        not null comment '状态：SUCCESS/FAILED',
    error_message    text                               null comment '错误信息',
    prompt          text                               null comment '使用的Prompt',
    input_data       json                               null comment '输入数据（JSON格式）',
    output_data      json                               null comment '输出数据（JSON格式）',
    create_time      datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time      datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete        tinyint     default 0              not null comment '是否删除',
    INDEX idx_task_id (task_id),
    INDEX idx_agent_name (agent_name),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) comment '智能体执行日志表' collate = utf8mb4_unicode_ci;
