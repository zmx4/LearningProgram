import axios from 'axios'

export function formatFileSize(size: number | null | undefined): string {
  if (!size || size < 0) return '—'
  if (size >= 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  if (size >= 1024) return `${Math.max(1, Math.round(size / 1024))} KB`
  return `${size} B`
}

/**
 * 通过下载接口取回文件并触发浏览器保存。
 * 接口用 Authorization 头鉴权（全局 axios 拦截器附加），所以要拿 blob 再交给 <a download>。
 */
export async function downloadResourceFile(id: number, fallbackName: string): Promise<void> {
  const response = await axios.get<Blob>(`/api/resources/${id}/download`, { responseType: 'blob' })
  const disposition = String(response.headers['content-disposition'] ?? '')
  const utf8Name = /filename\*=UTF-8''([^;]+)/i.exec(disposition)?.[1]
  const plainName = /filename="?([^";]+)"?/i.exec(disposition)?.[1]
  const fileName = utf8Name ? decodeURIComponent(utf8Name) : (plainName ?? fallbackName)
  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
