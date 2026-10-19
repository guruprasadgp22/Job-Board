import { useEffect, useState } from 'react'
import { api } from '../api'

export default function AuthPage({ onAuthenticated, pendingClip }) {
  const [mode, setMode] = useState('login')
  const [allowRegistration, setAllowRegistration] = useState(true)
  const [form, setForm] = useState({ name: '', email: '', password: '', confirm: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api.authConfig().then((c) => setAllowRegistration(c.allowRegistration)).catch(() => {})
  }, [])

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  const switchMode = (next) => {
    setMode(next)
    setError('')
  }

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    if (mode === 'register' && form.password !== form.confirm) {
      setError('Passwords do not match')
      return
    }
    setBusy(true)
    try {
      const res = mode === 'register'
        ? await api.register({ name: form.name, email: form.email, password: form.password })
        : await api.login({ email: form.email, password: form.password })
      onAuthenticated(res, mode === 'register')
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  const isRegister = mode === 'register'

  return (
    <div className="center auth-page">
      <div className="auth-wrap">
        <div className="auth-intro">
          <div className="brand big"><span className="logo" aria-hidden>JB</span> Job Board</div>
          <h2 className="tagline">Jobs that match your resume, in one place.</h2>
          <ul className="benefits">
            <li><span>1</span> Upload your resume and we find your skills</li>
            <li><span>2</span> We search job sites and rank jobs by how well you match</li>
            <li><span>3</span> Track every application, from saved to offer</li>
            <li><span>4</span> Closed jobs disappear automatically</li>
          </ul>
        </div>

        <form className="card auth-card" onSubmit={submit} noValidate={false}>
          {allowRegistration && (
            <div className="segmented" role="tablist">
              <button type="button" role="tab" aria-selected={!isRegister}
                      className={!isRegister ? 'active' : ''} onClick={() => switchMode('login')}>Log in</button>
              <button type="button" role="tab" aria-selected={isRegister}
                      className={isRegister ? 'active' : ''} onClick={() => switchMode('register')}>Create account</button>
            </div>
          )}

          <h1>{isRegister ? 'Create your account' : 'Welcome back'}</h1>
          {pendingClip && <p className="notice">Log in to save the job you just clipped.</p>}

          {isRegister && (
            <label className="field">
              <span>Full name</span>
              <input required autoComplete="name" value={form.name} onChange={set('name')} placeholder="Your name" />
            </label>
          )}
          <label className="field">
            <span>Email</span>
            <input type="email" required autoComplete="email" autoFocus value={form.email} onChange={set('email')}
                   placeholder="you@example.com" />
          </label>
          <label className="field">
            <span>Password</span>
            <input type="password" required minLength={isRegister ? 8 : undefined}
                   autoComplete={isRegister ? 'new-password' : 'current-password'}
                   value={form.password} onChange={set('password')}
                   placeholder={isRegister ? 'At least 8 characters' : ''} />
          </label>
          {isRegister && (
            <label className="field">
              <span>Confirm password</span>
              <input type="password" required autoComplete="new-password" value={form.confirm} onChange={set('confirm')} />
            </label>
          )}

          {error && <p className="error-text" role="alert">{error}</p>}

          <button className="btn primary full" type="submit" disabled={busy}>
            {busy ? 'Please wait…' : isRegister ? 'Create account' : 'Log in'}
          </button>

          {allowRegistration && (
            <p className="muted small center-text">
              {isRegister ? 'Already have an account? ' : "Don't have an account? "}
              <button type="button" className="link" onClick={() => switchMode(isRegister ? 'login' : 'register')}>
                {isRegister ? 'Log in' : 'Create one'}
              </button>
            </p>
          )}
        </form>
      </div>
    </div>
  )
}
