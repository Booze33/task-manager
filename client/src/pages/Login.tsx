import { useState } from 'react'
import { ArrowRight, Eye, EyeOff, Layers3, LockKeyhole, Mail, Moon, Sun, Zap } from 'lucide-react'
import { Navigate, useNavigate } from 'react-router-dom'
import { api, getApiErrorMessage, getApiFieldErrors } from '../api/service'
import { useAppStore } from '../store'

export default function Login() {
  const navigate = useNavigate()
  const user = useAppStore((state) => state.user)
  const setAuth = useAppStore((state) => state.setAuth)
  const theme = useAppStore((state) => state.theme)
  const toggleTheme = useAppStore((state) => state.toggleTheme)
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState('')
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to="/" replace />

  const submit = async (event: React.FormEvent) => {
    event.preventDefault()
    setBusy(true)
    setErrors({})
    setFormError('')
    try {
      const tokens = mode === 'login'
        ? await api.login(email, password)
        : await api.register(name, email, password)
      const currentUser = await api.me(tokens.accessToken)
      setAuth(currentUser, tokens.accessToken, tokens.refreshToken)
      navigate('/')
    } catch (error) {
      setErrors(getApiFieldErrors(error))
      if (!Object.keys(getApiFieldErrors(error)).length) setFormError(getApiErrorMessage(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="auth-page">
      <button className="icon-button theme-switch auth-theme" aria-label="Toggle light and dark theme" onClick={toggleTheme}>
        {theme === 'dark' ? <Sun size={17} /> : <Moon size={17} />}
      </button>
      <section className="auth-showcase">
        <a className="brand" href="#"><span className="brand-mark"><Layers3 size={19} /></span>taskflow<span className="brand-period">.</span></a>
        <div className="hero-copy">
          <div className="eyebrow"><span className="live-dot" /> YOUR WORK, IN FLOW</div>
          <h1>Small steps.<br /><span>Serious momentum.</span></h1>
          <p>A calm place for your daily tasks and the habits that move you forward.</p>
        </div>
        <div className="preview-card">
          <div className="preview-top"><span className="mono muted">MADE FOR MOMENTUM</span><span className="preview-score"><Zap size={13} fill="currentColor" /> ONE STEP AT A TIME</span></div>
          <div className="preview-message">Build routines that make room for your goals.</div>
          <div className="preview-tags"><span>Tasks</span><span>Habits</span><span>Progress</span></div>
        </div>
        <div className="auth-footer">DESIGNED FOR YOUR NEXT GOOD DAY <span>© 2025 TASKFLOW</span></div>
      </section>
      <section className="auth-panel">
        <div className="auth-form-wrap">
          <span className="mobile-brand"><span className="brand-mark"><Layers3 size={18} /></span>taskflow<span className="brand-period">.</span></span>
          <span className="form-kicker">{mode === 'login' ? 'WELCOME BACK' : 'YOUR NEXT CHAPTER'}</span>
          <h2>{mode === 'login' ? 'Pick up your flow.' : 'Make room to grow.'}</h2>
          <p className="form-subtitle">{mode === 'login' ? 'Your goals are right where you left them.' : 'A little progress adds up to a lot.'}</p>
          <div className="auth-tabs" role="tablist" aria-label="Authentication">
            <button className={mode === 'login' ? 'active' : ''} role="tab" aria-selected={mode === 'login'} onClick={() => { setMode('login'); setErrors({}); setFormError('') }}>Log in</button>
            <button className={mode === 'register' ? 'active' : ''} role="tab" aria-selected={mode === 'register'} onClick={() => { setMode('register'); setErrors({}); setFormError('') }}>Create account</button>
          </div>
          <form onSubmit={submit} noValidate>
            {formError && <div className="form-error" role="alert">{formError}</div>}
            {mode === 'register' && <label className="field-label">Your name<div className={`input-wrap ${errors.name ? 'has-error' : ''}`}><span className="input-icon"><Layers3 size={16} /></span><input autoComplete="name" placeholder="Alex Morgan" value={name} onChange={(e) => setName(e.target.value)} aria-invalid={!!errors.name} /></div>{errors.name && <small className="field-error">{errors.name}</small>}</label>}
            <label className="field-label">Email address<div className={`input-wrap ${errors.email ? 'has-error' : ''}`}><span className="input-icon"><Mail size={16} /></span><input type="email" autoComplete="email" placeholder="you@example.com" value={email} onChange={(e) => setEmail(e.target.value)} aria-invalid={!!errors.email} /></div>{errors.email && <small className="field-error">{errors.email}</small>}</label>
            <label className="field-label">Password<div className={`input-wrap ${errors.password ? 'has-error' : ''}`}><span className="input-icon"><LockKeyhole size={16} /></span><input type={showPassword ? 'text' : 'password'} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} placeholder={mode === 'register' ? 'At least 12 characters' : 'Enter your password'} value={password} onChange={(e) => setPassword(e.target.value)} aria-invalid={!!errors.password} /><button type="button" className="password-toggle" aria-label={showPassword ? 'Hide password' : 'Show password'} onClick={() => setShowPassword(!showPassword)}>{showPassword ? <EyeOff size={16} /> : <Eye size={16} />}</button></div>{errors.password && <small className="field-error">{errors.password}</small>}</label>
            <button className="button button-primary auth-submit" disabled={busy}>{busy ? <span className="spinner" /> : <>{mode === 'login' ? 'Log in to TaskFlow' : 'Create my account'}<ArrowRight size={16} /></>}</button>
          </form>
          <div className="auth-note"><span className="live-dot" /> Secure access to your TaskFlow workspace</div>
        </div>
      </section>
    </main>
  )
}
