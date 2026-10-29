# LearningProgram 数据库 E-R 图

本目录用 7 个聚焦视图描述 `learning` 库的实体关系：1 个图例页、1 个实体总览页、
5 个分域明细页。内容基于当前 DDL（`scripts/learning.sql`，28 张表）整理，
目的是让新成员、评审者和运维能同时看清“真实外键约束”和“仅靠字段约定的软引用”。

## 交付物

| 文件 | 角色 | 说明 |
| --- | --- | --- |
| `01-overview.mmd` … `06-social.mmd` | 规范源（canonical） | 每个分域一份 Mermaid `erDiagram`，便于评审、diff 与文档内嵌 |
| `learning-program-er.drawio` | 派生可编辑交付件 | 7 页 Draw.io 文件，含完整字段、主键/外键标记与关系说明 |
| `er.manifest.json` | 生成清单 | 页面、表数、关系数、画布尺寸，便于比对再生结果 |
| `evidence.md` | 证据说明 | 每张表、每条边的 DDL 行号、置信度与已知文档偏差 |
| `../../../scripts/generate-er-diagram.mjs` | 生成脚本 | 实体、字段、关系与布局的唯一模型源，同时生成 `.mmd` 与 `.drawio` |

> Draw.io 文件是由脚本模型派生的可编辑交付件，不是架构事实源。
> 在 Draw.io 中手工调整布局可以接受；但**新增/删除表、字段或关系**必须先改
> 生成脚本并重新生成，避免两套事实漂移。

## 页面索引

| 页面 | 主题 | 规范源 |
| --- | --- | --- |
| 00 | 图例与阅读说明（字段前缀、实线/虚线、连接表） | Draw.io 内（无需 Mermaid） |
| 01 | 实体总览：28 张表与 21 条数据库外键 | [01-overview.mmd](./01-overview.mmd) |
| 02 | 账号与积分：账号、积分总账、签到、通知 | [02-account.mmd](./02-account.mmd) |
| 03 | 课程学习：章节、进度、流水、完成奖励 | [03-course.mmd](./03-course.mmd) |
| 04 | 题库与测试：类型、题目、题集、成绩、CET 词库 | [04-test.mmd](./04-test.mmd) |
| 05 | 学习小组：成员、群消息、任务项、完成记录 | [05-group.mmd](./05-group.mmd) |
| 06 | 讨论、资源与词典：帖子评论、资源访问、独立词库 | [06-social.mmd](./06-social.mmd) |

## 如何读这张图

1. **先看 01 总览**：只画真实外键，用来理解模块边界与级联删除链。
2. **再看 02 - 06 分域页**：每页给出完整字段，并补上总览里省略的弱关联。
3. **虚线不要当成约束**：灰色虚线是软引用或冗余列，紫色虚线是多态 `ref_id`。

图形约定：

| 视觉元素 | 含义 |
| --- | --- |
| 蓝色表头 | 业务实体 |
| 紫色表头 | 多对多连接表（`db_course_question_set`、`db_question_set_item`、`db_study_group_member`） |
| 绿色表头 | 独立词库/词典（`db_cet4`、`db_cet6`、`db_dictionary`） |
| 灰色表头 | 跨页引用表（只显示 `id`，完整定义在所属分域页） |
| 实线 + 鸦爪 | 数据库 `FOREIGN KEY`，基数由鸦爪端表示 |
| 灰色虚线 + 鸦爪 | 软引用（字段命名/文档约定，无 FK） |
| 灰色虚线 + 普通箭头 | 冗余列（如 `course_id`） |
| 紫色虚线 + 普通箭头 | 多态引用（`item_type` + `ref_id`） |
| `PK` / `FK` / `UK` | 主键 / 外键 / 唯一键；`FK/UK` 常见于 1:0..1 |

## 关系规模

- 28 张表；21 条真实外键；3 张多对多连接表。
- 18 条非约束边：13 条软引用、3 条冗余列、2 条多态引用。
- 所有实体均为**当前状态（current）**，证据来自 `scripts/learning.sql` 的建表语句。

## 打开方式

- **Draw.io / diagrams.net**：打开 `learning-program-er.drawio`，左侧页面列表可切换 7 页；
  每页底部都有证据 Note。
- **Mermaid**：在支持 Mermaid 的编辑器或 Qoder 中直接预览对应 `.mmd` 文件。
- **本地 Draw.io MCP（可选）**：当前环境未安装 Draw.io MCP，`.drawio` 由脚本直接生成。
  如需让代理打开该文件，可配置：

```toml
[mcp_servers.drawio]
command = "npx"
args = ["-y", "@drawio/mcp"]
```

配置后可使用 `open_drawio_xml` 打开整份文件，或用 `open_drawio_mermaid` 单独打开某个 `.mmd`。

## 重新生成

```bash
node scripts/generate-er-diagram.mjs
xmllint --noout docs/architecture/data/learning-program-er.drawio
```

脚本在写入前会做以下校验，失败时直接退出：

- 同一页面内实体/注释 ID 唯一，边端点必须存在；
- 关系两端必须出现在当前页（跨页表必须显式声明为引用表或幽灵节点）；
- 节点包围盒不得重叠；
- 每个页面必须提供证据列表。

`er.manifest.json` 可用于比对再生前后的页面规模。

## 已知限制

- **软引用不是约束**：账号侧大量 `account_id` / `owner_id` 没有外键，图中虚线只表达
  逻辑归属；删除账号不会级联清理这些行，需业务代码或运维脚本处理。
- **文档偏差**：`docs/DATABASE.md` 的紧凑 E-R 图未包含软引用、冗余列和多态引用；
  且其“删除账号会级联删除测试记录”的描述与实际 DDL 不符——`db_test_record.account_id`
  没有外键。本目录以 `scripts/learning.sql` 为准。
- **字段展示宽度**：图中使用 `int`，DDL 中为 `int(11)`；MySQL 8 已弃用整数显示宽度，
  不影响类型语义。
- **渲染验收**：`.drawio` 已通过 XML 良构、ID 唯一、端点存在与节点重叠校验，
  但未在 diagrams.net 中做像素级渲染验收；打开后如个别连线绕行，可直接拖动调整布局。
- **未接入运行时数据**：没有读取生产库统计信息，行数、基数分布与索引命中不在本图范围内。
