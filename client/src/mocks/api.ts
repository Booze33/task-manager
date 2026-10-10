import type { PaginatedTasks, Task, TaskStatus, User, UserStats } from '../types'
import { PAGE_SIZE, RATE_LIMIT_MAX } from '../config'
import { createSeedTasks } from './seed'

type MockResult<T> = { data: T; headers: Record<string, string> }
let tasks = createSeedTasks()
let cachedStats: UserStats | null = null
let remaining = RATE_LIMIT_MAX
let windowStarted = Date.now()
let user: User | null = null

const resetWindowIfExpired = () => {
  if (Date.now() - windowStarted > 60_000) {
    remaining = RATE_LIMIT_MAX
    windowStarted = Date.now()
  }
}
const rateLimitHeaders = () => ({
  'x-ratelimit-remaining': String(remaining),
  'x-ratelimit-reset': String(Math.floor((windowStarted + 60_000) / 1000)),
})
const wait = async () => {
  resetWindowIfExpired()
  if (remaining <= 0) {
    throw {
      status: 429,
      response: {
        status: 429,
        data: { message: 'Request limit reached. Try again when the rate-limit window resets.' },
        headers: rateLimitHeaders(),
      },
    }
  }
  remaining -= 1
  await new Promise((resolve) => window.setTimeout(resolve, 200 + Math.random() * 400))
}
const response = <T>(data: T, extra: Record<string, string> = {}): MockResult<T> => {
  resetWindowIfExpired()
  return {
    data,
    headers: {
      ...rateLimitHeaders(),
      ...extra,
    },
  }
}

export const mockApi = {
  async login(email: string, password: string): Promise<MockResult<{ user: User; token: string }>> {
    await wait()
    if (!email.includes('@') || password.length < 6) {
      throw { status: 422, response: { data: { message: 'Please check your details.', errors: [
        ...(!email.includes('@') ? [{ field: 'email', message: 'Enter a valid email address.' }] : []),
        ...(password.length < 6 ? [{ field: 'password', message: 'Password must be at least 6 characters.' }] : []),
      ] } } }
    }
    user = { id: 'user-001', name: email.split('@')[0].replace(/[._-]/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase()), email }
    return response({ user, token: `mock-jwt-${crypto.randomUUID()}` })
  },
  async register(name: string, email: string, password: string): Promise<MockResult<{ user: User; token: string }>> {
    await wait()
    if (name.trim().length < 2 || !email.includes('@') || password.length < 6) {
      throw { status: 422, response: { data: { message: 'Please check your details.', errors: [
        ...(name.trim().length < 2 ? [{ field: 'name', message: 'Name must be at least 2 characters.' }] : []),
        ...(!email.includes('@') ? [{ field: 'email', message: 'Enter a valid email address.' }] : []),
        ...(password.length < 6 ? [{ field: 'password', message: 'Password must be at least 6 characters.' }] : []),
      ] } } }
    }
    user = { id: crypto.randomUUID(), name: name.trim(), email }
    return response({ user, token: `mock-jwt-${crypto.randomUUID()}` })
  },
  async me(): Promise<MockResult<User>> {
    await wait()
    return response(user ?? { id: 'user-001', name: 'Alex Morgan', email: 'alex@taskflow.dev' })
  },
  async listTasks(page: number, size: number, status: TaskStatus | 'all', habit: string): Promise<MockResult<PaginatedTasks>> {
    await wait()
    const filtered = tasks.filter((task) => (status === 'all' || task.status === status) && (habit === 'all' || task.habit === habit))
    const start = (page - 1) * size
    return response({ items: filtered.slice(start, start + size), total: filtered.length, page, size, totalPages: Math.max(1, Math.ceil(filtered.length / size)) })
  },
  async createTask(input: Pick<Task, 'title' | 'description' | 'habit' | 'dueDate' | 'priority'>): Promise<MockResult<Task>> {
    await wait()
    if (input.title.trim().length < 3) {
      throw { status: 422, response: { data: { message: 'Please fix the highlighted field.', errors: [{ field: 'title', message: 'Add a title with at least 3 characters.' }] } } }
    }
    const task: Task = { ...input, id: crypto.randomUUID(), status: 'todo', createdAt: new Date().toISOString() }
    tasks = [task, ...tasks]
    cachedStats = null
    return response(task)
  },
  async completeTask(id: string): Promise<MockResult<Task>> {
    await wait()
    const task = tasks.find((item) => item.id === id)
    if (!task) throw { status: 404, response: { data: { message: 'Task not found.' } } }
    const duplicate = task.status === 'done'
    if (!duplicate) {
      tasks = tasks.map((item) => item.id === id ? { ...item, status: 'done' } : item)
      cachedStats = null
    }
    return response(tasks.find((item) => item.id === id)!, duplicate ? { 'x-idempotent': 'true' } : {})
  },
  async getStats(): Promise<MockResult<UserStats>> {
    await wait()
    const hit = cachedStats !== null
    if (!cachedStats) {
      const completed = tasks.filter((task) => task.status === 'done').length
      cachedStats = {
        completedToday: tasks.filter((task) => task.status === 'done' && task.dueDate === new Date().toISOString().slice(0, 10)).length,
        totalTasks: tasks.length,
        completionRate: Math.round((completed / Math.max(1, tasks.length)) * 100),
        currentStreak: 7,
        weekly: [
          { day: 'Mon', value: 3 }, { day: 'Tue', value: 5 }, { day: 'Wed', value: 2 },
          { day: 'Thu', value: 6 }, { day: 'Fri', value: 4 }, { day: 'Sat', value: 7 }, { day: 'Sun', value: 4 },
        ],
      }
    }
    return response(cachedStats, { 'x-cache': hit ? 'HIT' : 'MISS' })
  },
  async invalidateStats(): Promise<MockResult<{ invalidated: boolean }>> {
    await wait()
    cachedStats = null
    return response({ invalidated: true })
  },
  async getRemaining(): Promise<number> { return remaining },
  pageSize: PAGE_SIZE,
}
