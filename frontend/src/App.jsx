import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, setToken, UnauthorizedError } from './api'
import AuthPage from './components/AuthPage'
import ClipModal from './components/ClipModal'
import Icon from './components/Icon'
import JobDetail from './components/JobDetail'
import Applications from './pages/Applications'
import Dashboard from './pages/Dashboard'
import Jobs from './pages/Jobs'
import Profile from './pages/Profile'

const NAV = [
  { id: 'dashboard', label: 'Dashboard', icon: 'home' },
  { id: 'jobs', label: 'Find jobs', icon: 'search' },
  { id: 'applications', label: 'My applications', icon: 'board' },
  { id: 'profile', label: 'Profile', icon: 'user' },
]

const CLIP_KEY = 'jobboard.pendingClip'

/** A job sent by the bookmarklet arrives as #save=<json>; keep it across the login screen. */
function readClip() {
  try {
    if (window.location.hash.startsWith('#save=')) {
      const raw = decodeURIComponent(window.location.hash.slice(6))
      sessionStorage.setItem(CLIP_KEY, raw)
      history.replaceState(null, '', window.location.pathname)
    }
    const stored = sessionStorage.getItem(CLIP_KEY)
    return stored ? JSON.parse(stored) : null
  } catch {
    return null
  }
}

export default function App() {
  const [user, setUser] = useState(undefined) // undefined = checking, null = logged out
  const [page, setPage] = useState('dashboard')
  const [profileSection, setProfileSection] = useState('personal')
  const [openJobId, setOpenJobId] = useState(null)
  const [refreshKey, setRefreshKey] = useState(0)
  const [toast, setToast] = useState(null)
  const [finding, setFinding] = useState(false)
  const [lastSyncAt, setLastSyncAt] = useState(null)
  const [clip, setClip] = useState(readClip)

  useEffect(() => {
    api.me().then(setUser).catch(() => setUser(null))
  }, [])

  // The bookmarklet normally opens a new tab, but also handle it when this tab is already open.
  useEffect(() => {
    const onHash = () => { const c = readClip(); if (c) setClip(c) }
    window.addEventListener('hashchange', onHash)
    return () => window.removeEventListener('hashchange', onHash)
  }, [])

  const notify = useCallback((message, kind = 'ok') => {
    setToast({ message, kind, id: Date.now() })
  }, [])

  useEffect(() => {
    if (!toast) return undefined
    const t = setTimeout(() => setToast(null), 4000)
    return () => clearTimeout(t)
  }, [toast])

  const logout = useCallback(() => {
    setToken('')
    setUser(null)
    setOpenJobId(null)
  }, [])

  const refresh = useCallback(() => setRefreshKey((k) => k + 1), [])

  const error = useCallback((e) => {
    if (e instanceof UnauthorizedError) logout()
    else notify(e.message, 'error')
  }, [notify, logout])

  // Shared job actions used by every page.
  const actions = useMemo(() => ({
    error,
    update: async (id, patch, message) => {
      try {
        await api.updateJob(id, patch)
        if (message) notify(message)
        refresh()
      } catch (e) { error(e) }
    },
    remove: async (id) => {
      try {
        await api.deleteJob(id)
        notify('Removed. You won’t see this job again.')
        refresh()
      } catch (e) { error(e) }
    },
  }), [error, notify, refresh])

  const go = useCallback((target, section) => {
    setPage(target)
    if (section) setProfileSection(section)
    window.scrollTo(0, 0)
  }, [])

  const findJobs = useCallback(async () => {
    setFinding(true)
    notify('Searching job sites for you. This takes about 10–20 seconds…')
    try {
      const r = await api.sync()
      const hint = r.sources.sync
      if (hint) notify(hint, 'error')
      else notify(r.added > 0 ? `Found ${r.added} new matching job${r.added === 1 ? '' : 's'}!` : 'No new matches right now. Try again later.')
      setLastSyncAt(new Date().toISOString())
      refresh()
    } catch (e) { error(e) } finally { setFinding(false) }
  }, [notify, refresh, error])

  const closeJob = useCallback(() => setOpenJobId(null), [])
  const closeClip = useCallback(() => {
    sessionStorage.removeItem(CLIP_KEY)
    setClip(null)
  }, [])

  const onAuthenticated = ({ token, user: u }, isNew) => {
    setToken(token)
    setUser(u)
    if (isNew) go('profile', 'resume')
    else go('dashboard')
  }

  if (user === undefined) return <div className="center muted">Loading…</div>
  if (user === null) return <AuthPage onAuthenticated={onAuthenticated} pendingClip={!!clip} />

  const pageProps = { refreshKey, actions, onOpen: setOpenJobId, go, onFindJobs: findJobs, finding, lastSyncAt }

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand"><span className="logo">JB</span> Job Board</div>
        <nav className="nav">
          {NAV.map((n) => (
            <button key={n.id} className={page === n.id ? 'nav-item active' : 'nav-item'} onClick={() => go(n.id)}
                    aria-current={page === n.id ? 'page' : undefined}>
              <Icon name={n.icon} size={19} /> <span>{n.label}</span>
            </button>
          ))}
        </nav>
        <div className="sidebar-user">
          <div className="avatar sm" aria-hidden>{user.name.split(/\s+/).map((w) => w[0]).slice(0, 2).join('').toUpperCase()}</div>
          <div className="who">
            <strong>{user.name}</strong>
            <span className="muted small">{user.email}</span>
          </div>
          <button className="icon-btn" onClick={logout} title="Log out" aria-label="Log out"><Icon name="logout" /></button>
        </div>
      </aside>

      <main className="main">
        {page === 'dashboard' && <Dashboard {...pageProps} />}
        {page === 'jobs' && <Jobs {...pageProps} />}
        {page === 'applications' && <Applications {...pageProps} />}
        {page === 'profile' && (
          <Profile section={profileSection} setSection={setProfileSection} actions={actions} notify={notify}
                   onChanged={refresh} go={go} onLoggedOut={logout}
                   onUserRenamed={(name) => setUser((u) => ({ ...u, name }))} />
        )}
      </main>

      {openJobId && (
        <JobDetail id={openJobId} onClose={closeJob} actions={actions} notify={notify} onChanged={refresh} />
      )}
      {clip && <ClipModal clip={clip} onClose={closeClip} onSaved={refresh} onError={error} />}

      {toast && (
        <div key={toast.id} className={`toast ${toast.kind}`} role="status">
          {toast.kind === 'error' ? '⚠️' : <Icon name="check" size={16} />} {toast.message}
        </div>
      )}
    </div>
  )
}
