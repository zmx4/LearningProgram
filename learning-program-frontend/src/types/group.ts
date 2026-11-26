export interface GroupSummary {
  id: number
  name: string
  ownerId: number
  ownerName: string
  inviteCode: string
  memberCount: number
  role: string
  createdAt: string
}

export interface GroupMember {
  accountId: number
  username: string
  role: string
  joinedAt: string
}

export interface GroupDetail {
  group: GroupSummary
  members: GroupMember[]
}

export interface GroupMessage {
  id: number
  accountId: number
  username: string
  role: string
  type: string
  title: string | null
  content: string
  createdAt: string
}

export type TaskItemType = 'course' | 'resource' | 'member_visit' | 'text'

export interface TaskItem {
  id: number
  type: TaskItemType
  refId: number | null
  title: string
  myCompleted: boolean | null
  myCompletedAt: string | null
  completedCount: number | null
}

export interface GroupTask {
  id: number
  title: string
  content: string
  accountId: number
  username: string
  createdAt: string
  items: TaskItem[]
  memberCount: number
  myCompleted: number
  totalItems: number
  totalCompletions: number
  progressPercent: number | null
}

export interface TaskProgressMatrix {
  taskId: number
  title: string
  items: TaskItem[]
  members: {
    accountId: number
    username: string
    role: string
    completedItems: number
    totalItems: number
    percent: number | null
    itemStatuses: { itemId: number; completed: boolean; completedAt: string | null }[]
  }[]
}

export interface NamedOption {
  id: number
  title: string
}

export interface TaskItemDraft {
  type: TaskItemType
  refId: number | null
  title: string
}