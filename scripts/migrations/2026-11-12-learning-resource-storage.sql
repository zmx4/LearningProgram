-- 学习资源支持对象存储：记录每个文件实际存放的位置
--
-- 背景：资源文件原本只落在后端 learning.resources.upload-dir 目录，新增对象存储支持后，
--       开启 oss.enable 时新文件写入 OSS。为了让“开开关”不影响老文件，每条资源记录自己
--       的存放位置；读取/删除时按记录走，不做全量搬迁。
-- 执行：mysql -u <user> -p learning < scripts/migrations/2026-11-12-learning-resource-storage.sql
-- 注意：新库直接导入 scripts/learning.sql 即可，本脚本只用于已有库补字段。
--       默认值 'local' 会把存量资源标记为磁盘文件，与改动前的实际行为一致。

ALTER TABLE `db_learning_resource`
  ADD COLUMN `storage` varchar(16) NOT NULL DEFAULT 'local'
    COMMENT '文件存放位置：local=本地磁盘，oss=对象存储' AFTER `stored_name`;

ALTER TABLE `db_learning_resource`
  MODIFY COLUMN `stored_name` varchar(100) NOT NULL
    COMMENT '托管文件名（UUID + 扩展名），对应本地磁盘文件名或对象存储对象键';

ALTER TABLE `db_learning_resource`
  COMMENT = '学习资源元数据，文件本体存放于本地磁盘或对象存储';
