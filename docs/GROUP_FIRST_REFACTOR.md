# 以学习小组为主线的重构方案

> 状态：已完成代码审查，待实施
> 日期：2026-11-24

## 1. 背景与问题

项目目前有 26 个 controller、28 张表、7 个一级导航入口，学习小组只是并列功能之一，
其他模块的数据也不流进小组，整体更像一份功能清单。具体表现：

| 现象 | 位置 |
|---|---|
| 入口平权：7 个导航权重相同，小组排第 6 | `learning-program-frontend/src/views/MainView.vue` |
| 首页与小组无关：只有个人统计、签到和一个跳向课程的引导卡 | `learning-program-frontend/src/views/page/HomePage.vue` |
| 小组任务只能绑定课程和资源，测试、签到、积分、讨论都不进小组 | `StudyGroupServiceImpl` 的 `ITEM_TYPES` |
| 签到、积分、排行榜服务里没有小组概念，排行榜只有全站榜 | `LeaderboardMapper` |
| 组长发任务、成员完成任务都不产生通知；铃铛红点是写死的常显 | `MainView.vue`、`NotificationService` 的调用方 |
| `member_visit`（进过小组就算完成）是自指型任务项 | `GroupTaskItem` |
| `/progress` 是“建设中”占位页 | `learning-program-frontend/src/router/index.ts` |
| 任务没有截止时间，小组列表卡片只有名称、组长、人数 | `GroupsPage.vue` |
| 邀请码生成误用 `nextInt(INVITE_CODE_LENGTH)`，只会用到字母表前 8 个字符 `A–H` | `StudyGroupServiceImpl.generateInviteCode` |

此外，核心代码已经偏大：`StudyGroupServiceImpl` 约 862 行，`GroupDetailPage.vue` 约 1529 行。
如果直接增加测试任务、结算和通知，会继续堆大大类与组件，因此必须先做结构拆分。

## 2. 目标形态

主线是一条闭环：

**组长发任务 → 成员学习 → 进度汇总到小组 → 组内排行与积分 → 通知回流**

其他模块按三层归位：

| 层级 | 模块 | 处理 |
|---|---|---|
| 核心 | 学习小组 | 成为首页，任务、通知、积分、排行都接入 |
| 支撑 | 课程、资源、测试、签到 | 作为任务的数据来源，页面本身不改 |
| 外围 | 排行榜、学习进度占位页 | 排行榜移出主导航并从小组进入；占位页删除 |
| 不动 | 讨论区、每日一词、游客模式、2FA、管理后台 | 保持现状 |

讨论区是否与小组结合留待以后决定。

## 3. 阶段 0：结构准备（行为不变）

这一阶段不新增产品功能，不改变现有接口和页面行为，目标是降低后续改动的风险。

### 3.1 后端拆分

| 类 | 职责 |
|---|---|
| `StudyGroupServiceImpl` | 小组、成员、消息主流程 |
| `GroupTaskService` | 任务发布、任务查询、手动完成 |
| `GroupTaskCompletionService` | 批量加载完成上下文、计算任务项状态 |
| `GroupTaskItemJudge` | 任务项判定策略接口 |
| `GroupTaskSettlementService` | 整任务完成、积分、通知、幂等台账 |
| `GroupOverviewService` | 首页小组概览和今日待办 |

任务项判定使用策略模式，避免 `StudyGroupServiceImpl` 中的 `switch` 继续膨胀：

```java
public interface GroupTaskItemJudge {
    String type();
    ItemState judge(GroupTaskItem item, Integer accountId,
                    LocalDateTime taskCreatedAt, CompletionContext context);
}
```

课程、资源、文本、单词测试、题集测试各一个实现，通过构造器注入
`List<GroupTaskItemJudge>` 建立类型映射。`ItemState` 和 `CompletionContext` 提取为任务域内的类型。

拆分原则：

- 保持现有 controller、service 接口和 VO 返回结构不变。
- 不引入通用框架、复杂事件总线或过度抽象。
- 每拆一个类就跑一次后端测试。

### 3.2 前端拆分

```text
GroupDetailPage.vue              页面容器
components/group/
  GroupMessageList.vue
  GroupMessageComposer.vue
  GroupTaskCard.vue
  GroupTaskForm.vue
  GroupMemberPanel.vue
  GroupProgressDialog.vue
composables/useGroupDetail.ts    请求、轮询、可见性处理
types/group.ts                   小组相关类型
```

组件通过 props 和 emits 通信，不引入 Pinia；请求与轮询集中在 composable 中。
`GroupTaskCard` 只负责展示和发出操作事件，不自行请求数据。

### 3.3 先补测试

现有 `StudyGroupServiceImplTest` 主要覆盖移除成员，任务判定还没有测试保护。
进入功能开发前至少补齐：

- 非成员不能查看任务和进度。
- 只有组长能发布任务、查看完整进度。
- 课程、资源、文本任务项的完成判定。
- 任务项为空时不解算整任务完成。
- 小组成员加入时间与任务结算范围。
- 结算幂等和并发重复触发。

## 4. 阶段 1：信息架构（仅前端）

1. **导航从 7 项减为 5 项**：我的小组、课程、资源、测试、讨论区。排行榜和学习进度移出导航。
2. **首页改成“我的小组”**，路由名 `index` 保持不变，登录跳转和路由守卫无需改动。
   - 已加入小组：小组卡片显示进行中任务数、我的完成率、逾期数；下方是跨小组的“今日待办”；签到卡保留。
   - 未加入小组：创建和邀请码加入的引导，表单复用现有小组页。
   - 游客：小组共学的介绍、公开课程和登录引导。
3. **排行榜收纳**：小组页和小组详情页放“本组排行”入口；排行榜页增加“范围”切换（全站或某个小组）。
4. **删除 `/progress` 占位页**及其路由。

## 5. 阶段 2：业务闭环（前后端）

### 5.1 任务增强与结算语义

- 任务增加截止时间。任务卡显示“剩 N 天”或“已逾期”，首页待办按截止时间排序。
- 新增两种测试任务项，达到分数线自动完成：

| 类型 | 绑定对象 | 完成判定 |
|---|---|---|
| `word_test` | CET-4 或 CET-6 词表 | 任务发布后的单词测试成绩达到分数线 |
| `question_set` | 题集 | 任务发布后的该题集知识测试成绩达到分数线 |

- 测试页需要支持从任务跳入时预选词表或题集。

结算必须遵守以下默认规则：

| 场景 | 规则 |
|---|---|
| 没有任何任务项 | 不视为完成，不发积分 |
| 超过截止时间后完成 | 允许完成，标记“逾期完成” |
| 成员在任务发布后才加入 | 不承担加入前发布的任务 |
| 旧 `member_visit` 任务 | 保留原判定，不阻断升级 |
| 测试项分数线 | 限制为 0–100，且发布时必填 |
| 同一成员重复学习、重复轮询 | 完成台账唯一键保证只奖励一次 |

### 5.2 完成判定使用批量查询

`GroupTaskCompletionService` 一次加载一组任务所需的上下文，不在“成员 × 任务项”循环中查库。

测试通过记录按任务项、成员批量关联查询；课程、资源、手动完成记录沿用现有批量加载方式。
实施时通过执行计划确认测试记录查询走现有索引，不预先增加冗余索引。

### 5.3 积分与通知：事件驱动，查询只读

不在 `GET /api/groups/{id}/tasks` 中发积分或通知。任务查询保持只读，避免轮询、重试和预取产生副作用。

在课程进度上报、测试提交、资源访问、手动完成任务等现有写事务中发布学习活动事件：

```java
public record GroupLearningActivityEvent(Integer accountId) {}

eventPublisher.publishEvent(new GroupLearningActivityEvent(accountId));

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
void onLearningActivity(GroupLearningActivityEvent event) {
    taskSettlementService.settleForAccount(event.accountId());
}
```

发布任务后也触发一次现有成员结算，以兼容“任务发布前已经完成课程”的情况。
旧 `member_visit` 的访问记录写入后同样触发结算，不新增其他 GET 副作用。

`GroupTaskSettlementService` 的流程：

1. 查询当前用户加入后仍有效的任务及任务项。
2. 判断是否所有任务项都已完成，且任务项非空。
3. 先插入 `db_group_task_finish` 完成台账。
4. 只有插入成功的一方发放积分并写入组长通知。
5. 重复事件或并发事件命中唯一键时直接结束。

### 5.4 通知回流

新增三个小组事件通知，点击跳回对应小组：

| 事件 | 接收人 |
|---|---|
| 组长发布任务 | 其他组员 |
| 成员完成整个任务 | 组长 |
| 新成员加入 | 组长 |

新增轻量未读数接口，铃铛红点接入真实未读数；页面重新可见时刷新，不随任务轮询频繁请求。

### 5.5 组内排行榜

使用独立接口，保持全站榜不变：

```http
GET /api/groups/{groupId}/leaderboard?metric=points|study|check-in|tasks
```

接口先校验当前用户是小组成员，再返回该组当前成员的榜单。
`tasks` 维度统计 `db_group_task_finish` 中的任务完成数；全站榜继续使用原接口。

### 5.6 首页概览的读模型

首页新增一个小组概览接口，一次返回数据，但不复用组长的完整进度矩阵：

- 小组卡片：进行中任务数、逾期数、完成任务数。
- 今日待办：只计算当前用户未完成的任务项。
- 小组成员的完整矩阵只在组长查看进度时加载。

服务端按小批量查询任务、成员和台账，避免逐个小组、逐条任务项查询。

## 6. 阶段 3：做减法与收尾

- `member_visit` 任务项不再出现在新建任务的表单里，已有任务保留可读。彻底删除需要删表，另行确认。
- 修复邀请码生成 bug，使用 `INVITE_CODE_ALPHABET.length()` 作为随机索引上界。
- 同步文档：`README.md`、`PRODUCT.md`、`docs/API.md`、`docs/DATABASE.md`、
  `docs/architecture/flows/06-study-group.mmd`，产品定位改为“小组共学”。

## 7. 数据库改动

建表以 `scripts/learning.sql` 为唯一来源（安装脚本不再执行迁移），另附一份给已有库使用的 ALTER 脚本。

| 表 | 改动 |
|---|---|
| `db_study_group_message` | 加 `due_at`（可空，旧任务视为无截止） |
| `db_group_task_item` | 加 `pass_score`、`source`（均可空） |
| `db_group_task_finish`（新表） | 任务完成台账：任务、成员、完成时间、发放积分；任务加成员唯一，任务删除时级联 |
| `db_notification` | 不改结构，`type` 新增小组事件取值 |

新增一张表后，安装脚本里的表数核对值（现为 28）同步加 1。

## 8. 接口调整

| 接口 | 调整 |
|---|---|
| `GET /api/groups/overview` | 新增，返回首页小组概览和今日待办 |
| `GET /api/notifications/unread-count` | 新增，使用计数查询，不拉取全部通知 |
| `GET /api/groups/{id}/leaderboard` | 新增，仅小组成员可访问 |
| `POST /api/groups/{id}/tasks` | 请求体增加 `dueAt`，任务项增加测试类型及分数线 |
| 现有任务查询接口 | 增加截止时间和任务状态字段，保持查询只读 |
| 测试页面路由 | 支持词表或题集的预选查询参数 |

## 9. 风险与取舍

- **读操作副作用**：已通过事件驱动消除，任务查询不再发积分或通知。
- **结算时机**：学习事件在事务提交后触发；手动完成任务也通过同一事件链路处理。
- **已有数据**：旧任务没有截止时间；旧 `member_visit` 保留可读、可判定。
- **首页性能**：只加载当前用户待办和汇总数据，不生成全成员完成矩阵。
- **组件与类膨胀**：阶段 0 先拆分，再开发新功能，避免继续堆叠。
- **改动规模**：拆类拆组件后文件数会明显高于初稿估算，必须按阶段提交，不能一次性混合提交。

## 10. 验证方式

### 后端

- 跑现有单测。
- 新增截止状态、测试项判定、加入时间范围、结算幂等、组内榜的用例。
- 验证重复事件和并发触发只发放一次积分。

### 前端

- 类型检查和构建通过。
- 拆分组件后页面行为与重构前一致。

### 手动主线

组长建组并发带截止时间的任务（含课程和测试项）→ 成员用邀请码加入 →
收到通知 → 学习和测试 → 小组进度更新 → 积分到账 → 组内榜出现排名。

额外验证：空任务项不发积分、逾期任务可完成、加入前的任务不进入新成员待办、刷新页面不重复奖励。

## 11. 提交边界

建议每一步都可独立回退：

1. 后端结构拆分与测试。
2. 前端组件和 composable 拆分。
3. 导航与首页调整。
4. 数据库与任务模型调整。
5. 事件结算、通知、积分。
6. 组内排行榜与首页概览接口。
7. 兼容处理和文档收尾。

## 12. 已确定的决策

| 事项 | 决定 |
|---|---|
| 外围功能 | 降级收纳，不删除代码 |
| 讨论区 | 暂时保留全局讨论区，以后再定 |
| 实施顺序 | 阶段 0 → 1 → 2 → 3 |
| 查询与写入 | 查询只读，结算通过写事务提交后的事件触发 |
