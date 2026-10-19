import { useState } from 'react'
import { api, EMPLOYMENT_TYPES } from '../../api'
import Icon from '../Icon'

const EMPTY = {
  title: '', location: '', remote: false, employmentType: 'Full-time', minExperience: '', maxExperience: '',
  salary: '', skills: '', description: '', deadline: '',
}

/** "2026-11-30" → end of that day in the employer's time zone, as an ISO instant. */
const toInstant = (date) => (date ? new Date(`${date}T23:59:59`).toISOString() : null)
const toDateInput = (iso) => (iso ? new Date(iso).toLocaleDateString('en-CA') : '')
const toNumber = (v) => (v === '' || v === null || v === undefined ? null : Number(v))

/** Modal to post a new job or edit an existing one. */
export default function PostingForm({ posting, organization, onClose, onSaved, onError }) {
  const editing = !!posting
  const [form, setForm] = useState(() => (editing ? {
    ...EMPTY,
    ...Object.fromEntries(Object.entries(posting).map(([k, v]) => [k, v ?? ''])),
    remote: posting.remote,
    deadline: toDateInput(posting.deadline),
  } : EMPTY))
  const [busy, setBusy] = useState(false)
  const set = (f) => (e) => setForm({ ...form, [f]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  const save = async (e) => {
    e.preventDefault()
    setBusy(true)
    const body = {
      title: form.title,
      location: form.location,
      remote: form.remote,
      employmentType: form.employmentType,
      minExperience: toNumber(form.minExperience),
      maxExperience: toNumber(form.maxExperience),
      salary: form.salary,
      skills: form.skills,
      description: form.description,
      deadline: toInstant(form.deadline),
    }
    try {
      const saved = editing ? await api.updatePosting(posting.id, body) : await api.createPosting(body)
      onSaved(saved, editing)
    } catch (err) { onError(err); setBusy(false) }
  }

  return (
    <div className="overlay center-overlay" onClick={onClose}>
      <div className="modal card wide-modal" onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true">
        <button className="close" onClick={onClose} aria-label="Close"><Icon name="x" /></button>
        <form onSubmit={save} className="stack">
          <div>
            <h2>{editing ? 'Edit job post' : 'Post a job'}</h2>
            <p className="muted small">For <strong>{organization}</strong>. Job seekers apply here with their resume.</p>
          </div>
          <label className="field">
            <span>Job title<em className="req"> *</em></span>
            <input required maxLength={200} value={form.title} onChange={set('title')} placeholder="Java Backend Developer" />
          </label>
          <div className="grid-2">
            <label className="field">
              <span>Location</span>
              <input maxLength={200} value={form.location} onChange={set('location')} placeholder="Bangalore" />
            </label>
            <label className="field">
              <span>Employment type</span>
              <select value={form.employmentType} onChange={set('employmentType')}>
                {EMPLOYMENT_TYPES.map((t) => <option key={t}>{t}</option>)}
              </select>
            </label>
            <label className="field">
              <span>Experience (years)</span>
              <div className="row range-row">
                <input type="number" min={0} max={60} value={form.minExperience} onChange={set('minExperience')} placeholder="Min" aria-label="Minimum years" />
                <span className="muted">to</span>
                <input type="number" min={0} max={60} value={form.maxExperience} onChange={set('maxExperience')} placeholder="Max" aria-label="Maximum years" />
              </div>
            </label>
            <label className="field">
              <span>Salary</span>
              <input maxLength={100} value={form.salary} onChange={set('salary')} placeholder="₹8–12 LPA" />
            </label>
          </div>
          <label className="toggle">
            <input type="checkbox" checked={form.remote} onChange={set('remote')} /><span className="toggle-ui" /> Remote job
          </label>
          <label className="field">
            <span>Skills needed</span>
            <input maxLength={1000} value={form.skills} onChange={set('skills')} placeholder="Java, Spring Boot, PostgreSQL, Docker" />
            <small className="help">Comma separated. Applicants are scored by how many of these are on their resume.</small>
          </label>
          <label className="field">
            <span>Job description<em className="req"> *</em></span>
            <textarea required rows={9} maxLength={20000} value={form.description} onChange={set('description')}
                      placeholder="What the role involves, what you're looking for, benefits…" />
          </label>
          <label className="field">
            <span>Last day to apply</span>
            <input type="date" value={form.deadline} onChange={set('deadline')}
                   min={editing ? undefined : new Date().toLocaleDateString('en-CA')} />
            <small className="help">Optional. The job stops taking applications after this day.</small>
          </label>
          <div className="actions">
            <button className="btn primary" disabled={busy || !form.title.trim() || !form.description.trim()}>
              {busy ? 'Saving…' : editing ? 'Save changes' : 'Post job'}
            </button>
            <button type="button" className="btn ghost" onClick={onClose}>Cancel</button>
          </div>
        </form>
      </div>
    </div>
  )
}
