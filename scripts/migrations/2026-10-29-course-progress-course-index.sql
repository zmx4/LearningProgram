-- 为 db_course_progress 补 (course_id, account_id) 索引（P0：小组任务判定按课程查询）
--
-- 背景：completionContext 按 course_id IN (...) + account_id IN (...)
--       统计组员在某课程下的已学章节数；原表只有 account_id 前置索引，
--       按 course_id 过滤只能走 fk_course_progress_chapter 或全表扫描。
-- 执行：mysql -u <user> -p learning < scripts/migrations/2026-10-29-course-progress-course-index.sql
-- 注意：若索引已存在会报 Duplicate key name，可忽略；全新库直接导入 learning.sql 即可。

ALTER TABLE `db_course_progress`
  ADD INDEX `idx_course_progress_course_account` (`course_id`, `account_id`),
  ALGORITHM=INPLACE, LOCK=NONE;
