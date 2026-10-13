# TaskFlow client

TaskFlow is a React, Vite, and TypeScript task/habit dashboard. It connects directly to the backend API; there is no mock-data mode.

## Run locally

```bash
npm install
npm run dev
```

Copy `.env.example` to `.env` and set the API base URL before starting the app. The local default is `http://localhost:8005/api`. Open the Vite URL (usually `http://localhost:5173`).

## Backend API contract

The Spring application currently listens on port `8005`. Its controllers use the `/api` prefix, so the local API base URL is `http://localhost:8005/api`. Override it in a `.env` file when needed:

```env
VITE_API_BASE_URL=http://localhost:8005/api
```

The implemented backend endpoints and contracts are:

| Endpoint | Method | Request | Response |
|---|---|---|---|
| `/api/auth/signup` | POST | `{ name, email, password }` | `201 { accessToken, refreshToken, tokenType, expiresIn }` |
| `/api/auth/login` | POST | `{ email, password }` | `200 { accessToken, refreshToken, tokenType, expiresIn }` |
| `/api/auth/refresh` | POST | `{ refreshToken }` | `200 { accessToken, refreshToken, tokenType, expiresIn }` |
| `/api/auth/logout` | POST | Bearer access token; optional `{ refreshToken }` | `204` |
| `/api/auth/logout-all` | POST | Bearer access token | `204` |
| `/api/users/me` | GET | Bearer access token | `200 { id, name, email }` |
| `/api/users/greet` | GET | Bearer access token | `200` plain text |

Signup and login return access/refresh tokens; the frontend then requests `/api/users/me` to load the current user's profile. The client persists both tokens, validates saved sessions on startup, uses the refresh endpoint when the saved access token has expired, and revokes the session on logout.

Validation failures return HTTP `400` with a Spring Problem Detail body. Its `errors` property is an object keyed by field (for example, `{ "errors": { "email": "must be a well-formed email address" } }`), not the `{ errors: [{ field, message }] }` array described in the original brief. Duplicate signup email returns `409`; invalid credentials return `401`. Signup requires a name of 2–100 characters, a valid email up to 255 characters, and a password of 12–128 characters.

The CORS allowed origin is controlled by `CORS_ALLOWED_ORIGINS` and defaults to `*`. The backend exposes `X-Cache`, `X-Idempotent`, `X-RateLimit-Remaining`, and `X-RateLimit-Reset` to browser JavaScript.

## Task APIs

| Endpoint | Method | Request | Response |
|---|---|---|---|
| `/api/tasks?page=1&size=10&status=…&habit=…` | GET | Authenticated | `200 { items, total, page, size, totalPages }` |
| `/api/tasks` | POST | Authenticated; `{ title, description, habit, dueDate, priority }` | `201` with the created task |
| `/api/tasks/{id}/complete` | POST | Authenticated | `200` with the task; `X-Idempotent: true` on duplicate completion |
| `/api/users/me/stats` | GET | Authenticated | `200 { completedToday, totalTasks, completionRate, currentStreak, weekly }`; `X-Cache: HIT\|MISS` |
| `/api/users/me/stats/cache` | DELETE | Authenticated | `204` with no response body |

Task creation derives the owner from the authenticated user; clients must not send an owner ID. The title must be 3–200 characters, habit is required (up to 100 characters), `dueDate` is an ISO `YYYY-MM-DD` date, and priority is `low`, `medium`, or `high`. Description is optional and limited to 2,000 characters. Task responses use the client fields `id`, `title`, `description`, `status`, `habit`, `dueDate`, `createdAt`, and `priority`; dates are serialized as ISO strings.

Pagination is one-based. Blank or whitespace-only `status` and `habit` filters are ignored. Status values are exactly `todo`, `in-progress`, and `done`; an unknown non-empty status returns `400`. Habit is a free-form, case-sensitive exact-match filter, so an unknown habit returns an empty page rather than an error. `page` must be at least 1 and `size` must be between 1 and 100. Tasks are ordered newest-first with a stable ID tie-breaker. Completion is an atomic, owner-scoped state transition; duplicate completions return the task with `X-Idempotent: true`. Stats cache entries are invalidated after task creation and first completion, not after duplicate completions.

Stats use UTC dates. `completedToday` counts tasks completed today; `completionRate` is the rounded percentage of all the user's tasks in the `done` state; `currentStreak` counts consecutive days ending today, or yesterday when there has not yet been a completion today. `weekly` contains counts for the seven UTC dates ending today, using short English weekday labels. Stats are cached per user in Redis for up to 30 seconds. Task creation and first-time completion invalidate that user's cached entry; duplicate completion does not; the cache endpoint explicitly clears it.

The API enforces a fixed-window limit of 100 requests per 60 seconds per authenticated user, or per remote IP before authentication. Responses include `X-RateLimit-Remaining` and `X-RateLimit-Reset` (Unix epoch seconds), including `429` responses. Redis is required for rate limiting and stats caching; configure `REDIS_HOST` and `REDIS_PORT` (defaults: `localhost:6379`). If Redis is unavailable, API requests fail closed with `503` rather than bypassing the rate limit.

## API client

All requests go through `src/api/service.ts` and the Axios instance in `src/api/client.ts`. Response payloads are checked against the frontend DTO contract. Network failures and malformed responses are surfaced as errors rather than replaced with demo values.

## What to explore

- Create, filter, paginate, and complete tasks using backend data.
- Switch on **Flaky Retry** before completing a task to send the idempotent completion request twice.
- Re-fetch stats and invalidate the stats cache to see the cache-aside state.
- Watch the API budget and open **System Design** to inspect request method, endpoint, status, latency, cache status, remaining quota, and deduplication signals returned by the backend.
- Use the theme control to switch between dark and light mode.
