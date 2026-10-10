import { useEffect } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import Login from './pages/Login'
import { useAppStore } from './store'

export default function App() {
  const theme = useAppStore((state) => state.theme)
  const token = useAppStore((state) => state.token)
  useEffect(() => { document.documentElement.dataset.theme = theme }, [theme])
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={token ? <Dashboard /> : <Navigate to="/login" replace />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
