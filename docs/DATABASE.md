# 数据库说明

本文档说明 LearningProgram 的 MySQL 数据库结构：表清单、表间关系、字段定义和维护约定。接口行为见 [API.md](./API.md)，环境搭建见 [INSTALLATION.md](./INSTALLATION.md)。

## 概述

- 数据库：MySQL 8.0+，库名默认 `learning`，字符集 `utf8mb4`。
- 所有表使用 InnoDB 引擎、`db_` 前缀命名，主键为 `AUTO_INCREMENT` 的 `id`。
- MyBatis-Plus 按驼峰 ↔ 下划线自动映射（如 `register_time` ↔ `registerTime`），个别字段用 `@TableField` 显式指定。
- 用户密码保存 BCrypt 密文，字段长度 `VARCHAR(255)`。
- 表结构的唯一来源是仓库根目录 `scripts/learning.sql`（整库 DDL，28 张表，含索引与外键）。项目未启用 Flyway，也没有增量迁移脚本，结构变更直接维护该文件。

## 表清单

| 分组 | 表名 | 用途 |
| --- | --- | --- |
| 账号与积分 | `db_account` | 用户账号 |
| 账号与积分 | `db_account_points` | 积分总账（每用户一行） |
| 账号与积分 | `db_check_in` | 每日签到记录 |
| 账号与积分 | `db_notification` | 站内通知 |
| 课程与学习 | `db_course` | 课程 |
| 课程与学习 | `db_course_chapter` | 课程章节（Markdown 正文） |
| 课程与学习 | `db_course_question_set` | 课程 ↔ 题集关联 |
| 课程与学习 | `db_course_progress` | 章节粒度的学习进度 |
| 课程与学习 | `db_course_study_log` | 每次学习上报的流水 |
| 课程与学习 | `db_course_reward` | 完成课程的积分发放记录 |
| 题库与测试 | `db_test_type` | 题目类型 |
| 题库与测试 | `db_test_question` | 题库题目 |
| 题库与测试 | `db_question_set` | 题集 |
| 题库与测试 | `db_question_set_item` | 题集 ↔ 题目关联 |
| 题库与测试 | `db_test_record` | 测试成绩记录（单词测试与知识测试统一存储，`category` 区分） |
| 学习小组 | `db_study_group` | 学习小组本身（名称、组长、邀请码） |
| 学习小组 | `db_study_group_member` | 小组成员，一个人可加入多个小组 |
| 学习小组 | `db_study_group_message` | 小组消息，含组长发布的学习任务 |
| 学习小组 | `db_group_task_item` | 学习任务绑定项（课程/资源/组员访问/纯文本） |
| 学习小组 | `db_group_task_progress` | 纯文本任务项的手动完成记录 |
| 学习小组 | `db_group_member_visit` | 成员最近访问小组的时间（member_visit 判定依据） |
| 词典与资源 | `db_dictionary` | 英文词典（word ↔ translation） |
| 词典与资源 | `db_cet4` / `db_cet6` | CET4 / CET6 词表 |
| 词典与资源 | `db_learning_resource` | 学习资源文件元信息 |
| 词典与资源 | `db_resource_access_log` | 资源下载访问流水（resource 任务项判定依据） |

## 实体关系

```mermaid
erDiagram
    db_account ||--o| db_account_points : "积分总账"
    db_account ||--o{ db_check_in : "签到"
    db_account ||--o{ db_notification : "通知"
    db_account ||--o{ db_test_record : "测试成绩"
    db_account ||--o{ db_course_progress : "学习进度"
    db_account ||--o{ db_course_study_log : "学习流水"
    db_account ||--o{ db_course_reward : "课程奖励"

    db_course ||--o{ db_course_chapter : "章节"
    db_course ||--o{ db_course_question_set : "关联题集"
    db_course ||--o{ db_course_reward : "奖励发放"
    db_question_set ||--o{ db_course_question_set : ""
    db_question_set ||--o{ db_question_set_item : "包含题目"
    db_test_question ||--o{ db_question_set_item : ""
    db_test_type ||--o{ db_test_question : "题目类型"
    db_question_set ||--o{ db_test_record : "题集模式成绩"
    db_course_chapter ||--o{ db_course_progress : ""
    db_course_chapter ||--o{ db_course_study_log : ""

    db_study_group ||--o{ db_study_group_member : "成员（含组长）"
    db_study_group ||--o{ db_study_group_message : "消息与任务"
    db_study_group ||--o{ db_group_member_visit : "访问记录"
    db_study_group_message ||--o{ db_group_task_item : "任务绑定项"
    db_group_task_item ||--o{ db_group_task_progress : "手动完成记录"
```

> 完整字段级 E-R 图（含软引用、冗余列与多态引用的可视化标注）见
> [architecture/data/README.md](architecture/data/README.md)，
> 可编辑文件为 `architecture/data/learning-program-er.drawio`。

级联删除链：删除 `db_course` 会级联删除其章节、题集关联和奖励记录；章节删除会级联删除对应的学习进度与流水；删除 `db_account` 会级联删除通知、签到、积分总账和测试记录。例外：`db_learning_resource.account_id` 是不带外键的软引用，删除账号后资源的“上传者”显示为空。

## 账号与积分

### db_account 用户账号

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| username | VARCHAR(32) | NOT NULL, UNIQUE | 用户名，登录凭据之一 |
| password | VARCHAR(255) | NOT NULL | BCrypt 密文 |
| email | VARCHAR(128) | NOT NULL, UNIQUE | 邮箱，登录凭据之一 |
| role | VARCHAR(32) | NOT NULL DEFAULT 'user' | `user` / `admin` |
| register_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 注册时间 |
| phone | VARCHAR(30) | NULL | 手机号 |
| bio | VARCHAR(120) | NULL | 个人简介 |
| points | INT | NOT NULL DEFAULT 0 | 早期遗留字段，注册时固定写 0；有效积分以 `db_account_points` 为准 |

### db_account_points 积分总账

积分的唯一权威来源。签到（`db_check_in`）与完成课程奖励（`db_course_reward`）通过 `addPoints` 累加到 `total_points`。

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| account_id | INT | NOT NULL, UNIQUE, FK → db_account(id) | 每用户一行 |
| total_points | INT | NOT NULL DEFAULT 0 | 当前总积分 |

### db_check_in 每日签到

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| account_id | INT | NOT NULL, FK → db_account(id) | |
| checkin_date | DATE | NOT NULL | 签到日；`(account_id, checkin_date)` 唯一，一天一次 |
| points | INT | NOT NULL | 本次签到获得积分 |
| streak | INT | NOT NULL | 连续签到天数 |

### db_notification 站内通知

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| account_id | INT | NOT NULL, FK → db_account(id) ON DELETE CASCADE | 收件人 |
| title | VARCHAR(128) | NOT NULL | |
| content | VARCHAR(1000) | NOT NULL | |
| type | VARCHAR(32) | NOT NULL DEFAULT 'system' | 通知类型，当前均为 `system` |
| is_read | TINYINT(1) | NOT NULL DEFAULT 0 | 已读标记 |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |

## 课程与学习

课程完成的判定：学员学完课程内全部章节（`db_course_progress` 覆盖全部章节）。

### db_course 课程

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| title | VARCHAR(100) | NOT NULL | |
| description | VARCHAR(500) | NULL | |
| icon | VARCHAR(16) | NULL | emoji 字符 |
| sort_order | INT | NOT NULL DEFAULT 0 | 学员端卡片展示顺序 |
| reward_points | INT | NOT NULL DEFAULT 0 | 完成奖励积分，0 表示不奖励 |
| created_at | DATETIME | NOT NULL | |

### db_course_chapter 课程章节

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| course_id | INT | NOT NULL, FK → db_course(id) ON DELETE CASCADE | |
| title | VARCHAR(150) | NOT NULL | |
| content | MEDIUMTEXT | NULL | 章节正文，支持 Markdown |
| sort_order | INT | NOT NULL DEFAULT 0 | 章节顺序 |
| created_at | DATETIME | NOT NULL | |

### db_course_question_set 课程 ↔ 题集关联

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| course_id | INT | NOT NULL, FK → db_course(id) ON DELETE CASCADE | |
| set_id | INT | NOT NULL, FK → db_question_set(id) ON DELETE CASCADE | |
| sort_order | INT | NOT NULL DEFAULT 0 | 课程内顺序 |
| created_at | DATETIME | NOT NULL | |

`(course_id, set_id)` 唯一。

### db_course_progress 章节学习进度

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| account_id | INT | NOT NULL | 与 `chapter_id` 组成唯一键，一章一进度 |
| course_id | INT | NOT NULL | 冗余存储便于统计，索引 `(course_id, account_id)` |
| chapter_id | INT | NOT NULL, FK → db_course_chapter(id) ON DELETE CASCADE | |
| duration_seconds | INT | NOT NULL DEFAULT 0 | 该章累计学习时长 |
| studied_count | INT | NOT NULL DEFAULT 1 | 该章被标记已学的次数 |
| created_at / updated_at | DATETIME | NOT NULL | |

### db_course_study_log 学习流水

每次上报追加一行，本周学习时长按 `created_at` 汇总，避免累计值被重复计入。

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| account_id | INT | NOT NULL | |
| course_id | INT | NOT NULL | 冗余存储 |
| chapter_id | INT | NOT NULL, FK → db_course_chapter(id) ON DELETE CASCADE | |
| duration_seconds | INT | NOT NULL | 本次时长（上限 3600 秒） |
| created_at | DATETIME | NOT NULL | 索引 `(account_id, created_at)` 支撑周统计 |

### db_course_reward 完成课程积分发放记录

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| account_id | INT | NOT NULL | 与 `course_id` 组成唯一键，保证每门课程的奖励只发放一次 |
| course_id | INT | NOT NULL, FK → db_course(id) ON DELETE CASCADE | |
| points | INT | NOT NULL | 发放时的积分数 |
| created_at | DATETIME | NOT NULL | |

课程奖励调整后只影响之后完成课程的学员，已发放的记录不补发或追回。

## 题库与测试

### db_test_type 题目类型

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| code | VARCHAR(50) | NOT NULL, UNIQUE | 类型编码 |
| name | VARCHAR(100) | NOT NULL | 类型名称 |
| description | VARCHAR(255) | NULL | |
| created_at | DATETIME | NOT NULL | |

### db_test_question 题库题目

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| type_id | INT | NOT NULL, FK → db_test_type(id) | |
| kind | VARCHAR(20) | NOT NULL | `single` 单选 / `multiple` 多选 / `blank` 填空 |
| content | LONGTEXT (JSON, CHECK json_valid) | NOT NULL | 题干、选项、答案与解析的 JSON |
| created_at | DATETIME | NOT NULL | |

索引 `(type_id, kind)` 支撑按类型和题型筛选。

### db_question_set 题集

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| title | VARCHAR(100) | NOT NULL | |
| description | VARCHAR(255) | NULL | |
| created_at | DATETIME | NOT NULL | |

### db_question_set_item 题集 ↔ 题目关联

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| set_id | INT | NOT NULL, FK → db_question_set(id) ON DELETE CASCADE | |
| question_id | INT | NOT NULL, FK → db_test_question(id) | |

`(set_id, question_id)` 唯一；删除题目不级联删除关联项，需先自行清理。

### db_test_record 测试成绩

单词测试与知识测试成绩统一存储在一张表中，`category` 区分测试大类（取代原 `db_word_test_record` 与 `db_knowledge_test_record`）。

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| account_id | INT | NOT NULL | 不带外键，索引 `(account_id, category, created_at)` |
| category | VARCHAR(20) | NOT NULL | 测试大类：`word`（单词测试）/ `knowledge`（知识测试） |
| source | VARCHAR(10) | NULL | 单词测试的词表来源 `cet4` / `cet6`；知识测试为空 |
| type_id | INT | NULL | 知识测试普通模式的题目类型，索引 `(type_id)`；其他情况为空 |
| set_id | INT | NULL, FK → db_question_set(id) | 知识测试题集模式的题集 |
| type_name | VARCHAR(100) | NULL | 知识测试的类型名快照；题集模式下为「题集：{title}」 |
| total_count / correct_count / wrong_count | INT | NOT NULL | 答题统计 |
| score | INT | NOT NULL | 得分（按正确率计算） |
| duration_seconds | INT | NOT NULL DEFAULT 0 | 用时 |
| detail | LONGTEXT | NULL, CHECK json_valid | 知识测试的答题明细 JSON（结构由后端写入，约束见 `scripts/learning.sql` 的 `chk_test_record_detail`） |
| created_at | DATETIME | NOT NULL | |

## 词典与资源

### db_dictionary / db_cet4 / db_cet6 词表

三张表结构相同：`id`（PK）、`word` VARCHAR(255) NOT NULL、`translation`（db_dictionary 为 TEXT，其余为 VARCHAR(512)）。`db_dictionary` 供查词使用，`db_cet4` / `db_cet6` 作为每日一词和单词测试的词库来源。`db_dictionary` 的 `word` 建有索引 `idx_dictionary_word`，支撑按词精确查询（340 万行，无索引会全表扫描）；已有库执行 `scripts/migrations/2026-10-29-dictionary-word-index.sql` 补齐。

### db_learning_resource 学习资源

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| title | VARCHAR(100) | NOT NULL | |
| description | VARCHAR(255) | NULL | |
| file_name | VARCHAR(255) | NOT NULL | 原始文件名 |
| stored_name | VARCHAR(100) | NOT NULL | 磁盘存储名（对应 `learning.resources.upload-dir`） |
| content_type | VARCHAR(100) | NULL | MIME 类型 |
| file_size | BIGINT | NOT NULL DEFAULT 0 | 字节数，单个上限 50MB |
| visible | TINYINT(1) | NOT NULL DEFAULT 1 | 是否在学员端展示 |
| account_id | INT | NULL | 上传者，软引用 db_account，不带外键 |
| created_at | DATETIME | NOT NULL | 索引 `(visible, created_at)` |

## 学习小组

### db_study_group 学习小组

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| name | VARCHAR(50) | NOT NULL | 小组名称 |
| owner_id | INT | NOT NULL | 组长（组长同时占一行成员记录），软引用 db_account，不带外键 |
| invite_code | VARCHAR(8) | NOT NULL, UNIQUE | 邀请码，组员凭此加入 |
| created_at | DATETIME | NOT NULL | |

### db_study_group_member 小组成员

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| group_id | INT | NOT NULL, FK → db_study_group(id) ON DELETE CASCADE | 删除小组级联清理成员 |
| account_id | INT | NOT NULL | 软引用 db_account，不带外键，索引 `(account_id)` |
| role | VARCHAR(10) | NOT NULL | `owner`（组长）/ `member`（组员） |
| joined_at | DATETIME | NOT NULL | 加入时间 |

`(group_id, account_id)` 唯一，一个人可以加入或创建多个小组。

### db_study_group_message 小组消息

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| group_id | INT | NOT NULL, FK → db_study_group(id) ON DELETE CASCADE | 索引 `(group_id, id)` |
| account_id | INT | NOT NULL | 发送者，软引用 db_account，不带外键 |
| type | VARCHAR(10) | NOT NULL | `text` 普通消息 / `task` 学习任务 |
| title | VARCHAR(100) | NULL | 任务标题，`type = task` 时有值 |
| content | VARCHAR(2000) | NOT NULL | 消息或任务内容 |
| created_at | DATETIME | NOT NULL | |

聊天区按 `(group_id, id)` 倒序取最近 200 条再正序展示；删除小组会级联删除消息。

### db_group_task_item 小组任务绑定项

学习任务（`type = task` 的消息）可绑定若干任务项，用于追踪组员完成情况。

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| group_id | INT | NOT NULL | 冗余小组 id，索引 `(group_id)` |
| message_id | INT | NOT NULL, FK → db_study_group_message(id) ON DELETE CASCADE | 任务消息 id，索引 `(message_id)` |
| item_type | VARCHAR(20) | NOT NULL | `course` / `resource` / `member_visit` / `text` |
| ref_id | INT | NULL | item_type 为 course / resource 时的课程或资源 id，软引用不带外键 |
| title | VARCHAR(200) | NOT NULL | course/resource 自动取关联实体标题，其余为自定义说明 |
| sort_order | INT | NOT NULL DEFAULT 0 | 展示顺序 |

完成判定：course 看 `db_course_progress` 是否覆盖课程全部章节；resource 看任务发布后是否有
`db_resource_access_log` 访问流水；member_visit 看任务发布后 `db_group_member_visit.last_visit_at`；
以上三类查询时实时计算，text 由组员手动标记并写入 `db_group_task_progress`。

### db_group_task_progress 任务项完成记录

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| item_id | INT | NOT NULL, FK → db_group_task_item(id) ON DELETE CASCADE | |
| account_id | INT | NOT NULL | 软引用 db_account，不带外键 |
| completed_at | DATETIME | NOT NULL | |

`(item_id, account_id)` 唯一，仅存 text 类型任务项的手动标记。

### db_resource_access_log 资源访问流水

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| account_id | INT | NOT NULL | 软引用 db_account，不带外键；与 `resource_id` 组成唯一键 `uk_access_log_account_resource` |
| resource_id | INT | NOT NULL | 软引用 db_learning_resource，不带外键，索引 `(resource_id)` |
| accessed_at | DATETIME | NOT NULL | 最后一次成功下载时间（同一人同一资源只保留一行，重复访问刷新该字段） |

### db_group_member_visit 小组访问记录

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | INT | PK, AUTO_INCREMENT | |
| group_id | INT | NOT NULL, FK → db_study_group(id) ON DELETE CASCADE | |
| account_id | INT | NOT NULL | 软引用 db_account，不带外键 |
| last_visit_at | DATETIME | NOT NULL | 最近一次访问小组详情页的时间，每次访问刷新 |

`(group_id, account_id)` 唯一，供 member_visit 任务项判定"任务发布后组员已访问小组"。

## 建表脚本

表结构的唯一来源是 `scripts/learning.sql`（整库 DDL，28 张表）。安装脚本检测到库为空时会自动导入它；也可以手工执行：

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p learning < scripts/learning.sql
```

需要了解的几点：

- 建表语句不带 `IF NOT EXISTS`，**只能导入空库**；库里已有表时会因同名表报错。已有库要按最新结构重建，用安装脚本的 `--reset-db` / `-ResetDb`（先整库删除再导入）。
- 文件开头 `SET FOREIGN_KEY_CHECKS = 0`、结尾恢复为 `1`，因此建表顺序不影响外键依赖（例如 `db_course_question_set` 出现在 `db_question_set` 之前）。
- 表结构变更时直接修改 `scripts/learning.sql`，不再新增 `V*__*.sql` 增量脚本。

历史沿革：早期按序号维护的增量迁移脚本（V7–V17，位于 `learning-program-backend/src/main/resources/db/migration/`）已删除，其内容全部并入 `scripts/learning.sql`；V1–V6 时期的基础表同样包含在该文件中。
