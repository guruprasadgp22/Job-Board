import { useState } from 'react'
import { daysLeft, sourceLabel, splitList, STATUS_LABELS, timeAgo } from '../api'
import Icon from './Icon'

/** Match percentage shown as a ring, with a word so it is understandable at a glance. */
export function MatchRing({ value, size = 52 }) {
  const level = value >= 70 ? 'high' : value >= 45 ? 'mid' : 'low'
  const r = 20
  const c = 2 * Math.PI * r
  return (
    <div className={`match-ring ${level}`} style={{ width: size, height: size }}
         title={`${value}% match with your resume and preferences`}>
      <svg viewBox="0 0 48 48" width={size} height={size} aria-hidden="true">
        <circle cx="24" cy="24" r={r} className="ring-bg" />
        <circle cx="24" cy="24" r={r} className="ring-fg" strokeDasharray={`${(value / 100) * c} ${c}`} />
      </svg>
      <span>{value}%</span>
    </div>
  )
}

export function matchWord(value) {
  return value >= 70 ? 'Great match' : value >= 45 ? 'Good match' : 'Fair match'
}

export function StatusBadge({ status }) {
  if (!status || status === 'NEW') return null
  return <span className={`status-badge s-${status}`}>{STATUS_LABELS[status]}</span>
}

export function SourceBadge({ source }) {
  return <span className={`source-badge src-${source}`}>{sourceLabel(source)}</span>
}

export function Deadline({ expiresAt }) {
  const d = daysLeft(expiresAt)
  if (d === null || d > 14) return null
  return (
    <span className={d <= 3 ? 'deadline urgent' : 'deadline'}>
      <Icon name="clock" size={13} /> {d <= 0 ? 'Closes today' : `Closes in ${d} day${d === 1 ? '' : 's'}`}
    </span>
  )
}

/**
 * A job in a list. Actions sit right on the card:
 * Save · Apply (opens the posting, then asks "Did you apply?") · Not interested.
 */
export default function JobCard({ job, onOpen, onUpdate, onRemove, compact = false }) {
  const [askApplied, setAskApplied] = useState(false)
  const have = splitList(job.matchedSkills)
  const total = have.length + splitList(job.missingSkills).length

  const apply = (e) => {
    e.stopPropagation()
    window.open(job.url, '_blank', 'noopener')
    if (job.status === 'NEW' || job.status === 'SAVED') setAskApplied(true)
  }

  const stop = (fn) => (e) => { e.stopPropagation(); fn() }

  return (
    <article className={`job-card ${compact ? 'compact' : ''}`} onClick={() => onOpen(job.id)}
             tabIndex={0} onKeyDown={(e) => { if (e.key === 'Enter') onOpen(job.id) }}>
      <div className="job-card-main">
        <MatchRing value={job.relevanceScore} size={compact ? 44 : 52} />
        <div className="job-card-body">
          <div className="job-card-top">
            <h3 className="job-title">{job.title}</h3>
            <StatusBadge status={job.status} />
          </div>
          <div className="job-facts">
            {job.company && <span><Icon name="building" size={14} /> {job.company}</span>}
            {job.location && <span><Icon name="pin" size={14} /> {job.location}</span>}
            {job.remote && <span className="pill green">Remote</span>}
            {!compact && job.salary && <span><Icon name="money" size={14} /> {job.salary}</span>}
          </div>
          {total > 0 && (
            <div className="skill-line">
              <span className="skill-count">You have {have.length} of {total} skills</span>
              {!compact && have.slice(0, 4).map((s) => <span key={s} className="skill have">{s}</span>)}
            </div>
          )}
          <div className="job-meta">
            <SourceBadge source={job.source} />
            <span>{job.postedAt && !job.addedManually ? `Posted ${timeAgo(job.postedAt)}` : `Found ${timeAgo(job.firstSeenAt)}`}</span>
            <Deadline expiresAt={job.expiresAt} />
          </div>
        </div>
      </div>

      {!compact && onUpdate && (
        askApplied ? (
          <div className="ask-applied" onClick={(e) => e.stopPropagation()}>
            <span>Did you apply for this job?</span>
            <button className="btn primary sm" onClick={stop(() => { setAskApplied(false); onUpdate(job.id, { status: 'APPLIED' }, 'Marked as applied. Good luck!') })}>
              <Icon name="check" size={15} /> Yes, I applied
            </button>
            <button className="btn ghost sm" onClick={stop(() => setAskApplied(false))}>Not yet</button>
          </div>
        ) : (
          <div className="job-actions">
            <button className="btn primary sm" onClick={apply}>
              Apply <Icon name="external" size={14} />
            </button>
            {job.status === 'NEW' && (
              <button className="btn sm" onClick={stop(() => onUpdate(job.id, { status: 'SAVED' }, 'Saved to your applications'))}>
                <Icon name="bookmark" size={14} /> Save
              </button>
            )}
            <button className="btn ghost sm muted-btn" title="Hide this job. It won't be shown again."
                    onClick={stop(() => onRemove(job.id))}>
              <Icon name="x" size={14} /> Not interested
            </button>
          </div>
        )
      )}
    </article>
  )
}
