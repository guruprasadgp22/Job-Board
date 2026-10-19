import { useEffect, useRef, useState } from 'react'
import { api, timeAgo } from '../api'

export default function Resume({ onError, notify, onChanged, onShowJobs }) {
  const [resume, setResume] = useState(null)
  const [skills, setSkills] = useState([])
  const [newSkill, setNewSkill] = useState('')
  const [pasteMode, setPasteMode] = useState(false)
  const [pasteText, setPasteText] = useState('')
  const [busy, setBusy] = useState('')
  const [dragging, setDragging] = useState(false)
  const [showText, setShowText] = useState(false)
  const [syncResult, setSyncResult] = useState(null)
  const fileInput = useRef(null)

  const apply = (r) => {
    setResume(r)
    setSkills(r.skills || [])
  }

  useEffect(() => {
    api.getResume().then(apply).catch(onError)
  }, [onError])

  const hasResume = !!resume?.text
  const skillsChanged = resume && skills.join('|') !== (resume.skills || []).join('|')

  const run = async (kind, fn) => {
    setBusy(kind)
    try { await fn() } catch (e) { onError(e) } finally { setBusy('') }
  }

  const upload = (file) => run('upload', async () => {
    if (!file) return
    const r = await api.uploadResume(file)
    apply(r)
    setPasteMode(false)
    notify(`Found ${r.skills.length} skills in your resume`)
    onChanged()
  })

  const savePasted = () => run('paste', async () => {
    const r = await api.saveResumeText(pasteText)
    apply(r)
    setPasteMode(false)
    setPasteText('')
    notify(`Found ${r.skills.length} skills in your resume`)
    onChanged()
  })

  const saveSkills = () => run('skills', async () => {
    apply(await api.saveSkills(skills))
    notify('Skills saved and jobs re-scored')
    onChanged()
  })

  const useAsKeywords = () => run('keywords', async () => {
    const prefs = await api.useSkillsAsKeywords(6)
    setResume({ ...resume, keywords: prefs.keywords })
    notify('Search keywords updated')
    onChanged()
  })

  const findJobs = () => run('sync', async () => {
    if (skillsChanged) apply(await api.saveSkills(skills))
    const r = await api.sync()
    setSyncResult(r)
    notify(`${r.added} new matching job${r.added === 1 ? '' : 's'} found`)
    onChanged()
  })

  const removeResume = () => run('delete', async () => {
    if (!confirm('Remove your resume and its skills?')) return
    await api.deleteResume()
    apply(await api.getResume())
    onChanged()
  })

  const addSkill = (e) => {
    e.preventDefault()
    const s = newSkill.trim()
    if (s && !skills.some((k) => k.toLowerCase() === s.toLowerCase())) setSkills([...skills, s])
    setNewSkill('')
  }

  const onDrop = (e) => {
    e.preventDefault()
    setDragging(false)
    upload(e.dataTransfer.files?.[0])
  }

  if (!resume) return <p className="muted">Loading…</p>

  return (
    <section className="resume">
      <div className="section-head">
        <h2>My resume</h2>
        {hasResume && (
          <span className="muted small">{resume.fileName} · updated {timeAgo(resume.updatedAt)}</span>
        )}
      </div>
      <p className="muted">
        Upload your resume and I'll pull out your skills, search for related jobs and score each job by how
        many of its required skills you have.
      </p>

      {!pasteMode ? (
        <div
          className={dragging ? 'dropzone card dragging' : 'dropzone card'}
          onDragOver={(e) => { e.preventDefault(); setDragging(true) }}
          onDragLeave={() => setDragging(false)}
          onDrop={onDrop}
        >
          <div className="drop-icon" aria-hidden>⇪</div>
          <p><strong>{hasResume ? 'Replace your resume' : 'Drop your resume here'}</strong></p>
          <p className="muted small">PDF, DOCX or TXT · up to 5 MB</p>
          <div className="actions center-actions">
            <button className="btn primary" disabled={!!busy} onClick={() => fileInput.current?.click()}>
              {busy === 'upload' ? 'Reading resume…' : 'Choose file'}
            </button>
            <button className="btn" disabled={!!busy} onClick={() => setPasteMode(true)}>Paste text instead</button>
          </div>
          <input ref={fileInput} type="file" hidden accept=".pdf,.docx,.txt,.md"
                 onChange={(e) => { upload(e.target.files?.[0]); e.target.value = '' }} />
        </div>
      ) : (
        <div className="card form">
          <label className="field">
            <span>Paste your resume</span>
            <textarea rows={12} autoFocus value={pasteText} onChange={(e) => setPasteText(e.target.value)}
                      placeholder="Copy everything from your resume and paste it here…" />
          </label>
          <div className="actions">
            <button className="btn primary" disabled={!pasteText.trim() || !!busy} onClick={savePasted}>
              {busy === 'paste' ? 'Reading…' : 'Extract skills'}
            </button>
            <button className="btn" onClick={() => setPasteMode(false)}>Cancel</button>
          </div>
        </div>
      )}

      {hasResume && (
        <>
          <div className="section-head"><h2>Your skills</h2><span className="muted">{skills.length}</span></div>
          <div className="card form">
            <p className="muted small" style={{ margin: 0 }}>
              Found in your resume, most mentioned first. Remove anything wrong and add what's missing.
              Jobs are scored against this list.
            </p>
            <div className="skill-chips">
              {skills.map((s) => (
                <span key={s} className="skill-chip">
                  {s}
                  <button aria-label={`Remove ${s}`} onClick={() => setSkills(skills.filter((k) => k !== s))}>×</button>
                </span>
              ))}
              {skills.length === 0 && <span className="muted">No skills found yet. Add some below.</span>}
            </div>
            <form className="row add-skill" onSubmit={addSkill}>
              <input value={newSkill} onChange={(e) => setNewSkill(e.target.value)} placeholder="Add a skill, e.g. Kafka" />
              <button className="btn" type="submit" disabled={!newSkill.trim()}>Add</button>
            </form>
            <div className="keywords-line">
              <span className="muted small">Search keywords: </span>
              <strong className="small">{resume.keywords || 'none'}</strong>
            </div>
            <div className="actions">
              {skillsChanged && (
                <button className="btn" disabled={!!busy} onClick={saveSkills}>
                  {busy === 'skills' ? 'Saving…' : 'Save skills'}
                </button>
              )}
              <button className="btn" disabled={!!busy || skills.length === 0} onClick={useAsKeywords}
                      title="Replace your search keywords with your top 6 skills">
                {busy === 'keywords' ? 'Updating…' : 'Use top skills as search keywords'}
              </button>
              <button className="btn primary" disabled={!!busy || skills.length === 0} onClick={findJobs}>
                {busy === 'sync' ? 'Searching job sites…' : 'Find matching jobs now'}
              </button>
            </div>
          </div>

          {syncResult && (
            <div className="card result">
              <strong>{syncResult.added} new matching job{syncResult.added === 1 ? '' : 's'} added</strong>
              <ul>{Object.entries(syncResult.sources).map(([s, r]) => <li key={s}><code>{s}</code> {r}</li>)}</ul>
              <button className="btn" onClick={onShowJobs}>See matched jobs →</button>
            </div>
          )}

          <div className="resume-footer">
            <button className="link" onClick={() => setShowText(!showText)}>
              {showText ? 'Hide resume text' : 'Show extracted resume text'}
            </button>
            <button className="link danger-link" onClick={removeResume} disabled={!!busy}>Remove resume</button>
          </div>
          {showText && <pre className="card resume-text">{resume.text}</pre>}
        </>
      )}
    </section>
  )
}
