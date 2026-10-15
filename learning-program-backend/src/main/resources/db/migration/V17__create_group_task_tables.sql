-- 小组学习任务扩展：任务项（绑定课程/资源/组员访问/纯文本）与成员完成进度。
-- 任务本体仍是 db_study_group_message 中 type = task 的消息，本组表以 message_id 挂接任务项。
-- item_type：course（完成课程全部章节，自动判定）、resource（任务发布后访问过资源，自动判定）、
--            member_visit（任务发布后访问过小组详情页，自动判定）、text（组员手动标记完成）。
CREATE TABLE db_group_task_item
(
    id         INT(11)      NOT NULL AUTO_INCREMENT,
    group_id   INT(11)      NOT NULL,
    message_id INT(11)      NOT NULL,
    item_type  VARCHAR(20)  NOT NULL,
    ref_id     INT(11)      DEFAULT NULL,
    title      VARCHAR(200) NOT NULL,
    sort_order INT(11)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_group_task_item_message (message_id),
    INDEX idx_group_task_item_group (group_id),
    CONSTRAINT fk_group_task_item_message FOREIGN KEY (message_id)
        REFERENCES db_study_group_message (id) ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 手动标记类任务项（text）的完成记录；自动判定类（course/resource/member_visit）不落此表。
CREATE TABLE db_group_task_progress
(
    id           INT(11)  NOT NULL AUTO_INCREMENT,
    item_id      INT(11)  NOT NULL,
    account_id   INT(11)  NOT NULL,
    completed_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_task_progress (item_id, account_id),
    CONSTRAINT fk_group_task_progress_item FOREIGN KEY (item_id)
        REFERENCES db_group_task_item (id) ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 资源访问流水：下载接口落记录，用于 resource 任务项的完成判定。
CREATE TABLE db_resource_access_log
(
    id          INT(11)  NOT NULL AUTO_INCREMENT,
    account_id  INT(11)  NOT NULL,
    resource_id INT(11)  NOT NULL,
    accessed_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_resource_access_log_account (account_id, resource_id),
    INDEX idx_resource_access_log_resource (resource_id)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 小组成员最近一次访问小组详情页的时间，用于 member_visit 任务项的完成判定。
CREATE TABLE db_group_member_visit
(
    id            INT(11)  NOT NULL AUTO_INCREMENT,
    group_id      INT(11)  NOT NULL,
    account_id    INT(11)  NOT NULL,
    last_visit_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_member_visit (group_id, account_id),
    CONSTRAINT fk_group_member_visit_group FOREIGN KEY (group_id)
        REFERENCES db_study_group (id) ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;
