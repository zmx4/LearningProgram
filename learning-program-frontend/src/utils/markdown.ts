import MarkdownIt from 'markdown-it'

// html:false 转义源文本中的全部 HTML，配合 markdown-it 默认的链接协议校验，输出可安全用于 v-html
const md = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: true,
})

/**
 * 把管理端撰写的 Markdown 渲染为 HTML，用于章节正文与题目解析。
 * 空内容返回空字符串，调用方按空态处理。
 */
export function renderMarkdown(text: string | null | undefined): string {
  if (!text) return ''
  return md.render(text)
}
