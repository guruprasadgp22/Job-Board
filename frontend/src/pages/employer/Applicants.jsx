import { useEffect, useState } from 'react'
import { api, APPLICANT_LABELS, APPLICANT_STATUSES } from '../../api'
import ApplicantRow from '../../components/employer/ApplicantRow'
import { Empty, PageLoading } from '../Dashboard'

/** Everyone who applied to any of the employer's jobs, filterable by stage and job. */
export default function Applicants({ refreshKey, actions, onOpenApplicant, go }) {
  const [all, setAll] = useState(null)
  const [status, setStatus] = useState('')
  const [postingId, setPostingId] = useState('')

  useEffect(() => {
    api.allApplicants().then(setAll).catch(actions.error)
  }, [refreshKey, actions.error])

  if (!all) return <PageLoading />

  const postings = [...new Map(all.map((a) => [a.postingId, a.postingTitle])).entries()]
  const counts = Object.fromEntries(APPLICANT_STATUSES.map((s) => [s, all.filter((a) => a.status === s).length]))
  const shown = all.filter((a) => (!status || a.status === status) && (!postingId || a.postingId === Number(postingId)))

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>Applicants</h1>
          <p className="muted">Everyone who applied to your jobs, newest first.</p>
        </div>
      </header>

      {all.length === 0 ? (
        <div className="card empty big-empty">
          <p><strong>No applicants yet</strong></p>
          <p className="muted">When job seekers apply to your jobs, they show up here.</p>
          <button className="btn primary" onClick={() => go('postings')}>Go to job posts</button>
        </div>
      ) : (
        <>
          <div className="toolbar">
            <div className="chips">
              <button className={status === '' ? 'chip active' : 'chip'} onClick={() => setStatus('')}>
                All <span className="chip-count">{all.length}</span>
              </button>
              {APPLICANT_STATUSES.map((s) => (
                <button key={s} className={status === s ? 'chip active' : 'chip'} onClick={() => setStatus(s)}>
                  {APPLICANT_LABELS[s]} <span className="chip-count">{counts[s]}</span>
                </button>
              ))}
            </div>
            {postings.length > 1 && (
              <select value={postingId} onChange={(e) => setPostingId(e.target.value)} aria-label="Filter by job">
                <option value="">All jobs</option>
                {postings.map(([id, title]) => <option key={id} value={id}>{title}</option>)}
              </select>
            )}
          </div>
          <section className="card">
            {shown.length === 0 ? <Empty text="No applicants match these filters." /> : (
              <div className="applicant-list">
                {shown.map((a) => <ApplicantRow key={a.id} applicant={a} onOpen={onOpenApplicant} showPosting />)}
              </div>
            )}
          </section>
        </>
      )}
    </div>
  )
}
