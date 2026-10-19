import { useEffect, useState } from 'react'
import { api, STATUS_LABELS, timeAgo } from '../api'
import Icon from '../components/Icon'
import JobCard, { Deadline } from '../components/JobCard'

const PIPELINE = ['SAVED', 'APPLIED', 'INTERVIEWING', 'OFFER']

export default function Dashboard({ refreshKey, actions, onOpen, go, onFindJobs, finding }) {
  const [data, setData] = useState(null)

  useEffect(() => {
    api.dashboard().then(setData).catch(actions.error)
  }, [refreshKey, actions.error])

  if (!data) return <PageLoading />

  const firstName = data.name.split(' ')[0]
  const todo = data.steps.filter((s) => !s.done)
  const applied = (data.byStatus.APPLIED || 0) + (data.byStatus.INTERVIEWING || 0) + (data.byStatus.OFFER || 0)
  const stepAction = {
    resume: () => go('profile', 'resume'),
    role: () => go('profile', 'preferences'),
    details: () => go('profile', 'preferences'),
    contact: () => go('profile', 'personal'),
    search: onFindJobs,
    apply: () => go('jobs'),
  }

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>{greeting()}, {firstName} 👋</h1>
          <p className="muted">
            {data.lastSyncAt
              ? `Your job matches were last updated ${timeAgo(data.lastSyncAt)}.`
              : 'Let’s find jobs that match your resume.'}
          </p>
        </div>
        <button className="btn primary" onClick={onFindJobs} disabled={finding}>
          <Icon name="refresh" size={16} className={finding ? 'spin' : ''} />
          {finding ? 'Searching job sites…' : 'Find new jobs'}
        </button>
      </header>

      {todo.length > 0 && (
        <section className="card setup">
          <div className="setup-head">
            <div>
              <h2>Finish setting up</h2>
              <p className="muted small">Each step makes your job matches better.</p>
            </div>
            <div className="progress-wrap" aria-label={`${data.profileCompletion}% complete`}>
              <div className="progress"><div style={{ width: `${data.profileCompletion}%` }} /></div>
              <span className="small muted">{data.profileCompletion}% done</span>
            </div>
          </div>
          <ol className="steps">
            {data.steps.map((s) => (
              <li key={s.key} className={s.done ? 'done' : ''}>
                <span className="step-dot">{s.done ? <Icon name="check" size={14} /> : ''}</span>
                <span className="step-label">{s.label}</span>
                {!s.done && stepAction[s.key] && (
                  <button className="link" onClick={stepAction[s.key]}>Do it <Icon name="arrow" size={14} /></button>
                )}
              </li>
            ))}
          </ol>
        </section>
      )}

      <section className="stat-grid">
        <Stat label="Matched jobs" value={data.totalJobs} hint={`${data.newThisWeek} new this week`} onClick={() => go('jobs')} />
        <Stat label="Average match" value={`${data.averageMatch}%`} hint="with your resume" />
        <Stat label="Applied" value={applied} hint="jobs in progress" onClick={() => go('applications')} />
        <Stat label="Interviews" value={data.byStatus.INTERVIEWING || 0} hint={`${data.byStatus.OFFER || 0} offer${data.byStatus.OFFER === 1 ? '' : 's'}`}
              onClick={() => go('applications')} />
      </section>

      <section className="card pipeline">
        <div className="card-head">
          <h2>Your application pipeline</h2>
          <button className="link" onClick={() => go('applications')}>Open board <Icon name="arrow" size={14} /></button>
        </div>
        <div className="pipeline-steps">
          {PIPELINE.map((s, i) => (
            <div key={s} className={`pipe-step s-${s}`}>
              <div className="pipe-count">{data.byStatus[s] || 0}</div>
              <div className="pipe-label">{STATUS_LABELS[s]}</div>
              {i < PIPELINE.length - 1 && <Icon name="arrow" size={16} className="pipe-arrow" />}
            </div>
          ))}
        </div>
      </section>

      <div className="dash-grid">
        <section className="card">
          <div className="card-head">
            <h2>Best new matches for you</h2>
            <button className="link" onClick={() => go('jobs')}>See all <Icon name="arrow" size={14} /></button>
          </div>
          {data.topMatches.length === 0 ? (
            <Empty
              text={data.hasResume ? 'No new matches yet. Click “Find new jobs”.' : 'Upload your resume to get matched jobs.'}
              action={data.hasResume ? null : { label: 'Upload resume', onClick: () => go('profile', 'resume') }}
            />
          ) : (
            <div className="list">
              {data.topMatches.map((j) => <JobCard key={j.id} job={j} onOpen={onOpen} compact />)}
            </div>
          )}
        </section>

        <div className="side-col">
          <section className="card">
            <div className="card-head"><h2>Skills employers want</h2></div>
            {data.skillsToLearn.length === 0 && data.yourTopSkills.length === 0 ? (
              <Empty text="Skill insights appear once you have matched jobs." />
            ) : (
              <>
                {data.yourTopSkills.length > 0 && (
                  <>
                    <p className="muted small">Your strongest skills (number of matching jobs asking for each)</p>
                    <SkillBars items={data.yourTopSkills} kind="have" />
                  </>
                )}
                {data.skillsToLearn.length > 0 && (
                  <>
                    <p className="muted small" style={{ marginTop: 14 }}>
                      Worth learning: asked for by your matches but missing from your resume
                    </p>
                    <SkillBars items={data.skillsToLearn} kind="lack" />
                  </>
                )}
              </>
            )}
          </section>

          {data.closingSoon.length > 0 && (
            <section className="card">
              <div className="card-head"><h2>Closing soon</h2></div>
              <ul className="mini-list">
                {data.closingSoon.map((j) => (
                  <li key={j.id}>
                    <button className="mini-job" onClick={() => onOpen(j.id)}>
                      <strong>{j.title}</strong>
                      <span className="muted small">{j.company}</span>
                      <Deadline expiresAt={j.expiresAt} />
                    </button>
                  </li>
                ))}
              </ul>
            </section>
          )}

          <section className="card">
            <div className="card-head"><h2>Recent applications</h2></div>
            {data.recentApplications.length === 0 ? (
              <Empty text="Jobs you apply to will show up here." />
            ) : (
              <ul className="mini-list">
                {data.recentApplications.map((j) => (
                  <li key={j.id}>
                    <button className="mini-job" onClick={() => onOpen(j.id)}>
                      <strong>{j.title}</strong>
                      <span className="muted small">{j.company} · {STATUS_LABELS[j.status]}{j.appliedAt ? ` · ${timeAgo(j.appliedAt)}` : ''}</span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      </div>
    </div>
  )
}

function greeting() {
  const h = new Date().getHours()
  return h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening'
}

function Stat({ label, value, hint, onClick }) {
  const Tag = onClick ? 'button' : 'div'
  return (
    <Tag className={`card stat ${onClick ? 'clickable' : ''}`} onClick={onClick}>
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
      {hint && <div className="stat-hint">{hint}</div>}
    </Tag>
  )
}

function SkillBars({ items, kind }) {
  const max = Math.max(...items.map((i) => i.jobs), 1)
  return (
    <ul className="skill-bars">
      {items.map((i) => (
        <li key={i.skill}>
          <span className="sb-name">{i.skill}</span>
          <span className="sb-track"><span className={`sb-fill ${kind}`} style={{ width: `${(i.jobs / max) * 100}%` }} /></span>
          <span className="sb-num">{i.jobs}</span>
        </li>
      ))}
    </ul>
  )
}

export function Empty({ text, action }) {
  return (
    <div className="empty">
      <p className="muted">{text}</p>
      {action && <button className="btn primary sm" onClick={action.onClick}>{action.label}</button>}
    </div>
  )
}

export function PageLoading() {
  return (
    <div className="page">
      <div className="skeleton title" />
      <div className="skeleton block" />
      <div className="skeleton block" />
    </div>
  )
}
