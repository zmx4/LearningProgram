-- 通知增加跳转地址，便于「文章被评论」这类通知直接点进对应页面
ALTER TABLE db_notification
    ADD COLUMN link VARCHAR(255) DEFAULT NULL AFTER content;

CREATE TABLE db_discussion_post
(
    id            INT(11)      NOT NULL AUTO_INCREMENT,
    account_id    INT(11)      NOT NULL,
    title         VARCHAR(150) NOT NULL,
    content       TEXT         NOT NULL,
    comment_count INT          NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_discussion_post_account FOREIGN KEY (account_id) REFERENCES db_account (id),
    INDEX idx_discussion_post_created (created_at)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 评论是平铺的（不支持楼中楼），按 post_id + created_at 建索引便于按时间正序读取
CREATE TABLE db_discussion_comment
(
    id         INT(11)       NOT NULL AUTO_INCREMENT,
    post_id    INT(11)       NOT NULL,
    account_id INT(11)       NOT NULL,
    content    VARCHAR(1000) NOT NULL,
    created_at DATETIME      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_discussion_comment_post FOREIGN KEY (post_id) REFERENCES db_discussion_post (id) ON DELETE CASCADE,
    CONSTRAINT fk_discussion_comment_account FOREIGN KEY (account_id) REFERENCES db_account (id),
    INDEX idx_discussion_comment_post (post_id, created_at)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;
