import { APPLICANT_LABELS, splitList, timeAgo } from '../../api'
import { MatchRing } from '../JobCard'

/** One applicant in a list. {@code showPosting} adds which job they applied for. */
export default function ApplicantRow({ applicant: a, onOpen, showPosting = false }) {
  const have = splitList(a.matchedSkills).length
  const total = have + splitList(a.missingSkills).length
  return (
    <button className="applicant-row" onClick={() => onOpen(a.id)}>
      <MatchRing value={a.matchScore} size={44} />
      <span className="applicant-main">
        <strong>{a.name}</strong>
        <span className="muted small">
          {[a.headline, a.experienceYears != null && `${a.experienceYears} yrs`, a.currentCity].filter(Boolean).join(' · ')}
        </span>
        <span className="muted small">
          {showPosting && <>{a.postingTitle} · </>}
          {total > 0 && <>{have} of {total} skills · </>}
          applied {timeAgo(a.appliedAt)}
        </span>
      </span>
      <span className={`status-badge s-${a.status}`}>{APPLICANT_LABELS[a.status]}</span>
    </button>
  )
}
