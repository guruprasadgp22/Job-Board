import { useEffect, useState } from 'react'
import { api, SEEKER_APPLICATION_LABELS, splitList, timeAgo } from '../api'
import Icon from './Icon'
import { Deadline, MatchRing, matchWord } from './JobCard'
import { OrgMark, PostingFacts } from './PostingCard'

const STATUS_HELP = {
  APPLIED: 'Your application was sent. The employer hasn’t reviewed it yet.',
  SHORTLISTED: 'Good news: the employer shortlisted you.',
  INTERVIEWING: 'The employer wants to interview you. Watch your email and phone.',
  OFFER: 'You have an offer! The employer will contact you with details.',
  REJECTED: 'The employer went ahead with other candidates this time.',
}

/** A job posted on this site: details, how the seeker's resume matches, and Apply / Withdraw. */
export default function PostingDetail({ id, onClose, notify, onError, onChanged, go }) {
  const [posting, setPosting] = useState(null)
  const [applying, setApplying] = useState(false)
  const [coverNote, setCoverNote] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api.getPosting(id).then(setPosting).catch((e) => { onError(e); onClose() })
  }, [id, onError, onClose])

  useEffect(() => {
    const onKey = (e) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => { window.removeEventListener('keydown', onKey); document.body.style.overflow = '' }
  }, [onClose])

  const apply = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      await api.applyToPosting(id, coverNote)
      notify('Application sent! You can follow it under My applications.')
      setApplying(false)
      setPosting(await api.getPosting(id))
      onChanged()
    } catch (err) { onError(err) } finally { setBusy(false) }
  }

  const withdraw = async () => {
    if (!confirm('Withdraw your application? The employer will no longer see it.')) return
    setBusy(true)
    try {
      await api.withdrawApplication(id)
      notify('Application withdrawn')
      onChanged()
    } catch (err) { onError(err); setBusy(false); return }
    try {
      setPosting(await api.getPosting(id))
    } catch {
      onClose() // a closed job can't be viewed once you're no longer an applicant
    } finally { setBusy(false) }
  }

  const have = splitList(posting?.matchedSkills)
  const lack = splitList(posting?.missingSkills)
  const total = have.length + lack.length

  return (
    <div className="overlay" onClick={onClose}>
      <aside className="drawer" onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true" aria-label="Job details">
        <button className="close" onClick={onClose} aria-label="Close"><Icon name="x" /></button>
        {!posting ? <div className="skeleton block" /> : (
          <>
            <div className="drawer-head">
              {total > 0 ? <MatchRing value={posting.matchScore} size={64} /> : <OrgMark name={posting.organization} size={64} />}
              <div>
                {total > 0 && <div className="eyebrow">{matchWord(posting.matchScore)}</div>}
                <h2>{posting.title}</h2>
                <PostingFacts posting={posting} full />
                <div className="job-meta">
                  <span className="source-badge src-here">Posted here</span>
                  <span>Posted {timeAgo(posting.createdAt)}</span>
                  <Deadline expiresAt={posting.deadline} />
                </div>
              </div>
            </div>

            {posting.myStatus ? (
              <section className="detail-section">
                <h3>Your application</h3>
                <p>
                  <span className={`status-badge s-${posting.myStatus}`}>{SEEKER_APPLICATION_LABELS[posting.myStatus]}</span>
                </p>
                <p className="muted small">{STATUS_HELP[posting.myStatus]}</p>
                {posting.myStatus === 'APPLIED' && (
                  <button className="link danger-link" onClick={withdraw} disabled={busy}>Withdraw application</button>
                )}
              </section>
            ) : !posting.acceptingApplications ? (
              <p className="notice">This job is no longer accepting applications.</p>
            ) : applying ? (
              <form className="card form stack" onSubmit={apply}>
                <p className="muted small" style={{ margin: 0 }}>
                  Your profile and current resume are sent to <strong>{posting.organization}</strong>.
                </p>
                <label className="field">
                  <span>Message to the employer (optional)</span>
                  <textarea rows={4} maxLength={3000} value={coverNote} onChange={(e) => setCoverNote(e.target.value)}
                            placeholder="Why you're a good fit, when you can join…" />
                </label>
                <div className="actions">
                  <button className="btn primary" disabled={busy}>{busy ? 'Sending…' : 'Send application'}</button>
                  <button type="button" className="btn ghost" onClick={() => setApplying(false)}>Cancel</button>
                </div>
              </form>
            ) : (
              <div className="actions">
                <button className="btn primary" onClick={() => setApplying(true)}>Apply now</button>
              </div>
            )}

            {total > 0 && (
              <section className="detail-section">
                <h3>How your resume matches</h3>
                <p className="muted small">You have <strong>{have.length}</strong> of the {total} skills this job asks for.</p>
                <div className="skill-list">
                  {have.map((s) => <span key={s} className="skill have">✓ {s}</span>)}
                  {lack.map((s) => <span key={s} className="skill lack" title="Not on your resume">{s}</span>)}
                </div>
                {have.length === 0 && (
                  <button className="link" onClick={() => { onClose(); go('profile', 'resume') }}>
                    Upload or update your resume <Icon name="arrow" size={14} />
                  </button>
                )}
              </section>
            )}

            <section className="detail-section">
              <h3>About the job</h3>
              <div className="description">{posting.description}</div>
            </section>
          </>
        )}
      </aside>
    </div>
  )
}
