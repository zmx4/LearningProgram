-- 用户头像：仅在启用对象存储（oss.enable=true）时可上传，文件存放于对象存储 avatars/ 前缀下
--
-- 背景：头像与学习资源共用一个桶，但对象键前缀不同（resources/ 与 avatars/）。
--       库里只记录托管文件名（UUID + 扩展名），不记录完整 URL，读取时由后端拼对象键。
-- 执行：mysql -u <user> -p learning < scripts/migrations/2026-11-13-account-avatar.sql
-- 注意：新库直接导入 scripts/learning.sql 即可，本脚本只用于已有库补字段。
--       未启用对象存储时该字段保持 NULL，头像功能不可用。

ALTER TABLE `db_account`
  ADD COLUMN `avatar` varchar(100) DEFAULT NULL
    COMMENT '头像托管文件名（UUID + 扩展名），存放于对象存储 avatars/ 前缀下，仅启用对象存储时可用' AFTER `bio`;