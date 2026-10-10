import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'
import type { ActivityEntry, User } from '../types'
import { RATE_LIMIT_MAX } from '../config'

interface AppState {
  user: User | null
  token: string | null
  theme: 'dark' | 'light'
  rateRemaining: number
  rateReset: number | null
  activity: ActivityEntry[]
  toast: { id: number; message: string; kind: 'success' | 'error' } | null
  setAuth: (user: User, token: string) => void
  clearAuth: () => void
  toggleTheme: () => void
  setRateLimit: (remaining: number, reset: number | null) => void
  logActivity: (entry: ActivityEntry) => void
  clearActivity: () => void
  showToast: (message: string, kind?: 'success' | 'error') => void
  dismissToast: () => void
}

export const useAppStore = create<AppState>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      theme: 'dark',
      rateRemaining: RATE_LIMIT_MAX,
      rateReset: null,
      activity: [],
      toast: null,
      setAuth: (user, token) => set({ user, token }),
      clearAuth: () => set({ user: null, token: null }),
      toggleTheme: () => set((state) => ({ theme: state.theme === 'dark' ? 'light' : 'dark' })),
      setRateLimit: (rateRemaining, rateReset) => set({ rateRemaining, rateReset }),
      logActivity: (entry) => set((state) => ({ activity: [entry, ...state.activity].slice(0, 40) })),
      clearActivity: () => set({ activity: [] }),
      showToast: (message, kind = 'success') => {
        const id = Date.now()
        set({ toast: { id, message, kind } })
        window.setTimeout(() => set((state) => state.toast?.id === id ? { toast: null } : state), 3200)
      },
      dismissToast: () => set({ toast: null }),
    }),
    {
      name: 'taskflow-preferences',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ user: state.user, token: state.token, theme: state.theme }),
    },
  ),
)
