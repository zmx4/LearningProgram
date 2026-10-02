-- 题集：一组题目（引用 db_test_question）的集合，包含标题与描述。
CREATE TABLE db_question_set
(
    id          INT(11)      NOT NULL AUTO_INCREMENT,
    title       VARCHAR(100) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 题集与题目的关联表，item id 的顺序即题目在题集中的顺序。
CREATE TABLE db_question_set_item
(
    id          INT(11) NOT NULL AUTO_INCREMENT,
    set_id      INT(11) NOT NULL,
    question_id INT(11) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_question_set_item (set_id, question_id),
    CONSTRAINT fk_question_set_item_set FOREIGN KEY (set_id) REFERENCES db_question_set (id) ON DELETE CASCADE,
    CONSTRAINT fk_question_set_item_question FOREIGN KEY (question_id) REFERENCES db_test_question (id),
    INDEX idx_question_set_item_question (question_id)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;
