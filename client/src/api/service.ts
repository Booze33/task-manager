import type { AxiosError, AxiosResponse } from 'axios'
import { useAppStore } from '../store'
import type { ActivityEntry, AuthTokens, PaginatedTasks, Task, TaskStatus, User, UserStats } from '../types'
import { http } from './client'

class ApiContractError extends Error {
  readonly status = 502
}

export class SessionExpiredError extends Error {
  readonly status = 401
}

const record = async <T>(
  method: string,
  endpoint: string,
  run: () => Promise<AxiosResponse<T>>,
  notifyErrors = true,
): Promise<T> => {
  const started = performance.now()
  let status = 0
  let headers: Record<string, string> = {}
  try {
    const result = await run()
    status = result.status
    headers = result.headers as Record<string, string>
    const remaining = parseHeader(headers['x-ratelimit-remaining'])
    const reset = parseHeader(headers['x-ratelimit-reset'])
    useAppStore.getState().setRateLimit(remaining, reset)
    const entry: ActivityEntry = {
      id: crypto.randomUUID(), method, endpoint, status,
      latency: Math.round(performance.now() - started),
      cache: headers['x-cache'] as ActivityEntry['cache'],
      remaining,
      idempotent: headers['x-idempotent'] === 'true',
      timestamp: new Date().toISOString(),
    }
    useAppStore.getState().logActivity(entry)
    return result.data
  } catch (error) {
    const caught = error as AxiosError<unknown> & { status?: number }
    status = caught.response?.status ?? caught.status ?? 0
    headers = (caught.response?.headers ?? {}) as Record<string, string>
    const remaining = parseHeader(headers['x-ratelimit-remaining'])
    const reset = parseHeader(headers['x-ratelimit-reset'])
    useAppStore.getState().setRateLimit(remaining, reset)
    useAppStore.getState().logActivity({
      id: crypto.randomUUID(), method, endpoint, status,
      latency: Math.round(performance.now() - started),
      remaining,
      timestamp: new Date().toISOString(),
    })
    if (notifyErrors) useAppStore.getState().showToast(getErrorMessage(caught), 'error')
    throw error
  }
}

export const api = {
  login: (email: string, password: string) => record('POST', '/auth/login', async () => {
    const response = await http.post<unknown>('/auth/login', { email, password })
    return { ...response, data: requireAuthResult(response.data, '/auth/login') }
  }),
  register: (name: string, email: string, password: string) => record('POST', '/auth/signup', async () => {
    const response = await http.post<unknown>('/auth/signup', { name, email, password })
    return { ...response, data: requireAuthResult(response.data, '/auth/signup') }
  }),
  me: (accessToken?: string, notifyErrors = true) => record('GET', '/users/me', async () => {
    const response = await http.get<unknown>('/users/me', accessToken
      ? { headers: { Authorization: `Bearer ${accessToken}` } }
      : undefined)
    return { ...response, data: requireUser(response.data, '/users/me') }
  }, notifyErrors),
  refresh: (refreshToken: string, notifyErrors = true) => record('POST', '/auth/refresh', async () => {
    const response = await http.post<unknown>('/auth/refresh', { refreshToken })
    return { ...response, data: requireAuthResult(response.data, '/auth/refresh') }
  }, notifyErrors),
  restoreSession: async (accessToken: string, refreshToken: string | null) => {
    try {
      const user = await api.me(accessToken, false)
      return { user, accessToken, refreshToken }
    } catch (error) {
      if (getHttpStatus(error) !== 401) throw error
      if (!refreshToken) throw new SessionExpiredError('Your session has expired. Please sign in again.')
    }

    let renewed: AuthTokens
    try {
      renewed = await api.refresh(refreshToken, false)
    } catch (error) {
      if (getHttpStatus(error) === 400 || getHttpStatus(error) === 401) {
        throw new SessionExpiredError('Your session has expired. Please sign in again.')
      }
      throw error
    }
    useAppStore.getState().setTokens(renewed.accessToken, renewed.refreshToken)

    try {
      const user = await api.me(renewed.accessToken, false)
      return { user, ...renewed }
    } catch (error) {
      if (getHttpStatus(error) === 401) throw new SessionExpiredError('Your session has expired. Please sign in again.')
      throw error
    }
  },
  logout: (accessToken: string, refreshToken: string | null) => record('POST', '/auth/logout', () =>
    http.post<void>('/auth/logout', refreshToken ? { refreshToken } : undefined, { headers: { Authorization: `Bearer ${accessToken}` } })),
  tasks: (page: number, size: number, status: TaskStatus | 'all', habit: string) => {
    const params = { page, size, status: status === 'all' ? undefined : status, habit: habit === 'all' ? undefined : habit }
    const query = new URLSearchParams()
    query.set('page', String(page))
    query.set('size', String(size))
    if (params.status) query.set('status', params.status)
    if (params.habit) query.set('habit', params.habit)
    return record('GET', `/tasks?${query.toString()}`, async () => {
      const response = await http.get<unknown>('/tasks', { params })
      return { ...response, data: requirePaginatedTasks(response.data, '/tasks') }
    })
  },
  createTask: (input: Pick<Task, 'title' | 'description' | 'habit' | 'dueDate' | 'priority'>) =>
    record('POST', '/tasks', async () => {
      const response = await http.post<unknown>('/tasks', input)
      return { ...response, data: requireTask(response.data, 'POST /tasks') }
    }),
  completeTask: async (id: string, flakyRetry = false) => {
    const send = () => record('POST', `/tasks/${id}/complete`, async () => {
      const response = await http.post<unknown>(`/tasks/${id}/complete`)
      return { ...response, data: requireTask(response.data, `POST /tasks/${id}/complete`) }
    })
    const first = await send()
    return flakyRetry ? send() : first
  },
  stats: () => record('GET', '/users/me/stats', async () => {
    const response = await http.get<unknown>('/users/me/stats')
    return { ...response, data: requireStats(response.data, '/users/me/stats') }
  }),
  invalidateStats: () => record('DELETE', '/users/me/stats/cache', () => http.delete<void>('/users/me/stats/cache')),
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function parseHeader(value: string | undefined): number | null {
  if (value === undefined || value.trim() === '') return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

function requireAuthResult(data: unknown, endpoint: string): AuthTokens {
  if (
    !isObject(data) ||
    typeof data.accessToken !== 'string' || !data.accessToken ||
    typeof data.refreshToken !== 'string' || !data.refreshToken ||
    data.tokenType !== 'Bearer' ||
    typeof data.expiresIn !== 'number' || !Number.isFinite(data.expiresIn) || data.expiresIn <= 0
  ) {
    throw new ApiContractError(`${endpoint} returned an unsupported response. Expected { accessToken, refreshToken, tokenType: "Bearer", expiresIn }.`)
  }
  return {
    accessToken: data.accessToken,
    refreshToken: data.refreshToken,
    tokenType: data.tokenType,
    expiresIn: data.expiresIn,
  }
}

function requireUser(data: unknown, endpoint: string): User {
  if (!isObject(data) || typeof data.id !== 'string' || !data.id || typeof data.name !== 'string' || !data.name || typeof data.email !== 'string' || !data.email) {
    throw new ApiContractError(`${endpoint} returned an unsupported user. Expected string fields: id, name, and email.`)
  }
  return { id: data.id, name: data.name, email: data.email }
}

function isTask(data: unknown): data is Task {
  return isObject(data) &&
    typeof data.id === 'string' &&
    typeof data.title === 'string' &&
    typeof data.description === 'string' &&
    (data.status === 'todo' || data.status === 'in-progress' || data.status === 'done') &&
    typeof data.habit === 'string' &&
    typeof data.dueDate === 'string' &&
    typeof data.createdAt === 'string' &&
    (data.priority === 'low' || data.priority === 'medium' || data.priority === 'high')
}

function requireTask(data: unknown, endpoint: string): Task {
  if (!isTask(data)) {
    throw new ApiContractError(`${endpoint} returned an unsupported task. Confirm the task DTO matches the client contract.`)
  }
  return data
}

function requirePaginatedTasks(data: unknown, endpoint: string): PaginatedTasks {
  if (
    !isObject(data) ||
    !Array.isArray(data.items) ||
    typeof data.total !== 'number' || !Number.isFinite(data.total) ||
    typeof data.page !== 'number' || !Number.isFinite(data.page) ||
    typeof data.size !== 'number' || !Number.isFinite(data.size) ||
    typeof data.totalPages !== 'number' || !Number.isFinite(data.totalPages)
  ) {
    throw new ApiContractError(`${endpoint} returned an unsupported page. Expected { items, total, page, size, totalPages }.`)
  }
  return {
    items: data.items.map((item) => requireTask(item, `GET ${endpoint}`)),
    total: data.total,
    page: data.page,
    size: data.size,
    totalPages: data.totalPages,
  }
}

function isUserStats(data: unknown): data is UserStats {
  return isObject(data) &&
    typeof data.completedToday === 'number' && Number.isFinite(data.completedToday) &&
    typeof data.totalTasks === 'number' && Number.isFinite(data.totalTasks) &&
    typeof data.completionRate === 'number' && Number.isFinite(data.completionRate) &&
    typeof data.currentStreak === 'number' && Number.isFinite(data.currentStreak) &&
    Array.isArray(data.weekly) &&
    data.weekly.every((item) => isObject(item) && typeof item.day === 'string' && typeof item.value === 'number' && Number.isFinite(item.value))
}

function requireStats(data: unknown, endpoint: string): UserStats {
  if (!isUserStats(data)) {
    throw new ApiContractError(`${endpoint} returned unsupported stats. Confirm the UserStats response contract.`)
  }
  return data
}

function getErrorMessage(error: AxiosError<unknown> & { status?: number }): string {
  if (error instanceof ApiContractError) return error.message
  const body = error.response?.data
  if (isObject(body)) {
    if (typeof body.message === 'string') return body.message
    if (typeof body.detail === 'string') return body.detail
    if (typeof body.title === 'string') return body.title
  }
  if (!error.response) return `Cannot reach the TaskFlow API at ${http.defaults.baseURL}. Check that the backend is running and VITE_API_BASE_URL is correct.`
  return error.message || `The API request failed with HTTP ${error.response.status}.`
}

function getHttpStatus(error: unknown): number | undefined {
  if (error instanceof SessionExpiredError || error instanceof ApiContractError) return error.status
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = error.response
    if (typeof response === 'object' && response !== null && 'status' in response && typeof response.status === 'number') {
      return response.status
    }
  }
  return undefined
}

export function getApiFieldErrors(error: unknown): Record<string, string> {
  if (typeof error !== 'object' || error === null || !('response' in error)) return {}
  const response = error.response
  if (typeof response !== 'object' || response === null || !('data' in response)) return {}
  const body = response.data
  if (!isObject(body) || !isObject(body.errors)) return {}
  return Object.fromEntries(
    Object.entries(body.errors).filter((entry): entry is [string, string] => typeof entry[1] === 'string'),
  )
}

export function getApiErrorMessage(error: unknown): string {
  const caught = error as AxiosError<unknown>
  return getErrorMessage(caught)
}
