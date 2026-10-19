import { useEffect, useState } from 'react'
import { api, experienceLabel, timeAgo } from '../../api'
import Icon from '../../components/Icon'
import { Deadline } from '../../components/JobCard'
import ApplicantRow from '../../components/employer/ApplicantRow'
import PostingForm from '../../components/employer/PostingForm'
import { Empty, PageLoading } from '../Dashboard'

export function PostingState({ posting }) {
  if (posting.acceptingApplications) return <span className="pill green">Open</span>
  return <span className="pill">{posting.open ? 'Deadline passed' : 'Closed'}</span>
}

/** The employer's job posts: post, edit, close, delete, and see who applied to each. */
export default function JobPosts({ refreshKey, actions, notify, onChanged, onOpenApplicant, organization,
                                   selectedId, setSelectedId, startNew, setStartNew }) {
  const [postings, setPostings] = useState(null)
  const [applicants, setApplicants] = useState(null)
  const [editing, setEditing] = useState(null) // null = closed, {} = new, posting = edit

  useEffect(() => {
    api.myPostings().then(setPostings).catch(actions.error)
  }, [refreshKey, actions.error])

  useEffect(() => {
    if (!selectedId) { setApplicants(null); return }
    api.postingApplicants(selectedId).then(setApplicants).catch(actions.error)
  }, [selectedId, refreshKey, actions.error])

  // "Post a job" pressed on the dashboard
  useEffect(() => {
    if (startNew) { setEditing({}); setStartNew(false) }
  }, [startNew, setStartNew])

  if (!postings) return <PageLoading />

  const selected = postings.find((p) => p.id === selectedId)

  const run = async (fn, message) => {
    try { await fn(); notify(message); onChanged() } catch (e) { actions.error(e) }
  }
  const close = (p) => run(() => api.closePosting(p.id), 'Job closed. It no longer takes applications.')
  const reopen = (p) => run(() => api.reopenPosting(p.id), 'Job reopened')
  const remove = (p) => {
    if (!confirm(`Delete “${p.title}” and all ${p.applicantCount} application(s) to it? This cannot be undone.\n\nTip: Close the job instead to keep its applicants.`)) return
    if (selectedId === p.id) setSelectedId(null)
    run(() => api.deletePosting(p.id), 'Job post deleted')
  }

  const form = editing && (
    <PostingForm posting={editing.id ? editing : null} organization={organization} onError={actions.error}
                 onClose={() => setEditing(null)}
                 onSaved={(saved, wasEdit) => {
                   setEditing(null)
                   notify(wasEdit ? 'Job post updated' : 'Job posted! Job seekers can apply now.')
                   onChanged()
                   if (!wasEdit) setSelectedId(saved.id)
                 }} />
  )

  if (selected) {
    return (
      <div className="page">
        <button className="link back-link" onClick={() => setSelectedId(null)}>← All job posts</button>
        <header className="page-head">
          <div>
            <h1>{selected.title}</h1>
            <p className="muted">
              {[selected.location, selected.remote && 'Remote', selected.employmentType,
                experienceLabel(selected.minExperience, selected.maxExperience), selected.salary].filter(Boolean).join(' · ')}
            </p>
            <div className="job-meta">
              <PostingState posting={selected} />
              <span>Posted {timeAgo(selected.createdAt)}</span>
              <Deadline expiresAt={selected.deadline} />
            </div>
          </div>
          <div className="actions">
            <button className="btn" onClick={() => setEditing(selected)}>Edit</button>
            {selected.open
              ? <button className="btn" onClick={() => close(selected)}>Close job</button>
              : <button className="btn" onClick={() => reopen(selected)}>Reopen</button>}
          </div>
        </header>

        <section className="card">
          <div className="card-head">
            <h2>Applicants</h2>
            <span className="muted small">Best skill match first</span>
          </div>
          {!applicants ? <div className="skeleton block" /> : applicants.length === 0 ? (
            <Empty text={selected.acceptingApplications
              ? 'No one has applied yet. Job seekers see this job under Company jobs.'
              : 'No one applied to this job.'} />
          ) : (
            <div className="applicant-list">
              {applicants.map((a) => <ApplicantRow key={a.id} applicant={a} onOpen={onOpenApplicant} />)}
            </div>
          )}
        </section>
        {form}
      </div>
    )
  }

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>Job posts</h1>
          <p className="muted">Jobs you posted for {organization}. Click one to see who applied.</p>
        </div>
        <button className="btn primary" onClick={() => setEditing({})}><Icon name="sparkle" size={16} /> Post a job</button>
      </header>

      {postings.length === 0 ? (
        <div className="card empty big-empty">
          <p><strong>You haven't posted a job yet</strong></p>
          <p className="muted">Post your first job and job seekers can apply with their resume in one click.</p>
          <button className="btn primary" onClick={() => setEditing({})}>Post a job</button>
        </div>
      ) : (
        <div className="list">
          {postings.map((p) => (
            <article key={p.id} className="job-card posting-row" onClick={() => setSelectedId(p.id)} tabIndex={0}
                     onKeyDown={(e) => { if (e.key === 'Enter') setSelectedId(p.id) }}>
              <div className="job-card-main">
                <div className="applicant-count" title="Applicants">
                  <strong>{p.applicantCount}</strong><span>applied</span>
                </div>
                <div className="job-card-body">
                  <div className="job-card-top">
                    <h3 className="job-title">{p.title}</h3>
                    <PostingState posting={p} />
                  </div>
                  <div className="job-facts">
                    {p.location && <span><Icon name="pin" size={14} /> {p.location}</span>}
                    {p.remote && <span className="pill green">Remote</span>}
                    {p.employmentType && <span>{p.employmentType}</span>}
                    {experienceLabel(p.minExperience, p.maxExperience) && (
                      <span><Icon name="clock" size={14} /> {experienceLabel(p.minExperience, p.maxExperience)}</span>
                    )}
                  </div>
                  <div className="job-meta">
                    {p.newApplicantCount > 0 && <span className="pill blue">{p.newApplicantCount} new</span>}
                    <span>Posted {timeAgo(p.createdAt)}</span>
                    <Deadline expiresAt={p.deadline} />
                  </div>
                </div>
              </div>
              <div className="job-actions" onClick={(e) => e.stopPropagation()}>
                <button className="btn sm" onClick={() => setEditing(p)}>Edit</button>
                {p.open
                  ? <button className="btn sm" onClick={() => close(p)}>Close</button>
                  : <button className="btn sm" onClick={() => reopen(p)}>Reopen</button>}
                <button className="btn ghost sm muted-btn" onClick={() => remove(p)}><Icon name="trash" size={14} /> Delete</button>
              </div>
            </article>
          ))}
        </div>
      )}
      {form}
    </div>
  )
}
