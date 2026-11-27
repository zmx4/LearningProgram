-- 阶段 2：小组任务截止时间、测试任务项与整任务完成台账
-- 执行：mysql -u <user> -p learning < scripts/migrations/2026-11-27-group-task-settlement.sql
-- 注意：新库直接导入 scripts/learning.sql 即可，本脚本只用于已有库升级。

ALTER TABLE `db_study_group_message`
  ADD COLUMN `due_at` datetime DEFAULT NULL COMMENT '任务截止时间，普通消息为空' AFTER `content`;

ALTER TABLE `db_group_task_item`
  ADD COLUMN `source` varchar(16) DEFAULT NULL COMMENT '单词测试词表来源：cet4 / cet6' AFTER `ref_id`,
  ADD COLUMN `pass_score` int(11) DEFAULT NULL COMMENT '测试任务项及格分（0-100）' AFTER `source`,
  MODIFY COLUMN `item_type` varchar(20) NOT NULL
    COMMENT '任务项类型：course / resource / member_visit / text / word_test / question_set';

CREATE TABLE `db_group_task_finish` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_id` int(11) NOT NULL COMMENT '小组 ID',
  `task_id` int(11) NOT NULL COMMENT '任务消息 ID',
  `account_id` int(11) NOT NULL COMMENT '完成成员账号',
  `completed_at` datetime NOT NULL COMMENT '完成时间',
  `points_awarded` int(11) NOT NULL COMMENT '本次发放积分',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_task_finish` (`task_id`,`account_id`),
  KEY `idx_group_task_finish_group` (`group_id`,`account_id`),
  CONSTRAINT `fk_group_task_finish_task` FOREIGN KEY (`task_id`) REFERENCES `db_study_group_message` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='小组成员整任务完成台账，唯一键保证只结算一次';
