# E-R 图证据与置信度

本文件记录 `learning-program-er.drawio` 与其 Mermaid 规范源（`01-overview.mmd` … `06-social.mmd`）
的建模依据。唯一事实源是 `scripts/learning.sql`（28 张表，第 8 - 338 行）；
MyBatis-Plus 实体位于 `learning-program-backend/src/main/java/com/tick/entity/dto/`。

置信度定义：

- **high**：直接来自 DDL 的 `PRIMARY KEY` / `UNIQUE KEY` / `CONSTRAINT FOREIGN KEY`。
- **medium**：字段存在且命名明确，但关系没有数据库约束（软引用）。
- **low**：语义依赖枚举值或业务约定（如多态 `ref_id`、`item_type`）。

## 1. 表清单与字段证据

| 表 | 实体类 | DDL 行号 | 关键约束 |
| --- | --- | --- | --- |
| `db_account` | `Account.java` | 8-23 | PK `id`；UK `username`、`email` |
| `db_account_points` | `AccountPoints.java` | 25-32 | PK `id`；UK `account_id`；FK → `db_account` |
| `db_cet4` | `Cet4Word.java` | 34-39 | PK `id` |
| `db_cet6` | `Cet6Word.java` | 41-46 | PK `id` |
| `db_check_in` | `CheckInRecord.java` | 48-57 | PK `id`；UK `(account_id, checkin_date)`；FK → `db_account` |
| `db_course` | `Course.java` | 59-68 | PK `id` |
| `db_course_chapter` | `CourseChapter.java` | 70-80 | PK `id`；FK → `db_course` |
| `db_course_progress` | `CourseProgress.java` | 82-96 | PK `id`；UK `(account_id, chapter_id)`；FK `chapter_id` → 章节 |
| `db_course_question_set` | `CourseQuestionSet.java` | 98-109 | PK `id`；UK `(course_id, set_id)`；FK → `db_course`、`db_question_set` |
| `db_course_reward` | `CourseReward.java` | 111-121 | PK `id`；UK `(account_id, course_id)`；FK → `db_course` |
| `db_course_study_log` | `CourseStudyLog.java` | 123-134 | PK `id`；FK `chapter_id` → 章节 |
| `db_dictionary` | `Dictionary.java` | 136-141 | PK `id` |
| `db_discussion_comment` | `DiscussionComment.java` | 143-154 | PK `id`；FK → `db_discussion_post`、`db_account` |
| `db_discussion_post` | `DiscussionPost.java` | 156-167 | PK `id`；FK → `db_account` |
| `db_group_member_visit` | `GroupMemberVisit.java` | 169-177 | PK `id`；UK `(group_id, account_id)`；FK → `db_study_group` |
| `db_group_task_item` | `GroupTaskItem.java` | 179-191 | PK `id`；FK `message_id` → 群消息 |
| `db_group_task_progress` | `GroupTaskProgress.java` | 193-201 | PK `id`；UK `(item_id, account_id)`；FK → `db_group_task_item` |
| `db_learning_resource` | `LearningResource.java` | 203-216 | PK `id` |
| `db_notification` | `Notification.java` | 218-230 | PK `id`；FK → `db_account`（CASCADE） |
| `db_question_set` | `QuestionSet.java` | 232-238 | PK `id` |
| `db_question_set_item` | `QuestionSetItem.java` | 240-249 | PK `id`；UK `(set_id, question_id)`；FK → `db_question_set`、`db_test_question` |
| `db_resource_access_log` | `ResourceAccessLog.java` | 251-259 | PK `id` |
| `db_study_group` | `StudyGroup.java` | 261-269 | PK `id`；UK `invite_code` |
| `db_study_group_member` | `StudyGroupMember.java` | 271-281 | PK `id`；UK `(group_id, account_id)`；FK → `db_study_group` |
| `db_study_group_message` | `StudyGroupMessage.java` | 283-294 | PK `id`；FK → `db_study_group` |
| `db_test_question` | `TestQuestion.java` | 296-305 | PK `id`；FK → `db_test_type`；`content` JSON 校验 |
| `db_test_record` | `TestRecord.java` | 307-328 | PK `id`；FK `set_id` → `db_question_set`；`detail` JSON 校验 |
| `db_test_type` | `TestType.java` | 330-338 | PK `id`；UK `code` |

字段名大小写差异（MyBatis-Plus 映射）：`Account.registerDate` → `register_time`，
`Notification.readStatus` → `is_read`，其余按驼峰-下划线自动映射。

## 2. 真实外键（21 条，high）

| 约束名 | 从 | 到 | DDL 行号 | 删除行为 |
| --- | --- | --- | --- | --- |
| `fk_account_points_account` | `db_account_points.account_id` | `db_account.id` | 31 | RESTRICT |
| `fk_check_in_account` | `db_check_in.account_id` | `db_account.id` | 56 | RESTRICT |
| `fk_notification_account` | `db_notification.account_id` | `db_account.id` | 229 | CASCADE |
| `fk_discussion_post_account` | `db_discussion_post.account_id` | `db_account.id` | 166 | RESTRICT |
| `fk_discussion_comment_account` | `db_discussion_comment.account_id` | `db_account.id` | 152 | RESTRICT |
| `fk_discussion_comment_post` | `db_discussion_comment.post_id` | `db_discussion_post.id` | 153 | CASCADE |
| `fk_course_chapter_course` | `db_course_chapter.course_id` | `db_course.id` | 79 | CASCADE |
| `fk_course_progress_chapter` | `db_course_progress.chapter_id` | `db_course_chapter.id` | 95 | CASCADE |
| `fk_course_study_log_chapter` | `db_course_study_log.chapter_id` | `db_course_chapter.id` | 133 | CASCADE |
| `fk_course_question_set_course` | `db_course_question_set.course_id` | `db_course.id` | 107 | CASCADE |
| `fk_course_question_set_set` | `db_course_question_set.set_id` | `db_question_set.id` | 108 | CASCADE |
| `fk_course_reward_course` | `db_course_reward.course_id` | `db_course.id` | 120 | CASCADE |
| `fk_test_question_type` | `db_test_question.type_id` | `db_test_type.id` | 304 | RESTRICT |
| `fk_question_set_item_set` | `db_question_set_item.set_id` | `db_question_set.id` | 248 | CASCADE |
| `fk_question_set_item_question` | `db_question_set_item.question_id` | `db_test_question.id` | 247 | RESTRICT |
| `fk_test_record_set` | `db_test_record.set_id` | `db_question_set.id` | 326 | RESTRICT |
| `fk_study_group_member_group` | `db_study_group_member.group_id` | `db_study_group.id` | 280 | CASCADE |
| `fk_study_group_message_group` | `db_study_group_message.group_id` | `db_study_group.id` | 293 | CASCADE |
| `fk_group_member_visit_group` | `db_group_member_visit.group_id` | `db_study_group.id` | 176 | CASCADE |
| `fk_group_task_item_message` | `db_group_task_item.message_id` | `db_study_group_message.id` | 190 | CASCADE |
| `fk_group_task_progress_item` | `db_group_task_progress.item_id` | `db_group_task_item.id` | 200 | CASCADE |

## 3. 软引用（13 条，medium）

字段存在且命名/注释明确，但 DDL 没有 `FOREIGN KEY`；图中用灰色虚线 + 鸦爪表示逻辑 1:N。

| 边 | 字段 | DDL 行号 | 依据 |
| --- | --- | --- | --- |
| `db_account` → `db_course_progress` | `account_id` | 84 | 唯一键 `(account_id, chapter_id)` 暗示归属 |
| `db_account` → `db_course_study_log` | `account_id` | 125 | 索引 `(account_id, created_at)` |
| `db_account` → `db_course_reward` | `account_id` | 113 | 唯一键 `(account_id, course_id)` |
| `db_account` → `db_test_record` | `account_id` | 309 | 索引 `(account_id, category, created_at)` |
| `db_test_type` → `db_test_record` | `type_id` | 312 | 可空、有索引；另有 `type_name` 快照 |
| `db_account` → `db_study_group` | `owner_id` | 264 | 组长归属；`docs/DATABASE.md:290` |
| `db_account` → `db_study_group_member` | `account_id` | 274 | 唯一键 `(group_id, account_id)` |
| `db_account` → `db_study_group_message` | `account_id` | 286 | 发送者 |
| `db_account` → `db_group_task_progress` | `account_id` | 196 | 唯一键 `(item_id, account_id)` |
| `db_account` → `db_group_member_visit` | `account_id` | 172 | 唯一键 `(group_id, account_id)` |
| `db_account` → `db_learning_resource` | `account_id` | 212 | 列注释明确“软引用，不带外键”，可空 |
| `db_account` → `db_resource_access_log` | `account_id` | 253 | 访问流水 |
| `db_learning_resource` → `db_resource_access_log` | `resource_id` | 254 | 访问流水 |

## 4. 冗余列（3 条，medium）

字段值是上游键的副本，用于查询便利，但数据库不保证一致性。

| 边 | 字段 | DDL 行号 | 说明 |
| --- | --- | --- | --- |
| `db_course` → `db_course_progress` | `course_id` | 85 | 真实归属经 `chapter_id` 推导 |
| `db_course` → `db_course_study_log` | `course_id` | 126 | 真实归属经 `chapter_id` 推导 |
| `db_study_group` → `db_group_task_item` | `group_id` | 181 | 真实归属经 `message_id` 推导 |

## 5. 多态引用（2 条，low）

`db_group_task_item.ref_id`（179-191 行）的含义由 `item_type` 决定：

| `item_type` | `ref_id` 指向 | 图中边 |
| --- | --- | --- |
| `course` | `db_course.id` | 紫色虚线 → `db_course` |
| `resource` | `db_learning_resource.id` | 紫色虚线 → `db_learning_resource` |
| `text` | 空 | 无 |
| `member_visit` | 空 | 无 |

单个外键无法表达这种目标可变的引用，因此图中用紫色虚线，且置信度标为 low。

## 6. 多对多连接表（3 张，high）

| 连接表 | 左实体 | 右实体 | 载荷字段 | DDL 行号 |
| --- | --- | --- | --- | --- |
| `db_course_question_set` | `db_course` | `db_question_set` | `sort_order`、`created_at` | 98-109 |
| `db_question_set_item` | `db_question_set` | `db_test_question` | 仅代理主键 `id` | 240-249 |
| `db_study_group_member` | `db_study_group` | `db_account`（软引用） | `role`、`joined_at` | 271-281 |

`db_study_group_member.account_id` 不是外键，因此“小组 ↔ 账号”的 M:N 只有左半边受约束。

## 7. 已知文档偏差与建模注意事项

1. **`docs/DATABASE.md:73` 的级联描述不准确**：该段称删除账号会级联删除
   “通知、签到、积分总账和测试记录”。实际 DDL 中只有 `db_notification` 是
   `ON DELETE CASCADE`；`db_check_in`、`db_account_points`、`db_discussion_post`
   等账号外键为默认 `RESTRICT`，删除账号会被阻止；`db_test_record.account_id`
   （309 行）根本没有外键。以 DDL 为准。
2. **文档缺字段**：`docs/DATABASE.md` 未列出 `db_account.totp_secret` / `totp_enabled`
   （DDL 18-19 行）与 `db_notification.link`（DDL 223 行）。本图以 DDL 为准。
3. **`db_account.points` 是旧字段**（DDL 17 行）：权威积分在 `db_account_points.total_points`，
   图中已在 02 页用 Note 标注。
4. **`db_test_record` 合并了旧表**：`category = word | knowledge` 区分原单词测试与知识测试，
   不要再查找 `db_word_test_record` / `db_knowledge_test_record`。
5. **英文测试成绩不关联题集**：`category = word` 时题目来自 `db_cet4` / `db_cet6`
   （靠 `source` 区分），与 `db_question_set` / `db_test_question` 无关。
6. **整数显示宽度**：DDL 使用 `int(11)` / `bigint(20)`，图中简写为 `int` / `bigint`，
   不改变类型语义。

## 8. 再生与校验

```bash
node scripts/generate-er-diagram.mjs
xmllint --noout docs/architecture/data/learning-program-er.drawio
```

生成脚本内建校验：页面 ID 唯一、关系端点存在、节点包围盒不重叠、每页必须有证据列表。
Draw.io 的像素级渲染未做自动化验收，打开后可自由调整连线走向而不影响模型事实。
