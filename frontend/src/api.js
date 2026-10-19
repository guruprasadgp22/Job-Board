const TOKEN_KEY = 'jobboard.token'

export const getToken = () => {
  try { return localStorage.getItem(TOKEN_KEY) || '' } catch { return '' }
}

export const setToken = (value) => {
  try {
    if (value) localStorage.setItem(TOKEN_KEY, value)
    else localStorage.removeItem(TOKEN_KEY)
  } catch { /* private mode */ }
}

export class UnauthorizedError extends Error {}

async function request(method, path, body) {
  const headers = {}
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  const isForm = body instanceof FormData
  if (body !== undefined && !isForm) headers['Content-Type'] = 'application/json'
  const res = await fetch(`/api${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : isForm ? body : JSON.stringify(body),
  })
  if (res.status === 204) return null
  const data = await res.json().catch(() => null)
  // A 401 on login/register means wrong credentials, not an expired session.
  if (res.status === 401 && !path.startsWith('/auth/')) throw new UnauthorizedError(data?.error || 'Please log in')
  if (!res.ok) throw new Error(data?.error || `Request failed (${res.status})`)
  return data
}

const query = (params) => {
  const q = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (Array.isArray(v)) v.forEach((item) => q.append(k, item))
    else if (v !== '' && v !== null && v !== undefined) q.set(k, v)
  })
  return q.toString()
}

export const api = {
  authConfig: () => request('GET', '/auth/config'),
  register: (data) => request('POST', '/auth/register', data),
  login: (data) => request('POST', '/auth/login', data),
  me: () => request('GET', '/auth/me'),

  listJobs: (params) => request('GET', `/jobs?${query(params)}`),
  getJob: (id) => request('GET', `/jobs/${id}`),
  addJob: (job) => request('POST', '/jobs', job),
  updateJob: (id, patch) => request('PATCH', `/jobs/${id}`, patch),
  deleteJob: (id) => request('DELETE', `/jobs/${id}`),
  checkJob: (id) => request('POST', `/jobs/${id}/check`),

  getResume: () => request('GET', '/resume'),
  uploadResume: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request('POST', '/resume/upload', form)
  },
  saveResumeText: (text) => request('PUT', '/resume/text', { text }),
  saveSkills: (skills) => request('PUT', '/resume/skills', { skills }),
  useSkillsAsKeywords: (count = 6) => request('POST', `/resume/use-as-keywords?count=${count}`),
  deleteResume: () => request('DELETE', '/resume'),

  dashboard: () => request('GET', '/dashboard'),
  getProfile: () => request('GET', '/profile'),
  saveProfile: (profile) => request('PUT', '/profile', profile),
  changePassword: (currentPassword, newPassword) => request('PUT', '/profile/password', { currentPassword, newPassword }),
  deleteAccount: (password) => request('POST', '/profile/delete', { password }),
  sync: () => request('POST', '/sync'),
  cleanup: () => request('POST', '/cleanup'),
  stats: () => request('GET', '/stats'),

  // Job seekers: jobs employers posted on this site
  browsePostings: (params) => request('GET', `/postings?${query(params)}`),
  getPosting: (id) => request('GET', `/postings/${id}`),
  applyToPosting: (id, coverNote) => request('POST', `/postings/${id}/apply`, { coverNote }),
  withdrawApplication: (id) => request('DELETE', `/postings/${id}/apply`),
  myApplications: () => request('GET', '/applications'),

  // Employers
  employerDashboard: () => request('GET', '/employer/dashboard'),
  myPostings: () => request('GET', '/employer/postings'),
  getMyPosting: (id) => request('GET', `/employer/postings/${id}`),
  createPosting: (posting) => request('POST', '/employer/postings', posting),
  updatePosting: (id, posting) => request('PUT', `/employer/postings/${id}`, posting),
  closePosting: (id) => request('POST', `/employer/postings/${id}/close`),
  reopenPosting: (id) => request('POST', `/employer/postings/${id}/reopen`),
  deletePosting: (id) => request('DELETE', `/employer/postings/${id}`),
  postingApplicants: (id) => request('GET', `/employer/postings/${id}/applicants`),
  allApplicants: () => request('GET', '/employer/applicants'),
  getApplicant: (id) => request('GET', `/employer/applicants/${id}`),
  updateApplicant: (id, patch) => request('PATCH', `/employer/applicants/${id}`, patch),
}

export const APPLICANT_STATUSES = ['APPLIED', 'SHORTLISTED', 'INTERVIEWING', 'OFFER', 'REJECTED']

export const APPLICANT_LABELS = {
  APPLIED: 'New',
  SHORTLISTED: 'Shortlisted',
  INTERVIEWING: 'Interviewing',
  OFFER: 'Offer',
  REJECTED: 'Rejected',
}

/** What a job seeker sees for each stage the employer sets. */
export const SEEKER_APPLICATION_LABELS = {
  APPLIED: 'Applied',
  SHORTLISTED: 'Shortlisted',
  INTERVIEWING: 'Interviewing',
  OFFER: 'Offer',
  REJECTED: 'Not selected',
}

export const EMPLOYMENT_TYPES = ['Full-time', 'Part-time', 'Contract', 'Internship']

export function experienceLabel(min, max) {
  if (min == null && max == null) return ''
  if (min != null && max != null) return min === max ? `${min} yrs` : `${min}–${max} yrs`
  return min != null ? `${min}+ yrs` : `Up to ${max} yrs`
}

export const STATUSES = ['NEW', 'SAVED', 'APPLIED', 'INTERVIEWING', 'OFFER', 'REJECTED']

export const STATUS_LABELS = {
  NEW: 'New',
  SAVED: 'Saved',
  APPLIED: 'Applied',
  INTERVIEWING: 'Interviewing',
  OFFER: 'Offer',
  REJECTED: 'Rejected',
}

/** Friendly names for where a job came from. */
export const SOURCE_LABELS = {
  linkedin: 'LinkedIn', naukri: 'Naukri', indeed: 'Indeed', wellfound: 'Wellfound', glassdoor: 'Glassdoor',
  foundit: 'foundit', instahyre: 'Instahyre', ziprecruiter: 'ZipRecruiter', remotive: 'Remotive',
  remoteok: 'RemoteOK', arbeitnow: 'Arbeitnow', themuse: 'The Muse', adzuna: 'Adzuna', jsearch: 'Google Jobs',
}

export const sourceLabel = (s) => SOURCE_LABELS[s] || s

export function daysLeft(iso) {
  if (!iso) return null
  return Math.ceil((new Date(iso).getTime() - Date.now()) / 86400000)
}

export const splitList = (csv) => (csv ? csv.split(',').map((s) => s.trim()).filter(Boolean) : [])

export function timeAgo(iso) {
  if (!iso) return ''
  const seconds = (Date.now() - new Date(iso).getTime()) / 1000
  if (seconds < 3600) return `${Math.max(1, Math.round(seconds / 60))}m ago`
  if (seconds < 86400) return `${Math.round(seconds / 3600)}h ago`
  const days = Math.round(seconds / 86400)
  return days < 60 ? `${days}d ago` : new Date(iso).toLocaleDateString()
}
