CREATE TABLE db_test_type
(
    id          INT(11)      NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_test_type_code (code)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- content 使用 JSON 列存放题目内容，结构随题型而定：
--   single / multiple: {"stem": "...", "options": [{"key": "A", "text": "..."}], "answer": ["A"], "analysis": "..."}
--   blank:             {"stem": "…… ____ ……", "options": [], "answer": ["答案1", "答案2"], "analysis": "..."}
-- single 的 answer 恰好 1 项，multiple 至少 2 项，blank 的 answer 按空格顺序每空一项。
CREATE TABLE db_test_question
(
    id         INT(11)     NOT NULL AUTO_INCREMENT,
    type_id    INT(11)     NOT NULL,
    kind       VARCHAR(20) NOT NULL,
    content    JSON        NOT NULL,
    created_at DATETIME    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_test_question_type FOREIGN KEY (type_id) REFERENCES db_test_type (id),
    INDEX idx_test_question_type_kind (type_id, kind)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

INSERT INTO db_test_type (code, name, description, created_at)
VALUES ('knowledge-basic', '计算机基础', '计算机基础知识测试，包含单选、多选与填空三种题型。', NOW());

INSERT INTO db_test_question (type_id, kind, content, created_at)
SELECT id,
       'single',
       '{"stem":"HTTP 协议默认使用的端口号是？","options":[{"key":"A","text":"21"},{"key":"B","text":"80"},{"key":"C","text":"443"},{"key":"D","text":"3306"}],"answer":["B"],"analysis":"HTTP 默认端口为 80，HTTPS 默认端口为 443。"}',
       NOW()
FROM db_test_type
WHERE code = 'knowledge-basic';

INSERT INTO db_test_question (type_id, kind, content, created_at)
SELECT id,
       'multiple',
       '{"stem":"下列哪些属于关系型数据库？","options":[{"key":"A","text":"MySQL"},{"key":"B","text":"Redis"},{"key":"C","text":"MariaDB"},{"key":"D","text":"PostgreSQL"}],"answer":["A","C","D"],"analysis":"Redis 是键值型 NoSQL 数据库，其余三个都是关系型数据库。"}',
       NOW()
FROM db_test_type
WHERE code = 'knowledge-basic';

INSERT INTO db_test_question (type_id, kind, content, created_at)
SELECT id,
       'blank',
       '{"stem":"Java 中用于声明常量的关键字是 ____，可变字符串类是 ____。","options":[],"answer":["final","StringBuilder"],"analysis":"final 用于声明常量；StringBuilder 适合频繁修改字符串的场景。"}',
       NOW()
FROM db_test_type
WHERE code = 'knowledge-basic';
