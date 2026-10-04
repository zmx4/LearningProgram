-- 完成课程奖励积分：reward_points 为学员学完全部章节后一次性发放的积分，0 表示不奖励。
ALTER TABLE db_course
    ADD COLUMN reward_points INT(11) NOT NULL DEFAULT 0;

-- 积分发放记录：(account_id, course_id) 唯一，从数据库层面保证每门课程的奖励只发放一次；
-- 课程删除时发放记录级联删除（积分总账 db_account_points 不回收）。
CREATE TABLE db_course_reward
(
    id         INT(11)  NOT NULL AUTO_INCREMENT,
    account_id INT(11)  NOT NULL,
    course_id  INT(11)  NOT NULL,
    points     INT(11)  NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_reward (account_id, course_id),
    CONSTRAINT fk_course_reward_course FOREIGN KEY (course_id) REFERENCES db_course (id) ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;
