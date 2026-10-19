import { useEffect, useState } from 'react'
import { api } from '../api'
import Icon from '../components/Icon'
import PostingCard from '../components/PostingCard'
import { Empty, PageLoading } from './Dashboard'

/** Jobs employers posted on this site. Job seekers apply here in one click. */
export default function CompanyJobs({ refreshKey, actions, onOpenPosting, go }) {
  const [q, setQ] = useState('')
  const [debouncedQ, setDebouncedQ] = useState('')
  const [remoteOnly, setRemoteOnly] = useState(false)
  const [sort, setSort] = useState('match')
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const t = setTimeout(() => setDebouncedQ(q), 300)
    return () => clearTimeout(t)
  }, [q])

  useEffect(() => { setPage(0) }, [debouncedQ, remoteOnly, sort])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    api.browsePostings({ q: debouncedQ, remote: remoteOnly ? true : '', sort, page, size: 15 })
      .then((d) => { if (!cancelled) setData(d) })
      .catch(actions.error)
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [debouncedQ, remoteOnly, sort, page, refreshKey, actions.error])

  if (!data && loading) return <PageLoading />

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>Company jobs</h1>
          <p className="muted">Jobs posted directly by employers on Job Board. Apply in one click with your resume.</p>
        </div>
      </header>

      <div className="toolbar">
        <div className="search-box">
          <Icon name="search" size={16} />
          <input placeholder="Search job title, company, city or skill" value={q} onChange={(e) => setQ(e.target.value)} />
          {q && <button className="icon-btn" onClick={() => setQ('')} aria-label="Clear search"><Icon name="x" size={14} /></button>}
        </div>
        <label className="toggle">
          <input type="checkbox" checked={remoteOnly} onChange={(e) => setRemoteOnly(e.target.checked)} />
          <span className="toggle-ui" /> Remote only
        </label>
        <select value={sort} onChange={(e) => setSort(e.target.value)} aria-label="Sort by">
          <option value="match">Best match first</option>
          <option value="newest">Newest first</option>
        </select>
      </div>

      {data && data.items.length === 0 ? (
        <div className="card">
          {debouncedQ || remoteOnly
            ? <Empty text="No jobs match these filters." />
            : <Empty text="No employer has posted a job yet. Meanwhile, see jobs from other sites under Find jobs."
                     action={{ label: 'Find jobs', onClick: () => go('jobs') }} />}
        </div>
      ) : (
        <div className={loading ? 'list loading' : 'list'}>
          {data?.items.map((p) => <PostingCard key={p.id} posting={p} onOpen={onOpenPosting} />)}
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
