#!/usr/bin/env node
/**
 * 生成 LearningProgram 操作流程图：
 * - 每个操作输出一份 Mermaid 源文件（可维护的规范源）
 * - 汇总输出一份多页 .drawio（可编辑交付件）
 *
 * 运行：node scripts/generate-flowcharts.mjs
 */
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const outDir = join(root, 'docs/architecture/flows')

const L = {
  marginX: 40,
  marginY: 70,
  cellW: 270,
  cellH: 100,
  nodeW: 220,
  nodeH: 58,
  decisionW: 190,
  decisionH: 84,
  startW: 170,
  startH: 54,
  noteW: 280,
  noteH: 84,
}

const KIND_STYLE = {
  start: 'ellipse;whiteSpace=wrap;html=1;fillColor=#d5e8d4;strokeColor=#82b366;fontStyle=1;fontSize=12;',
  finish: 'ellipse;whiteSpace=wrap;html=1;fillColor=#d5e8d4;strokeColor=#82b366;fontStyle=1;fontSize=12;',
  ui: 'rounded=1;whiteSpace=wrap;html=1;fillColor=#e7f0ff;strokeColor=#4a7ebb;fontSize=11;',
  api: 'rounded=1;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;fontSize=11;',
  service: 'rounded=1;whiteSpace=wrap;html=1;fillColor=#d5e8d4;strokeColor=#82b366;fontSize=11;',
  process: 'rounded=1;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;fontSize=11;',
  decision: 'rhombus;whiteSpace=wrap;html=1;fillColor=#fff2cc;strokeColor=#d6b656;fontSize=11;',
  data: 'shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;size=12;fillColor=#e1d5e7;strokeColor=#9673a6;fontSize=10;',
  cache: 'shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;size=12;fillColor=#ffe6cc;strokeColor=#d79b00;fontSize=10;',
  ext: 'rounded=0;whiteSpace=wrap;html=1;fillColor=#f5f5f5;strokeColor=#666666;fontColor=#333333;fontSize=11;',
  error: 'rounded=1;whiteSpace=wrap;html=1;fillColor=#f8cecc;strokeColor=#b85450;fontSize=11;',
  note: 'shape=note;whiteSpace=wrap;html=1;backgroundOutline=1;darkOpacity=0.05;fillColor=#fff2cc;strokeColor=#d6b656;align=left;verticalAlign=top;spacingLeft=8;spacingTop=4;fontSize=10;',
}

const ROUTE_ANCHORS = {
  v: [0.5, 1, 0.5, 0],
  h: [1, 0.5, 0, 0.5],
  vl: [0.5, 1, 0, 0.5],
  vr: [0.5, 1, 1, 0.5],
  hv: [1, 0.5, 0.5, 0],
  hl: [0, 0.5, 1, 0.5],
  up: [0.5, 0, 0.5, 1],
}

const PAGES = [
  {
    id: '00-legend',
    name: '00 图例与阅读说明',
    title: '图例与阅读说明',
    noMermaid: true,
    nodes: [
      { id: 'l_start', kind: 'start', label: '开始 / 结束\n（椭圆）', c: 0, r: 1 },
      { id: 'l_ui', kind: 'ui', label: '前端页面 / 用户操作', c: 1, r: 1 },
      { id: 'l_api', kind: 'api', label: '接口 / Controller', c: 2, r: 1 },
      { id: 'l_service', kind: 'service', label: 'Service / 业务规则', c: 3, r: 1 },
      { id: 'l_decision', kind: 'decision', label: '判断分支', c: 0, r: 2 },
      { id: 'l_data', kind: 'data', label: '数据库表', c: 1, r: 2 },
      { id: 'l_cache', kind: 'cache', label: 'Redis 缓存', c: 2, r: 2 },
      { id: 'l_ext', kind: 'ext', label: '外部系统 / 中间件', c: 3, r: 2 },
      { id: 'l_error', kind: 'error', label: '失败 / 异常提示', c: 0, r: 3 },
      { id: 'l_process', kind: 'process', label: '普通处理步骤', c: 1, r: 3 },
      { id: 'l_note', kind: 'note', label: '证据 / 补充说明', c: 2, r: 3 },
      {
        id: 'l_index',
        kind: 'note',
        label: '页面索引\n\n01 注册与找回密码\n02 登录、两步验证与会话\n03 课程学习与积分奖励\n04 单词测试与知识测试\n05 每日签到与积分\n06 学习小组\n07 讨论区与通知\n08 管理后台\n09 邮件验证码异步投递',
        c: 3,
        r: 5,
        w: 300,
        h: 230,
      },
    ],
    edges: [],
    evidence: [
      '阅读顺序：先看 01 / 02 认证，再看 03 - 05 学习主链路，最后看 06 - 09 互动、后台与支撑流程。',
      '所有流程均为当前代码状态（current），证据见各页底部 Note 与 evidence.md。',
    ],
  },
  {
    id: '01-register-reset',
    name: '01 注册与找回密码',
    title: '注册与找回密码：邮箱验证码驱动的账号流程',
    nodes: [
      { id: 's0', kind: 'start', label: '用户打开注册 / 找回密码页', c: 0, r: 0 },
      { id: 'p1', kind: 'ui', label: '填写邮箱，点击获取验证码\nGET /api/auth/ask-code', c: 0, r: 1 },
      { id: 'p2', kind: 'api', label: 'AccountService.sendEmailCode\n按用途做前置校验', c: 0, r: 2 },
      { id: 'd1', kind: 'decision', label: '邮箱状态符合用途？\n注册=未注册；找回=已注册', c: 0, r: 3 },
      { id: 'x1', kind: 'error', label: '返回业务错误提示\n（邮箱已注册 / 尚未注册）', c: 1, r: 3 },
      { id: 'p3', kind: 'service', label: 'EmailCodeService.sendCode\nRedis 频控 setIfAbsent(60s)', c: 0, r: 4 },
      { id: 'd2', kind: 'decision', label: '频控通过？', c: 0, r: 5 },
      { id: 'x2', kind: 'error', label: '提示：请 60 秒后再试', c: 1, r: 5 },
      { id: 'p4', kind: 'service', label: '生成 6 位码 → Redis 存 300s\n→ MailDispatcher 异步投递', c: 0, r: 6 },
      { id: 'n_mail', kind: 'note', label: '邮件投递细节\n见流程 09', c: 1, r: 6 },
      { id: 'p5', kind: 'ui', label: '用户查收验证码并填写表单', c: 0, r: 7 },
      { id: 'p6', kind: 'api', label: '注册\nPOST /api/auth/register', c: 0, r: 8 },
      { id: 'd3', kind: 'decision', label: '邮箱已注册？', c: 0, r: 9 },
      { id: 'x3', kind: 'error', label: '提示：邮箱已被注册', c: 1, r: 9 },
      { id: 'p7', kind: 'service', label: 'verifyAndConsume(REGISTER)\n+ 密码策略责任链校验', c: 0, r: 10 },
      { id: 'n_flag', kind: 'note', label: '邮箱验证受 spring.security.email.enable 控制\n关闭时注册 / 重置跳过验证码校验', c: 1, r: 10 },
      { id: 'd4', kind: 'decision', label: '验证码与密码通过？', c: 0, r: 11 },
      { id: 'x4', kind: 'error', label: '返回具体校验错误', c: 1, r: 11 },
      { id: 'p8', kind: 'data', label: 'BCrypt 加密 → 写 account\n→ 写入欢迎通知', c: 0, r: 12 },
      { id: 'e0', kind: 'finish', label: '注册成功，跳转登录', c: 0, r: 13 },
      { id: 'p9', kind: 'api', label: '找回密码\nPOST /api/auth/reset-confirm\n（只校验不消费）', c: 3, r: 8 },
      { id: 'd5', kind: 'decision', label: '验证码有效？', c: 3, r: 9 },
      { id: 'x5', kind: 'error', label: '提示验证码有误\n或请先获取验证码', c: 4, r: 9 },
      { id: 'p10', kind: 'ui', label: '第二步：输入新密码\nPOST /api/auth/reset-password', c: 3, r: 10 },
      { id: 'p11', kind: 'service', label: '再次 verify(RESET)\n+ 密码策略校验', c: 3, r: 11 },
      { id: 'd6', kind: 'decision', label: '校验通过？', c: 3, r: 12 },
      { id: 'x6', kind: 'error', label: '返回具体校验错误', c: 4, r: 12 },
      { id: 'p12', kind: 'data', label: '按 email 更新密码\n→ clear(RESET) → 跳转登录', c: 3, r: 13 },
      { id: 'e1', kind: 'finish', label: '重置成功', c: 3, r: 14 },
    ],
    edges: [
      { from: 's0', to: 'p1' },
      { from: 'p1', to: 'p2' },
      { from: 'p2', to: 'd1' },
      { from: 'd1', to: 'x1', label: '否', route: 'h' },
      { from: 'd1', to: 'p3', label: '是' },
      { from: 'p3', to: 'd2' },
      { from: 'd2', to: 'x2', label: '否', route: 'h' },
      { from: 'd2', to: 'p4', label: '是' },
      { from: 'p4', to: 'n_mail', label: '异步', route: 'h', dashed: true },
      { from: 'p4', to: 'p5' },
      { from: 'p5', to: 'p6', label: '注册' },
      { from: 'p5', to: 'p9', label: '找回密码', route: 'hv' },
      { from: 'p6', to: 'd3' },
      { from: 'd3', to: 'x3', label: '是', route: 'h' },
      { from: 'd3', to: 'p7', label: '否' },
      { from: 'p7', to: 'n_flag', label: '配置开关', route: 'h', dashed: true },
      { from: 'p7', to: 'd4' },
      { from: 'd4', to: 'x4', label: '否', route: 'h' },
      { from: 'd4', to: 'p8', label: '是' },
      { from: 'p8', to: 'e0' },
      { from: 'p9', to: 'd5' },
      { from: 'd5', to: 'x5', label: '否', route: 'h' },
      { from: 'd5', to: 'p10', label: '是' },
      { from: 'p10', to: 'p11' },
      { from: 'p11', to: 'd6' },
      { from: 'd6', to: 'x6', label: '否', route: 'h' },
      { from: 'd6', to: 'p12', label: '是' },
      { from: 'p12', to: 'e1' },
    ],
    evidence: [
      'AuthorizeController.java:42-90（password-policy / register / ask-code / reset-confirm / reset-password）',
      'AccountServiceImpl.java:59-144（发送验证码、注册、消费验证码）',
      'AccountServiceImpl.java:204-236（找回密码先校验后改密）',
      'EmailCodeService.java:48-103（Redis 频控、生成、校验、消费）',
      'SecurityConfiguration.java:53-69（/api/auth/** 匿名放行）',
      'RegisterPage.vue:160-201 / ForgetPage.vue:156-196（前端两步操作）',
    ],
  },
  {
    id: '02-login-session',
    name: '02 登录、两步验证与会话',
    title: '登录、两步验证与会话：JWT 无状态认证',
    nodes: [
      { id: 's0', kind: 'start', label: '登录页：输入用户名 / 密码 / 记住我', c: 0, r: 0 },
      { id: 'p1', kind: 'ui', label: '提交登录表单\nPOST /api/auth/login (form-urlencoded)', c: 0, r: 1 },
      { id: 'p2', kind: 'service', label: 'JwtAuthorizeFilter 先执行\n无 Bearer Token → 放行', c: 0, r: 2 },
      { id: 'p3', kind: 'service', label: 'Spring Security 认证\nloadUserByUsername + BCrypt 校验', c: 0, r: 3 },
      { id: 'd1', kind: 'decision', label: '用户名 / 密码正确？', c: 0, r: 4 },
      { id: 'x1', kind: 'error', label: '401 认证失败\n返回统一错误信息', c: 1, r: 4 },
      { id: 'd2', kind: 'decision', label: '账号已启用 TOTP？', c: 0, r: 5 },
      { id: 'p4', kind: 'api', label: '返回 428 + challenge\nRedis 保存 5 分钟', c: 1, r: 5 },
      { id: 'p5', kind: 'ui', label: '前端弹出两步验证输入框\n输入 6 位动态码', c: 1, r: 6 },
      { id: 'p6', kind: 'api', label: 'POST /api/2fa/login\n校验 challenge + TOTP', c: 1, r: 7 },
      { id: 'd3', kind: 'decision', label: 'challenge 与验证码有效？', c: 1, r: 8 },
      { id: 'x2', kind: 'error', label: '401 验证码错误\n或挑战已过期', c: 2, r: 8 },
      { id: 'p7', kind: 'service', label: 'writeAuthorization：签发 JWT\nid / username / authorities / exp', c: 0, r: 6 },
      { id: 'p8', kind: 'ui', label: '按「记住我」写入\nlocalStorage / sessionStorage 的 authorize', c: 0, r: 7 },
      { id: 'p9', kind: 'ui', label: '跳转 /index\n路由守卫校验登录态与 adminOnly 角色', c: 0, r: 8 },
      { id: 'p10', kind: 'ui', label: '后续请求：Axios 注入 Bearer Token', c: 0, r: 9 },
      { id: 'p11', kind: 'service', label: 'JwtAuthorizeFilter.resolveJwt\n验签 + Redis 黑名单 + 过期校验', c: 0, r: 10 },
      { id: 'd4', kind: 'decision', label: 'Token 有效？', c: 0, r: 11 },
      { id: 'x3', kind: 'error', label: '受保护接口 401\n清登录态 → 登录页', c: 1, r: 11 },
      { id: 'p12', kind: 'service', label: '写入 SecurityContext 与 request.id\nController 读取当前用户', c: 0, r: 12 },
      { id: 'e0', kind: 'finish', label: '访问业务接口', c: 0, r: 13 },
      { id: 'p13', kind: 'ui', label: '登出：POST /api/auth/logout', c: 3, r: 9 },
      { id: 'p14', kind: 'service', label: 'invalidateToken\njti 写入 Redis 黑名单至过期', c: 3, r: 10 },
      { id: 'p15', kind: 'ui', label: '清除 authorize\n跳转登录页', c: 3, r: 11 },
      {
        id: 'n_guest',
        kind: 'note',
        label: '游客模式\n/index、GET /api/courses/progress、\nGET /api/dictionary/** 允许匿名只读',
        c: 3,
        r: 2,
      },
    ],
    edges: [
      { from: 's0', to: 'p1' },
      { from: 'p1', to: 'p2' },
      { from: 'p2', to: 'p3' },
      { from: 'p3', to: 'd1' },
      { from: 'd1', to: 'x1', label: '否', route: 'h' },
      { from: 'd1', to: 'd2', label: '是' },
      { from: 'd2', to: 'p4', label: '是', route: 'h' },
      { from: 'd4', to: 'x3', label: '否', route: 'h' },
      { from: 'd2', to: 'p7', label: '否' },
      { from: 'p4', to: 'p5' },
      { from: 'p5', to: 'p6' },
      { from: 'p6', to: 'd3' },
      { from: 'd3', to: 'x2', label: '否', route: 'h' },
      { from: 'd3', to: 'p7', label: '是', route: 'hl' },
      { from: 'p7', to: 'p8' },
      { from: 'p8', to: 'p9' },
      { from: 'p9', to: 'p10' },
      { from: 'p10', to: 'p11' },
      { from: 'p11', to: 'd4' },
      { from: 'd4', to: 'p12', label: '是' },
      { from: 'p12', to: 'e0' },
      { from: 'p9', to: 'p13', label: '登出', route: 'hv' },
      { from: 'p13', to: 'p14' },
      { from: 'p14', to: 'p15' },
    ],
    evidence: [
      'SecurityConfiguration.java:53-85（放行规则 / admin 角色 / 无状态会话）',
      'SecurityConfiguration.java:104-155（2FA 428、JWT 签发、登出黑名单）',
      'JwtAuthorizeFilter.java:32-49（解析 token 并注入 request.id）',
      'JwtUtils.java:43-105（黑名单、验签、过期、签发）',
      'TwoFactorServiceImpl.java:78-96 / TwoFactorLoginController.java:25-42（challenge 与 TOTP 校验）',
      'net/index.ts:249-315（登录、2FA、登出、401 处理）',
      'router/index.ts:218-245（路由守卫与游客模式）',
      'LoginPage.vue:94-128（428 弹窗）',
    ],
  },
  {
    id: '03-course-learning',
    name: '03 课程学习与积分奖励',
    title: '课程学习：章节进度上报与完成奖励',
    nodes: [
      { id: 's0', kind: 'start', label: '进入课程列表', c: 0, r: 0 },
      { id: 'p1', kind: 'api', label: 'GET /api/courses/progress\n游客可读，登录用户读个人进度', c: 0, r: 1 },
      { id: 'p2', kind: 'service', label: '聚合 course + chapter + progress\n+ 本周 study_log', c: 0, r: 2 },
      { id: 'p3', kind: 'ui', label: '选择课程\nGET /api/courses/{id}', c: 0, r: 3 },
      { id: 'p4', kind: 'api', label: 'GET /api/courses/{id}/progress\n已学章节 / 总章节 / 状态', c: 0, r: 4 },
      { id: 'p5', kind: 'ui', label: '打开章节，前端开始计时', c: 0, r: 5 },
      { id: 'p6', kind: 'api', label: 'POST /api/courses/{courseId}/progress\n{chapterId, durationSeconds}', c: 0, r: 6 },
      { id: 'p7', kind: 'service', label: '校验课程与章节归属\n时长截断 0..3600s', c: 0, r: 7 },
      { id: 'p8', kind: 'data', label: 'upsert course_progress\ninsert course_study_log', c: 0, r: 8 },
      { id: 'p9', kind: 'service', label: '统计已学章节\nnot_started / in_progress / completed', c: 0, r: 9 },
      { id: 'd1', kind: 'decision', label: '课程已完成？', c: 0, r: 10 },
      { id: 'e0', kind: 'finish', label: '继续学习下一章', c: 0, r: 12 },
      { id: 'p10', kind: 'service', label: 'awardCourseRewardOnce\n(account_id, course_id) 唯一', c: 1, r: 10 },
      { id: 'd2', kind: 'decision', label: '是否首次完成？', c: 1, r: 11 },
      { id: 'x1', kind: 'error', label: '不重复发放积分', c: 2, r: 11 },
      { id: 'p11', kind: 'data', label: '写 course_reward\nAccountPoints.addPoints', c: 1, r: 12 },
      { id: 'e1', kind: 'finish', label: '返回奖励积分\n前端刷新进度', c: 1, r: 13 },
      { id: 'n_q', kind: 'note', label: '课程关联题集\n→ 跳转知识测试（流程 04）', c: 2, r: 4 },
      { id: 'n_guest', kind: 'note', label: '游客仅能读课程总览\n课程详情 / 进度上报需登录', c: 2, r: 1 },
    ],
    edges: [
      { from: 's0', to: 'p1' },
      { from: 'p1', to: 'p2' },
      { from: 'p2', to: 'p3' },
      { from: 'p3', to: 'p4' },
      { from: 'p4', to: 'p5' },
      { from: 'p5', to: 'p6' },
      { from: 'p6', to: 'p7' },
      { from: 'p7', to: 'p8' },
      { from: 'p8', to: 'p9' },
      { from: 'p9', to: 'd1' },
      { from: 'd1', to: 'e0', label: '否' },
      { from: 'd1', to: 'p10', label: '是', route: 'h' },
      { from: 'p10', to: 'd2' },
      { from: 'd2', to: 'x1', label: '否', route: 'h' },
      { from: 'd2', to: 'p11', label: '是' },
      { from: 'p11', to: 'e1' },
    ],
    evidence: [
      'CourseProgressController.java:30-64（总览 / 详情 / 进度 / 上报）',
      'CourseProgressServiceImpl.java:86-117（总览聚合）',
      'CourseProgressServiceImpl.java:149-209（章节上报与状态计算）',
      'CourseProgressServiceImpl.java:214-233（完成奖励唯一发放）',
      'CourseStudyPage.vue:78-130（打开章节与上报时长）',
      'SecurityConfiguration.java:60-63（游客可读课程进度）',
    ],
  },
  {
    id: '04-tests',
    name: '04 单词测试与知识测试',
    title: '测试流程：单词测试与知识测试',
    nodes: [
      { id: 's0', kind: 'start', label: '测试中心', c: 0, r: 0 },
      { id: 'd0', kind: 'decision', label: '选择测试类型', c: 0, r: 1 },
      { id: 'p1', kind: 'api', label: '单词：GET /api/dictionary/{cet4|cet6}?count=10', c: 0, r: 2 },
      { id: 'p2', kind: 'ui', label: '过滤无释义单词\n生成 4 选 1 选项', c: 0, r: 3 },
      { id: 'p3', kind: 'ui', label: '逐题作答，前端本地计分', c: 0, r: 4 },
      { id: 'p4', kind: 'api', label: 'POST /api/tests/words/results\n{source,total,correct,duration}', c: 0, r: 5 },
      { id: 'p5', kind: 'service', label: 'validateCounts\nsource ∈ cet4 / cet6', c: 0, r: 6 },
      { id: 'p6', kind: 'data', label: 'score = correct*100/total\n写 test_record(category=word)', c: 0, r: 7 },
      { id: 'p7', kind: 'ui', label: '返回记录，刷新历史与汇总', c: 0, r: 8 },
      { id: 'p8', kind: 'ui', label: '知识：普通模式 / 课程题集模式', c: 2, r: 2 },
      { id: 'd1', kind: 'decision', label: '是否带 setId？', c: 2, r: 3 },
      { id: 'p9', kind: 'api', label: 'GET /api/question-sets/{id}\n读取题集题目', c: 3, r: 3 },
      { id: 'p10', kind: 'api', label: 'GET /api/test-types\n→ GET /api/test-questions?typeId&count', c: 3, r: 4 },
      { id: 'p11', kind: 'ui', label: '题型 single / multiple / blank\n前端逐题判分并生成 detail', c: 2, r: 5 },
      { id: 'p12', kind: 'api', label: 'POST /api/tests/knowledge/results\n{typeId XOR setId,total,correct,detail}', c: 2, r: 6 },
      { id: 'p13', kind: 'service', label: '校验 typeId / setId 二选一\ncounts ≤ 100，清洗 detail', c: 2, r: 7 },
      { id: 'p14', kind: 'data', label: '写 test_record(category=knowledge)\ntypeName 快照', c: 2, r: 8 },
      { id: 'p15', kind: 'ui', label: '返回记录，刷新历史与汇总', c: 2, r: 9 },
      { id: 'p16', kind: 'api', label: 'GET /api/tests/{words|knowledge}/history\n汇总平均分 / 最高分 / 正确率', c: 1, r: 10 },
      { id: 'e0', kind: 'finish', label: '历史与汇总展示', c: 1, r: 11 },
      { id: 'n1', kind: 'note', label: '注意：正确数由前端计算后提交，\n后端只做范围与结构校验', c: 4, r: 6 },
    ],
    edges: [
      { from: 's0', to: 'd0' },
      { from: 'd0', to: 'p1', label: '单词测试' },
      { from: 'd0', to: 'p8', label: '知识测试', route: 'h' },
      { from: 'p1', to: 'p2' },
      { from: 'p2', to: 'p3' },
      { from: 'p3', to: 'p4' },
      { from: 'p4', to: 'p5' },
      { from: 'p5', to: 'p6' },
      { from: 'p6', to: 'p7' },
      { from: 'p7', to: 'p16', route: 'hv' },
      { from: 'p8', to: 'd1' },
      { from: 'd1', to: 'p9', label: '是', route: 'h' },
      { from: 'd1', to: 'p10', label: '否', route: 'hv' },
      { from: 'p9', to: 'p11', route: 'vl' },
      { from: 'p10', to: 'p11', route: 'vl' },
      { from: 'p11', to: 'p12' },
      { from: 'p12', to: 'p13' },
      { from: 'p13', to: 'p14' },
      { from: 'p14', to: 'p15' },
      { from: 'p15', to: 'p16', route: 'vl' },
      { from: 'p16', to: 'e0' },
    ],
    evidence: [
      'WordTestPage.vue:90-122（取词、选项、计分、提交）',
      'WordTestController.java:19-37 / WordTestServiceImpl.java:22-33（单词成绩）',
      'KnowledgeTestPage.vue:144-288（类型 / 题集、判分、detail、提交）',
      'KnowledgeTestServiceImpl.java:44-78（typeId / setId 校验与落库）',
      'BaseTestService.java:28-55（计数校验、按正确率计分）',
      'TestQuestionController.java:33 / QuestionSetController.java:26-35（取题接口）',
    ],
  },
  {
    id: '05-checkin-points',
    name: '05 每日签到与积分',
    title: '每日签到：连续天数与积分累计',
    nodes: [
      { id: 's0', kind: 'start', label: '登录用户进入首页', c: 0, r: 0 },
      { id: 'p1', kind: 'api', label: 'GET /api/check-in?year&month', c: 0, r: 1 },
      { id: 'p2', kind: 'service', label: '查询本月记录与今日记录\n计算连续天数 / 总积分', c: 0, r: 2 },
      { id: 'p3', kind: 'ui', label: '渲染日历、连续天数、积分流水', c: 0, r: 3 },
      { id: 'p4', kind: 'ui', label: '点击「立即签到」', c: 0, r: 4 },
      { id: 'p5', kind: 'api', label: 'POST /api/check-in', c: 0, r: 5 },
      { id: 'd1', kind: 'decision', label: '今日已签到？', c: 0, r: 6 },
      { id: 'e0', kind: 'finish', label: '返回已有记录（幂等）', c: 1, r: 6 },
      { id: 'p6', kind: 'service', label: 'currentStreak：今天 / 昨天有记录则延续\n否则归零，streak + 1', c: 0, r: 7 },
      { id: 'p7', kind: 'data', label: 'points = streak\ninsert check_in_record(account,date)\n唯一键保证一天一条', c: 0, r: 8 },
      { id: 'd2', kind: 'decision', label: '并发唯一键冲突？', c: 0, r: 9 },
      { id: 'e1', kind: 'finish', label: '回查今日记录并返回', c: 1, r: 9 },
      { id: 'p8', kind: 'service', label: 'AccountPoints.addPoints', c: 0, r: 10 },
      { id: 'e2', kind: 'finish', label: '返回签到记录\n前端重新加载状态', c: 0, r: 11 },
      { id: 'p9', kind: 'api', label: 'GET /api/points\n读取总积分（只读）', c: 3, r: 4 },
      { id: 'n_guest', kind: 'note', label: '游客：不显示签到卡片\n只浏览公开课程与每日一词', c: 3, r: 1 },
    ],
    edges: [
      { from: 's0', to: 'p1' },
      { from: 'p1', to: 'p2' },
      { from: 'p2', to: 'p3' },
      { from: 'p3', to: 'p4' },
      { from: 'p4', to: 'p5' },
      { from: 'p5', to: 'd1' },
      { from: 'd1', to: 'e0', label: '是', route: 'h' },
      { from: 'd1', to: 'p6', label: '否' },
      { from: 'p6', to: 'p7' },
      { from: 'p7', to: 'd2' },
      { from: 'd2', to: 'e1', label: '是', route: 'h' },
      { from: 'd2', to: 'p8', label: '否' },
      { from: 'p8', to: 'e2' },
    ],
    evidence: [
      'HomePage.vue:97-127（加载签到状态、切换月份、提交签到）',
      'CheckInServiceImpl.java:31-62（状态聚合与连续天数）',
      'CheckInServiceImpl.java:64-96（幂等签到、积分发放）',
      'CheckInController.java:29-45（GET / POST /api/check-in）',
      'AccountPointsService.java（积分累计）',
    ],
  },
  {
    id: '06-study-group',
    name: '06 学习小组',
    title: '学习小组：邀请码、群聊与组长任务',
    nodes: [
      { id: 's0', kind: 'start', label: '学习小组列表\nGET /api/groups', c: 0, r: 0 },
      { id: 'd0', kind: 'decision', label: '选择操作', c: 0, r: 1 },
      { id: 'p1', kind: 'api', label: '创建小组\nPOST /api/groups', c: 0, r: 2 },
      { id: 'p2', kind: 'service', label: '生成 8 位邀请码\ninsert group + owner membership', c: 0, r: 3 },
      { id: 'e0', kind: 'finish', label: '成为组长', c: 0, r: 4 },
      { id: 'p3', kind: 'api', label: '邀请码加入\nPOST /api/groups/join', c: 2, r: 2 },
      { id: 'd1', kind: 'decision', label: '邀请码有效且未加入？', c: 2, r: 3 },
      { id: 'x1', kind: 'error', label: '返回邀请码无效 / 已在小组', c: 3, r: 3 },
      { id: 'p4', kind: 'data', label: 'insert member(role=member)', c: 2, r: 4 },
      { id: 'e1', kind: 'finish', label: '进入小组', c: 2, r: 5 },
      { id: 'p5', kind: 'api', label: 'GET /api/groups/{id}\n成员校验 + 记录本次访问', c: 0, r: 5 },
      { id: 'p6', kind: 'api', label: '聊天：GET / POST /{id}/messages\n仅成员可见', c: 0, r: 6 },
      { id: 'p7', kind: 'api', label: '组长发布任务 POST /{id}/tasks\n保存 type=task 消息 + 任务项', c: 0, r: 7 },
      { id: 'p8', kind: 'service', label: '任务项类型：course / resource /\nmember_visit / text', c: 0, r: 8 },
      { id: 'p9', kind: 'api', label: 'GET /{id}/tasks\n按任务项实时计算完成度', c: 0, r: 9 },
      { id: 'd2', kind: 'decision', label: '任务项类型', c: 0, r: 10 },
      { id: 'p10', kind: 'service', label: 'text：组员手动标记\n写 group_task_progress', c: 1, r: 10 },
      { id: 'p11', kind: 'service', label: 'course：章节覆盖率 100%', c: 1, r: 11 },
      { id: 'p12', kind: 'service', label: 'resource：任务发布后的\n资源访问流水', c: 1, r: 12 },
      { id: 'p13', kind: 'service', label: 'member_visit：任务发布后的\n小组访问记录', c: 1, r: 13 },
      { id: 'p14', kind: 'api', label: '组长查看进度\nGET /{id}/tasks/{taskId}/progress', c: 0, r: 14 },
      { id: 'p15', kind: 'api', label: '退出 POST /{id}/leave\n组长退出=解散；组员退出=移除成员', c: 0, r: 15 },
      { id: 'p16', kind: 'api', label: '组长移除组员\nDELETE /{id}/members/{memberId}', c: 0, r: 16 },
    ],
    edges: [
      { from: 's0', to: 'd0' },
      { from: 'd0', to: 'p1', label: '创建' },
      { from: 'd0', to: 'p3', label: '邀请码加入', route: 'h' },
      { from: 'p1', to: 'p2' },
      { from: 'p2', to: 'e0' },
      { from: 'p3', to: 'd1' },
      { from: 'd1', to: 'x1', label: '否', route: 'h' },
      { from: 'd1', to: 'p4', label: '是' },
      { from: 'p4', to: 'e1' },
      { from: 'e0', to: 'p5' },
      { from: 'e1', to: 'p5', route: 'hl' },
      { from: 'p5', to: 'p6' },
      { from: 'p6', to: 'p7' },
      { from: 'p7', to: 'p8' },
      { from: 'p8', to: 'p9' },
      { from: 'p9', to: 'd2' },
      { from: 'd2', to: 'p10', label: 'text', route: 'h' },
      { from: 'd2', to: 'p11', label: 'course', route: 'hv' },
      { from: 'd2', to: 'p12', label: 'resource', route: 'hv' },
      { from: 'd2', to: 'p13', label: 'member_visit', route: 'hv' },
      { from: 'p10', to: 'p14', route: 'vl' },
      { from: 'p11', to: 'p14', route: 'vl' },
      { from: 'p12', to: 'p14', route: 'vl' },
      { from: 'p13', to: 'p14', route: 'vl' },
      { from: 'p14', to: 'p15' },
      { from: 'p15', to: 'p16' },
    ],
    evidence: [
      'StudyGroupController.java:31-153（小组、消息、任务、成员接口）',
      'StudyGroupServiceImpl.java:144-195（创建与邀请码加入）',
      'StudyGroupServiceImpl.java:197-248（详情访问记录、群聊）',
      'StudyGroupServiceImpl.java:250-331（组长发布任务、手动完成）',
      'StudyGroupServiceImpl.java:333-440（进度、退出、移除成员）',
      'StudyGroupServiceImpl.java:542-569（四类任务项完成判定）',
    ],
  },
  {
    id: '07-discussion-notification',
    name: '07 讨论区与通知',
    title: '讨论区：发帖、评论与站内通知',
    nodes: [
      { id: 's0', kind: 'start', label: '讨论区列表\nGET /api/discussions', c: 0, r: 0 },
      { id: 'p1', kind: 'service', label: '分页查询 + 摘要（≤120 字）\n批量换取作者用户名', c: 0, r: 1 },
      { id: 'd0', kind: 'decision', label: '用户操作', c: 0, r: 2 },
      { id: 'p2', kind: 'api', label: '发帖 POST /api/discussions\n标题≤150，正文≤5000', c: 1, r: 2 },
      { id: 'e2', kind: 'finish', label: '返回新文章\n刷新列表', c: 2, r: 2 },
      { id: 'p3', kind: 'api', label: '查看详情 GET /{id}\n评论 GET /{id}/comments', c: 0, r: 3 },
      { id: 'p4', kind: 'api', label: '评论 POST /{id}/comments\n内容≤1000', c: 1, r: 3 },
      { id: 'p5', kind: 'data', label: 'insert comment\npost.comment_count + 1', c: 1, r: 4 },
      { id: 'd1', kind: 'decision', label: '评论者 = 文章作者？', c: 1, r: 5 },
      { id: 'p6', kind: 'service', label: '写 notification(type=discussion)\nlink=/discussions/{id}', c: 2, r: 5 },
      { id: 'p7', kind: 'process', label: '不发送通知', c: 2, r: 6 },
      { id: 'p8', kind: 'ui', label: '返回评论，刷新列表', c: 1, r: 7 },
      { id: 'e0', kind: 'finish', label: '评论成功', c: 1, r: 8 },
      { id: 'p9', kind: 'service', label: '删除文章 / 评论：作者或 admin\n文章删除级联评论；评论删除计数 -1', c: 0, r: 8 },
      { id: 'e1', kind: 'finish', label: '删除成功', c: 0, r: 9 },
      { id: 's1', kind: 'start', label: '站内通知', c: 3, r: 0 },
      { id: 'p11', kind: 'api', label: 'GET /api/notifications\n列表 + 未读数', c: 3, r: 1 },
      { id: 'p12', kind: 'ui', label: 'POST /{id}/read\nPOST /read-all', c: 3, r: 2 },
      { id: 'p13', kind: 'service', label: '管理员定向通知（流程 08）\n写 notification', c: 3, r: 3 },
    ],
    edges: [
      { from: 's0', to: 'p1' },
      { from: 'p1', to: 'd0' },
      { from: 'd0', to: 'p2', label: '发帖', route: 'h' },
      { from: 'd0', to: 'p3', label: '查看 / 评论' },
      { from: 'p2', to: 'e2', route: 'h' },
      { from: 'p3', to: 'p4', label: '评论' },
      { from: 'p4', to: 'p5' },
      { from: 'p5', to: 'd1' },
      { from: 'd1', to: 'p6', label: '否', route: 'h' },
      { from: 'd1', to: 'p7', label: '是' },
      { from: 'p6', to: 'p8', route: 'vl' },
      { from: 'p7', to: 'p8' },
      { from: 'p8', to: 'e0' },
      { from: 'p3', to: 'p9', label: '作者 / 管理员删除', route: 'vl' },
      { from: 'p9', to: 'e1' },
      { from: 'p6', to: 'p13', label: '写入通知', route: 'hv', dashed: true },
      { from: 's1', to: 'p11' },
      { from: 'p11', to: 'p12' },
      { from: 'p13', to: 'p11', route: 'up' },
    ],
    evidence: [
      'DiscussionController.java:27-76（列表、详情、发帖、删除、评论）',
      'DiscussionServiceImpl.java:95-132（发帖校验、作者 / 管理员删除）',
      'DiscussionServiceImpl.java:156-198（评论、计数、删除评论）',
      'DiscussionServiceImpl.java:200-225（评论通知作者）',
      'NotificationController.java:28-50（通知列表、单条已读、全部已读）',
    ],
  },
  {
    id: '08-admin',
    name: '08 管理后台',
    title: '管理后台：权限边界与六类管理操作',
    nodes: [
      { id: 's0', kind: 'start', label: '管理员登录，role = admin', c: 0, r: 0 },
      { id: 'p1', kind: 'api', label: '前端 /admin 路由 adminOnly\n后端 /api/admin/** hasRole(admin)', c: 0, r: 1 },
      { id: 'd0', kind: 'decision', label: '权限校验通过？', c: 0, r: 2 },
      { id: 'x0', kind: 'error', label: '403 / 跳回首页', c: 1, r: 2 },
      { id: 'p2', kind: 'ui', label: '管理员中心', c: 0, r: 3 },
      { id: 'p3', kind: 'api', label: '用户管理\nGET /api/admin/users', c: 0, r: 4 },
      { id: 'p4', kind: 'api', label: '课程管理\n/api/admin/courses', c: 2, r: 4 },
      { id: 'p5', kind: 'api', label: '题库管理\n/api/admin/test-types|test-questions', c: 3, r: 4 },
      { id: 'p6', kind: 'api', label: '题集管理\n/api/admin/question-sets', c: 4, r: 4 },
      { id: 'p7', kind: 'api', label: '资源管理\n/api/admin/resources', c: 5, r: 4 },
      { id: 'p8', kind: 'api', label: '通知\nPOST /api/admin/notifications', c: 6, r: 4 },
      { id: 'p9', kind: 'service', label: '调整角色 PUT /users/{id}/role\n不能降级当前管理员', c: 0, r: 5 },
      { id: 'p10', kind: 'service', label: '删除用户 DELETE /users/{id}\n不能删除当前账号', c: 0, r: 6 },
      { id: 'p11', kind: 'service', label: '重置密码 PUT /users/{id}/password\n随机密码 + 站内通知', c: 0, r: 7 },
      { id: 'p12', kind: 'service', label: '批量创建 POST /users/batch\n逐条返回失败原因', c: 0, r: 8 },
      { id: 'p13', kind: 'service', label: '新增 / 编辑 / 删除课程\n维护章节与题集关联', c: 2, r: 5 },
      { id: 'p14', kind: 'service', label: '新增 / 编辑 / 删除\n测试类型与题目', c: 3, r: 5 },
      { id: 'p15', kind: 'service', label: '新增 / 编辑 / 删除题集\n维护题目顺序', c: 4, r: 5 },
      { id: 'p16', kind: 'service', label: '上传(multipart) / 编辑 / 删除\nvisible 控制可见性', c: 5, r: 5 },
      { id: 'p17', kind: 'service', label: '目标 all / role / users\n批量写 notification', c: 6, r: 5 },
      { id: 'e0', kind: 'finish', label: '用户操作完成', c: 0, r: 9 },
      { id: 'e1', kind: 'finish', label: '课程操作完成', c: 2, r: 6 },
      { id: 'e2', kind: 'finish', label: '题库操作完成', c: 3, r: 6 },
      { id: 'e3', kind: 'finish', label: '题集操作完成', c: 4, r: 6 },
      { id: 'e4', kind: 'finish', label: '资源操作完成', c: 5, r: 6 },
      { id: 'e5', kind: 'finish', label: '通知发送完成', c: 6, r: 6 },
    ],
    edges: [
      { from: 's0', to: 'p1' },
      { from: 'p1', to: 'd0' },
      { from: 'd0', to: 'x0', label: '否', route: 'h' },
      { from: 'd0', to: 'p2', label: '是' },
      { from: 'p2', to: 'p3', label: '用户' },
      { from: 'p2', to: 'p4', route: 'hv' },
      { from: 'p2', to: 'p5', route: 'hv' },
      { from: 'p2', to: 'p6', route: 'hv' },
      { from: 'p2', to: 'p7', route: 'hv' },
      { from: 'p2', to: 'p8', route: 'hv' },
      { from: 'p3', to: 'p9' },
      { from: 'p9', to: 'p10' },
      { from: 'p10', to: 'p11' },
      { from: 'p11', to: 'p12' },
      { from: 'p12', to: 'e0' },
      { from: 'p4', to: 'p13' },
      { from: 'p5', to: 'p14' },
      { from: 'p6', to: 'p15' },
      { from: 'p7', to: 'p16' },
      { from: 'p8', to: 'p17' },
      { from: 'p13', to: 'e1' },
      { from: 'p14', to: 'e2' },
      { from: 'p15', to: 'e3' },
      { from: 'p16', to: 'e4' },
      { from: 'p17', to: 'e5' },
    ],
    evidence: [
      'router/index.ts:153-188,236-238（/admin 路由与 adminOnly 守卫）',
      'SecurityConfiguration.java:68（/api/admin/** hasRole admin）',
      'AdminController.java:41-102（用户、角色、删除、重置密码、批量创建、通知）',
      'AdminServiceImpl.java:34-80（批量建号与定向通知）',
      'AdminCourseController.java:22-58 / AdminTestBankController.java:22-76（课程与题库）',
      'AdminQuestionSetController.java:19-40 / AdminResourceController.java:26-60（题集与资源）',
    ],
  },
  {
    id: '09-mail-dispatch',
    name: '09 邮件验证码异步投递',
    title: '邮件验证码投递：RabbitMQ 异步与多级降级',
    nodes: [
      { id: 's0', kind: 'start', label: '业务触发验证码\n注册 / 找回密码 / 2FA', c: 0, r: 0 },
      { id: 'p1', kind: 'service', label: 'EmailCodeService.sendCode\nRedis 频控 + 生成 6 位码', c: 0, r: 1 },
      { id: 'p2', kind: 'cache', label: 'Redis 存 email::code:<type>:<email>\nTTL 300s', c: 0, r: 2 },
      { id: 'p3', kind: 'service', label: 'MailDispatcher.dispatch', c: 0, r: 3 },
      { id: 'd1', kind: 'decision', label: '队列已启用且生产者可用？', c: 0, r: 4 },
      { id: 'p4', kind: 'service', label: 'RabbitMailProducer.publish\nexchange + routingKey', c: 1, r: 4 },
      { id: 'd2', kind: 'decision', label: '发布成功？', c: 1, r: 5 },
      { id: 'p5', kind: 'ext', label: 'RabbitMQ\nlearning.mail.queue', c: 2, r: 5 },
      { id: 'p6', kind: 'service', label: 'RabbitMailConsumer.onMessage\n反序列化 EmailMessage', c: 2, r: 6 },
      { id: 'p7', kind: 'service', label: 'MailDelivery.send', c: 2, r: 7 },
      { id: 'p8', kind: 'service', label: '准备投递', c: 0, r: 8 },
      { id: 'd3', kind: 'decision', label: 'smtp 模式且发送成功？', c: 0, r: 9 },
      { id: 'p9', kind: 'ext', label: 'SMTP 服务器\n真实发信', c: 1, r: 9 },
      { id: 'p10', kind: 'service', label: '降级：写日志 [mail:log]\ncode=xxxxxx', c: 2, r: 10 },
      { id: 'e0', kind: 'finish', label: '用户获取验证码\n（开发环境看日志）', c: 2, r: 11 },
      { id: 'n0', kind: 'note', label: '投递不会抛异常：\n队列不可用 → 直接发送；\nSMTP 失败 → 写日志', c: 4, r: 3 },
    ],
    edges: [
      { from: 's0', to: 'p1' },
      { from: 'p1', to: 'p2' },
      { from: 'p2', to: 'p3' },
      { from: 'p3', to: 'd1' },
      { from: 'd1', to: 'p4', label: '是', route: 'h' },
      { from: 'd1', to: 'p8', label: '否' },
      { from: 'p4', to: 'd2' },
      { from: 'd2', to: 'p5', label: '是', route: 'h' },
      { from: 'd2', to: 'p8', label: '否', route: 'vl' },
      { from: 'p5', to: 'p6' },
      { from: 'p6', to: 'p7' },
      { from: 'p7', to: 'p8', route: 'vl' },
      { from: 'p8', to: 'd3' },
      { from: 'd3', to: 'p9', label: '是', route: 'h' },
      { from: 'd3', to: 'p10', label: '否', route: 'hv' },
      { from: 'p9', to: 'e0', route: 'vl' },
      { from: 'p10', to: 'e0' },
    ],
    evidence: [
      'EmailCodeService.java:48-73（频控、生成、存 Redis、投递）',
      'MailDispatcher.java:35-47（队列优先与降级）',
      'MailDelivery.java:32-66（SMTP / 日志双模式，永不抛异常）',
      'MailProperties.java:13-46（delivery、queue、code 配置）',
      'RabbitMailProducer.java:30-35 / RabbitMailConsumer.java:23-31（发布与消费）',
      'AuthorizeController.java:58-67（ask-code 接口）',
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

function mermaidLabel(text) {
  return String(text).replaceAll('"', "'").replaceAll('\n', '<br/>')
}

function nodeSize(node) {
  const defaults = {
    start: [L.startW, L.startH],
    finish: [L.startW, L.startH],
    decision: [L.decisionW, L.decisionH],
    note: [L.noteW, L.noteH],
  }
  const [w, h] = defaults[node.kind] ?? [L.nodeW, L.nodeH]
  return { w: node.w ?? w, h: node.h ?? h }
}

function nodeBox(node) {
  const { w, h } = nodeSize(node)
  if (node.abs) {
    return { x: node.abs.x, y: node.abs.y, w: node.abs.w ?? w, h: node.abs.h ?? h }
  }
  return {
    x: L.marginX + node.c * L.cellW + (L.cellW - w) / 2,
    y: L.marginY + node.r * L.cellH + (L.cellH - h) / 2,
    w,
    h,
  }
}

function validatePage(page) {
  const ids = new Set()
  for (const node of page.nodes) {
    if (ids.has(node.id)) throw new Error(`${page.id}: duplicate node id ${node.id}`)
    if (node.id === 'end') throw new Error(`${page.id}: node id "end" is reserved in Mermaid`)
    if (!KIND_STYLE[node.kind]) throw new Error(`${page.id}: unknown kind ${node.kind}`)
    ids.add(node.id)
  }
  for (const edge of page.edges) {
    if (!ids.has(edge.from)) throw new Error(`${page.id}: edge from missing node ${edge.from}`)
    if (!ids.has(edge.to)) throw new Error(`${page.id}: edge to missing node ${edge.to}`)
    if (edge.route && !ROUTE_ANCHORS[edge.route]) throw new Error(`${page.id}: unknown route ${edge.route}`)
  }
  if (!page.evidence?.length) throw new Error(`${page.id}: evidence is required`)
}

function buildPage(page) {
  validatePage(page)
  const boxes = new Map(page.nodes.map(node => [node.id, nodeBox(node)]))
  const entries = [...boxes.entries()]
  for (let i = 0; i < entries.length; i += 1) {
    for (let j = i + 1; j < entries.length; j += 1) {
      const [firstId, first] = entries[i]
      const [secondId, second] = entries[j]
      const overlapX = Math.min(first.x + first.w, second.x + second.w) - Math.max(first.x, second.x)
      const overlapY = Math.min(first.y + first.h, second.y + second.h) - Math.max(first.y, second.y)
      if (overlapX > 2 && overlapY > 2) {
        throw new Error(`${page.id}: node overlap ${firstId} <> ${secondId}`)
      }
    }
  }
  const right = Math.max(...[...boxes.values()].map(box => box.x + box.w))
  const bottom = Math.max(...[...boxes.values()].map(box => box.y + box.h))
  const pageWidth = Math.max(1200, Math.ceil(right + 40))
  const evidenceY = Math.ceil(bottom + 26)
  const evidenceHeight = Math.min(150, 34 + page.evidence.length * 14)
  const pageHeight = evidenceY + evidenceHeight + 40
  const prefix = `${page.id}-`

  const cells = []

  cells.push(
    `<mxCell id="${prefix}title" value="${drawioLabel(page.title)}" style="text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;fontSize=20;fontStyle=1;fontColor=#1f2d3d;" vertex="1" parent="1"><mxGeometry x="40" y="20" width="${pageWidth - 80}" height="34" as="geometry" /></mxCell>`,
  )

  for (const node of page.nodes) {
    const box = boxes.get(node.id)
    cells.push(
      `<mxCell id="${prefix}${node.id}" value="${drawioLabel(node.label)}" style="${KIND_STYLE[node.kind]}" vertex="1" parent="1"><mxGeometry x="${Math.round(box.x)}" y="${Math.round(box.y)}" width="${Math.round(box.w)}" height="${Math.round(box.h)}" as="geometry" /></mxCell>`,
    )
  }

  let edgeIndex = 0
  for (const edge of page.edges) {
    const anchors = ROUTE_ANCHORS[edge.route ?? 'v']
    const [exitX, exitY, entryX, entryY] = anchors
    const style = [
      'edgeStyle=orthogonalEdgeStyle',
      'rounded=0',
      'orthogonalLoop=1',
      'jettySize=auto',
      'html=1',
      'strokeColor=#5b6b7c',
      'strokeWidth=1',
      'fontSize=10',
      'fontColor=#3d4b5c',
      'labelBackgroundColor=#ffffff',
      `exitX=${exitX}`,
      `exitY=${exitY}`,
      'exitDx=0',
      'exitDy=0',
      `entryX=${entryX}`,
      `entryY=${entryY}`,
      'entryDx=0',
      'entryDy=0',
      edge.dashed ? 'dashed=1' : '',
    ]
      .filter(Boolean)
      .join(';')
    const value = edge.label ? ` value="${drawioLabel(edge.label)}"` : ''
    cells.push(
      `<mxCell id="${prefix}e${++edgeIndex}"${value} style="${style};" edge="1" parent="1" source="${prefix}${edge.from}" target="${prefix}${edge.to}"><mxGeometry relative="1" as="geometry" /></mxCell>`,
    )
  }

  cells.push(
    `<mxCell id="${prefix}evidence" value="${drawioLabel(`证据（当前代码）：\n${page.evidence.join('\n')}`)}" style="${KIND_STYLE.note}" vertex="1" parent="1"><mxGeometry x="40" y="${evidenceY}" width="${pageWidth - 80}" height="${evidenceHeight}" as="geometry" /></mxCell>`,
  )

  const xml = `<diagram id="${page.id}" name="${drawioLabel(page.name)}">
      <mxGraphModel dx="${pageWidth}" dy="${pageHeight}" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${pageWidth}" pageHeight="${pageHeight}" math="0" shadow="0">
        <root>
          <mxCell id="0" />
          <mxCell id="1" parent="0" />
          ${cells.join('\n          ')}
        </root>
      </mxGraphModel>
    </diagram>`

  return { xml, pageWidth, pageHeight }
}

function buildMermaid(page) {
  if (page.noMermaid) return null
  const lines = [
    `%% ${page.name}`,
    '%% 由 scripts/generate-flowcharts.mjs 生成；如需修改请改脚本后重新生成。',
    'flowchart TD',
  ]

  for (const node of page.nodes) {
    const label = `"${mermaidLabel(node.label)}"`
    const shape = {
      start: `([${label}])`,
      finish: `([${label}])`,
      decision: `{${label}}`,
      data: `[(${label})]`,
      cache: `[(${label})]`,
      ext: `[[${label}]]`,
      note: `[[${label}]]`,
    }[node.kind] ?? `[${label}]`
    lines.push(`  ${node.id}${shape}`)
  }

  lines.push('')
  for (const edge of page.edges) {
    const arrow = edge.dashed ? '-.->' : '-->'
    const label = edge.label ? `|"${mermaidLabel(edge.label)}"|` : ''
    lines.push(`  ${edge.from} ${arrow}${label} ${edge.to}`)
  }

  lines.push('', '  %% 证据：')
  for (const item of page.evidence) {
    lines.push(`  %% - ${item}`)
  }
  return `${lines.join('\n')}\n`
}

function main() {
  mkdirSync(outDir, { recursive: true })
  const seen = new Set()
  const diagrams = []
  const summary = []

  for (const page of PAGES) {
    if (seen.has(page.id)) throw new Error(`duplicate page id ${page.id}`)
    seen.add(page.id)
    const { xml, pageWidth, pageHeight } = buildPage(page)
    diagrams.push(xml)
    const mmd = buildMermaid(page)
    if (mmd) {
      writeFileSync(join(outDir, `${page.id}.mmd`), mmd, 'utf8')
    }
    summary.push({
      id: page.id,
      name: page.name,
      nodes: page.nodes.length,
      edges: page.edges.length,
      width: pageWidth,
      height: pageHeight,
      mermaid: Boolean(mmd),
    })
  }

  const mxfile = `<mxfile host="app.diagrams.net" agent="Qoder" version="24.7.17" type="device">
  ${diagrams.join('\n  ')}
</mxfile>
`
  writeFileSync(join(outDir, 'learning-program-flows.drawio'), mxfile, 'utf8')
  writeFileSync(join(outDir, 'flows.manifest.json'), `${JSON.stringify(summary, null, 2)}\n`, 'utf8')

  console.log(`generated ${summary.length} pages -> ${outDir}`)
  for (const item of summary) {
    console.log(`  ${item.id}: ${item.nodes} nodes, ${item.edges} edges, ${item.width}x${item.height}${item.mermaid ? '' : ' (drawio only)'}`)
  }
}

main()
