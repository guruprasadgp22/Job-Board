# Job Board

A job board that matches jobs to **your resume**, with a personal dashboard for every user:

- **Dashboard:** setup checklist, stats, application pipeline, best new matches, jobs closing soon, and the skills employers want that you have or are missing.
- **Find jobs:** jobs ranked by match % with Apply / Save / Not interested on each card. After Apply it asks "Did you apply?" and tracks it.
- **My applications:** a board with Saved → Applied → Interviewing → Offer → Rejected columns.
- **Profile:** personal details, career (experience, notice period, salary), job preferences (desired role, location, remote), resume & skills, the "Save from job sites" button, password change and account deletion.

1. **Accounts:** register and log in (email + password, BCrypt-hashed, JWT tokens). Every user has their own jobs, resume, preferences and applications.
2. **Resume matching:** upload a PDF/DOCX/TXT resume or paste it. Your skills are extracted (Java, Spring Boot, React, …), used as search terms, and every job is scored by how many of **its** required skills you have, with ✓ matched and missing skills shown per job.
3. **Finds jobs** from free job APIs (Remotive, RemoteOK, Arbeitnow, The Muse), plus **LinkedIn, Indeed, Naukri and Glassdoor through JSearch** and Adzuna (both need a free API key), and stores the matches in **PostgreSQL**.
4. **Save from job sites:** a bookmarklet (Profile → Save from job sites) saves the job you're viewing on LinkedIn, Naukri, Indeed, **Wellfound** or any careers page in one click.
5. **Tracks applications**: New → Saved → Applied → Interviewing → Offer / Rejected, with notes.
6. **Deletes jobs once they stop accepting applications.** Each job's link is re-checked regularly. If the page returns 404/410 or says "no longer accepting applications", "this job has expired", "position has been filled" and so on, the job **and its application data** are deleted. Deleted jobs are remembered so the next sync doesn't add them again.

Stack: **Java 21 + Spring Boot 3.5 (Spring Security, JPA)**, **React 18 (Vite)**, **PostgreSQL**. One deployable app: Spring Boot serves the React build.

```
job-board/
├── backend/     Spring Boot API (port 8080)
├── frontend/    React UI (Vite, port 5173 in dev)
├── Dockerfile   builds both into one image
├── render.yaml  free hosting on Render
└── .github/workflows/refresh-jobs.yml   free cron that syncs + cleans every 6h
```

## Run locally

Prerequisites: Java 21+, Node 18+, PostgreSQL.

```bash
# 1. Create the database (once)
psql -U postgres -c "CREATE DATABASE jobboard"

# 2. Start the backend
cd backend
DB_PASSWORD=<your postgres password> JWT_SECRET=any-long-random-text ./mvnw spring-boot:run

# 3. Start the frontend (new terminal)
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 → **Create account** → **My resume** → upload your resume → **Find matching jobs now**.

Without `JWT_SECRET`, a random key is used and you get logged out whenever the backend restarts.

## LinkedIn, Naukri, Indeed and Wellfound

These sites have no public job API, and their terms forbid scraping (they also block bots), so the app doesn't scrape them. Instead:

| Site | How jobs get in |
|---|---|
| LinkedIn, Indeed, Naukri, Glassdoor | **JSearch API** (Google for Jobs) when `JSEARCH_API_KEY` is set. Each job is labelled with the site it came from. Get a key on RapidAPI ("JSearch") or at openwebninja.com. The free plan has a small monthly quota, so each search runs at most every 12h (`JSEARCH_MIN_HOURS`). |
| All four, incl. Wellfound | **"Save to Job Board" bookmarklet**: drag it from Profile → Save from job sites to your bookmarks bar, open any job, click it. It reads the page's title, company, location, description and application deadline. |

Jobs with a known deadline (from JSearch or the page's JobPosting data) are deleted once the deadline passes.

## How matching works

- **Skills** are found with a catalog of about 200 technologies and their spellings (`reactjs` → React, `k8s` → Kubernetes, `springboot` → Spring Boot). You can remove wrong ones and add your own on the resume page.
- **Search terms:** your desired role first, then your keywords (filled automatically with your top resume skills).
- **Score (0–100)** = average of
  - *keyword score*: keywords in the title count 3, in tags 2, in the description 1, and
  - *resume score*: share of the skills the job mentions that you have. Spring Boot also covers "Spring", and PostgreSQL covers "SQL". A job naming only 1–2 skills is counted out of at least 5, so it can't reach 100%.
  - +10 when the job is in your preferred location, +10 when the title contains your desired role.
  - Experience fit: senior/lead titles −20 if you have under 4 years; intern/fresher titles −15 if you have 3+.
- Jobs below your **minimum match %** (Settings, default 30) are not stored. Editing skills or settings re-scores stored jobs instantly.

## How cleanup decides a job is closed

| Rule | Applies to | Default |
|---|---|---|
| Job page returns 404/410 or contains a "closed" phrase | every job, including ones you applied to | 60 links checked per run, each at most every 12h |
| Source stopped listing it (Remotive only, since it returns complete search results) | New / Saved / Rejected. Applied jobs get a link check first | after 5 days |
| Too old and never acted on | New / Saved / Rejected (not manually pasted jobs) | 30 days |

Pages that block the checker (403, 429, timeouts) are **never** deleted. They're just retried later.

## Configuration (environment variables)

Never put real passwords or secrets in `application.properties`: it goes to GitHub. Pass them as environment variables (locally, or in the Render dashboard).

| Variable | Purpose | Default |
|---|---|---|
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/jobboard` |
| `DB_USERNAME` / `DB_PASSWORD` | DB credentials | `postgres` / `postgres` |
| `JWT_SECRET` | Signs login tokens. Use a long random string. | random per restart |
| `JWT_DAYS` | How long a login lasts | 30 |
| `ALLOW_REGISTRATION` | `false` blocks new sign-ups (e.g. after you've made your account) | `true` |
| `CRON_SECRET` | Secret for `POST /api/refresh` (GitHub Actions) | empty = disabled |
| `JSEARCH_API_KEY` / `JSEARCH_URL` | LinkedIn, Indeed, Naukri, Glassdoor jobs via JSearch. URL defaults to RapidAPI | off |
| `JSEARCH_MIN_HOURS` / `JSEARCH_PAGES` | Quota protection: hours between identical searches / pages per search | 12 / 1 |
| `ADZUNA_APP_ID` / `ADZUNA_APP_KEY` | Free keys from https://developer.adzuna.com. Adds on-site jobs in India and 15+ other countries | off |
| `SYNC_CRON` / `CLEANUP_CRON` | Spring cron for the in-app schedule | every 6h |
| `CLEANUP_MAX_AGE_DAYS` / `CLEANUP_UNLISTED_DAYS` / `CLEANUP_LINK_CHECKS` | Cleanup tuning | 30 / 5 / 60 |

> 💡 The free no-key sources are mostly remote/Europe/US jobs. **For jobs in India, get the free Adzuna key** (2 minutes, no card) and set Country = India and your city in Settings.

## Free hosting (Render + Neon), about 15 minutes

Free plans change, so check the providers' current limits.

### 1. Database: Neon (free Postgres)
1. Sign up at https://neon.tech → create a project.
2. From the connection string `postgresql://USER:PASSWORD@HOST/DBNAME?sslmode=require`, build:
   - `DB_URL` = `jdbc:postgresql://HOST/DBNAME?sslmode=require`
   - `DB_USERNAME` = `USER`
   - `DB_PASSWORD` = `PASSWORD`

### 2. Push the code to GitHub
```bash
cd job-board
git init && git add . && git commit -m "Job board"
git branch -M main
git remote add origin https://github.com/<you>/job-board.git
git push -u origin main
```

### 3. App: Render (free web service)
1. Sign up at https://render.com with GitHub → **New → Blueprint** → pick the repo (it reads `render.yaml`).
2. Fill in `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (and the Adzuna keys if you have them). `JWT_SECRET` and `CRON_SECRET` are generated for you.
3. Deploy. The first build takes about 5–8 minutes. You'll get a URL like `https://job-board-xxxx.onrender.com`.
4. Open it and create your account. If you don't want strangers signing up, set `ALLOW_REGISTRATION` to `false` afterwards.

### 4. Keep jobs fresh: GitHub Actions (free)
Free Render instances **sleep after 15 minutes idle**, so the built-in schedule only runs while the app is awake.
The included workflow wakes the app every 6 hours and runs sync (for every user) + cleanup:

GitHub repo → **Settings → Secrets and variables → Actions → New repository secret**:
- `APP_URL` = `https://job-board-xxxx.onrender.com` (no trailing slash)
- `CRON_SECRET` = copy the value from Render → your service → Environment

Then **Actions → Refresh jobs → Run workflow** to test it.

> The first page load after the app has slept takes about 30–60 seconds while it wakes up. That's normal on the free plan.

## API

All endpoints except register/login need `Authorization: Bearer <token>`.

| Method | Path | |
|---|---|---|
| POST | `/api/auth/register` | `{name, email, password}` → `{token, user}` |
| POST | `/api/auth/login` | `{email, password}` → `{token, user}` |
| GET | `/api/auth/me` | current user |
| GET | `/api/dashboard` | your personal dashboard data |
| GET/PUT | `/api/profile` | personal, career and job-preference fields |
| PUT | `/api/profile/password` | `{currentPassword, newPassword}` |
| POST | `/api/profile/delete` | `{password}`: delete account and all data |
| GET | `/api/resume` | your resume text + skills |
| POST | `/api/resume/upload` | multipart `file` (PDF/DOCX/TXT) |
| PUT | `/api/resume/text` | `{text}`: pasted resume |
| PUT | `/api/resume/skills` | `{skills: [...]}`: edit skills (re-scores jobs) |
| POST | `/api/resume/use-as-keywords?count=6` | top skills → search keywords |
| GET | `/api/jobs?q=&status=&source=&remote=&minScore=&sort=score\|newest\|company&page=&size=` | list/search your jobs |
| GET/PATCH/DELETE | `/api/jobs/{id}` | details / `{status, notes}` / delete job + application |
| POST | `/api/jobs` | save a job (used by the bookmarklet) |
| POST | `/api/jobs/{id}/check` | check now if still open (deletes if closed) |
| GET/PUT | `/api/preferences` | keywords, exclude words, location, country, remote-only, min score |
| POST | `/api/sync` · `/api/cleanup` | find jobs for you / remove closed jobs |
| POST | `/api/refresh` | header `X-Cron-Secret`: sync all users + cleanup |
| GET | `/api/stats` | your counts per status and source |
