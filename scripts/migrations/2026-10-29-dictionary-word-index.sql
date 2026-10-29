-- 为 db_dictionary.word 补索引（P0：查词全表扫描）
--
-- 背景：/api/dictionary/** 按 word 精确查词，db_dictionary 有 340 万+ 行，
--       但表上只有主键，每次查询都要全表扫描。
-- 影响：340 万行建二级索引需要数十秒到数分钟，MySQL 8 支持在线 DDL，
--       显式指定 INPLACE + LOCK=NONE 可在建索引期间继续读写，建议低峰执行。
-- 执行：mysql -u <user> -p learning < scripts/migrations/2026-10-29-dictionary-word-index.sql
-- 注意：若索引已存在会报 Duplicate key name，可忽略；全新库直接导入 learning.sql 即可。

ALTER TABLE `db_dictionary`
  ADD INDEX `idx_dictionary_word` (`word`),
  ALGORITHM=INPLACE, LOCK=NONE;
