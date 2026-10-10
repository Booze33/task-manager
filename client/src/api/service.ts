import type { AxiosResponse } from 'axios'
import { USE_MOCKS } from '../config'
import { mockApi } from '../mocks/api'
import { useAppStore } from '../store'
import type { ActivityEntry, PaginatedTasks, Task, TaskStatus, User, UserStats } from '../types'
import { http } from './client'

type Response<T> = { data: T; headers: Record<string, string> }

const record = async <T>(
  method: string,
  endpoint: string,
  run: () => Promise<Response<T> | AxiosResponse<T>>,
): Promise<T> => {
  const started = performance.now()
  let status = 200
  let headers: Record<string, string> = {}
  try {
    const result = await run()
    if ('status' in result) {
      status = result.status
      headers = result.headers as Record<string, string>
    } else {
      headers = result.headers
    }
    const remaining = Number(headers['x-ratelimit-remaining'] ?? useAppStore.getState().rateRemaining)
    const reset = Number(headers['x-ratelimit-reset'])
    if (Number.isFinite(remaining)) useAppStore.getState().setRateLimit(remaining, Number.isFinite(reset) ? reset : null)
    const entry: ActivityEntry = {
      id: crypto.randomUUID(), method, endpoint, status,
      latency: Math.round(performance.now() - started),
      cache: headers['x-cache'] as ActivityEntry['cache'],
      remaining: Number.isFinite(remaining) ? remaining : 0,
      idempotent: headers['x-idempotent'] === 'true',
      timestamp: new Date().toISOString(),
    }
    useAppStore.getState().logActivity(entry)
    return result.data
  } catch (error) {
    const caught = error as { status?: number; response?: { status?: number; headers?: Record<string, string>; data?: { message?: string } }; message?: string }
    status = caught.response?.status ?? caught.status ?? 0
    headers = caught.response?.headers ?? {}
    const remaining = Number(headers['x-ratelimit-remaining'] ?? useAppStore.getState().rateRemaining)
    const reset = Number(headers['x-ratelimit-reset'])
    if (Number.isFinite(remaining)) useAppStore.getState().setRateLimit(remaining, Number.isFinite(reset) ? reset : null)
    useAppStore.getState().logActivity({
      id: crypto.randomUUID(), method, endpoint, status,
      latency: Math.round(performance.now() - started),
      remaining: Number.isFinite(remaining) ? remaining : 0,
      timestamp: new Date().toISOString(),
    })
    useAppStore.getState().showToast(caught.response?.data?.message ?? caught.message ?? 'Something went wrong. Try again.', 'error')
    throw error
  }
}

export const api = {
  login: (email: string, password: string) => record('POST', '/auth/login', async () => {
    if (USE_MOCKS) return mockApi.login(email, password)
    const result = await http.post('/auth/login', { email, password })
    return { ...result, data: normalizeAuth(result.data, email) }
  }),
  register: (name: string, email: string, password: string) => record('POST', '/auth/register', async () => {
    if (USE_MOCKS) return mockApi.register(name, email, password)
    const result = await http.post('/auth/register', { name, email, password })
    return { ...result, data: normalizeAuth(result.data, email, name) }
  }),
  me: () => record('GET', '/users/me', () => USE_MOCKS ? mockApi.me() : http.get<User>('/users/me')),
  tasks: (page: number, size: number, status: TaskStatus | 'all', habit: string) =>
    record('GET', `/tasks?page=${page}&size=${size}`, () => USE_MOCKS
      ? mockApi.listTasks(page, size, status, habit)
      : http.get<PaginatedTasks>('/tasks', { params: { page, size, status: status === 'all' ? undefined : status, habit: habit === 'all' ? undefined : habit } })),
  createTask: (input: Pick<Task, 'title' | 'description' | 'habit' | 'dueDate' | 'priority'>) =>
    record('POST', '/tasks', () => USE_MOCKS ? mockApi.createTask(input) : http.post<Task>('/tasks', input)),
  completeTask: async (id: string, flakyRetry = false) => {
    const send = () => record('POST', `/tasks/${id}/complete`, async () => {
      if (USE_MOCKS) return mockApi.completeTask(id)
      return http.post<Task>(`/tasks/${id}/complete`)
    })
    const first = await send()
    return flakyRetry ? send() : first
  },
  stats: () => record('GET', '/users/me/stats', () => USE_MOCKS ? mockApi.getStats() : http.get<UserStats>('/users/me/stats')),
  invalidateStats: () => record('DELETE', '/users/me/stats/cache', () => USE_MOCKS
    ? mockApi.invalidateStats()
    : http.delete<{ invalidated: boolean }>('/users/me/stats/cache')),
}

function normalizeAuth(data: unknown, email: string, name?: string): { user: User; token: string } {
  const body = data as { token?: string; user?: User; id?: string; username?: string; email?: string }
  return {
    token: body.token ?? '',
    user: body.user ?? { id: body.id ?? 'user', name: name ?? body.username ?? email.split('@')[0], email: body.email ?? email },
  }
}
