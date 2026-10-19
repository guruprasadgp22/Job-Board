import { useEffect, useState } from 'react'
import { api, STATUS_LABELS, STATUSES, timeAgo } from '../api'
import { Deadline, SourceBadge } from '../components/JobCard'
import { PageLoading } from './Dashboard'

const COLUMNS = [
  { status: 'SAVED', hint: 'Jobs you want to apply to' },
  { status: 'APPLIED', hint: 'Waiting to hear back' },
  { status: 'INTERVIEWING', hint: 'In the interview process' },
  { status: 'OFFER', hint: 'You got an offer 🎉' },
  { status: 'REJECTED', hint: 'Not this time' },
]

/** Board view of your applications: one column per stage; move a job with the dropdown on its card. */
export default function Applications({ refreshKey, actions, onOpen, go }) {
  const [jobs, setJobs] = useState(null)

  useEffect(() => {
    api.listJobs({ status: COLUMNS.map((c) => c.status), sort: 'newest', size: 100 })
      .then((d) => setJobs(d.items))
      .catch(actions.error)
  }, [refreshKey, actions.error])

  if (!jobs) return <PageLoading />

  const move = (job, status) => {
    setJobs(jobs.map((j) => (j.id === job.id ? { ...j, status } : j)))
    actions.update(job.id, { status }, `Moved to ${STATUS_LABELS[status]}`)
  }

  return (
    <div className="page wide">
      <header className="page-head">
        <div>
          <h1>My applications</h1>
          <p className="muted">Track every job from saved to offer. Change a job's stage with the dropdown on its card.</p>
        </div>
      </header>

      {jobs.length === 0 ? (
        <div className="card empty big-empty">
          <p><strong>Nothing here yet</strong></p>
          <p className="muted">Click <b>Save</b> or <b>Apply</b> on a job and it will appear on this board.</p>
          <button className="btn primary" onClick={() => go('jobs')}>Browse matched jobs</button>
        </div>
      ) : (
        <div className="board">
          {COLUMNS.map((col) => {
            const items = jobs.filter((j) => j.status === col.status)
            return (
              <section key={col.status} className={`column s-${col.status}`}>
                <div className="column-head">
                  <h2>{STATUS_LABELS[col.status]}</h2>
                  <span className="count">{items.length}</span>
                </div>
                <p className="column-hint">{col.hint}</p>
                <div className="column-body">
                  {items.map((job) => (
                    <div key={job.id} className="board-card" onClick={() => onOpen(job.id)}>
                      <strong className="board-title">{job.title}</strong>
                      <span className="muted small">{job.company || '—'}</span>
                      <div className="board-meta">
                        <SourceBadge source={job.source} />
                        <span className="muted small">
                          {job.appliedAt ? `Applied ${timeAgo(job.appliedAt)}` : `Saved ${timeAgo(job.firstSeenAt)}`}
                        </span>
                      </div>
                      <Deadline expiresAt={job.expiresAt} />
                      <select value={job.status} onClick={(e) => e.stopPropagation()}
                              onChange={(e) => move(job, e.target.value)} aria-label="Move to stage">
                        {STATUSES.filter((s) => s !== 'NEW').map((s) => <option key={s} value={s}>{STATUS_LABELS[s]}</option>)}
                      </select>
                    </div>
                  ))}
                  {items.length === 0 && <div className="column-empty">No jobs</div>}
                </div>
              </section>
            )
          })}
        </div>
      )}
    </div>
  )
}
