// Builds the "Save to Job Board" bookmarklet. When clicked on a job page (LinkedIn, Naukri, Indeed, Wellfound
// or any site that publishes schema.org JobPosting data), it reads the job you are looking at and opens your
// board with it pre-filled. It runs only when you click it, in your own browser, on the page you're viewing.
function clipper(APP) {
  const q = (sel) => document.querySelector(sel)
  const text = (sels) => {
    for (const s of sels) {
      const el = q(s)
      if (el && el.innerText.trim()) return el.innerText.trim()
    }
    return ''
  }
  let posting = {}
  for (const s of document.querySelectorAll('script[type="application/ld+json"]')) {
    try {
      const data = JSON.parse(s.textContent)
      for (const item of [].concat(data['@graph'] || data)) {
        if (item && [].concat(item['@type']).includes('JobPosting')) posting = item
      }
    } catch (e) { /* ignore broken JSON-LD */ }
  }
  const html = document.createElement('div')
  html.innerHTML = posting.description || ''
  const org = posting.hiringOrganization || {}
  const place = [].concat(posting.jobLocation || [])[0] || {}
  const addr = place.address || {}
  let url = location.href
  const li = url.match(/currentJobId=(\d+)/) || url.match(/linkedin\.com\/jobs\/view\/(?:[^/]*-)?(\d+)/)
  if (/linkedin\.com/.test(url) && li) url = 'https://www.linkedin.com/jobs/view/' + li[1] + '/'
  else {
    const c = q('link[rel="canonical"]')
    if (c && /^https?:/.test(c.href)) url = c.href
  }
  const country = addr.addressCountry && (addr.addressCountry.name || addr.addressCountry)
  const job = {
    url,
    title: posting.title || text([
      '.job-details-jobs-unified-top-card__job-title', '.top-card-layout__title',
      '[data-testid="jobsearch-JobInfoHeader-title"]', '[class*="jd-header-title"]', 'h1']) || document.title,
    company: org.name || text([
      '.job-details-jobs-unified-top-card__company-name', '.topcard__org-name-link',
      '[data-testid="inlineHeader-companyName"]', '[data-company-name]', '[class*="jd-header-comp-name"]',
      '[class*="companyName"]']),
    location: [addr.addressLocality, addr.addressRegion, country].filter(Boolean).join(', ') || text([
      '.job-details-jobs-unified-top-card__primary-description-container .tvm__text', '.topcard__flavor--bullet',
      '[data-testid="inlineHeader-companyLocation"]', '[data-testid="job-location"]', '[class*="location"]']),
    description: (html.innerText || text([
      '#job-details', '.jobs-description__content', '.description__text', '#jobDescriptionText',
      '[class*="job-desc"]', '[class*="description"]', 'main'])).slice(0, 6000),
    remote: posting.jobLocationType === 'TELECOMMUTE' || /\bremote\b/i.test(document.title),
    expiresAt: posting.validThrough || null,
  }
  window.open(APP + '/#save=' + encodeURIComponent(JSON.stringify(job)), '_blank')
}

export function bookmarkletHref(appOrigin) {
  const code = `(${clipper.toString()})(${JSON.stringify(appOrigin)})`
  return 'javascript:' + encodeURIComponent(code)
}
