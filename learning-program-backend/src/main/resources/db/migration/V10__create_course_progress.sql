-- 课程学习进度：章节粒度，每个用户对每章保存一条进度记录。
-- 学习完课程内全部章节即视为课程完成；duration_seconds 累计该章的学习时长。
-- course_id 冗余存储便于统计，一致性由章节的级联删除链条保证。
CREATE TABLE db_course_progress
(
    id               INT(11)  NOT NULL AUTO_INCREMENT,
    account_id       INT(11)  NOT NULL,
    course_id        INT(11)  NOT NULL,
    chapter_id       INT(11)  NOT NULL,
    duration_seconds INT(11)  NOT NULL DEFAULT 0,
    studied_count    INT(11)  NOT NULL DEFAULT 1,
    created_at       DATETIME NOT NULL,
    updated_at       DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_progress (account_id, chapter_id),
    INDEX idx_course_progress_account (account_id, updated_at),
    CONSTRAINT fk_course_progress_chapter FOREIGN KEY (chapter_id) REFERENCES db_course_chapter (id) ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 单次学习流水：每次上报追加一行，本周学习时长按 created_at 汇总，避免累计值被重复计入。
CREATE TABLE db_course_study_log
(
    id               INT(11)  NOT NULL AUTO_INCREMENT,
    account_id       INT(11)  NOT NULL,
    course_id        INT(11)  NOT NULL,
    chapter_id       INT(11)  NOT NULL,
    duration_seconds INT(11)  NOT NULL,
    created_at       DATETIME NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_course_study_log_account (account_id, created_at),
    CONSTRAINT fk_course_study_log_chapter FOREIGN KEY (chapter_id) REFERENCES db_course_chapter (id) ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;
