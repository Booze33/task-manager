export type TaskStatus = 'todo' | 'in-progress' | 'done'

export interface User {
  id: string
  name: string
  email: string
}

export interface Task {
  id: string
  title: string
  description: string
  status: TaskStatus
  habit: string
  dueDate: string
  createdAt: string
  priority: 'low' | 'medium' | 'high'
}

export interface PaginatedTasks {
  items: Task[]
  total: number
  page: number
  size: number
  totalPages: number
}

export interface UserStats {
  completedToday: number
  totalTasks: number
  completionRate: number
  currentStreak: number
  weekly: { day: string; value: number }[]
}

export interface ApiErrorField {
  field: string
  message: string
}

export interface ApiErrorBody {
  message: string
  errors?: ApiErrorField[]
}

export interface ActivityEntry {
  id: string
  method: string
  endpoint: string
  status: number
  latency: number
  cache?: 'HIT' | 'MISS'
  remaining: number
  idempotent?: boolean
  timestamp: string
}
