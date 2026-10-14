-- 学习小组：小组、成员与消息三张表。
-- 一个人可以加入或创建多个小组；组长同样占一行成员记录（role = owner），便于统一成员关系与权限判断。
-- invite_code 为组长分享给组员的入群凭证（8 位，去除易混淆字符）。
-- message.type：text 普通消息（组长组员均可发），task 学习任务（仅组长可发，带 title）。
CREATE TABLE db_study_group
(
    id          INT(11)     NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL,
    owner_id    INT(11)     NOT NULL,
    invite_code VARCHAR(8)  NOT NULL,
    created_at  DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_study_group_invite_code (invite_code)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE db_study_group_member
(
    id         INT(11)     NOT NULL AUTO_INCREMENT,
    group_id   INT(11)     NOT NULL,
    account_id INT(11)     NOT NULL,
    role       VARCHAR(10) NOT NULL,
    joined_at  DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_study_group_member (group_id, account_id),
    INDEX idx_study_group_member_account (account_id),
    CONSTRAINT fk_study_group_member_group FOREIGN KEY (group_id) REFERENCES db_study_group (id) ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE db_study_group_message
(
    id         INT(11)       NOT NULL AUTO_INCREMENT,
    group_id   INT(11)       NOT NULL,
    account_id INT(11)       NOT NULL,
    type       VARCHAR(10)   NOT NULL,
    title      VARCHAR(100)  DEFAULT NULL,
    content    VARCHAR(2000) NOT NULL,
    created_at DATETIME      NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_study_group_message_group (group_id, id),
    CONSTRAINT fk_study_group_message_group FOREIGN KEY (group_id) REFERENCES db_study_group (id) ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;
