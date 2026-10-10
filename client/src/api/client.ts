import axios from 'axios'
import { API_BASE_URL } from '../config'
import { useAppStore } from '../store'

export const http = axios.create({ baseURL: API_BASE_URL, timeout: 10_000 })

http.interceptors.request.use((config) => {
  const token = useAppStore.getState().token
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use((response) => {
  const remaining = Number(response.headers['x-ratelimit-remaining'])
  const reset = Number(response.headers['x-ratelimit-reset'])
  if (Number.isFinite(remaining)) useAppStore.getState().setRateLimit(remaining, Number.isFinite(reset) ? reset : null)
  return response
})
