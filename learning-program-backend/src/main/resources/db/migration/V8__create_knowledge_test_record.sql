-- detail 使用 JSON 列存放答题明细，结构如下（kind 取值与 db_test_question.kind 一致）：
--   [{"questionId": 1, "kind": "single", "userAnswer": ["B"], "correct": true},
--    {"questionId": 2, "kind": "multiple", "userAnswer": ["A", "C"], "correct": false},
--    {"questionId": 3, "kind": "blank", "userAnswer": ["final", "StringBuilder"], "correct": true}]
CREATE TABLE db_knowledge_test_record
(
    id               INT(11)      NOT NULL AUTO_INCREMENT,
    account_id       INT(11)      NOT NULL,
    type_id          INT(11)      NOT NULL,
    type_name        VARCHAR(100) NOT NULL,
    total_count      INT(11)      NOT NULL,
    correct_count    INT(11)      NOT NULL,
    wrong_count      INT(11)      NOT NULL,
    score            INT(11)      NOT NULL,
    duration_seconds INT(11)      NOT NULL,
    detail           JSON         DEFAULT NULL,
    created_at       DATETIME     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_knowledge_test_record_account (account_id, created_at),
    CONSTRAINT fk_knowledge_test_record_type FOREIGN KEY (type_id) REFERENCES db_test_type (id)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;
