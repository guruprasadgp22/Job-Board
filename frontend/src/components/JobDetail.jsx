import { useEffect, useState } from 'react'
import { api, splitList, STATUSES, STATUS_LABELS, timeAgo } from '../api'
import Icon from './Icon'
import { Deadline, MatchRing, matchWord, SourceBadge } from './JobCard'

const STAGE_HELP = {
  NEW: 'You haven’t acted on this job yet',
  SAVED: 'Saved for later',
  APPLIED: 'You applied. Waiting to hear back',
  INTERVIEWING: 'You are interviewing',
  OFFER: 'You received an offer',
  REJECTED: 'Not this time',
}

export default function JobDetail({ id, onClose, actions, notify, onChanged }) {
  const [job, setJob] = useState(null)
  const [notes, setNotes] = useState('')
  const [busy, setBusy] = useState(false)
  const [askApplied, setAskApplied] = useState(false)
  const [showAll, setShowAll] = useState(false)

  useEffect(() => {
    api.getJob(id)
      .then((j) => { setJob(j); setNotes(j.notes || '') })
      .catch((e) => { actions.error(e); onClose() })
  }, [id, actions, onClose])

  useEffect(() => {
    const onKey = (e) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => { window.removeEventListener('keydown', onKey); document.body.style.overflow = '' }
  }, [onClose])

  const update = async (patch, message) => {
    setBusy(true)
    try {
      setJob(await api.updateJob(id, patch))
      notify(message)
      onChanged()
    } catch (e) { actions.error(e) } finally { setBusy(false) }
  }

  const apply = () => {
    window.open(job.url, '_blank', 'noopener')
    if (job.status === 'NEW' || job.status === 'SAVED') setAskApplied(true)
  }

  const check = async () => {
    setBusy(true)
    try {
      const r = await api.checkJob(id)
      if (r.deleted) {
        notify('This job is no longer accepting applications, so it was removed.')
        onChanged()
        onClose()
        return
      }
      notify(r.result === 'OPEN' ? 'Good news: the job is still open.' : 'Could not check: the site blocked us. Open the posting to see.')
      setJob(await api.getJob(id))
    } catch (e) { actions.error(e) }
    setBusy(false)
  }

  const have = splitList(job?.matchedSkills)
  const lack = splitList(job?.missingSkills)
  const description = job?.description || ''
  const longDescription = description.length > 1500

  return (
    <div className="overlay" onClick={onClose}>
      <aside className="drawer" onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true" aria-label="Job details">
        <button className="close" onClick={onClose} aria-label="Close"><Icon name="x" /></button>
        {!job ? <div className="skeleton block" /> : (
          <>
            <div className="drawer-head">
              <MatchRing value={job.relevanceScore} size={64} />
              <div>
                <div className="eyebrow">{matchWord(job.relevanceScore)}</div>
                <h2>{job.title}</h2>
                <div className="job-facts">
                  {job.company && <span><Icon name="building" size={14} /> {job.company}</span>}
                  {job.location && <span><Icon name="pin" size={14} /> {job.location}</span>}
                  {job.remote && <span className="pill green">Remote</span>}
                  {job.salary && <span><Icon name="money" size={14} /> {job.salary}</span>}
                </div>
                <div className="job-meta">
                  <SourceBadge source={job.source} />
                  <span>{job.postedAt && !job.addedManually ? `Posted ${timeAgo(job.postedAt)}` : `Added ${timeAgo(job.firstSeenAt)}`}</span>
                  <Deadline expiresAt={job.expiresAt} />
                </div>
              </div>
            </div>

            {askApplied ? (
              <div className="ask-applied big">
                <span>Did you apply for this job?</span>
                <button className="btn primary" onClick={() => { setAskApplied(false); update({ status: 'APPLIED' }, 'Marked as applied. Good luck!') }}>
                  <Icon name="check" size={16} /> Yes, I applied
                </button>
                <button className="btn ghost" onClick={() => setAskApplied(false)}>Not yet</button>
              </div>
            ) : (
              <div className="actions">
                <button className="btn primary" onClick={apply}>Apply now <Icon name="external" size={15} /></button>
                {job.status === 'NEW' && (
                  <button className="btn" onClick={() => update({ status: 'SAVED' }, 'Saved to your applications')} disabled={busy}>
                    <Icon name="bookmark" size={15} /> Save for later
                  </button>
                )}
              </div>
            )}

            <section className="detail-section">
              <h3>Application stage</h3>
              <div className="stage-picker" role="radiogroup" aria-label="Application stage">
                {STATUSES.map((s) => (
                  <button key={s} role="radio" aria-checked={job.status === s} disabled={busy}
                          className={job.status === s ? `stage active s-${s}` : 'stage'}
                          onClick={() => job.status !== s && update({ status: s }, `Moved to ${STATUS_LABELS[s]}`)}>
                    {STATUS_LABELS[s]}
                  </button>
                ))}
              </div>
              <p className="muted small">{STAGE_HELP[job.status]}{job.appliedAt ? ` · applied ${timeAgo(job.appliedAt)}` : ''}</p>
            </section>

            {(have.length > 0 || lack.length > 0) && (
              <section className="detail-section">
                <h3>How your resume matches</h3>
                <p className="muted small">You have <strong>{have.length}</strong> of the {have.length + lack.length} skills this job mentions.</p>
                <div className="skill-list">
                  {have.map((s) => <span key={s} className="skill have">✓ {s}</span>)}
                  {lack.map((s) => <span key={s} className="skill lack" title="Not on your resume">{s}</span>)}
                </div>
              </section>
            )}

            <section className="detail-section">
              <h3>My notes</h3>
              <textarea rows={3} value={notes} onChange={(e) => setNotes(e.target.value)}
                        placeholder="Recruiter name, referral, interview date, salary discussed…" />
              {notes !== (job.notes || '') && (
                <button className="btn sm" disabled={busy} onClick={() => update({ notes }, 'Notes saved')}>Save notes</button>
              )}
            </section>

            <section className="detail-section">
              <h3>Job description</h3>
              {description ? (
                <>
                  <div className={showAll || !longDescription ? 'description' : 'description clipped'}>{description}</div>
                  {longDescription && (
                    <button className="link" onClick={() => setShowAll(!showAll)}>{showAll ? 'Show less' : 'Read full description'}</button>
                  )}
                </>
              ) : <p className="muted">No description available. Open the posting to read it.</p>}
            </section>

            <div className="drawer-foot">
              <button className="link" onClick={check} disabled={busy}>Check if still open</button>
              <button className="link danger-link" disabled={busy}
                      onClick={() => { actions.remove(id); onClose() }}>Not interested, remove</button>
            </div>
          </>
        )}
      </aside>
    </div>
  )
}
