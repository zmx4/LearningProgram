# LearningProgram 操作流程图

本目录用 9 个聚焦视图描述平台的主要用户与管理员操作，配套 1 个图例页。
内容基于当前代码状态（`main` 分支）整理，目的是帮助新成员、评审者和运维快速理解
一次操作从前端到接口、服务、存储的完整路径。

## 交付物

| 文件 | 角色 | 说明 |
| --- | --- | --- |
| `*.mmd` | 规范源（canonical） | 每个操作一份 Mermaid 流程图，便于评审、diff 和文档内嵌 |
| `learning-program-flows.drawio` | 派生可编辑交付件 | 多页 Draw.io 文件，页面与下方索引一一对应 |
| `flows.manifest.json` | 生成清单 | 页面、节点数、边数、画布尺寸，便于校验再生结果 |
| `evidence.md` | 证据说明 | 每个页面的代码引用、置信度与已知限制 |
| `../../scripts/generate-flowcharts.mjs` | 生成脚本 | 流程模型与布局的唯一来源，同时生成 `.mmd` 和 `.drawio` |

> Draw.io 文件是由 Mermaid/脚本模型派生的可编辑交付件，不是架构事实源。
> 手工在 Draw.io 中修改流程后，应同步修改生成脚本并重新生成，避免两套事实漂移。

## 页面索引

| 页面 | 主题 | 规范源 |
| --- | --- | --- |
| 00 | 图例与阅读说明 | Draw.io 内（无需 Mermaid） |
| 01 | 注册与找回密码：邮箱验证码驱动的账号流程 | [01-register-reset.mmd](./01-register-reset.mmd) |
| 02 | 登录、两步验证与会话：JWT 无状态认证 | [02-login-session.mmd](./02-login-session.mmd) |
| 03 | 课程学习：章节进度上报与完成奖励 | [03-course-learning.mmd](./03-course-learning.mmd) |
| 04 | 测试流程：单词测试与知识测试 | [04-tests.mmd](./04-tests.mmd) |
| 05 | 每日签到：连续天数与积分累计 | [05-checkin-points.mmd](./05-checkin-points.mmd) |
| 06 | 学习小组：邀请码、群聊与组长任务 | [06-study-group.mmd](./06-study-group.mmd) |
| 07 | 讨论区：发帖、评论与站内通知 | [07-discussion-notification.mmd](./07-discussion-notification.mmd) |
| 08 | 管理后台：权限边界与六类管理操作 | [08-admin.mmd](./08-admin.mmd) |
| 09 | 邮件验证码投递：RabbitMQ 异步与多级降级 | [09-mail-dispatch.mmd](./09-mail-dispatch.mmd) |

## 建议阅读顺序

1. **认证**：01 注册/找回 → 02 登录/2FA/会话。
2. **学习主链路**：03 课程 → 04 测试 → 05 签到积分。
3. **互动与后台**：06 学习小组 → 07 讨论区/通知 → 08 管理后台。
4. **支撑流程**：09 邮件投递（01、02 中验证码的实际去向）。

## 打开方式

- **Draw.io / diagrams.net**：打开 `learning-program-flows.drawio`，左侧页面列表可切换 10 个页面。
- **Mermaid**：在支持 Mermaid 的编辑器或 Qoder 中直接预览对应 `.mmd` 文件。
- **本地 Draw.io MCP（可选）**：如果已配置 `@drawio/mcp`，可让代理用 `open_drawio_xml`
  打开该文件，或用 `open_drawio_mermaid` 单独打开某个 `.mmd` 页面。

```toml
[mcp_servers.drawio]
command = "npx"
args = ["-y", "@drawio/mcp"]
```

## 重新生成

流程模型在 [scripts/generate-flowcharts.mjs](../../../scripts/generate-flowcharts.mjs)。
修改节点、边、坐标或证据后执行：

```bash
node scripts/generate-flowcharts.mjs
xmllint --noout docs/architecture/flows/learning-program-flows.drawio
```

脚本会做以下校验，失败时直接退出：

- 页面与节点 ID 唯一，边端点必须存在；
- 节点包围盒不得重叠；
- 每个页面必须提供证据列表。

`flows.manifest.json` 可用于比对再生前后的页面规模。

## 证据与置信度

- 所有流程均为**当前状态（current）**，证据来自控制器、服务实现、过滤器、前端页面与路由守卫。
- 页脚 Note 与 [evidence.md](./evidence.md) 列出了每个流程的关键 `sourceRefs`。
- 所有已绘制节点/边均为**高置信度**（直接代码证据）；推断性说明只出现在 Note 中。
- Draw.io 不能原生表达“置信度”字段，因此置信度与证据只在 Note/本文件中维护，不在图形上编码。
  Mermaid 同样不支持置信度字段，其证据以 `%%` 注释附在文件末尾。

## 已知限制

- 未接入运行时日志、链路追踪或数据库实际数据；流程是静态代码推导，不代表特定环境下的时序。
- 知识测试的 `correctCount` 与 `detail` 由前端计算后提交，后端只校验数量范围与结构，
  见 04 页面 Note 与 `evidence.md`。这属于当前产品行为，不是流程图遗漏。
- 邮件默认 `delivery=log`、`queue.enabled=false`，开发环境验证码在服务端日志中查看；
  09 页面同时画出了 SMTP 与队列分支。
- 本环境没有 Draw.io MCP/CLI，`.drawio` 由脚本直接生成；已通过 XML 良构与节点重叠校验，
  但未在 diagrams.net 中做像素级渲染验收。打开后如有个别连线绕行，可直接拖动调整。
