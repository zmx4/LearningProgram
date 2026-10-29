#!/usr/bin/env node
/**
 * 生成 LearningProgram 数据库 E-R 图：
 * - 每个分域输出一份 Mermaid erDiagram（可维护的规范源）
 * - 汇总输出一份多页 .drawio（可编辑交付件）
 *
 * 模型事实来源：scripts/learning.sql（28 张表 DDL）
 * 运行：node scripts/generate-er-diagram.mjs
 */
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const outDir = join(root, 'docs/architecture/data')

const TABLE_W = 320
const HEAD_H = 30
const ROW_H = 22
const PAD_B = 6
const COMPACT_W = 210
const COMPACT_H = 50

const TONE = {
  core: { fill: '#dae8fc', stroke: '#6c8ebf' },
  junction: { fill: '#e1d5e7', stroke: '#9673a6' },
  lookup: { fill: '#d5e8d4', stroke: '#82b366' },
  ref: { fill: '#eef1f5', stroke: '#9aa6b2' },
}

const CARD = {
  'one-zeroOne': { mermaid: '||--o|', arrows: 'ERzeroToOne', text: '1:0..1' },
  'one-zeroMany': { mermaid: '||--o{', arrows: 'ERzeroToMany', text: '1:0..N' },
  'one-oneMany': { mermaid: '||--|{', arrows: 'ERoneToMany', text: '1:1..N' },
}

// 字段元组：[键标记, 字段名, 类型, 说明]
const TABLES = {
  db_account: {
    title: 'db_account 用户账号',
    source: 'scripts/learning.sql:8-23',
    entity: 'Account.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['UK', 'username', 'varchar(32)', '唯一'],
      ['', 'password', 'varchar(255)', 'BCrypt'],
      ['UK', 'email', 'varchar(128)', '唯一'],
      ['', 'role', 'varchar(32)', '默认 user'],
      ['', 'register_time', 'datetime', '注册时间'],
      ['', 'phone', 'varchar(30)', '可空'],
      ['', 'bio', 'varchar(120)', '可空'],
      ['', 'points', 'int(11)', '旧字段，默认 0'],
      ['', 'totp_secret', 'varchar(64)', '可空'],
      ['', 'totp_enabled', 'tinyint(1)', '默认 0'],
    ],
  },
  db_account_points: {
    title: 'db_account_points 积分总账',
    source: 'scripts/learning.sql:25-32',
    entity: 'AccountPoints.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['FK/UK', 'account_id', 'int(11)', '→ db_account'],
      ['', 'total_points', 'int(11)', '默认 0'],
    ],
  },
  db_cet4: {
    title: 'db_cet4 四级词库',
    source: 'scripts/learning.sql:34-39',
    entity: 'Cet4Word.java',
    tone: 'lookup',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['', 'word', 'varchar(255)', ''],
      ['', 'translation', 'varchar(512)', '可空'],
    ],
  },
  db_cet6: {
    title: 'db_cet6 六级词库',
    source: 'scripts/learning.sql:41-46',
    entity: 'Cet6Word.java',
    tone: 'lookup',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['', 'word', 'varchar(255)', ''],
      ['', 'translation', 'varchar(512)', '可空'],
    ],
  },
  db_check_in: {
    title: 'db_check_in 每日签到',
    source: 'scripts/learning.sql:48-57',
    entity: 'CheckInRecord.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['FK', 'account_id', 'int(11)', '→ db_account'],
      ['UK', 'checkin_date', 'date', '与账号联合唯一'],
      ['', 'points', 'int(11)', '当日积分'],
      ['', 'streak', 'int(11)', '连续天数'],
    ],
  },
  db_course: {
    title: 'db_course 课程',
    source: 'scripts/learning.sql:59-68',
    entity: 'Course.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['', 'title', 'varchar(100)', ''],
      ['', 'description', 'varchar(500)', '可空'],
      ['', 'icon', 'varchar(16)', '可空'],
      ['', 'sort_order', 'int(11)', '默认 0'],
      ['', 'created_at', 'datetime', ''],
      ['', 'reward_points', 'int(11)', '默认 0'],
    ],
  },
  db_course_chapter: {
    title: 'db_course_chapter 课程章节',
    source: 'scripts/learning.sql:70-80',
    entity: 'CourseChapter.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['FK', 'course_id', 'int(11)', '→ db_course，级联删除'],
      ['', 'title', 'varchar(150)', ''],
      ['', 'content', 'mediumtext', '可空'],
      ['', 'sort_order', 'int(11)', '默认 0'],
      ['', 'created_at', 'datetime', ''],
    ],
  },
  db_course_progress: {
    title: 'db_course_progress 章节进度',
    source: 'scripts/learning.sql:82-96',
    entity: 'CourseProgress.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['', 'account_id', 'int(11)', '软引用 db_account'],
      ['', 'course_id', 'int(11)', '冗余列，无 FK'],
      ['FK', 'chapter_id', 'int(11)', '→ 章节，级联删除'],
      ['', 'duration_seconds', 'int(11)', '默认 0'],
      ['', 'studied_count', 'int(11)', '默认 1'],
      ['', 'created_at', 'datetime', ''],
      ['', 'updated_at', 'datetime', ''],
    ],
  },
  db_course_question_set: {
    title: 'db_course_question_set 课程↔题集',
    source: 'scripts/learning.sql:98-109',
    entity: 'CourseQuestionSet.java',
    tone: 'junction',
    junction: true,
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['FK', 'course_id', 'int(11)', '→ db_course，级联删除'],
      ['FK', 'set_id', 'int(11)', '→ 题集，级联删除'],
      ['', 'sort_order', 'int(11)', '默认 0'],
      ['', 'created_at', 'datetime', ''],
    ],
  },
  db_course_reward: {
    title: 'db_course_reward 课程奖励',
    source: 'scripts/learning.sql:111-121',
    entity: 'CourseReward.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['', 'account_id', 'int(11)', '软引用，联合唯一'],
      ['FK', 'course_id', 'int(11)', '→ db_course，级联删除'],
      ['', 'points', 'int(11)', '发放积分'],
      ['', 'created_at', 'datetime', ''],
    ],
  },
  db_course_study_log: {
    title: 'db_course_study_log 学习流水',
    source: 'scripts/learning.sql:123-134',
    entity: 'CourseStudyLog.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['', 'account_id', 'int(11)', '软引用 db_account'],
      ['', 'course_id', 'int(11)', '冗余列，无 FK'],
      ['FK', 'chapter_id', 'int(11)', '→ 章节，级联删除'],
      ['', 'duration_seconds', 'int(11)', ''],
      ['', 'created_at', 'datetime', ''],
    ],
  },
  db_dictionary: {
    title: 'db_dictionary 词典',
    source: 'scripts/learning.sql:136-141',
    entity: 'Dictionary.java',
    tone: 'lookup',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['', 'word', 'varchar(255)', ''],
      ['', 'translation', 'text', '可空'],
    ],
  },
  db_discussion_comment: {
    title: 'db_discussion_comment 帖子评论',
    source: 'scripts/learning.sql:143-154',
    entity: 'DiscussionComment.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['FK', 'post_id', 'int(11)', '→ 帖子，级联删除'],
      ['FK', 'account_id', 'int(11)', '→ db_account'],
      ['', 'content', 'varchar(1000)', ''],
      ['', 'created_at', 'datetime', ''],
    ],
  },
  db_discussion_post: {
    title: 'db_discussion_post 讨论帖',
    source: 'scripts/learning.sql:156-167',
    entity: 'DiscussionPost.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['FK', 'account_id', 'int(11)', '→ db_account'],
      ['', 'title', 'varchar(150)', ''],
      ['', 'content', 'text', ''],
      ['', 'comment_count', 'int(11)', '冗余计数，默认 0'],
      ['', 'created_at', 'datetime', ''],
    ],
  },
  db_group_member_visit: {
    title: 'db_group_member_visit 成员访问',
    source: 'scripts/learning.sql:169-177',
    entity: 'GroupMemberVisit.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['FK', 'group_id', 'int(11)', '→ 小组，联合唯一'],
      ['', 'account_id', 'int(11)', '软引用，联合唯一'],
      ['', 'last_visit_at', 'datetime', ''],
    ],
  },
  db_group_task_item: {
    title: 'db_group_task_item 小组任务项',
    source: 'scripts/learning.sql:179-191',
    entity: 'GroupTaskItem.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['', 'group_id', 'int(11)', '冗余列，无 FK'],
      ['FK', 'message_id', 'int(11)', '→ 群消息，级联删除'],
      ['', 'item_type', 'varchar(20)', 'course/resource/…'],
      ['', 'ref_id', 'int(11)', '多态引用，可空'],
      ['', 'title', 'varchar(200)', ''],
      ['', 'sort_order', 'int(11)', '默认 0'],
    ],
  },
  db_group_task_progress: {
    title: 'db_group_task_progress 任务完成',
    source: 'scripts/learning.sql:193-201',
    entity: 'GroupTaskProgress.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['FK', 'item_id', 'int(11)', '→ 任务项，联合唯一'],
      ['', 'account_id', 'int(11)', '软引用，联合唯一'],
      ['', 'completed_at', 'datetime', ''],
    ],
  },
  db_learning_resource: {
    title: 'db_learning_resource 学习资源',
    source: 'scripts/learning.sql:203-216',
    entity: 'LearningResource.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['', 'title', 'varchar(100)', ''],
      ['', 'description', 'varchar(255)', '可空'],
      ['', 'file_name', 'varchar(255)', '原始文件名'],
      ['', 'stored_name', 'varchar(100)', '磁盘存储名'],
      ['', 'content_type', 'varchar(100)', '可空'],
      ['', 'file_size', 'bigint', '默认 0'],
      ['', 'visible', 'tinyint(1)', '默认 1'],
      ['', 'account_id', 'int(11)', '上传者，软引用，可空'],
      ['', 'created_at', 'datetime', ''],
    ],
  },
  db_notification: {
    title: 'db_notification 站内通知',
    source: 'scripts/learning.sql:218-230',
    entity: 'Notification.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['FK', 'account_id', 'int(11)', '→ db_account，级联删除'],
      ['', 'title', 'varchar(128)', ''],
      ['', 'content', 'varchar(1000)', ''],
      ['', 'link', 'varchar(255)', '可空'],
      ['', 'type', 'varchar(32)', '默认 system'],
      ['', 'is_read', 'tinyint(1)', '默认 0'],
      ['', 'created_at', 'datetime', ''],
    ],
  },
  db_question_set: {
    title: 'db_question_set 题集',
    source: 'scripts/learning.sql:232-238',
    entity: 'QuestionSet.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['', 'title', 'varchar(100)', ''],
      ['', 'description', 'varchar(255)', '可空'],
      ['', 'created_at', 'datetime', ''],
    ],
  },
  db_question_set_item: {
    title: 'db_question_set_item 题集↔题目',
    source: 'scripts/learning.sql:240-249',
    entity: 'QuestionSetItem.java',
    tone: 'junction',
    junction: true,
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['FK', 'set_id', 'int(11)', '→ 题集，级联删除'],
      ['FK', 'question_id', 'int(11)', '→ db_test_question'],
    ],
  },
  db_resource_access_log: {
    title: 'db_resource_access_log 资源访问',
    source: 'scripts/learning.sql:251-259',
    entity: 'ResourceAccessLog.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['', 'account_id', 'int(11)', '软引用 db_account'],
      ['', 'resource_id', 'int(11)', '软引用学习资源'],
      ['', 'accessed_at', 'datetime', ''],
    ],
  },
  db_study_group: {
    title: 'db_study_group 学习小组',
    source: 'scripts/learning.sql:261-269',
    entity: 'StudyGroup.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['', 'name', 'varchar(50)', ''],
      ['', 'owner_id', 'int(11)', '软引用 db_account'],
      ['UK', 'invite_code', 'varchar(8)', '唯一邀请码'],
      ['', 'created_at', 'datetime', ''],
    ],
  },
  db_study_group_member: {
    title: 'db_study_group_member 小组成员',
    source: 'scripts/learning.sql:271-281',
    entity: 'StudyGroupMember.java',
    tone: 'junction',
    junction: true,
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['FK', 'group_id', 'int(11)', '→ 小组，级联删除'],
      ['', 'account_id', 'int(11)', '软引用，联合唯一'],
      ['', 'role', 'varchar(10)', 'owner/member'],
      ['', 'joined_at', 'datetime', ''],
    ],
  },
  db_study_group_message: {
    title: 'db_study_group_message 群消息',
    source: 'scripts/learning.sql:283-294',
    entity: 'StudyGroupMessage.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['FK', 'group_id', 'int(11)', '→ 小组，级联删除'],
      ['', 'account_id', 'int(11)', '软引用（发送者）'],
      ['', 'type', 'varchar(10)', 'text/task'],
      ['', 'title', 'varchar(100)', '可空'],
      ['', 'content', 'varchar(2000)', ''],
      ['', 'created_at', 'datetime', ''],
    ],
  },
  db_test_question: {
    title: 'db_test_question 测试题目',
    source: 'scripts/learning.sql:296-305',
    entity: 'TestQuestion.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['FK', 'type_id', 'int(11)', '→ db_test_type'],
      ['', 'kind', 'varchar(20)', 'single/multiple/blank'],
      ['', 'content', 'longtext', 'JSON，json_valid 校验'],
      ['', 'created_at', 'datetime', ''],
    ],
  },
  db_test_record: {
    title: 'db_test_record 测试成绩',
    source: 'scripts/learning.sql:307-328',
    entity: 'TestRecord.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['', 'account_id', 'int(11)', '软引用 db_account'],
      ['', 'category', 'varchar(20)', 'word/knowledge'],
      ['', 'source', 'varchar(10)', 'cet4/cet6，可空'],
      ['', 'type_id', 'int(11)', '软引用，可空'],
      ['FK', 'set_id', 'int(11)', '→ 题集，可空'],
      ['', 'type_name', 'varchar(100)', '类型名快照'],
      ['', 'total_count', 'int(11)', ''],
      ['', 'correct_count', 'int(11)', ''],
      ['', 'wrong_count', 'int(11)', ''],
      ['', 'score', 'int(11)', ''],
      ['', 'duration_seconds', 'int(11)', '默认 0'],
      ['', 'detail', 'longtext', 'JSON 明细，可空'],
      ['', 'created_at', 'datetime', ''],
    ],
  },
  db_test_type: {
    title: 'db_test_type 测试类型',
    source: 'scripts/learning.sql:330-338',
    entity: 'TestType.java',
    fields: [
      ['PK', 'id', 'int(11)', '自增'],
      ['UK', 'code', 'varchar(50)', '唯一编码'],
      ['', 'name', 'varchar(100)', ''],
      ['', 'description', 'varchar(255)', '可空'],
      ['', 'created_at', 'datetime', ''],
    ],
  },
  // 仅用于图例说明的示例表，不是真实表
  db_xxx: {
    title: 'db_xxx 示例表（非真实表）',
    source: '图例说明',
    entity: '—',
    fields: [
      ['PK', 'id', 'int(11)', '主键'],
      ['FK', 'parent_id', 'int(11)', '外键'],
      ['UK', 'code', 'varchar(50)', '唯一键'],
      ['', 'name', 'varchar(100)', '普通字段'],
    ],
  },
}

// kind: fk=数据库外键 | soft=软引用(无 FK) | denorm=冗余列 | poly=多态引用
const RELATIONS = [
  { id: 'r_acc_points', from: 'db_account', to: 'db_account_points', card: 'one-zeroOne', kind: 'fk', label: '积分总账' },
  { id: 'r_acc_checkin', from: 'db_account', to: 'db_check_in', card: 'one-zeroMany', kind: 'fk', label: '签到' },
  { id: 'r_acc_notif', from: 'db_account', to: 'db_notification', card: 'one-zeroMany', kind: 'fk', label: '站内通知' },
  { id: 'r_acc_post', from: 'db_account', to: 'db_discussion_post', card: 'one-zeroMany', kind: 'fk', label: '发布帖子' },
  { id: 'r_acc_comment', from: 'db_account', to: 'db_discussion_comment', card: 'one-zeroMany', kind: 'fk', label: '发表评论' },
  { id: 'r_post_comment', from: 'db_discussion_post', to: 'db_discussion_comment', card: 'one-zeroMany', kind: 'fk', label: '帖子评论' },
  { id: 'r_course_chapter', from: 'db_course', to: 'db_course_chapter', card: 'one-zeroMany', kind: 'fk', label: '包含章节' },
  { id: 'r_chapter_progress', from: 'db_course_chapter', to: 'db_course_progress', card: 'one-zeroMany', kind: 'fk', label: '章节进度' },
  { id: 'r_chapter_studylog', from: 'db_course_chapter', to: 'db_course_study_log', card: 'one-zeroMany', kind: 'fk', label: '学习流水' },
  { id: 'r_course_cqs', from: 'db_course', to: 'db_course_question_set', card: 'one-zeroMany', kind: 'fk', label: '关联题集' },
  { id: 'r_set_cqs', from: 'db_question_set', to: 'db_course_question_set', card: 'one-zeroMany', kind: 'fk', label: '被课程关联' },
  { id: 'r_course_reward', from: 'db_course', to: 'db_course_reward', card: 'one-zeroMany', kind: 'fk', label: '课程奖励' },
  { id: 'r_type_question', from: 'db_test_type', to: 'db_test_question', card: 'one-zeroMany', kind: 'fk', label: '题目类型' },
  { id: 'r_set_itemset', from: 'db_question_set', to: 'db_question_set_item', card: 'one-zeroMany', kind: 'fk', label: '包含题目' },
  { id: 'r_question_itemset', from: 'db_test_question', to: 'db_question_set_item', card: 'one-zeroMany', kind: 'fk', label: '被题集收录' },
  { id: 'r_set_testrec', from: 'db_question_set', to: 'db_test_record', card: 'one-zeroMany', kind: 'fk', label: '题集模式成绩' },
  { id: 'r_group_member', from: 'db_study_group', to: 'db_study_group_member', card: 'one-zeroMany', kind: 'fk', label: '小组成员' },
  { id: 'r_group_message', from: 'db_study_group', to: 'db_study_group_message', card: 'one-zeroMany', kind: 'fk', label: '群消息' },
  { id: 'r_group_visit', from: 'db_study_group', to: 'db_group_member_visit', card: 'one-zeroMany', kind: 'fk', label: '成员访问' },
  { id: 'r_msg_taskitem', from: 'db_study_group_message', to: 'db_group_task_item', card: 'one-zeroMany', kind: 'fk', label: '任务项' },
  { id: 'r_taskitem_progress', from: 'db_group_task_item', to: 'db_group_task_progress', card: 'one-zeroMany', kind: 'fk', label: '任务完成' },

  { id: 's_acc_progress', from: 'db_account', to: 'db_course_progress', card: 'one-zeroMany', kind: 'soft', label: '软引用 account_id' },
  { id: 's_acc_studylog', from: 'db_account', to: 'db_course_study_log', card: 'one-zeroMany', kind: 'soft', label: '软引用 account_id' },
  { id: 's_acc_reward', from: 'db_account', to: 'db_course_reward', card: 'one-zeroMany', kind: 'soft', label: '软引用 account_id' },
  { id: 's_acc_testrec', from: 'db_account', to: 'db_test_record', card: 'one-zeroMany', kind: 'soft', label: '软引用 account_id' },
  { id: 's_type_testrec', from: 'db_test_type', to: 'db_test_record', card: 'one-zeroOne', kind: 'soft', label: '软引用 type_id（可空）' },
  { id: 's_acc_group', from: 'db_account', to: 'db_study_group', card: 'one-zeroMany', kind: 'soft', label: '软引用 owner_id' },
  { id: 's_acc_member', from: 'db_account', to: 'db_study_group_member', card: 'one-zeroMany', kind: 'soft', label: '软引用 account_id' },
  { id: 's_acc_message', from: 'db_account', to: 'db_study_group_message', card: 'one-zeroMany', kind: 'soft', label: '软引用发送者' },
  { id: 's_acc_taskprogress', from: 'db_account', to: 'db_group_task_progress', card: 'one-zeroMany', kind: 'soft', label: '软引用 account_id' },
  { id: 's_acc_visit', from: 'db_account', to: 'db_group_member_visit', card: 'one-zeroMany', kind: 'soft', label: '软引用 account_id' },
  { id: 's_acc_resource', from: 'db_account', to: 'db_learning_resource', card: 'one-zeroMany', kind: 'soft', label: '上传者（可空）' },
  { id: 's_acc_accesslog', from: 'db_account', to: 'db_resource_access_log', card: 'one-zeroMany', kind: 'soft', label: '软引用 account_id' },
  { id: 's_resource_accesslog', from: 'db_learning_resource', to: 'db_resource_access_log', card: 'one-zeroMany', kind: 'soft', label: '软引用 resource_id' },

  { id: 'd_course_progress', from: 'db_course', to: 'db_course_progress', card: 'one-zeroMany', kind: 'denorm', label: '冗余 course_id' },
  { id: 'd_course_studylog', from: 'db_course', to: 'db_course_study_log', card: 'one-zeroMany', kind: 'denorm', label: '冗余 course_id' },
  { id: 'd_group_taskitem', from: 'db_study_group', to: 'db_group_task_item', card: 'one-zeroMany', kind: 'denorm', label: '冗余 group_id' },

  { id: 'p_taskitem_course', from: 'db_group_task_item', to: 'db_course', card: 'one-zeroMany', kind: 'poly', label: 'ref_id → 课程' },
  { id: 'p_taskitem_resource', from: 'db_group_task_item', to: 'db_learning_resource', card: 'one-zeroMany', kind: 'poly', label: 'ref_id → 资源' },
]

const FK_IDS = RELATIONS.filter(rel => rel.kind === 'fk').map(rel => rel.id)

const PAGES = [
  {
    id: '00-legend',
    name: '00 图例与阅读说明',
    title: 'E-R 图图例：如何阅读本组视图',
    noMermaid: true,
    nodes: [
      { table: 'db_xxx', x: 80, y: 110 },
      { id: 'n_keys', note: '字段前缀\n\nPK  主键\nFK  数据库外键（真实 CONSTRAINT）\nUK  唯一键\n\nFK/UK 表示既是外键又唯一，\n常见于 1:0..1 关系。', x: 440, y: 110, w: 420, h: 150 },
      { compact: 'db_course', id: 'lg_course1', label: 'db_course', x: 80, y: 300 },
      { compact: 'db_course_chapter', id: 'lg_chapter', label: 'db_course_chapter', x: 400, y: 300 },
      { id: 'n_fk', note: '实线 + 鸦爪：数据库外键\n\n线段两端标出基数：1 端为竖线，N 端为鸦爪；\nON DELETE CASCADE 在目标表名后标注。', x: 80, y: 380, w: 600, h: 100 },
      { compact: 'db_account', id: 'lg_account', label: 'db_account', x: 80, y: 520 },
      { compact: 'db_test_record', id: 'lg_testrec', label: 'db_test_record', x: 400, y: 520 },
      { id: 'n_soft', note: '灰色虚线：软引用 / 冗余列 / 多态引用\n\n软引用：只有字段命名与文档约定，数据库不建 FK；\n冗余列、多态 ref_id 也使用虚线，不表示强约束。', x: 80, y: 600, w: 600, h: 110 },
      { compact: 'db_course', id: 'lg_course2', label: 'db_course', x: 80, y: 740 },
      { compact: 'db_question_set', id: 'lg_qset', label: 'db_question_set', x: 400, y: 740 },
      { table: 'db_course_question_set', x: 200, y: 830 },
      { id: 'n_mn', note: '连接表与 M:N\n\n两个 1:N 外键组成多对多：\n课程 ↔ 题集（db_course_question_set）\n题集 ↔ 题目（db_question_set_item）\n小组 ↔ 账号（db_study_group_member）', x: 620, y: 830, w: 480, h: 150 },
      { id: 'n_poly', note: '多态引用：db_group_task_item.ref_id\n\nitem_type = course   → ref_id 指向 db_course\nitem_type = resource → ref_id 指向 db_learning_resource\nitem_type = text / member_visit → ref_id 为空\n数据库无法用单个 FK 表达，图中用紫色虚线标注。', x: 80, y: 1020, w: 1020, h: 110 },
    ],
    sampleEdges: [
      { from: 'lg_course1', to: 'lg_chapter', card: 'one-zeroMany', kind: 'fk', label: '1 : 0..N' },
      { from: 'lg_account', to: 'lg_testrec', card: 'one-zeroMany', kind: 'soft', label: '软引用' },
      { from: 'lg_course2', to: 'db_course_question_set', card: 'one-zeroMany', kind: 'fk', label: '1 : 0..N' },
      { from: 'lg_qset', to: 'db_course_question_set', card: 'one-zeroMany', kind: 'fk', label: '1 : 0..N' },
    ],
    evidence: [
      'scripts/learning.sql:8-338（28 张表 DDL；图例中的 db_xxx 为示例，不是真实表）',
      '字段前缀来自各表 PRIMARY KEY / UNIQUE KEY / CONSTRAINT FOREIGN KEY 子句',
    ],
  },
  {
    id: '01-overview',
    name: '01 实体总览',
    title: '实体总览：28 张表与数据库外键关系',
    overview: true,
    nodes: [
      { bg: true, id: 'c1', label: '账号与积分', x: 40, y: 80, w: 520, h: 300 },
      { compact: 'db_account', x: 70, y: 150 },
      { compact: 'db_account_points', x: 320, y: 150 },
      { compact: 'db_check_in', x: 70, y: 250 },
      { compact: 'db_notification', x: 320, y: 250 },

      { bg: true, id: 'c2', label: '课程学习', x: 600, y: 80, w: 560, h: 360 },
      { compact: 'db_course', x: 630, y: 150 },
      { compact: 'db_course_chapter', x: 920, y: 150 },
      { compact: 'db_course_progress', x: 630, y: 250 },
      { compact: 'db_course_study_log', x: 920, y: 250 },
      { compact: 'db_course_reward', x: 630, y: 350 },

      { bg: true, id: 'c3', label: '题库与测试', x: 1200, y: 80, w: 560, h: 420 },
      { compact: 'db_test_type', x: 1230, y: 150 },
      { compact: 'db_test_question', x: 1230, y: 250 },
      { compact: 'db_question_set', x: 1230, y: 350 },
      { compact: 'db_test_record', x: 1520, y: 150 },
      { compact: 'db_question_set_item', x: 1520, y: 250 },
      { compact: 'db_course_question_set', x: 1520, y: 350 },

      { bg: true, id: 'c4', label: '学习小组', x: 40, y: 520, w: 560, h: 460 },
      { compact: 'db_study_group', x: 70, y: 590 },
      { compact: 'db_study_group_member', x: 320, y: 590 },
      { compact: 'db_study_group_message', x: 320, y: 690 },
      { compact: 'db_group_member_visit', x: 70, y: 690 },
      { compact: 'db_group_task_item', x: 320, y: 790 },
      { compact: 'db_group_task_progress', x: 70, y: 790 },

      { bg: true, id: 'c5', label: '讨论与资源', x: 640, y: 520, w: 560, h: 300 },
      { compact: 'db_discussion_post', x: 670, y: 590 },
      { compact: 'db_discussion_comment', x: 920, y: 590 },
      { compact: 'db_learning_resource', x: 670, y: 690 },
      { compact: 'db_resource_access_log', x: 920, y: 690 },

      { bg: true, id: 'c6', label: '词典与词库（独立表，无外键）', x: 640, y: 860, w: 840, h: 160 },
      { compact: 'db_dictionary', x: 670, y: 920 },
      { compact: 'db_cet4', x: 920, y: 920 },
      { compact: 'db_cet6', x: 1170, y: 920 },

      {
        id: 'n_read',
        note: '阅读说明\n\n本页只画数据库真实外键（实线）。\naccount_id / owner_id / type_id 等\n软引用不在本页展开，见 02 - 06 分域页。\n\n紫色连接表表示 M:N：\n课程 ↔ 题集、题集 ↔ 题目、小组 ↔ 账号。',
        x: 1200,
        y: 520,
        w: 520,
        h: 300,
      },
    ],
    relations: FK_IDS,
    evidence: [
      'scripts/learning.sql:8-338（28 张表 DDL）',
      '21 条 CONSTRAINT ... FOREIGN KEY 子句；连接表：db_course_question_set、db_question_set_item、db_study_group_member',
      '软引用、冗余列、多态引用在 02 - 06 分域页用虚线标注',
    ],
  },
  {
    id: '02-account',
    name: '02 账号与积分',
    title: '账号与积分：账号、积分总账、签到与通知',
    nodes: [
      { table: 'db_account', x: 60, y: 110 },
      { table: 'db_account_points', x: 460, y: 110 },
      { table: 'db_check_in', x: 460, y: 270 },
      { table: 'db_notification', x: 460, y: 474 },
      {
        id: 'n1',
        note: '1 : 0..1 积分总账\n\ndb_account_points.account_id 同时是\n外键与唯一键，因此每个账号最多一行；\n权威积分取 total_points。',
        x: 840,
        y: 110,
        w: 340,
        h: 150,
      },
      {
        id: 'n2',
        note: 'db_account.points 为历史遗留字段\n（learning.sql:17，默认 0）。\n当前积分以 db_account_points\n为准，迁移或统计时不要重复计算。',
        x: 840,
        y: 300,
        w: 340,
        h: 150,
      },
    ],
    relations: ['r_acc_points', 'r_acc_checkin', 'r_acc_notif'],
    evidence: [
      'scripts/learning.sql:8-32（db_account、db_account_points 及 FK/UNIQUE）',
      'scripts/learning.sql:48-57（db_check_in，唯一键 account_id + checkin_date）',
      'scripts/learning.sql:218-230（db_notification，FK ON DELETE CASCADE）',
      'docs/DATABASE.md:86-112（points 旧字段说明）',
    ],
  },
  {
    id: '03-course',
    name: '03 课程学习',
    title: '课程学习：章节、进度、流水与完成奖励',
    nodes: [
      { table: 'db_course', x: 80, y: 110 },
      { table: 'db_course_chapter', x: 460, y: 110 },
      { table: 'db_course_progress', x: 460, y: 330 },
      { table: 'db_course_study_log', x: 460, y: 610 },
      { table: 'db_course_reward', x: 840, y: 110 },
      { table: 'db_account', rows: ['id'], ref: true, x: 840, y: 320 },
      { table: 'db_course_question_set', x: 840, y: 440 },
      { ghost: 'db_question_set', label: 'db_question_set\n（完整定义见 04 题库页）', x: 860, y: 650 },
      {
        id: 'n1',
        note: '冗余列：\n\ndb_course_progress.course_id 与\ndb_course_study_log.course_id\n没有外键，课程归属应通过 chapter_id\n关联 db_course_chapter 推导。',
        x: 1180,
        y: 110,
        w: 360,
        h: 170,
      },
      {
        id: 'n2',
        note: '唯一性：\n\nuk_course_progress(account_id, chapter_id)\n保证每个用户每章只有一行进度；\nuk_course_reward(account_id, course_id)\n保证课程奖励只发一次。',
        x: 1180,
        y: 320,
        w: 360,
        h: 170,
      },
    ],
    relations: [
      'r_course_chapter',
      'r_chapter_progress',
      'r_chapter_studylog',
      'r_course_reward',
      'r_course_cqs',
      'r_set_cqs',
      's_acc_progress',
      's_acc_studylog',
      's_acc_reward',
      'd_course_progress',
      'd_course_studylog',
    ],
    evidence: [
      'scripts/learning.sql:59-96（课程、章节、进度及级联删除）',
      'scripts/learning.sql:98-121（课程↔题集连接表、课程奖励）',
      'scripts/learning.sql:123-134（学习流水，仅 chapter_id 有 FK）',
      'scripts/learning.sql:84-85,125-126（account_id / course_id 无 FK）',
    ],
  },
  {
    id: '04-test',
    name: '04 题库与测试',
    title: '题库与测试：类型、题目、题集与成绩',
    nodes: [
      { table: 'db_test_type', x: 80, y: 120 },
      { table: 'db_test_question', x: 470, y: 120 },
      { table: 'db_question_set', x: 80, y: 360 },
      { table: 'db_question_set_item', x: 470, y: 360 },
      { table: 'db_course_question_set', x: 860, y: 360 },
      { table: 'db_test_record', x: 1200, y: 120 },
      { table: 'db_account', rows: ['id'], ref: true, x: 1200, y: 520 },
      { ghost: 'db_course', label: 'db_course\n（完整定义见 03 课程页）', x: 470, y: 560 },
      { table: 'db_cet4', x: 80, y: 600 },
      { table: 'db_cet6', x: 80, y: 740 },
      {
        id: 'n1',
        note: '单词测试不落成绩题集：\n\ndb_test_record.category = word 时，\nsource 取 cet4 / cet6，\n题目来自 db_cet4 / db_cet6，\n与 db_test_question 无关。',
        x: 470,
        y: 690,
        w: 340,
        h: 170,
      },
      {
        id: 'n2',
        note: 'db_test_record 的弱引用：\n\naccount_id 无 FK；type_id 可空且无 FK，\n只保留 type_name 快照；\nset_id 是可空外键，指向 db_question_set。\ncategory = word | knowledge。',
        x: 860,
        y: 560,
        w: 340,
        h: 180,
      },
    ],
    relations: [
      'r_type_question',
      'r_set_itemset',
      'r_question_itemset',
      'r_course_cqs',
      'r_set_cqs',
      'r_set_testrec',
      's_acc_testrec',
      's_type_testrec',
    ],
    evidence: [
      'scripts/learning.sql:296-305（题目，FK type_id）',
      'scripts/learning.sql:330-338（测试类型）',
      'scripts/learning.sql:232-249（题集与题集↔题目连接表）',
      'scripts/learning.sql:307-328（成绩表：仅 set_id 有 FK，account_id / type_id 无 FK）',
      'scripts/learning.sql:34-46（db_cet4 / db_cet6 独立词库）',
    ],
  },
  {
    id: '05-group',
    name: '05 学习小组',
    title: '学习小组：成员、群消息、任务项与完成记录',
    nodes: [
      { table: 'db_study_group', x: 80, y: 120 },
      { table: 'db_study_group_member', x: 470, y: 120 },
      { table: 'db_study_group_message', x: 470, y: 320 },
      { table: 'db_group_member_visit', x: 860, y: 120 },
      { table: 'db_group_task_item', x: 860, y: 320 },
      { table: 'db_group_task_progress', x: 860, y: 560 },
      { table: 'db_account', rows: ['id'], ref: true, x: 80, y: 340 },
      { ghost: 'db_course', label: 'db_course\n（item_type = course）', x: 1200, y: 320 },
      { ghost: 'db_learning_resource', label: 'db_learning_resource\n（item_type = resource）', x: 1200, y: 440 },
      {
        id: 'n1',
        note: '账号侧全部是软引用：\nowner_id、成员的 account_id、\n消息发送者、任务完成人、访问记录\n均无 FK，详见 learning.sql。',
        x: 80,
        y: 470,
        w: 320,
        h: 170,
      },
      {
        id: 'n2',
        note: '多态 ref_id：\n\nitem_type = course → db_course\nitem_type = resource → db_learning_resource\nitem_type = text / member_visit → ref_id 为空\n无法用单个 FK 表达。',
        x: 1200,
        y: 560,
        w: 340,
        h: 170,
      },
    ],
    relations: [
      'r_group_member',
      'r_group_message',
      'r_group_visit',
      'r_msg_taskitem',
      'r_taskitem_progress',
      's_acc_group',
      's_acc_member',
      's_acc_message',
      's_acc_visit',
      's_acc_taskprogress',
      'd_group_taskitem',
      'p_taskitem_course',
      'p_taskitem_resource',
    ],
    evidence: [
      'scripts/learning.sql:261-294（小组、成员连接表、群消息）',
      'scripts/learning.sql:169-201（访问记录、任务项、任务完成）',
      'scripts/learning.sql:179-191（ref_id 多态引用，无 FK）',
      'docs/DATABASE.md:290-367（账号侧软引用说明）',
    ],
  },
  {
    id: '06-social',
    name: '06 讨论、资源与词典',
    title: '讨论、资源与词典：帖子评论、资源访问与独立词库',
    nodes: [
      { table: 'db_discussion_post', x: 80, y: 120 },
      { table: 'db_discussion_comment', x: 470, y: 120 },
      { table: 'db_learning_resource', x: 860, y: 120 },
      { table: 'db_resource_access_log', x: 1200, y: 120 },
      { table: 'db_dictionary', x: 80, y: 420 },
      { table: 'db_account', rows: ['id'], ref: true, x: 470, y: 360 },
      {
        id: 'n1',
        note: '资源访问流水：\n\naccount_id、resource_id 都是软引用，\n无 FK；删除资源后流水保留，\n用于统计与成员访问判定。',
        x: 1200,
        y: 320,
        w: 340,
        h: 160,
      },
      {
        id: 'n2',
        note: 'db_dictionary / db_cet4 / db_cet6\n是独立词库表，没有外键；\ndb_cet4、db_cet6 见 04 题库页。',
        x: 80,
        y: 600,
        w: 320,
        h: 130,
      },
    ],
    relations: ['r_acc_post', 'r_acc_comment', 'r_post_comment', 's_acc_resource', 's_acc_accesslog', 's_resource_accesslog'],
    evidence: [
      'scripts/learning.sql:143-167（帖子、评论及 FK）',
      'scripts/learning.sql:203-216（资源，上传者 account_id 可空软引用）',
      'scripts/learning.sql:251-259（访问流水，无 FK）',
      'scripts/learning.sql:136-141（词典独立表）',
    ],
  },
]

function xmlEscape(text) {
  return String(text)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
}

function drawioLabel(text) {
  return xmlEscape(text).replaceAll('\n', '&lt;br&gt;')
}

function mermaidText(text) {
  return String(text).replaceAll('"', "'").replaceAll('\n', ' ')
}

function tableRows(node) {
  const table = TABLES[node.table]
  return node.rows ? table.fields.filter(field => node.rows.includes(field[1])) : table.fields
}

function nodeBox(node) {
  if (node.bg || node.note || node.ghost) {
    return { x: node.x, y: node.y, w: node.w ?? 320, h: node.h ?? (node.ghost ? 64 : 120) }
  }
  if (node.compact) {
    return { x: node.x, y: node.y, w: node.w ?? COMPACT_W, h: node.h ?? COMPACT_H }
  }
  const rows = tableRows(node)
  return { x: node.x, y: node.y, w: node.w ?? TABLE_W, h: node.h ?? HEAD_H + rows.length * ROW_H + PAD_B }
}

function toneOf(node) {
  if (node.ref) return TONE.ref
  return TONE[TABLES[node.table].tone ?? 'core']
}

function renderTable(page, node) {
  const table = TABLES[node.table]
  const rows = tableRows(node)
  const box = nodeBox(node)
  const id = `${page.id}-${node.table}`
  const tone = toneOf(node)
  const title = node.ref ? `${node.table}（引用）` : table.title
  const cells = [
    `<mxCell id="${id}" value="${drawioLabel(title)}" style="swimlane;fontStyle=1;align=left;verticalAlign=top;spacingLeft=8;startSize=${HEAD_H};container=1;collapsible=0;childLayout=stackLayout;horizontal=1;horizontalStack=0;resizeParent=1;resizeParentMax=0;resizeLast=0;marginBottom=0;html=1;whiteSpace=wrap;rounded=0;fillColor=#ffffff;swimlaneFillColor=${tone.fill};strokeColor=${tone.stroke};fontSize=12;fontColor=#1f2d3d;" vertex="1" parent="1"><mxGeometry x="${box.x}" y="${box.y}" width="${box.w}" height="${box.h}" as="geometry" /></mxCell>`,
  ]
  rows.forEach((field, index) => {
    const [key, name, type, note] = field
    const lead = key ? `${key} ` : '\u00a0\u00a0\u00a0\u00a0\u00a0\u00a0'
    const text = `${lead}${name} : ${type}${note ? ` · ${note}` : ''}`
    const bold = key.startsWith('PK') ? 'fontStyle=1;' : ''
    cells.push(
      `<mxCell id="${id}-f${index}" value="${drawioLabel(text)}" style="text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;spacingLeft=10;spacingRight=4;fontSize=10;fontColor=#243447;overflow=hidden;whiteSpace=nowrap;${bold}" vertex="1" parent="${id}"><mxGeometry y="${HEAD_H + index * ROW_H}" width="${box.w}" height="${ROW_H}" as="geometry" /></mxCell>`,
    )
  })
  return { cells, box }
}

function renderCompact(page, node) {
  const table = TABLES[node.compact]
  const tone = TONE[table.tone ?? 'core']
  const box = nodeBox(node)
  const label = node.label ?? table.title
  return {
    cells: [
      `<mxCell id="${page.id}-${node.id ?? node.compact}" value="${drawioLabel(label)}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${tone.fill};strokeColor=${tone.stroke};fontSize=11;fontColor=#1f2d3d;" vertex="1" parent="1"><mxGeometry x="${box.x}" y="${box.y}" width="${box.w}" height="${box.h}" as="geometry" /></mxCell>`,
    ],
    box,
  }
}

function renderGhost(page, node) {
  const box = nodeBox(node)
  return {
    cells: [
      `<mxCell id="${page.id}-${node.ghost}" value="${drawioLabel(node.label ?? node.ghost)}" style="rounded=1;whiteSpace=wrap;html=1;dashed=1;fillColor=#f5f5f5;strokeColor=#9aa6b2;fontColor=#6b7785;fontSize=10;align=center;verticalAlign=middle;" vertex="1" parent="1"><mxGeometry x="${box.x}" y="${box.y}" width="${box.w}" height="${box.h}" as="geometry" /></mxCell>`,
    ],
    box,
  }
}

function renderNote(page, node) {
  const box = nodeBox(node)
  return {
    cells: [
      `<mxCell id="${page.id}-${node.id}" value="${drawioLabel(node.note)}" style="shape=note;whiteSpace=wrap;html=1;backgroundOutline=1;darkOpacity=0.05;fillColor=#fff2cc;strokeColor=#d6b656;align=left;verticalAlign=top;spacingLeft=8;spacingTop=4;fontSize=10;fontColor=#5b4a1f;" vertex="1" parent="1"><mxGeometry x="${box.x}" y="${box.y}" width="${box.w}" height="${box.h}" as="geometry" /></mxCell>`,
    ],
    box,
  }
}

function renderBg(page, node) {
  const box = nodeBox(node)
  return {
    cells: [
      `<mxCell id="${page.id}-${node.id}" value="${drawioLabel(node.label)}" style="rounded=0;whiteSpace=wrap;html=1;fillColor=#f7f9fc;strokeColor=#c7d2e0;dashed=1;verticalAlign=top;align=left;spacingLeft=10;spacingTop=6;fontSize=12;fontStyle=1;fontColor=#52627a;" vertex="1" parent="1"><mxGeometry x="${box.x}" y="${box.y}" width="${box.w}" height="${box.h}" as="geometry" /></mxCell>`,
    ],
    box,
  }
}

function edgeStyle(rel) {
  const parts = [
    'edgeStyle=entityRelationEdgeStyle',
    'rounded=0',
    'html=1',
    'fontSize=9',
    'fontColor=#3d4b5c',
    'labelBackgroundColor=#ffffff',
    'startFill=0',
    'endFill=0',
  ]
  if (rel.kind === 'fk') {
    parts.push('strokeColor=#4a6da7', 'strokeWidth=1.2', 'startArrow=ERmandOne', `endArrow=${CARD[rel.card].arrows}`)
  } else if (rel.kind === 'soft') {
    parts.push('strokeColor=#9aa6b2', 'dashed=1', 'fontColor=#7b8794', 'startArrow=ERmandOne', `endArrow=${CARD[rel.card].arrows}`)
  } else if (rel.kind === 'denorm') {
    parts.push('strokeColor=#9aa6b2', 'dashed=1', 'fontColor=#7b8794', 'startArrow=none', 'endArrow=open')
  } else {
    parts.push('strokeColor=#a680c4', 'dashed=1', 'fontColor=#8a5fa8', 'startArrow=none', 'endArrow=open')
  }
  return `${parts.join(';')};`
}

function renderEdge(page, rel, index) {
  const label = rel.label ? ` value="${drawioLabel(rel.label)}"` : ''
  return `<mxCell id="${page.id}-e${index}"${label} style="${edgeStyle(rel)}" edge="1" parent="1" source="${page.id}-${rel.from}" target="${page.id}-${rel.to}"><mxGeometry relative="1" as="geometry" /></mxCell>`
}

function validatePage(page) {
  const ids = new Set()
  for (const node of page.nodes) {
    const id = node.bg || node.note ? node.id : node.ghost ? node.ghost : node.table ?? node.compact
    const key = node.compact ? node.id ?? node.compact : id
    if (!key) throw new Error(`${page.id}: node without id`)
    if (ids.has(key)) throw new Error(`${page.id}: duplicate node id ${key}`)
    ids.add(key)
    if (node.table && !TABLES[node.table]) throw new Error(`${page.id}: unknown table ${node.table}`)
    if (node.compact && !TABLES[node.compact]) throw new Error(`${page.id}: unknown table ${node.compact}`)
  }
  const relations = [...(page.relations ?? [])]
  for (const id of relations) {
    const rel = RELATIONS.find(item => item.id === id)
    if (!rel) throw new Error(`${page.id}: unknown relation ${id}`)
    if (!ids.has(rel.from)) throw new Error(`${page.id}: relation ${id} missing from node ${rel.from}`)
    if (!ids.has(rel.to)) throw new Error(`${page.id}: relation ${id} missing to node ${rel.to}`)
  }
  for (const rel of page.sampleEdges ?? []) {
    if (!ids.has(rel.from)) throw new Error(`${page.id}: sample edge missing from ${rel.from}`)
    if (!ids.has(rel.to)) throw new Error(`${page.id}: sample edge missing to ${rel.to}`)
    if (!CARD[rel.card]) throw new Error(`${page.id}: unknown card ${rel.card}`)
  }
  if (!page.evidence?.length) throw new Error(`${page.id}: evidence is required`)
  const boxes = []
  for (const node of page.nodes) {
    if (node.bg) continue
    const box = nodeBox(node)
    boxes.push([node.id ?? node.ghost ?? node.table ?? node.compact, box])
  }
  for (let i = 0; i < boxes.length; i += 1) {
    for (let j = i + 1; j < boxes.length; j += 1) {
      const [firstId, first] = boxes[i]
      const [secondId, second] = boxes[j]
      const overlapX = Math.min(first.x + first.w, second.x + second.w) - Math.max(first.x, second.x)
      const overlapY = Math.min(first.y + first.h, second.y + second.h) - Math.max(first.y, second.y)
      if (overlapX > 2 && overlapY > 2) throw new Error(`${page.id}: node overlap ${firstId} <> ${secondId}`)
    }
  }
}

function buildPage(page) {
  validatePage(page)
  const bgCells = []
  const cells = []
  const bounds = []
  for (const node of page.nodes) {
    if (node.bg) {
      const { cells: bg, box } = renderBg(page, node)
      bgCells.push(...bg)
      bounds.push(box)
      continue
    }
    let rendered
    if (node.note) rendered = renderNote(page, node)
    else if (node.ghost) rendered = renderGhost(page, node)
    else if (node.compact) rendered = renderCompact(page, node)
    else rendered = renderTable(page, node)
    cells.push(...rendered.cells)
    if (rendered.box) bounds.push(rendered.box)
  }

  let edgeIndex = 0
  const edges = []
  for (const id of page.relations ?? []) {
    const rel = RELATIONS.find(item => item.id === id)
    edges.push(renderEdge(page, rel, ++edgeIndex))
  }
  for (const rel of page.sampleEdges ?? []) {
    edges.push(renderEdge(page, rel, ++edgeIndex))
  }

  const right = Math.max(...bounds.map(box => box.x + box.w))
  const bottom = Math.max(...bounds.map(box => box.y + box.h))
  const pageWidth = Math.max(1200, Math.ceil(right + 40))
  const evidenceY = Math.ceil(bottom + 26)
  const evidenceHeight = Math.min(150, 34 + page.evidence.length * 14)
  const pageHeight = evidenceY + evidenceHeight + 40
  const prefix = page.id

  const evidenceCell = `<mxCell id="${prefix}-evidence" value="${drawioLabel(`证据（当前 DDL）：\n${page.evidence.join('\n')}`)}" style="shape=note;whiteSpace=wrap;html=1;backgroundOutline=1;darkOpacity=0.05;fillColor=#f5f5f5;strokeColor=#999999;align=left;verticalAlign=top;spacingLeft=8;spacingTop=4;fontSize=10;fontColor=#333333;" vertex="1" parent="1"><mxGeometry x="40" y="${evidenceY}" width="${pageWidth - 80}" height="${evidenceHeight}" as="geometry" /></mxCell>`

  const all = [
    `<mxCell id="${prefix}-title" value="${drawioLabel(page.title)}" style="text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;fontSize=20;fontStyle=1;fontColor=#1f2d3d;" vertex="1" parent="1"><mxGeometry x="40" y="20" width="${pageWidth - 80}" height="34" as="geometry" /></mxCell>`,
    ...bgCells,
    ...cells,
    ...edges,
    evidenceCell,
  ]

  const xml = `<diagram id="${page.id}" name="${drawioLabel(page.name)}">
      <mxGraphModel dx="${pageWidth}" dy="${pageHeight}" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${pageWidth}" pageHeight="${pageHeight}" math="0" shadow="0">
        <root>
          <mxCell id="0" />
          <mxCell id="1" parent="0" />
          ${all.join('\n          ')}
        </root>
      </mxGraphModel>
    </diagram>`

  return { xml, pageWidth, pageHeight }
}

function buildMermaid(page) {
  if (page.noMermaid) return null
  const lines = [
    `%% ${page.name}（由 scripts/generate-er-diagram.mjs 生成，修改请改脚本）`,
    'erDiagram',
  ]
  const seen = new Set()
  for (const node of page.nodes) {
    const name = node.ghost ?? node.table ?? node.compact
    if (!name || node.bg || node.note) continue
    if (seen.has(name)) continue
    seen.add(name)
    const table = TABLES[name]
    const rows = page.overview || node.compact || node.ghost || node.ref ? table.fields.filter(field => field[1] === 'id') : tableRows(node)
    lines.push(`  ${name} {`)
    for (const [key, field, type, note] of rows) {
      const keyToken = key ? key.split('/')[0] : ''
      const comment = `${type}${note ? ` ${note}` : ''}`
      lines.push(`    ${type.replace(/\(.*\)$/, '')} ${field}${keyToken ? ` ${keyToken}` : ''} "${mermaidText(comment)}"`)
    }
    lines.push('  }')
  }

  lines.push('')
  for (const id of page.relations ?? []) {
    const rel = RELATIONS.find(item => item.id === id)
    const dotted = rel.kind === 'fk' ? '--' : '..'
    const card = CARD[rel.card].mermaid.replace('--', dotted)
    lines.push(`  ${rel.from} ${card} ${rel.to} : "${mermaidText(rel.label)} ${CARD[rel.card].text}"`)
  }

  lines.push('', '  %% 证据：')
  for (const item of page.evidence) lines.push(`  %% - ${item}`)
  return `${lines.join('\n')}\n`
}

function main() {
  mkdirSync(outDir, { recursive: true })
  const diagrams = []
  const summary = []
  const seen = new Set()

  for (const page of PAGES) {
    if (seen.has(page.id)) throw new Error(`duplicate page id ${page.id}`)
    seen.add(page.id)
    const { xml, pageWidth, pageHeight } = buildPage(page)
    diagrams.push(xml)
    const mmd = buildMermaid(page)
    if (mmd) writeFileSync(join(outDir, `${page.id}.mmd`), mmd, 'utf8')
    const tableCount = page.nodes.filter(node => node.table && !node.note).length
    summary.push({
      id: page.id,
      name: page.name,
      tables: tableCount,
      relations: (page.relations?.length ?? 0) + (page.sampleEdges?.length ?? 0),
      width: pageWidth,
      height: pageHeight,
      mermaid: Boolean(mmd),
    })
  }

  const mxfile = `<mxfile host="app.diagrams.net" agent="Qoder" version="24.7.17" type="device">
  ${diagrams.join('\n  ')}
</mxfile>
`
  writeFileSync(join(outDir, 'learning-program-er.drawio'), mxfile, 'utf8')
  writeFileSync(join(outDir, 'er.manifest.json'), `${JSON.stringify(summary, null, 2)}\n`, 'utf8')

  console.log(`generated ${summary.length} pages -> ${outDir}`)
  for (const item of summary) {
    console.log(`  ${item.id}: ${item.tables} tables, ${item.relations} edges, ${item.width}x${item.height}${item.mermaid ? '' : ' (drawio only)'}`)
  }
}

main()
