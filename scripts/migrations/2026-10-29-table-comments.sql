-- 为全部业务表补充表级 COMMENT（MariaDB 10.11）
-- 只改表元数据，不影响数据与索引；可重复执行

SET NAMES utf8mb4;

ALTER TABLE `db_account` COMMENT = '用户账号，role 区分 user / admin';
ALTER TABLE `db_account_points` COMMENT = '用户积分总账，与 db_account 一对一';
ALTER TABLE `db_cet4` COMMENT = 'CET-4 四级词库';
ALTER TABLE `db_cet6` COMMENT = 'CET-6 六级词库';
ALTER TABLE `db_check_in` COMMENT = '每日签到记录，每人每天一条';
ALTER TABLE `db_course` COMMENT = '课程';
ALTER TABLE `db_course_chapter` COMMENT = '课程章节，content 为章节正文';
ALTER TABLE `db_course_progress` COMMENT = '账号章节学习进度，按账号 + 章节聚合';
ALTER TABLE `db_course_question_set` COMMENT = '课程与题集的关联';
ALTER TABLE `db_course_reward` COMMENT = '课程完成积分发放记录，每人每课只发一次';
ALTER TABLE `db_course_study_log` COMMENT = '章节学习时长流水，用于周学习时长统计';
ALTER TABLE `db_dictionary` COMMENT = '英汉词典词条，查词接口数据源';
ALTER TABLE `db_discussion_comment` COMMENT = '讨论区评论';
ALTER TABLE `db_discussion_post` COMMENT = '讨论区帖子，comment_count 为冗余评论数';
ALTER TABLE `db_group_member_visit` COMMENT = '小组成员最近访问时间，用于任务已读判定';
ALTER TABLE `db_group_task_item` COMMENT = '小组任务项，item_type 区分课程 / 资源 / 成员访问 / 文本';
ALTER TABLE `db_group_task_progress` COMMENT = '小组成员任务项完成记录（手动勾选）';
ALTER TABLE `db_learning_resource` COMMENT = '学习资源元数据，文件本体存放于磁盘';
ALTER TABLE `db_notification` COMMENT = '站内通知';
ALTER TABLE `db_question_set` COMMENT = '题集';
ALTER TABLE `db_question_set_item` COMMENT = '题集与题目的关联';
ALTER TABLE `db_resource_access_log` COMMENT = '资源访问记录，每人每资源只保留最近一次';
ALTER TABLE `db_study_group` COMMENT = '学习小组，invite_code 用于邀请加入';
ALTER TABLE `db_study_group_member` COMMENT = '小组成员及角色';
ALTER TABLE `db_study_group_message` COMMENT = '小组消息，type=task 的消息即任务';
ALTER TABLE `db_test_question` COMMENT = '题库题目，content 为 JSON 结构';
ALTER TABLE `db_test_record` COMMENT = '测试提交记录，detail 为作答明细 JSON';
ALTER TABLE `db_test_type` COMMENT = '测试类型（词汇 / 语法等）';
