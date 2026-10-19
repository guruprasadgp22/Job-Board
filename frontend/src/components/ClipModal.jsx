import { useState } from 'react'
import { api, sourceLabel, splitList } from '../api'
import Icon from './Icon'
import { MatchRing } from './JobCard'

/** Shown when you arrive from the "Save to Job Board" bookmarklet with a job pre-filled. */
export default function ClipModal({ clip, onClose, onSaved, onError }) {
  const [job, setJob] = useState({
    url: clip.url || '',
    title: clip.title || '',
    company: clip.company || '',
    location: clip.location || '',
    remote: !!clip.remote,
    description: clip.description || '',
    expiresAt: clip.expiresAt || null,
  })
  const [saved, setSaved] = useState(null)
  const [busy, setBusy] = useState(false)
  const set = (f) => (e) => setJob({ ...job, [f]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  let site = ''
  try { site = new URL(job.url).hostname.replace(/^www\./, '') } catch { /* invalid url */ }

  const save = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      const expiresAt = job.expiresAt && !Number.isNaN(Date.parse(job.expiresAt)) ? new Date(job.expiresAt).toISOString() : null
      const result = await api.addJob({ ...job, expiresAt })
      setSaved(result)
      onSaved()
    } catch (err) { onError(err) } finally { setBusy(false) }
  }

  return (
    <div className="overlay center-overlay" onClick={onClose}>
      <div className="modal card" onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true">
        <button className="close" onClick={onClose} aria-label="Close"><Icon name="x" /></button>
        {saved ? (
          <div className="saved-state">
            <MatchRing value={saved.relevanceScore} size={64} />
            <h2>Saved to your board</h2>
            <p className="muted">
              {splitList(saved.matchedSkills).length > 0
                ? `You have ${splitList(saved.matchedSkills).length} of the ${splitList(saved.matchedSkills).length + splitList(saved.missingSkills).length} skills it mentions.`
                : 'You can find it under My applications → Saved.'}
            </p>
            <button className="btn primary" onClick={onClose}>Done</button>
          </div>
        ) : (
          <form onSubmit={save} className="stack">
            <div>
              <h2>Save this job</h2>
              <p className="muted small">From {sourceLabel(site.split('.').slice(-2, -1)[0]) || site}. Check the details and save.</p>
            </div>
            <label className="field"><span>Job title</span><input required value={job.title} onChange={set('title')} /></label>
            <div className="grid-2">
              <label className="field"><span>Company</span><input value={job.company} onChange={set('company')} /></label>
              <label className="field"><span>Location</span><input value={job.location} onChange={set('location')} /></label>
            </div>
            <label className="toggle">
              <input type="checkbox" checked={job.remote} onChange={set('remote')} /><span className="toggle-ui" /> Remote
            </label>
            <details>
              <summary className="muted small">Job description ({job.description.length} characters)</summary>
              <textarea rows={6} value={job.description} onChange={set('description')} />
            </details>
            <div className="actions">
              <button className="btn primary" disabled={busy || !job.title.trim()}>{busy ? 'Saving…' : 'Save job'}</button>
              <button type="button" className="btn ghost" onClick={onClose}>Cancel</button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
