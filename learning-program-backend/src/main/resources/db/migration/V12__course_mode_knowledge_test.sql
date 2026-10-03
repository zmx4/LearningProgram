-- 知识测试支持课程题集模式：请求体 setId 与 typeId 二选一，题集模式下 typeName 快照为「题集：{title}」。
-- set_id 列与可空的 type_id 此前已直接改过远程库（该表实际已无 type 外键约束），本文件补齐题集外键。
ALTER TABLE db_knowledge_test_record
    ADD CONSTRAINT fk_knowledge_test_record_set FOREIGN KEY (set_id) REFERENCES db_question_set (id);
