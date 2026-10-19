import { experienceLabel, SEEKER_APPLICATION_LABELS, splitList, timeAgo } from '../api'
import Icon from './Icon'
import { Deadline, MatchRing } from './JobCard'

/** Organization initials, shown instead of a match ring when the job lists no skills to match against. */
export function OrgMark({ name, size = 52 }) {
  const letters = (name || '?').split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0].toUpperCase()).join('')
  return <div className="org-mark" style={{ width: size, height: size }} aria-hidden>{letters}</div>
}

export function PostingFacts({ posting, full = false }) {
  const exp = experienceLabel(posting.minExperience, posting.maxExperience)
  return (
    <div className="job-facts">
      <span><Icon name="building" size={14} /> {posting.organization}</span>
      {posting.location && <span><Icon name="pin" size={14} /> {posting.location}</span>}
      {posting.remote && <span className="pill green">Remote</span>}
      {posting.employmentType && <span>{posting.employmentType}</span>}
      {exp && <span><Icon name="clock" size={14} /> {exp}</span>}
      {(full || posting.salary) && posting.salary && <span><Icon name="money" size={14} /> {posting.salary}</span>}
    </div>
  )
}

/** A job posted on this site, as a job seeker sees it in a list. */
export default function PostingCard({ posting, onOpen }) {
  const have = splitList(posting.matchedSkills)
  const total = have.length + splitList(posting.missingSkills).length
  return (
    <article className="job-card" onClick={() => onOpen(posting.id)} tabIndex={0}
             onKeyDown={(e) => { if (e.key === 'Enter') onOpen(posting.id) }}>
      <div className="job-card-main">
        {total > 0 ? <MatchRing value={posting.matchScore} /> : <OrgMark name={posting.organization} />}
        <div className="job-card-body">
          <div className="job-card-top">
            <h3 className="job-title">{posting.title}</h3>
            {posting.myStatus && (
              <span className={`status-badge s-${posting.myStatus}`}>{SEEKER_APPLICATION_LABELS[posting.myStatus]}</span>
            )}
          </div>
          <PostingFacts posting={posting} />
          {total > 0 && (
            <div className="skill-line">
              <span className="skill-count">You have {have.length} of {total} skills</span>
              {have.slice(0, 4).map((s) => <span key={s} className="skill have">{s}</span>)}
            </div>
          )}
          <div className="job-meta">
            <span className="source-badge src-here">Posted here</span>
            <span>Posted {timeAgo(posting.createdAt)}</span>
            <Deadline expiresAt={posting.deadline} />
          </div>
        </div>
      </div>
    </article>
  )
}
