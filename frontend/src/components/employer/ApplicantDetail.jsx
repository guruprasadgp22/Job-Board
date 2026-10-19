import { useEffect, useState } from 'react'
import { api, APPLICANT_LABELS, APPLICANT_STATUSES, splitList, timeAgo } from '../../api'
import Icon from '../Icon'
import { MatchRing } from '../JobCard'

/** An applicant as the employer sees them: contact details, match, resume, stage and private notes. */
export default function ApplicantDetail({ id, onClose, notify, onError, onChanged }) {
  const [a, setA] = useState(null)
  const [notes, setNotes] = useState('')
  const [busy, setBusy] = useState(false)
  const [showResume, setShowResume] = useState(false)

  useEffect(() => {
    api.getApplicant(id).then((x) => { setA(x); setNotes(x.employerNotes || '') }).catch((e) => { onError(e); onClose() })
  }, [id, onError, onClose])

  useEffect(() => {
    const onKey = (e) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => { window.removeEventListener('keydown', onKey); document.body.style.overflow = '' }
  }, [onClose])

  const update = async (patch, message) => {
    setBusy(true)
    try {
      const x = await api.updateApplicant(id, patch)
      setA(x)
      notify(message)
      onChanged()
    } catch (e) { onError(e) } finally { setBusy(false) }
  }

  const have = splitList(a?.matchedSkills)
  const lack = splitList(a?.missingSkills)
  const links = a ? [['LinkedIn', a.linkedinUrl], ['GitHub', a.githubUrl], ['Portfolio', a.portfolioUrl]]
    .filter(([, url]) => url && /^https?:\/\//i.test(url)) : []

  return (
    <div className="overlay" onClick={onClose}>
      <aside className="drawer" onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true" aria-label="Applicant">
        <button className="close" onClick={onClose} aria-label="Close"><Icon name="x" /></button>
        {!a ? <div className="skeleton block" /> : (
          <>
            <div className="drawer-head">
              <MatchRing value={a.matchScore} size={64} />
              <div>
                <div className="eyebrow">Applied for {a.postingTitle}</div>
                <h2>{a.name}</h2>
                {a.headline && <p className="muted">{a.headline}</p>}
                <div className="job-facts">
                  {a.currentCompany && <span><Icon name="building" size={14} /> {a.currentCompany}</span>}
                  {a.currentCity && <span><Icon name="pin" size={14} /> {a.currentCity}</span>}
                  {a.experienceYears != null && <span><Icon name="clock" size={14} /> {a.experienceYears} yrs experience</span>}
                </div>
                <div className="job-meta"><span>Applied {timeAgo(a.appliedAt)}</span></div>
              </div>
            </div>

            <section className="detail-section">
              <h3>Contact</h3>
              <div className="contact-grid">
                {a.email && <a href={`mailto:${a.email}`}>{a.email}</a>}
                {a.phone && <a href={`tel:${a.phone.replace(/\s+/g, '')}`}>{a.phone}</a>}
                {links.map(([label, url]) => (
                  <a key={label} href={url} target="_blank" rel="noopener noreferrer">{label} <Icon name="external" size={13} /></a>
                ))}
              </div>
              {(a.noticePeriod || a.expectedSalary) && (
                <p className="muted small">
                  {[a.noticePeriod && `Notice period: ${a.noticePeriod}`, a.expectedSalary && `Expects: ${a.expectedSalary}`]
                    .filter(Boolean).join(' · ')}
                </p>
              )}
            </section>

            <section className="detail-section">
              <h3>Stage</h3>
              <div className="stage-picker" role="radiogroup" aria-label="Applicant stage">
                {APPLICANT_STATUSES.map((s) => (
                  <button key={s} role="radio" aria-checked={a.status === s} disabled={busy}
                          className={a.status === s ? `stage active s-${s}` : 'stage'}
                          onClick={() => a.status !== s && update({ status: s }, `Moved to ${APPLICANT_LABELS[s]}`)}>
                    {APPLICANT_LABELS[s]}
                  </button>
                ))}
              </div>
              <p className="muted small">The candidate sees this stage on their applications page.</p>
            </section>

            {(have.length > 0 || lack.length > 0) && (
              <section className="detail-section">
                <h3>Skill match</h3>
                <p className="muted small">Has <strong>{have.length}</strong> of the {have.length + lack.length} skills the job asks for.</p>
                <div className="skill-list">
                  {have.map((s) => <span key={s} className="skill have">✓ {s}</span>)}
                  {lack.map((s) => <span key={s} className="skill lack" title="Not on their resume">{s}</span>)}
                </div>
              </section>
            )}

            {a.coverNote && (
              <section className="detail-section">
                <h3>Message from the candidate</h3>
                <div className="description">{a.coverNote}</div>
              </section>
            )}

            <section className="detail-section">
              <h3>Private notes</h3>
              <textarea rows={3} value={notes} onChange={(e) => setNotes(e.target.value)}
                        placeholder="Interview feedback, next steps… Only your organization sees this." />
              {notes !== (a.employerNotes || '') && (
                <button className="btn sm" disabled={busy} onClick={() => update({ employerNotes: notes }, 'Notes saved')}>Save notes</button>
              )}
            </section>

            <section className="detail-section">
              <h3>Resume {a.resumeFileName && <span className="muted small">· {a.resumeFileName}</span>}</h3>
              {splitList(a.resumeSkills).length > 0 && (
                <div className="skill-list">{splitList(a.resumeSkills).map((s) => <span key={s} className="skill">{s}</span>)}</div>
              )}
              <button className="link" onClick={() => setShowResume(!showResume)}>
                {showResume ? 'Hide resume' : 'Read full resume'}
              </button>
              {showResume && <pre className="card resume-text">{a.resumeText}</pre>}
            </section>
          </>
        )}
      </aside>
    </div>
  )
}
