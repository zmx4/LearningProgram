-- 课程主表：icon 存放 emoji 字符，sort_order 决定学员端课程卡片的展示顺序。
CREATE TABLE db_course
(
    id          INT(11)      NOT NULL AUTO_INCREMENT,
    title       VARCHAR(100) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    icon        VARCHAR(16)  DEFAULT NULL,
    sort_order  INT(11)      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 课程章节：content 为章节正文，(course_id, sort_order) 决定章节顺序；
-- 课程删除时章节级联删除，学员进度（db_course_progress/db_course_study_log）再随章节级联删除。
CREATE TABLE db_course_chapter
(
    id         INT(11)      NOT NULL AUTO_INCREMENT,
    course_id  INT(11)      NOT NULL,
    title      VARCHAR(150) NOT NULL,
    content    MEDIUMTEXT   DEFAULT NULL,
    sort_order INT(11)      NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_course_chapter_course (course_id, sort_order),
    CONSTRAINT fk_course_chapter_course FOREIGN KEY (course_id) REFERENCES db_course (id) ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 课程与练习题集的关联表，sort_order 决定题集在课程内的顺序；
-- 课程或题集任一删除时关联记录级联删除。
CREATE TABLE db_course_question_set
(
    id         INT(11)  NOT NULL AUTO_INCREMENT,
    course_id  INT(11)  NOT NULL,
    set_id     INT(11)  NOT NULL,
    sort_order INT(11)  NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_question_set (course_id, set_id),
    INDEX fk_course_question_set_set (set_id),
    CONSTRAINT fk_course_question_set_course FOREIGN KEY (course_id) REFERENCES db_course (id) ON DELETE CASCADE,
    CONSTRAINT fk_course_question_set_set FOREIGN KEY (set_id) REFERENCES db_question_set (id) ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;
