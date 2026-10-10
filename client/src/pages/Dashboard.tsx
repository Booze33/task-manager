import { useMemo, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Activity, ArrowDown, ArrowUpRight, CalendarDays, Check, CheckCheck, ChevronLeft, ChevronRight, Circle, CircleCheck, Clock3, Code2, Database, Flame, Layers3, ListFilter, LogOut, Moon, Plus, RefreshCw, Search, Sparkles, Sun, Target, X, Zap } from 'lucide-react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAppStore } from '../store'
import { useCompleteTask, useCreateTask, useInvalidateStats, useStats, useTasks } from '../hooks/queries'
import { PAGE_SIZE, RATE_LIMIT_MAX } from '../config'
import type { ActivityEntry, Task, TaskStatus } from '../types'

const habits = ['all', 'Daily reading', 'Morning movement', 'Deep work']
const statusLabels: Record<TaskStatus, string> = { todo: 'To do', 'in-progress': 'In progress', done: 'Done' }

export default function Dashboard() {
  const navigate = useNavigate()
  const user = useAppStore((state) => state.user)
  const clearAuth = useAppStore((state) => state.clearAuth)
  const toggleTheme = useAppStore((state) => state.toggleTheme)
  const theme = useAppStore((state) => state.theme)
  const rateRemaining = useAppStore((state) => state.rateRemaining)
  const activity = useAppStore((state) => state.activity)
  const [page, setPage] = useState(1)
  const [status, setStatus] = useState<TaskStatus | 'all'>('all')
  const [habit, setHabit] = useState('all')
  const [flaky, setFlaky] = useState(false)
  const [logOpen, setLogOpen] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [search, setSearch] = useState('')
  const { data: stats, isLoading: statsLoading } = useStats()
  const { data: taskPage, isLoading: tasksLoading, isError: tasksError, refetch } = useTasks(page, status, habit)
  const complete = useCompleteTask()
  const create = useCreateTask()
  const invalidate = useInvalidateStats()
  const queryClient = useQueryClient()

  const pages = taskPage?.totalPages ?? 1
  const tasks = useMemo(() => (taskPage?.items ?? []).filter((task) => task.title.toLowerCase().includes(search.toLowerCase())), [taskPage?.items, search])
  const logout = () => { clearAuth(); navigate('/login') }

  if (!user) return <Navigate to="/login" replace />

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="/" aria-label="TaskFlow home"><span className="brand-mark"><Layers3 size={18} /></span>taskflow<span className="brand-period">.</span></a>
        <div className="topbar-center"><span className="workspace-dot" /> PERSONAL WORKSPACE <span className="topbar-slash">/</span> OVERVIEW</div>
        <div className="topbar-actions">
          <RateMeter remaining={rateRemaining} />
          <span className="topbar-divider" />
          <button className="icon-button" aria-label="Toggle theme" onClick={toggleTheme}>{theme === 'dark' ? <Sun size={17} /> : <Moon size={17} />}</button>
          <button className="profile-chip" aria-label="Sign out" onClick={logout}><span>{user.name.split(' ').map((part) => part[0]).slice(0, 2).join('').toUpperCase()}</span><b>{user.name}</b><LogOut size={15} /></button>
        </div>
      </header>

      <main className="dashboard">
        <section className="welcome-row">
          <div><div className="eyebrow"><span className="live-dot" /> {new Intl.DateTimeFormat('en-US', { weekday: 'long', month: 'long', day: 'numeric' }).format(new Date()).toUpperCase()}</div><h1>Good morning, {user.name.split(' ')[0]}<span className="wave">✳</span></h1><p>Keep showing up. The results will follow.</p></div>
          <button className="button button-primary add-task-top" onClick={() => setModalOpen(true)}><Plus size={17} /> New task</button>
        </section>

        <section className="stats-grid" aria-label="Task statistics">
          <StatCard label="COMPLETED TODAY" value={statsLoading ? '—' : String(stats?.completedToday ?? 0).padStart(2, '0')} sub="tasks checked off" icon={<CheckCheck size={17} />} accent="green" />
          <StatCard label="CURRENT STREAK" value={statsLoading ? '—' : String(stats?.currentStreak ?? 0)} sub="days in a row" icon={<Flame size={17} />} accent="orange" suffix={<span className="streak-unit">DAYS</span>} />
          <StatCard label="COMPLETION RATE" value={statsLoading ? '—' : `${stats?.completionRate ?? 0}%`} sub="of all tasks complete" icon={<Target size={17} />} accent="indigo" suffix={<MiniDonut value={stats?.completionRate ?? 0} />} />
          <div className="stat-card week-card"><div className="stat-card-head"><span className="stat-label">THIS WEEK</span><span className="stat-icon purple"><Activity size={17} /></span></div><div className="week-chart"><div className="chart-guides"><i /><i /><i /></div>{(stats?.weekly ?? []).map((item) => <div className="chart-col" key={item.day}><div className="chart-track"><span style={{ height: `${Math.max(8, item.value / 8 * 100)}%` }} /></div><small>{item.day[0]}</small></div>)}</div><div className="stat-card-bottom"><span>DAILY FINISHES</span><span className="mono"><ArrowUpRight size={12} /> +18%</span></div></div>
        </section>

        <div className="content-grid">
          <section className="tasks-panel panel">
            <div className="panel-heading"><div><div className="eyebrow"><CalendarDays size={13} /> YOUR ROUTINE</div><h2>Tasks & habits<span className="task-count">{taskPage?.total ?? '—'}</span></h2></div><button className="button button-subtle" onClick={() => setModalOpen(true)}><Plus size={15} /> Add task</button></div>
            <div className="task-toolbar">
              <div className="status-tabs" role="tablist" aria-label="Filter tasks by status">
                {(['all', 'todo', 'in-progress', 'done'] as const).map((item) => <button key={item} role="tab" aria-selected={status === item} className={status === item ? 'selected' : ''} onClick={() => { setStatus(item); setPage(1) }}>{item === 'all' ? 'All tasks' : statusLabels[item]}</button>)}
              </div>
              <label className="search-box"><Search size={14} /><input placeholder="Find a task..." value={search} onChange={(event) => setSearch(event.target.value)} aria-label="Search tasks" /></label>
            </div>
            <div className="habit-filter"><ListFilter size={14} /><span>HABIT</span>{habits.map((item) => <button key={item} className={habit === item ? 'active' : ''} onClick={() => { setHabit(item); setPage(1) }}>{item === 'all' ? 'All' : item}</button>)}</div>
            <div className="task-list">
              {tasksLoading ? <TaskSkeleton /> : tasksError ? <div className="empty-state"><p>We couldn't load your tasks.</p><button className="button button-subtle" onClick={() => void refetch()}><RefreshCw size={14} /> Try again</button></div> : tasks.length === 0 ? <div className="empty-state"><span className="empty-icon"><Sparkles size={20} /></span><b>No tasks in this view</b><p>Try another filter or create a fresh task.</p><button className="button button-subtle" onClick={() => setModalOpen(true)}><Plus size={14} /> Create task</button></div> : tasks.map((task) => <TaskRow key={task.id} task={task} onComplete={() => complete.mutate({ id: task.id, flakyRetry: flaky })} busy={complete.isPending && complete.variables?.id === task.id} />)}
            </div>
            <div className="task-footer"><span>Showing <b>{taskPage?.items.length ?? 0}</b> of <b>{taskPage?.total ?? 0}</b> tasks <span className="footer-dot">·</span> page {page} of {pages}</span><div className="pagination"><button aria-label="Previous page" disabled={page <= 1} onClick={() => setPage((current) => current - 1)}><ChevronLeft size={16} /></button><span className="mono">{String(page).padStart(2, '0')} <i>/</i> {String(pages).padStart(2, '0')}</span><button aria-label="Next page" disabled={page >= pages} onClick={() => setPage((current) => current + 1)}><ChevronRight size={16} /></button></div></div>
            <div className="pagination-note"><Code2 size={13} /><span className="mono">GET /tasks?page={page}&amp;size={PAGE_SIZE}</span><span className="note-separator">·</span> {taskPage?.total ?? 47} records</div>
          </section>

          <aside className="right-column">
            <section className="focus-card">
              <div className="focus-head"><span><Zap size={14} fill="currentColor" /> DAILY FOCUS</span><span className="focus-live"><i /> LIVE</span></div>
              <h3>Make today<br />count.</h3><p>One focused step at a time is how big things get done.</p>
              <div className="focus-progress"><div><span>TODAY'S PROGRESS</span><b>{stats?.completionRate ?? 0}%</b></div><div className="progress-track"><span style={{ width: `${stats?.completionRate ?? 0}%` }} /></div></div>
              <button className="focus-link" onClick={() => { setStatus('todo'); setPage(1); window.scrollTo({ top: 520, behavior: 'smooth' }) }}>Find your next task <ArrowUpRight size={14} /></button>
              <div className="focus-decoration">✳</div>
            </section>
            <section className="system-card panel">
              <div className="system-heading"><div><div className="eyebrow"><span className="live-dot" /> UNDER THE HOOD</div><h3>System signals</h3></div><span className="live-tag">LIVE</span></div>
              <div className="cache-row"><div className="signal-icon"><RefreshCw size={15} /></div><div className="signal-copy"><b>Stats cache</b><small>Redis · cache-aside</small></div><CacheBadge entry={activity.find((entry) => entry.endpoint === '/users/me/stats')} /><button aria-label="Read stats from cache" title="Re-fetch stats without invalidating the cache" className="icon-button small-icon" onClick={() => void queryClient.refetchQueries({ queryKey: ['stats'] })}><Database size={14} /></button><button aria-label="Invalidate stats cache" title="Clear cached stats, then fetch again" className="icon-button small-icon" onClick={() => invalidate.mutate()} disabled={invalidate.isPending}><RefreshCw size={14} /></button></div>
              <div className="system-separator" />
              <div className="cache-row"><div className="signal-icon violet"><CircleCheck size={15} /></div><div className="signal-copy"><b>Idempotency</b><small>Safe completion retries</small></div><span className={`signal-state ${activity.some((entry) => entry.idempotent) ? 'state-active' : ''}`}><i />{activity.some((entry) => entry.idempotent) ? 'PROVEN' : 'READY'}</span></div>
              <div className="cache-row"><div className="signal-icon blue"><ListFilter size={15} /></div><div className="signal-copy"><b>Pagination</b><small>Page-based · {PAGE_SIZE} / page</small></div><span className="signal-count mono">{String(page).padStart(2, '0')} <span>/</span> {String(pages).padStart(2, '0')}</span></div>
              <div className="retry-toggle-row"><span>Flaky Retry</span><small>Send completion twice</small><button className={`toggle ${flaky ? 'on' : ''}`} role="switch" aria-checked={flaky} aria-label="Enable flaky retry" onClick={() => setFlaky(!flaky)}><i /></button></div>
            </section>
            <ActivityPanel entries={activity} open={logOpen} onToggle={() => setLogOpen(!logOpen)} />
          </aside>
        </div>
        <footer className="page-footer"><span>MADE FOR THE LONG GAME <span className="footer-heart">✳</span></span><span>MOCK API <i /> LATENCY 200–600MS</span></footer>
      </main>
      {modalOpen && <TaskModal onClose={() => setModalOpen(false)} onCreate={(input) => create.mutate(input, { onSuccess: () => setModalOpen(false) })} pending={create.isPending} />}
      <Toast />
    </div>
  )
}

function StatCard({ label, value, sub, icon, accent, suffix }: { label: string; value: string; sub: string; icon: React.ReactNode; accent: string; suffix?: React.ReactNode }) {
  return <div className="stat-card"><div className="stat-card-head"><span className="stat-label">{label}</span><span className={`stat-icon ${accent}`}>{icon}</span></div><div className="stat-value">{value}{suffix}</div><div className="stat-card-bottom"><span>{sub}</span>{accent === 'green' && <span className="delta mono"><ArrowUpRight size={13} /> +2</span>}</div></div>
}

function MiniDonut({ value }: { value: number }) {
  return <span className="mini-donut" style={{ background: `conic-gradient(var(--accent) ${value}%, var(--line) 0)` }}><i /></span>
}

function RateMeter({ remaining }: { remaining: number }) {
  const percent = Math.max(0, remaining / RATE_LIMIT_MAX * 100)
  const tone = remaining <= 5 ? 'danger' : remaining <= 20 ? 'warning' : ''
  return <div className={`rate-meter ${tone}`} role="meter" aria-label="API rate limit remaining" aria-valuemin={0} aria-valuemax={RATE_LIMIT_MAX} aria-valuenow={remaining}>
    <span className="rate-caption">API BUDGET</span><span className="rate-number mono">{String(remaining).padStart(3, '0')}<i> / {RATE_LIMIT_MAX}</i></span><div className="rate-track"><span style={{ width: `${percent}%` }} /></div>
  </div>
}

function TaskRow({ task, onComplete, busy }: { task: Task; onComplete: () => void; busy: boolean }) {
  const done = task.status === 'done'
  const overdue = !done && new Date(`${task.dueDate}T23:59:59`) < new Date()
  const formatted = new Date(`${task.dueDate}T12:00:00`).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
  return <article className={`task-row ${done ? 'is-done' : ''}`}>
    <button className={`task-check ${done ? 'checked' : ''}`} aria-label={done ? `${task.title} completed` : `Mark ${task.title} complete`} onClick={onComplete} disabled={done || busy}>{busy ? <span className="spinner small" /> : done ? <Check size={15} /> : <Circle size={17} />}</button>
    <div className="task-copy"><div className="task-title-line"><h3>{task.title}</h3><span className={`priority priority-${task.priority}`}><i />{task.priority}</span></div><div className="task-meta"><span className={`habit-dot habit-${task.habit === 'Daily reading' ? 'blue' : task.habit === 'Morning movement' ? 'orange' : 'violet'}`} />{task.habit}<span className="task-meta-sep">·</span><span className={overdue ? 'overdue' : ''}><Clock3 size={12} />{overdue ? 'Overdue' : formatted}</span></div></div>
    <span className={`status-pill status-${task.status}`}>{statusLabels[task.status]}</span>
    <button className="task-more icon-button" aria-label={`Complete ${task.title}`} onClick={onComplete} disabled={done || busy}>{done ? <Check size={15} /> : <ArrowUpRight size={15} />}</button>
  </article>
}

function TaskSkeleton() {
  return <div className="skeleton-list" aria-label="Loading tasks">{Array.from({ length: 4 }, (_, i) => <div className="skeleton-row" key={i}><i /><div><b /><span /></div></div>)}</div>
}

function CacheBadge({ entry }: { entry?: ActivityEntry }) {
  if (!entry?.cache) return <span className="cache-pill cache-idle" aria-label="Cache status unavailable">IDLE</span>
  return <span className={`cache-pill cache-${entry.cache.toLowerCase()}`} aria-label={`Cache ${entry.cache}`}>{entry.cache === 'HIT' ? <Zap size={11} fill="currentColor" /> : <ArrowDown size={11} />}{entry.cache}</span>
}

function ActivityPanel({ entries, open, onToggle }: { entries: ActivityEntry[]; open: boolean; onToggle: () => void }) {
  const clearActivity = useAppStore((state) => state.clearActivity)
  return <section className={`activity-panel panel ${open ? '' : 'collapsed'}`}>
    <div className="activity-header"><button className="activity-title" aria-expanded={open} onClick={onToggle}><span className="activity-symbol"><Activity size={15} /></span><span><b>System Design</b><small>API activity log <i className="live-dot" /></small></span><ChevronRight className="activity-chevron" size={15} /></button><button className="text-button" onClick={clearActivity} disabled={!entries.length}>CLEAR</button></div>
    {open && <><div className="log-summary"><span><i /> LIVE CAPTURE</span><span className="mono">{String(entries.length).padStart(2, '0')} REQUESTS</span></div><div className="activity-list">{entries.length === 0 ? <div className="log-empty"><span className="log-empty-icon"><Code2 size={17} /></span><p>API calls will show up here.</p><small>Every request, visible in real time.</small></div> : entries.slice(0, 8).map((entry) => <div className="log-entry" key={entry.id}><div className={`method-badge method-${entry.method.toLowerCase()}`}>{entry.method}</div><div className="log-details"><div className="log-route">{entry.endpoint}</div><div className="log-meta"><span className={entry.status >= 400 ? 'log-fail' : ''}>{entry.status || 'ERR'}</span><i />{entry.latency}ms{entry.cache && <><i />CACHE {entry.cache}</>}{entry.idempotent && <><i />IDEMPOTENT</>}</div></div><span className="log-remaining mono">{entry.remaining}<small> left</small></span></div>)}</div><div className="log-footer"><Code2 size={12} /><span>HTTP INTERCEPTOR</span><span className="log-footer-right">LATENCY <b>{entries[0]?.latency ?? '—'}ms</b></span></div></>}
  </section>
}

function TaskModal({ onClose, onCreate, pending }: { onClose: () => void; onCreate: (input: Pick<Task, 'title' | 'description' | 'habit' | 'dueDate' | 'priority'>) => void; pending: boolean }) {
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [habit, setHabit] = useState('Daily reading')
  const [dueDate, setDueDate] = useState(new Date().toISOString().slice(0, 10))
  const [priority, setPriority] = useState<Task['priority']>('medium')
  const [titleError, setTitleError] = useState('')
  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    if (title.trim().length < 3) { setTitleError('Add a title with at least 3 characters.'); return }
    onCreate({ title: title.trim(), description: description.trim(), habit, dueDate, priority })
  }
  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}><section className="task-modal" role="dialog" aria-modal="true" aria-labelledby="modal-title"><div className="modal-head"><div><div className="eyebrow"><Sparkles size={13} /> MAKE IT HAPPEN</div><h2 id="modal-title">Add a task</h2></div><button className="icon-button" aria-label="Close dialog" onClick={onClose}><X size={18} /></button></div><form onSubmit={submit} noValidate><label className="field-label">Task name<div className={`input-wrap ${titleError ? 'has-error' : ''}`}><input autoFocus placeholder="What would you like to get done?" value={title} onChange={(event) => { setTitle(event.target.value); setTitleError('') }} aria-invalid={!!titleError} /></div>{titleError && <small className="field-error">{titleError}</small>}</label><label className="field-label">A little context <span className="optional">(optional)</span><div className="input-wrap"><input placeholder="Add a note to your future self..." value={description} onChange={(event) => setDescription(event.target.value)} /></div></label><div className="modal-fields"><label className="field-label">Habit<select value={habit} onChange={(event) => setHabit(event.target.value)}>{habits.slice(1).map((item) => <option key={item}>{item}</option>)}</select></label><label className="field-label">Due date<div className="input-wrap date-input"><input type="date" value={dueDate} onChange={(event) => setDueDate(event.target.value)} /></div></label></div><label className="field-label">Priority<div className="priority-select">{(['low', 'medium', 'high'] as const).map((item) => <button type="button" key={item} className={priority === item ? `selected priority-${item}` : ''} onClick={() => setPriority(item)}><i />{item}</button>)}</div></label><div className="modal-actions"><button type="button" className="button button-subtle" onClick={onClose}>Cancel</button><button className="button button-primary" disabled={pending}>{pending ? <span className="spinner" /> : <><Plus size={15} /> Create task</>}</button></div></form></section></div>
}

function Toast() {
  const toast = useAppStore((state) => state.toast)
  const dismiss = useAppStore((state) => state.dismissToast)
  if (!toast) return null
  return <div className={`toast toast-${toast.kind}`} role="status"><span>{toast.kind === 'success' ? <Check size={15} /> : <X size={15} />}</span>{toast.message}<button aria-label="Dismiss notification" onClick={dismiss}><X size={14} /></button></div>
}
