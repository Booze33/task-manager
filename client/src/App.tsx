import { useEffect, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Navigate, Route, Routes } from 'react-router-dom'
import { api, SessionExpiredError } from './api/service'
import Dashboard from './pages/Dashboard'
import Login from './pages/Login'
import { useAppStore } from './store'

export default function App() {
  const theme = useAppStore((state) => state.theme)
  const token = useAppStore((state) => state.token)
  const setAuth = useAppStore((state) => state.setAuth)
  const clearAuth = useAppStore((state) => state.clearAuth)
  const queryClient = useQueryClient()
  const restorePromises = useRef(new Map<number, ReturnType<typeof api.restoreSession>>())
  const [sessionState, setSessionState] = useState<'checking' | 'ready' | 'unavailable'>(
    token ? 'checking' : 'ready',
  )
  const [retry, setRetry] = useState(0)

  useEffect(() => { document.documentElement.dataset.theme = theme }, [theme])

  useEffect(() => {
    const { token: accessToken, refreshToken: savedRefreshToken } = useAppStore.getState()
    if (!accessToken) {
      if (useAppStore.getState().user) clearAuth()
      return
    }
    let active = true
    setSessionState('checking')
    let restore = restorePromises.current.get(retry)
    if (!restore) {
      restore = api.restoreSession(accessToken, savedRefreshToken)
      restorePromises.current.set(retry, restore)
    }

    void restore.then((session) => {
      if (!active) return
      setAuth(session.user, session.accessToken, session.refreshToken)
      setSessionState('ready')
    }).catch((error: unknown) => {
      if (!active) return
      if (error instanceof SessionExpiredError) {
        clearAuth()
        queryClient.clear()
        restorePromises.current.clear()
        setSessionState('ready')
        return
      }
      setSessionState('unavailable')
    })

    return () => { active = false }
  }, [retry, clearAuth, queryClient, setAuth])

  if (sessionState === 'checking') {
    return <main className="session-state"><span className="spinner" /><p>Checking your session…</p></main>
  }

  if (sessionState === 'unavailable') {
    return (
      <main className="session-state">
        <p>TaskFlow couldn’t reach the API to verify your session.</p>
        <button className="button button-primary" onClick={() => setRetry((value) => value + 1)}>Try again</button>
        <button className="button button-subtle" onClick={() => { clearAuth(); queryClient.clear(); restorePromises.current.clear(); setSessionState('ready') }}>Sign out</button>
      </main>
    )
  }

  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={token ? <Dashboard /> : <Navigate to="/login" replace />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
