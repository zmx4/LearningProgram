-- 单词测试与知识测试成绩合并为一张 db_test_record 表，category 区分测试大类（word / knowledge）：
--   word：source 记录词表来源（cet4 / cet6），type_id / set_id / type_name / detail 为空；
--   knowledge：type_id 与 set_id 二选一（普通模式 / 题集模式），type_name 为名称快照，
--              detail 以 JSON 存放答题明细（结构见 V8 注释）。
-- 历史数据由 db_word_test_record 与 db_knowledge_test_record 迁入后删除旧表。
CREATE TABLE db_test_record
(
    id               INT(11)      NOT NULL AUTO_INCREMENT,
    account_id       INT(11)      NOT NULL,
    category         VARCHAR(20)  NOT NULL,
    source           VARCHAR(10)  DEFAULT NULL,
    type_id          INT(11)      DEFAULT NULL,
    set_id           INT(11)      DEFAULT NULL,
    type_name        VARCHAR(100) DEFAULT NULL,
    total_count      INT(11)      NOT NULL,
    correct_count    INT(11)      NOT NULL,
    wrong_count      INT(11)      NOT NULL,
    score            INT(11)      NOT NULL,
    duration_seconds INT(11)      NOT NULL DEFAULT 0,
    detail           LONGTEXT     DEFAULT NULL,
    created_at       DATETIME     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_test_record_account (account_id, category, created_at),
    INDEX idx_test_record_type (type_id),
    CONSTRAINT fk_test_record_set FOREIGN KEY (set_id) REFERENCES db_question_set (id),
    CONSTRAINT chk_test_record_detail CHECK (detail IS NULL OR JSON_VALID(detail))
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

INSERT INTO db_test_record (account_id, category, source, total_count, correct_count, wrong_count, score,
                            duration_seconds, created_at)
SELECT account_id, 'word', source, total_count, correct_count, wrong_count, score, duration_seconds, created_at
FROM db_word_test_record;

INSERT INTO db_test_record (account_id, category, type_id, set_id, type_name, total_count, correct_count,
                            wrong_count, score, duration_seconds, detail, created_at)
SELECT account_id, 'knowledge', type_id, set_id, type_name, total_count, correct_count, wrong_count, score,
       duration_seconds, detail, created_at
FROM db_knowledge_test_record;

DROP TABLE db_word_test_record;
DROP TABLE db_knowledge_test_record;
