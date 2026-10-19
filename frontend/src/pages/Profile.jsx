import { useEffect, useRef, useState } from 'react'
import { api, timeAgo } from '../api'
import Icon from '../components/Icon'
import Resume from '../components/Resume'
import { bookmarkletHref } from '../components/bookmarklet'
import { PageLoading } from './Dashboard'

const SECTIONS = [
  { id: 'personal', label: 'Personal details' },
  { id: 'career', label: 'Career' },
  { id: 'preferences', label: 'Job preferences' },
  { id: 'resume', label: 'Resume & skills' },
  { id: 'clipper', label: 'Save from job sites' },
  { id: 'account', label: 'Account & security' },
]

const COUNTRIES = [
  ['in', 'India'], ['us', 'United States'], ['gb', 'United Kingdom'], ['ca', 'Canada'], ['au', 'Australia'],
  ['de', 'Germany'], ['nl', 'Netherlands'], ['sg', 'Singapore'], ['ae', 'UAE'], ['fr', 'France'], ['nz', 'New Zealand'],
]

export default function Profile({ section, setSection, actions, notify, onChanged, go, onUserRenamed, onLoggedOut }) {
  const [profile, setProfile] = useState(null)
  const [saved, setSaved] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api.getProfile().then((p) => { setProfile(p); setSaved(p) }).catch(actions.error)
  }, [actions.error])

  if (!profile) return <PageLoading />

  const dirty = JSON.stringify(profile) !== JSON.stringify(saved)
  const set = (field) => (e) => {
    const t = e.target
    let v = t.type === 'checkbox' ? t.checked : t.value
    if (t.type === 'number') v = t.value === '' ? null : Number(t.value)
    setProfile({ ...profile, [field]: v })
  }

  const save = async () => {
    setBusy(true)
    try {
      const p = await api.saveProfile(profile)
      setProfile(p)
      setSaved(p)
      onUserRenamed(p.name)
      notify('Profile saved. Your job matches were updated.')
      onChanged()
    } catch (e) { actions.error(e) } finally { setBusy(false) }
  }

  const field = (name, label, props = {}) => (
    <label className="field">
      <span>{label}{props.required && <em className="req"> *</em>}</span>
      {props.textarea
        ? <textarea rows={props.rows || 4} value={profile[name] ?? ''} onChange={set(name)} placeholder={props.placeholder} />
        : <input type={props.type || 'text'} value={profile[name] ?? ''} onChange={set(name)} placeholder={props.placeholder}
                 required={props.required} min={props.min} max={props.max} />}
      {props.help && <small className="help">{props.help}</small>}
    </label>
  )

  const completion = [profile.phone, profile.currentCity, profile.linkedinUrl, profile.headline, profile.experienceYears,
    profile.desiredRole, profile.location].filter((v) => v !== null && v !== undefined && v !== '').length

  return (
    <div className="page">
      <header className="page-head profile-head">
        <div className="avatar" aria-hidden>{initials(profile.name)}</div>
        <div>
          <h1>{profile.name}</h1>
          <p className="muted">
            {profile.headline || 'Add a headline in Career'}{profile.currentCity ? ` · ${profile.currentCity}` : ''}
          </p>
          <p className="muted small">{profile.email} · member since {new Date(profile.memberSince).toLocaleDateString()}</p>
        </div>
        <div className="profile-progress">
          <div className="progress"><div style={{ width: `${(completion / 7) * 100}%` }} /></div>
          <span className="small muted">Profile {Math.round((completion / 7) * 100)}% complete</span>
        </div>
      </header>

      <div className="profile-layout">
        <nav className="section-nav" aria-label="Profile sections">
          {SECTIONS.map((s) => (
            <button key={s.id} className={section === s.id ? 'active' : ''} onClick={() => setSection(s.id)}>{s.label}</button>
          ))}
        </nav>

        <div className="section-body">
          {section === 'personal' && (
            <Card title="Personal details" desc="How recruiters can reach you. Only you can see this.">
              <div className="grid-2">
                {field('name', 'Full name', { required: true })}
                <label className="field"><span>Email</span><input value={profile.email} disabled /><small className="help">Used to log in</small></label>
                {field('phone', 'Phone number', { type: 'tel', placeholder: '+91 98765 43210' })}
                {field('currentCity', 'Current city', { placeholder: 'Bangalore' })}
              </div>
              <div className="grid-3">
                {field('linkedinUrl', 'LinkedIn profile', { type: 'url', placeholder: 'https://linkedin.com/in/…' })}
                {field('githubUrl', 'GitHub', { type: 'url', placeholder: 'https://github.com/…' })}
                {field('portfolioUrl', 'Portfolio / website', { type: 'url', placeholder: 'https://…' })}
              </div>
              {field('about', 'About me', { textarea: true, placeholder: 'A short summary about you and what you are looking for' })}
            </Card>
          )}

          {section === 'career' && (
            <Card title="Career" desc="Your current situation. Years of experience is used to rank jobs at your level.">
              <div className="grid-2">
                {field('headline', 'Headline / current title', { placeholder: 'Java Full Stack Developer' })}
                {field('currentCompany', 'Current company', { placeholder: 'Leave empty if not working' })}
                {field('experienceYears', 'Years of experience', {
                  type: 'number', min: 0, max: 60, placeholder: '2',
                  help: 'Senior/lead roles rank lower if you have under 4 years; intern/fresher roles if you have 3+.',
                })}
                {field('noticePeriod', 'Notice period', { placeholder: 'Immediate / 30 days / 60 days' })}
                {field('expectedSalary', 'Expected salary', { placeholder: '₹10 LPA' })}
              </div>
            </Card>
          )}

          {section === 'preferences' && (
            <Card title="Job preferences" desc="What jobs should we look for? Changes re-score your jobs instantly.">
              <div className="grid-2">
                {field('desiredRole', 'Job title you want', {
                  placeholder: 'Java Developer', help: 'Your main search. Jobs with this title get a boost.',
                })}
                {field('location', 'Preferred job location', { placeholder: 'Bangalore', help: 'Jobs in this city get a boost.' })}
                <label className="field">
                  <span>Country</span>
                  <select value={profile.country} onChange={set('country')}>
                    {COUNTRIES.map(([code, name]) => <option key={code} value={code}>{name}</option>)}
                  </select>
                  <small className="help">Used for Google Jobs and Adzuna searches</small>
                </label>
                <label className="field">
                  <span>Minimum match to keep a job: <strong>{profile.minScore}%</strong></span>
                  <input type="range" min={0} max={90} step={5} value={profile.minScore}
                         onChange={(e) => setProfile({ ...profile, minScore: Number(e.target.value) })} />
                  <small className="help">Lower = more jobs, higher = only close matches</small>
                </label>
              </div>
              <label className="toggle big">
                <input type="checkbox" checked={profile.remoteOnly} onChange={set('remoteOnly')} />
                <span className="toggle-ui" /> Only show remote jobs
              </label>
              {field('keywords', 'Extra search keywords', {
                placeholder: 'java, spring boot, react',
                help: 'Comma separated. Filled in from your resume automatically. The first ones are used as searches.',
              })}
              {field('excludeKeywords', 'Hide jobs with these words in the title', { placeholder: 'php, sales, manager' })}
            </Card>
          )}

          {section === 'resume' && (
            <Resume onError={actions.error} notify={notify} onChanged={onChanged} onShowJobs={() => go('jobs')} />
          )}

          {section === 'clipper' && <ClipperSetup />}

          {section === 'account' && <Account actions={actions} notify={notify} profile={profile} onLoggedOut={onLoggedOut} />}

          {['personal', 'career', 'preferences'].includes(section) && (
            <div className={dirty ? 'save-bar visible' : 'save-bar'}>
              <span>{dirty ? 'You have unsaved changes' : 'All changes saved'}</span>
              <div className="actions">
                {dirty && <button className="btn ghost" onClick={() => setProfile(saved)} disabled={busy}>Discard</button>}
                <button className="btn primary" onClick={save} disabled={!dirty || busy || !profile.name?.trim()}>
                  {busy ? 'Saving…' : 'Save changes'}
                </button>
              </div>
            </div>
          )}
          {section === 'preferences' && saved.lastSyncAt && (
            <p className="muted small">Last job search: {timeAgo(saved.lastSyncAt)}</p>
          )}
        </div>
      </div>
    </div>
  )
}

function Card({ title, desc, children }) {
  return (
    <section className="card form-card">
      <div className="form-card-head">
        <h2>{title}</h2>
        {desc && <p className="muted small">{desc}</p>}
      </div>
      {children}
    </section>
  )
}

function ClipperSetup() {
  const link = useRef(null)
  useEffect(() => {
    // React warns about javascript: URLs in JSX, so the bookmarklet is set directly on the element.
    link.current?.setAttribute('href', bookmarkletHref(window.location.origin))
  }, [])
  return (
    <Card title="Save jobs from LinkedIn, Naukri, Indeed & Wellfound"
          desc="Found a job while browsing? Save it to your board in one click. It's matched against your resume and tracked like any other job.">
      <ol className="howto">
        <li>
          <strong>Drag this button to your browser's bookmarks bar</strong>
          <div className="bookmarklet-row">
            {/* eslint-disable-next-line jsx-a11y/anchor-is-valid */}
            <a ref={link} className="bookmarklet" onClick={(e) => { e.preventDefault(); alert('Drag this button to your bookmarks bar, then click it on a job page.') }}>
              <Icon name="bookmark" size={15} /> Save to Job Board
            </a>
            <span className="muted small">Bookmarks bar hidden? Press <kbd>Ctrl/⌘</kbd> + <kbd>Shift</kbd> + <kbd>B</kbd></span>
          </div>
        </li>
        <li><strong>Open a job</strong> on LinkedIn, Naukri, Indeed, Wellfound or a company careers page.</li>
        <li><strong>Click “Save to Job Board”</strong> in your bookmarks. A tab opens with the job filled in. Click Save.</li>
      </ol>
      <p className="muted small">
        Closed postings are still removed automatically: when the page says it's no longer accepting applications,
        or its deadline passes.
      </p>
    </Card>
  )
}

function Account({ actions, notify, onLoggedOut }) {
  const [pw, setPw] = useState({ current: '', next: '', confirm: '' })
  const [deletePw, setDeletePw] = useState('')
  const [busy, setBusy] = useState('')
  const set = (f) => (e) => setPw({ ...pw, [f]: e.target.value })

  const change = async (e) => {
    e.preventDefault()
    if (pw.next !== pw.confirm) { actions.error(new Error('New passwords do not match')); return }
    setBusy('pw')
    try {
      await api.changePassword(pw.current, pw.next)
      setPw({ current: '', next: '', confirm: '' })
      notify('Password changed')
    } catch (err) { actions.error(err) } finally { setBusy('') }
  }

  const remove = async (e) => {
    e.preventDefault()
    if (!confirm('Delete your account, resume, jobs and applications forever? This cannot be undone.')) return
    setBusy('del')
    try {
      await api.deleteAccount(deletePw)
      onLoggedOut()
    } catch (err) { actions.error(err); setBusy('') }
  }

  return (
    <>
      <Card title="Change password">
        <form className="stack" onSubmit={change}>
          <label className="field"><span>Current password</span>
            <input type="password" autoComplete="current-password" required value={pw.current} onChange={set('current')} /></label>
          <div className="grid-2">
            <label className="field"><span>New password</span>
              <input type="password" autoComplete="new-password" minLength={8} required value={pw.next} onChange={set('next')} />
              <small className="help">At least 8 characters</small></label>
            <label className="field"><span>Repeat new password</span>
              <input type="password" autoComplete="new-password" required value={pw.confirm} onChange={set('confirm')} /></label>
          </div>
          <div><button className="btn primary" disabled={busy === 'pw'}>{busy === 'pw' ? 'Saving…' : 'Change password'}</button></div>
        </form>
      </Card>
      <section className="card form-card danger-zone">
        <div className="form-card-head">
          <h2>Delete account</h2>
          <p className="muted small">Removes your profile, resume, jobs and applications permanently.</p>
        </div>
        <form className="row" onSubmit={remove}>
          <input type="password" placeholder="Enter your password to confirm" required value={deletePw}
                 onChange={(e) => setDeletePw(e.target.value)} />
          <button className="btn danger" disabled={busy === 'del'}><Icon name="trash" size={15} /> Delete my account</button>
        </form>
      </section>
    </>
  )
}

function initials(name) {
  return (name || '?').split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0].toUpperCase()).join('')
}
