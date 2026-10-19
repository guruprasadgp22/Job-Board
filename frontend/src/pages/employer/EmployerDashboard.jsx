import { useEffect, useState } from 'react'
import { api, APPLICANT_LABELS, timeAgo } from '../../api'
import Icon from '../../components/Icon'
import { Deadline } from '../../components/JobCard'
import ApplicantRow from '../../components/employer/ApplicantRow'
import { Empty, PageLoading } from '../Dashboard'

const PIPELINE = ['APPLIED', 'SHORTLISTED', 'INTERVIEWING', 'OFFER']

/** The employer's home: their jobs, applicant pipeline and newest applicants. */
export default function EmployerDashboard({ refreshKey, actions, go, onOpenApplicant, onOpenPosting, onPostJob }) {
  const [data, setData] = useState(null)

  useEffect(() => {
    api.employerDashboard().then(setData).catch(actions.error)
  }, [refreshKey, actions.error])

  if (!data) return <PageLoading />

  const firstName = data.name.split(' ')[0]
  const total = data.openPostings + data.closedPostings

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>Hello, {firstName} 👋</h1>
          <p className="muted">Hiring for <strong>{data.organization}</strong></p>
        </div>
        <button className="btn primary" onClick={onPostJob}><Icon name="sparkle" size={16} /> Post a job</button>
      </header>

      {total === 0 && (
        <section className="card setup">
          <h2>Get your first applicants</h2>
          <ol className="steps">
            <li><span className="step-dot" /><span className="step-label">Post a job with the skills you need</span>
              <button className="link" onClick={onPostJob}>Do it <Icon name="arrow" size={14} /></button></li>
            <li><span className="step-dot" /><span className="step-label">Job seekers see it under “Company jobs” and apply with their resume</span></li>
            <li><span className="step-dot" /><span className="step-label">Review applicants, best skill match first, and move them to interview</span></li>
          </ol>
        </section>
      )}

      <section className="stat-grid">
        <Stat label="Open jobs" value={data.openPostings} hint={`${data.closedPostings} closed`} onClick={() => go('postings')} />
        <Stat label="Applicants" value={data.totalApplicants} hint={`${data.applicantsThisWeek} this week`} onClick={() => go('applicants')} />
        <Stat label="To review" value={data.byStatus.APPLIED || 0} hint="new applications" onClick={() => go('applicants')} />
        <Stat label="Interviewing" value={data.byStatus.INTERVIEWING || 0}
              hint={`${data.byStatus.OFFER || 0} offer${data.byStatus.OFFER === 1 ? '' : 's'}`} onClick={() => go('applicants')} />
      </section>

      <section className="card pipeline">
        <div className="card-head">
          <h2>Hiring pipeline</h2>
          <button className="link" onClick={() => go('applicants')}>All applicants <Icon name="arrow" size={14} /></button>
        </div>
        <div className="pipeline-steps">
          {PIPELINE.map((s, i) => (
            <div key={s} className={`pipe-step s-${s}`}>
              <div className="pipe-count">{data.byStatus[s] || 0}</div>
              <div className="pipe-label">{APPLICANT_LABELS[s]}</div>
              {i < PIPELINE.length - 1 && <Icon name="arrow" size={16} className="pipe-arrow" />}
            </div>
          ))}
        </div>
      </section>

      <div className="dash-grid">
        <section className="card">
          <div className="card-head">
            <h2>Newest applicants</h2>
            <button className="link" onClick={() => go('applicants')}>See all <Icon name="arrow" size={14} /></button>
          </div>
          {data.recentApplicants.length === 0 ? (
            <Empty text={total === 0 ? 'Post a job to start receiving applications.' : 'No applications yet.'} />
          ) : (
            <div className="applicant-list">
              {data.recentApplicants.map((a) => <ApplicantRow key={a.id} applicant={a} onOpen={onOpenApplicant} showPosting />)}
            </div>
          )}
        </section>

        <div className="side-col">
          <section className="card">
            <div className="card-head">
              <h2>Your open jobs</h2>
              <button className="link" onClick={() => go('postings')}>Manage <Icon name="arrow" size={14} /></button>
            </div>
            {data.topPostings.length === 0 ? <Empty text="No open jobs." /> : (
              <ul className="mini-list">
                {data.topPostings.map((p) => (
                  <li key={p.id}>
                    <button className="mini-job" onClick={() => onOpenPosting(p.id)}>
                      <strong>{p.title}</strong>
                      <span className="muted small">
                        {p.applicantCount} applicant{p.applicantCount === 1 ? '' : 's'}
                        {p.newApplicantCount > 0 && ` · ${p.newApplicantCount} new`} · posted {timeAgo(p.createdAt)}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </section>

          {data.closingSoon.length > 0 && (
            <section className="card">
              <div className="card-head"><h2>Closing soon</h2></div>
              <ul className="mini-list">
                {data.closingSoon.map((p) => (
                  <li key={p.id}>
                    <button className="mini-job" onClick={() => onOpenPosting(p.id)}>
                      <strong>{p.title}</strong>
                      <Deadline expiresAt={p.deadline} />
                    </button>
                  </li>
                ))}
              </ul>
            </section>
          )}
        </div>
      </div>
    </div>
  )
}

function Stat({ label, value, hint, onClick }) {
  return (
    <button className="card stat clickable" onClick={onClick}>
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
      {hint && <div className="stat-hint">{hint}</div>}
    </button>
  )
}
