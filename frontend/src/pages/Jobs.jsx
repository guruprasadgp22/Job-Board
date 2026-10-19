import { useEffect, useState } from 'react'
import { api, sourceLabel, timeAgo } from '../api'
import Icon from '../components/Icon'
import JobCard from '../components/JobCard'
import { Empty, PageLoading } from './Dashboard'

const VIEWS = [
  { id: 'new', label: 'New for you', status: ['NEW'] },
  { id: 'saved', label: 'Saved', status: ['SAVED'] },
  { id: 'all', label: 'All jobs', status: [] },
]

export default function Jobs({ refreshKey, actions, onOpen, go, onFindJobs, finding, lastSyncAt }) {
  const [view, setView] = useState('new')
  const [q, setQ] = useState('')
  const [debouncedQ, setDebouncedQ] = useState('')
  const [source, setSource] = useState('')
  const [remoteOnly, setRemoteOnly] = useState(false)
  const [sort, setSort] = useState('score')
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const t = setTimeout(() => setDebouncedQ(q), 300)
    return () => clearTimeout(t)
  }, [q])

  useEffect(() => { setPage(0) }, [view, debouncedQ, source, remoteOnly, sort])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    const status = VIEWS.find((v) => v.id === view).status
    api.listJobs({ q: debouncedQ, source, remote: remoteOnly ? true : '', sort, status, page, size: 15 })
      .then((d) => { if (!cancelled) setData(d) })
      .catch(actions.error)
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [view, debouncedQ, source, remoteOnly, sort, page, refreshKey, actions.error])

  useEffect(() => {
    api.stats().then(setStats).catch(() => {})
  }, [refreshKey])

  const sources = stats ? Object.keys(stats.bySource) : []
  const counts = stats?.byStatus || {}
  const filtersActive = debouncedQ || source || remoteOnly

  if (!data && loading) return <PageLoading />

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>Find jobs</h1>
          <p className="muted">
            Jobs matched to your resume, best match first.
            {lastSyncAt && <> Updated {timeAgo(lastSyncAt)}.</>}
          </p>
        </div>
        <button className="btn primary" onClick={onFindJobs} disabled={finding}>
          <Icon name="refresh" size={16} className={finding ? 'spin' : ''} />
          {finding ? 'Searching job sites…' : 'Find new jobs'}
        </button>
      </header>

      <div className="tabs-row" role="tablist">
        {VIEWS.map((v) => (
          <button key={v.id} role="tab" aria-selected={view === v.id}
                  className={view === v.id ? 'seg active' : 'seg'} onClick={() => setView(v.id)}>
            {v.label}
            {v.id === 'new' && counts.NEW > 0 && <span className="count">{counts.NEW}</span>}
            {v.id === 'saved' && counts.SAVED > 0 && <span className="count">{counts.SAVED}</span>}
          </button>
        ))}
      </div>

      <div className="toolbar">
        <div className="search-box">
          <Icon name="search" size={16} />
          <input placeholder="Search job title, company or city" value={q} onChange={(e) => setQ(e.target.value)} />
          {q && <button className="icon-btn" onClick={() => setQ('')} aria-label="Clear search"><Icon name="x" size={14} /></button>}
        </div>
        <label className="toggle">
          <input type="checkbox" checked={remoteOnly} onChange={(e) => setRemoteOnly(e.target.checked)} />
          <span className="toggle-ui" /> Remote only
        </label>
        <select value={sort} onChange={(e) => setSort(e.target.value)} aria-label="Sort by">
          <option value="score">Best match first</option>
          <option value="newest">Newest first</option>
          <option value="company">Company A–Z</option>
        </select>
      </div>

      {sources.length > 1 && (
        <div className="chips" aria-label="Filter by job site">
          <button className={source === '' ? 'chip active' : 'chip'} onClick={() => setSource('')}>All sites</button>
          {sources.map((s) => (
            <button key={s} className={source === s ? 'chip active' : 'chip'} onClick={() => setSource(s)}>
              {sourceLabel(s)} <span className="chip-count">{stats.bySource[s]}</span>
            </button>
          ))}
        </div>
      )}

      {data && data.items.length === 0 ? (
        <div className="card">
          {filtersActive ? (
            <Empty text="No jobs match these filters." />
          ) : view === 'saved' ? (
            <Empty text="You haven't saved any jobs yet. Click “Save” on a job to keep it here." />
          ) : (
            <Empty
              text={stats?.total === 0
                ? 'No jobs yet. Upload your resume and click “Find new jobs”. We search the job sites for you.'
                : 'You’ve gone through all new jobs. Click “Find new jobs” to look for more.'}
              action={stats?.total === 0 ? { label: 'Upload my resume', onClick: () => go('profile', 'resume') } : null}
            />
          )}
        </div>
      ) : (
        <div className={loading ? 'list loading' : 'list'}>
          {data?.items.map((job) => (
            <JobCard key={job.id} job={job} onOpen={onOpen} onUpdate={actions.update} onRemove={actions.remove} />
          ))}
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="pager">
          <button className="btn" disabled={page === 0} onClick={() => { setPage(page - 1); window.scrollTo(0, 0) }}>← Previous</button>
          <span className="muted">Page {page + 1} of {data.totalPages}</span>
          <button className="btn" disabled={page + 1 >= data.totalPages} onClick={() => { setPage(page + 1); window.scrollTo(0, 0) }}>Next →</button>
        </div>
      )}
    </div>
  )
}
