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


-- ============================================================
-- Phase 2 核心体验 - 数据库变更
-- ============================================================

-- 1. article 表新增 state_snapshot 字段（断点续传）
ALTER TABLE article
    ADD COLUMN state_snapshot TEXT NULL COMMENT '状态快照（JSON，断点续传用）' AFTER error_message;

-- 2. article 表新增 is_favorited 字段（收藏功能）
ALTER TABLE article
    ADD COLUMN is_favorited TINYINT NOT NULL DEFAULT 0 COMMENT '收藏状态：0-未收藏 1-已收藏' AFTER state_snapshot;

-- 3. article 表新增 tags 字段（标签系统）
ALTER TABLE article
    ADD COLUMN tags JSON NULL COMMENT '文章标签（JSON数组）' AFTER is_favorited;

-- 4. 为收藏查询添加索引
ALTER TABLE article
    ADD INDEX idx_user_id_favorited (user_id, is_favorited);

-- 5. 创建模板表
CREATE TABLE IF NOT EXISTS `template`
(
    `id`                      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `name`                    VARCHAR(100) NOT NULL COMMENT '模板名称',
    `category`                VARCHAR(50)  NOT NULL COMMENT '分类：marketing/knowledge/story/review',
    `platform`                VARCHAR(50)  NOT NULL COMMENT '平台：wechat/xiaohongshu/douyin/weibo',
    `style`                   VARCHAR(20)           DEFAULT NULL COMMENT '文章风格：tech/emotional/educational/humorous',
    `topic_example`           VARCHAR(500)          DEFAULT NULL COMMENT '示例选题',
    `recommended_image_methods` JSON                DEFAULT NULL COMMENT '推荐配图方式（JSON数组）',
    `description`             VARCHAR(500)          DEFAULT NULL COMMENT '模板描述',
    `sort_order`              INT          NOT NULL DEFAULT 0 COMMENT '排序顺序',
    `status`                  TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `create_time`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`               TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_platform` (`platform`),
    INDEX `idx_status` (`status`),
    INDEX `idx_sort_order` (`sort_order`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='文案模板表';

-- 6. 插入示例模板数据
INSERT INTO `template` (`name`, `category`, `platform`, `style`, `topic_example`, `description`, `sort_order`) VALUES
('公众号爆款标题', 'marketing', 'wechat', 'emotional', '如何在30天内改变自己', '适用于公众号推文的爆款标题模板，吸引读者点击', 1),
('小红书种草笔记', 'review', 'xiaohongshu', 'emotional', '这款护肤品让我皮肤变好了', '小红书风格的种草笔记模板，真实感强', 2),
('抖音文案脚本', 'marketing', 'douyin', 'humorous', '程序员的日常崩溃瞬间', '适合短视频的轻松幽默文案', 3),
('知识科普长文', 'knowledge', 'wechat', 'educational', 'AI是如何工作的', '深度科普文章模板，结构清晰易懂', 4),
('产品评测文章', 'review', 'xiaohongshu', 'tech', 'iPhone 16 值得买吗', '客观产品评测模板，有理有据', 5),
('情感故事文', 'story', 'wechat', 'emotional', '那些年我们错过的爱情', '情感类故事文模板，引发共鸣', 6),
('微博热点评论', 'marketing', 'weibo', 'humorous', '今天的热搜太离谱了', '微博热点评论模板，简短有趣', 7),
('行业分析报告', 'knowledge', 'wechat', 'tech', '2026年AI行业趋势分析', '专业行业分析模板，数据驱动', 8);

-- ============================================================
-- Phase 3 质量与商业化 - 数据库变更
-- ============================================================

-- 1. article 表新增审核相关字段
ALTER TABLE article
    ADD COLUMN review_score INT NULL COMMENT '内容质量评分（0-100）' AFTER tags;

ALTER TABLE article
    ADD COLUMN review_suggestions JSON NULL COMMENT '审核改进建议（JSON数组）' AFTER review_score;

-- 2. user 表新增 vip_level 字段（VIP 分级：0=普通, 1=基础, 2=专业, 3=旗舰）
ALTER TABLE user
    ADD COLUMN vip_level TINYINT NOT NULL DEFAULT 0 COMMENT 'VIP等级：0-普通用户 1-基础版 2-专业版 3-旗舰版' AFTER vip_type;

-- 3. 为 vip_level 添加索引
ALTER TABLE user
    ADD INDEX idx_vip_level (vip_level);

-- ============================================================
-- Phase 4 v2.0 内容类型扩展 - 数据库变更
-- ============================================================

-- 1. article 表新增内容类型字段
ALTER TABLE article
    ADD COLUMN content_type VARCHAR(30) NOT NULL DEFAULT 'ARTICLE' COMMENT '内容类型：ARTICLE/SHORT_VIDEO_SCRIPT/LIVE_SCRIPT/INTERVIEW_SCRIPT/EVENT_SCRIPT/DRAMA_SCRIPT' AFTER review_suggestions;

-- 2. article 表新增脚本结构字段（JSON格式存储分镜/环节/对话等结构化数据）
ALTER TABLE article
    ADD COLUMN script_structure TEXT NULL COMMENT '脚本结构（JSON，存储分镜/环节/对话等）' AFTER content_type;

-- 3. 为内容类型添加索引
ALTER TABLE article
    ADD INDEX idx_content_type (content_type);

-- 4. article 表新增平台和时长字段（脚本类型使用）
ALTER TABLE article
    ADD COLUMN platform VARCHAR(30) NULL COMMENT '平台：douyin/bilibili/weixin_video' AFTER script_structure;

ALTER TABLE article
    ADD COLUMN duration VARCHAR(20) NULL COMMENT '时长：15s/30s/60s/3min/1h/2h/4h' AFTER platform;

-- 4. 创建脚本模板表
CREATE TABLE IF NOT EXISTS `script_template`
(
    `id`                      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `name`                    VARCHAR(100) NOT NULL COMMENT '模板名称',
    `content_type`            VARCHAR(30)  NOT NULL COMMENT '内容类型：SHORT_VIDEO_SCRIPT/LIVE_SCRIPT',
    `platform`                VARCHAR(50)  NOT NULL COMMENT '平台：douyin/bilibili/weixin_video',
    `duration`                VARCHAR(20)           DEFAULT NULL COMMENT '时长：15s/30s/60s/3min/1h/2h/4h',
    `style`                   VARCHAR(20)           DEFAULT NULL COMMENT '风格：funny/knowledge/emotional/product',
    `topic_example`           VARCHAR(500)          DEFAULT NULL COMMENT '示例选题',
    `description`             VARCHAR(500)          DEFAULT NULL COMMENT '模板描述',
    `structure_template`      TEXT                  DEFAULT NULL COMMENT '结构模板（JSON）',
    `sort_order`              INT          NOT NULL DEFAULT 0 COMMENT '排序顺序',
    `status`                  TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `create_time`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`               TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_content_type` (`content_type`),
    INDEX `idx_platform` (`platform`),
    INDEX `idx_status` (`status`),
    INDEX `idx_sort_order` (`sort_order`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='脚本模板表';

-- 5. 插入短视频脚本模板数据
INSERT INTO `script_template` (`name`, `content_type`, `platform`, `duration`, `style`, `topic_example`, `description`, `sort_order`) VALUES
('抖音搞笑短片', 'SHORT_VIDEO_SCRIPT', 'douyin', '15s', 'funny', '程序员的日常崩溃瞬间', '适合抖音的轻松搞笑短视频脚本', 1),
('B站知识科普', 'SHORT_VIDEO_SCRIPT', 'bilibili', '3min', 'knowledge', 'AI是如何工作的', 'B站风格的知识科普视频脚本', 2),
('视频号情感故事', 'SHORT_VIDEO_SCRIPT', 'weixin_video', '60s', 'emotional', '那些年我们错过的爱情', '视频号情感类短视频脚本', 3),
('抖音产品种草', 'SHORT_VIDEO_SCRIPT', 'douyin', '30s', 'product', '这款护肤品让我皮肤变好了', '抖音产品种草视频脚本', 4),
('B站游戏解说', 'SHORT_VIDEO_SCRIPT', 'bilibili', '3min', 'funny', '这游戏太上头了', 'B站游戏解说视频脚本', 5),
('抖音舞蹈挑战', 'SHORT_VIDEO_SCRIPT', 'douyin', '15s', 'funny', '最火舞蹈挑战', '抖音舞蹈挑战视频脚本', 6),
('视频号生活技巧', 'SHORT_VIDEO_SCRIPT', 'weixin_video', '60s', 'knowledge', '10个超实用生活小技巧', '视频号生活技巧视频脚本', 7),
('B站美食制作', 'SHORT_VIDEO_SCRIPT', 'bilibili', '3min', 'knowledge', '在家也能做出餐厅级美食', 'B站美食制作视频脚本', 8),
('抖音旅行Vlog', 'SHORT_VIDEO_SCRIPT', 'douyin', '60s', 'emotional', '周末去哪玩', '抖音旅行Vlog视频脚本', 9),
('视频号职场干货', 'SHORT_VIDEO_SCRIPT', 'weixin_video', '3min', 'knowledge', '面试技巧分享', '视频号职场干货视频脚本', 10);

-- 6. 插入直播台本模板数据
INSERT INTO `script_template` (`name`, `content_type`, `platform`, `duration`, `style`, `topic_example`, `description`, `sort_order`) VALUES
('电商带货台本', 'LIVE_SCRIPT', 'douyin', '2h', 'product', '护肤品专场直播', '电商带货直播台本模板', 11),
('知识分享台本', 'LIVE_SCRIPT', 'bilibili', '1h', 'knowledge', 'AI技术分享直播', '知识分享类直播台本模板', 12),
('活动发布台本', 'LIVE_SCRIPT', 'weixin_video', '2h', 'emotional', '新品发布会直播', '活动发布类直播台本模板', 13),
('游戏直播台本', 'LIVE_SCRIPT', 'douyin', '4h', 'funny', '新游戏试玩直播', '游戏直播台本模板', 14),
('美食探店台本', 'LIVE_SCRIPT', 'douyin', '1h', 'emotional', '网红餐厅探店', '美食探店直播台本模板', 15),
('美妆教程台本', 'LIVE_SCRIPT', 'douyin', '2h', 'product', '日常妆容教程', '美妆教程直播台本模板', 16),
('健身教学台本', 'LIVE_SCRIPT', 'bilibili', '1h', 'knowledge', '居家健身指导', '健身教学直播台本模板', 17),
('读书分享台本', 'LIVE_SCRIPT', 'weixin_video', '2h', 'knowledge', '好书推荐分享', '读书分享直播台本模板', 18),
('音乐表演台本', 'LIVE_SCRIPT', 'douyin', '2h', 'emotional', '音乐直播表演', '音乐表演直播台本模板', 19),
('聊天互动台本', 'LIVE_SCRIPT', 'douyin', '4h', 'funny', '粉丝互动聊天', '聊天互动直播台本模板', 20);

-- ============================================================
-- Phase 5 长期迭代 - 数据库变更
-- ============================================================

-- 1. 文章反馈表
CREATE TABLE IF NOT EXISTS `article_feedback`
(
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `article_id`  BIGINT   NOT NULL COMMENT '文章ID',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `rating`      TINYINT  NOT NULL COMMENT '评分：1=满意 0=不满意',
    `comment`     VARCHAR(500) DEFAULT NULL COMMENT '评论内容',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_article_id` (`article_id`),
    INDEX `idx_user_id` (`user_id`),
    UNIQUE INDEX `uk_article_user` (`article_id`, `user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='文章反馈表';

-- 2. 新内容类型模板数据（访谈脚本 10 条）
INSERT INTO `script_template` (`name`, `content_type`, `platform`, `duration`, `style`, `topic_example`, `description`, `sort_order`) VALUES
('播客访谈', 'INTERVIEW_SCRIPT', 'podcast', '30min', 'professional', 'AI 对创业者意味着什么', '播客风格的深度访谈脚本', 21),
('人物专访', 'INTERVIEW_SCRIPT', 'podcast', '60min', 'emotional', '创业者的十年心路', '人物专访风格的访谈脚本', 22),
('行业对话', 'INTERVIEW_SCRIPT', 'podcast', '45min', 'professional', '2026年AI行业趋势', '行业深度对话访谈脚本', 23),
('轻松聊天', 'INTERVIEW_SCRIPT', 'podcast', '20min', 'humorous', '年轻人的职场困惑', '轻松聊天风格的访谈脚本', 24),
('专家解读', 'INTERVIEW_SCRIPT', 'podcast', '30min', 'educational', '如何高效学习新技能', '专家解读风格的访谈脚本', 25),
('圆桌讨论', 'INTERVIEW_SCRIPT', 'podcast', '60min', 'professional', '创业者圆桌：机遇与挑战', '多人圆桌讨论访谈脚本', 26),
('用户故事', 'INTERVIEW_SCRIPT', 'podcast', '20min', 'emotional', '用户如何改变产品', '用户故事风格的访谈脚本', 27),
('技术深聊', 'INTERVIEW_SCRIPT', 'podcast', '45min', 'professional', '从零到一的技术架构', '技术深度访谈脚本', 28),
('文化访谈', 'INTERVIEW_SCRIPT', 'podcast', '30min', 'emotional', '传统文化的现代传承', '文化类访谈脚本', 29),
('热点对话', 'INTERVIEW_SCRIPT', 'podcast', '20min', 'humorous', '今天的热搜怎么看', '热点话题对话访谈脚本', 30);

-- 3. 新内容类型模板数据（活动台本 10 条）
INSERT INTO `script_template` (`name`, `content_type`, `platform`, `duration`, `style`, `topic_example`, `description`, `sort_order`) VALUES
('产品发布会', 'EVENT_SCRIPT', 'offline', '2h', 'professional', '新品发布会', '产品发布会完整台本', 31),
('年度盛典', 'EVENT_SCRIPT', 'offline', '3h', 'emotional', '公司年会', '年度盛典活动台本', 32),
('技术峰会', 'EVENT_SCRIPT', 'offline', '4h', 'professional', 'AI技术峰会', '技术峰会活动台本', 33),
('开业典礼', 'EVENT_SCRIPT', 'offline', '1h', 'emotional', '新店开业', '开业典礼活动台本', 34),
('颁奖典礼', 'EVENT_SCRIPT', 'offline', '2h', 'emotional', '年度颁奖典礼', '颁奖典礼活动台本', 35),
('培训讲座', 'EVENT_SCRIPT', 'offline', '3h', 'educational', '新员工培训', '培训讲座活动台本', 36),
('签约仪式', 'EVENT_SCRIPT', 'offline', '1h', 'professional', '战略合作签约', '签约仪式活动台本', 37),
('客户答谢会', 'EVENT_SCRIPT', 'offline', '2h', 'emotional', 'VIP客户答谢', '客户答谢会活动台本', 38),
('路演活动', 'EVENT_SCRIPT', 'offline', '1h', 'professional', '项目路演', '路演活动台本', 39),
('团建活动', 'EVENT_SCRIPT', 'offline', '4h', 'humorous', '团队建设活动', '团建活动台本', 40);

-- 4. 新内容类型模板数据（剧本 10 条）
INSERT INTO `script_template` (`name`, `content_type`, `platform`, `duration`, `style`, `topic_example`, `description`, `sort_order`) VALUES
('都市情感短剧', 'DRAMA_SCRIPT', 'short_drama', '5min', 'emotional', '错过与重逢', '都市情感类短剧剧本', 41),
('搞笑日常短剧', 'DRAMA_SCRIPT', 'short_drama', '3min', 'funny', '办公室的奇葩日常', '搞笑日常短剧剧本', 42),
('悬疑微电影', 'DRAMA_SCRIPT', 'short_drama', '10min', 'thriller', '消失的第七天', '悬疑微电影剧本', 43),
('青春校园短剧', 'DRAMA_SCRIPT', 'short_drama', '5min', 'emotional', '毕业季的告白', '青春校园短剧剧本', 44),
('职场短剧', 'DRAMA_SCRIPT', 'short_drama', '5min', 'professional', '实习生的逆袭', '职场题材短剧剧本', 45),
('古风短剧', 'DRAMA_SCRIPT', 'short_drama', '8min', 'emotional', '长安夜雨', '古风题材短剧剧本', 46),
('科幻微电影', 'DRAMA_SCRIPT', 'short_drama', '10min', 'professional', '最后的信号', '科幻微电影剧本', 47),
('家庭伦理短剧', 'DRAMA_SCRIPT', 'short_drama', '5min', 'emotional', '回家的路', '家庭伦理短剧剧本', 48),
('穿越短剧', 'DRAMA_SCRIPT', 'short_drama', '8min', 'funny', '穿越回古代当网红', '穿越题材短剧剧本', 49),
('励志短剧', 'DRAMA_SCRIPT', 'short_drama', '5min', 'emotional', '从零开始', '励志题材短剧剧本', 50);

-- 5. Prompt 模板管理表
CREATE TABLE IF NOT EXISTS `prompt_template`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`           VARCHAR(100) NOT NULL COMMENT '模板名称',
    `agent_name`     VARCHAR(50)  NOT NULL COMMENT '所属智能体名称',
    `prompt_content` TEXT         NOT NULL COMMENT 'Prompt内容',
    `version`        INT          NOT NULL DEFAULT 1 COMMENT '版本号',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    INDEX `idx_agent_name` (`agent_name`),
    INDEX `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Prompt模板管理表';
