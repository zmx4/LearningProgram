-- 资源访问流水收敛：每个 (account_id, resource_id) 只保留一行（P0：流水表无界增长）
--
-- 背景：原实现每次下载插一行，表随点击次数无限增长；
--       任务完成判定只需要「最后一次访问时间」，保留最新记录即可。
-- 步骤：1) 去重，保留 id 最大（即最后一次插入）的一行；
--       2) 用唯一键 uk_access_log_account_resource 替换同列的非唯一索引。
-- 执行：mysql -u <user> -p learning < scripts/migrations/2026-10-29-resource-access-log-unique.sql
-- 注意：代码侧已改为 INSERT ... ON DUPLICATE KEY UPDATE，必须先完成本迁移再发布新版本，
--       否则新代码没有唯一键可依赖，仍会继续插入重复行。

DELETE r1 FROM `db_resource_access_log` r1
JOIN `db_resource_access_log` r2
  ON r1.account_id = r2.account_id
 AND r1.resource_id = r2.resource_id
 AND r1.id < r2.id;

ALTER TABLE `db_resource_access_log`
  DROP INDEX `idx_resource_access_log_account`,
  ADD UNIQUE KEY `uk_access_log_account_resource` (`account_id`, `resource_id`),
  ALGORITHM=INPLACE, LOCK=NONE;
