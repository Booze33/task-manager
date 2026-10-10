# TaskFlow client

TaskFlow is a React, Vite, and TypeScript task/habit dashboard. It runs on an in-memory mock API by default, so the UI can be explored without starting the Spring service.

## Run locally

```bash
npm install
npm run dev
```

Open the Vite URL (usually `http://localhost:5173`). The demo accepts any valid email and a password with at least six characters. The register tab also accepts a display name. Mock data resets when the page is refreshed.

## API mode

All data access goes through `src/api/service.ts`. Set `USE_MOCKS` to `false` in `src/config.ts` to use the Axios client, then add a `.env` file:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

The real API is expected to provide the authentication, user, stats, cache invalidation, and paginated task endpoints described in the TaskFlow brief. Rate-limit/cache/idempotency headers are read from responses; the mock API includes these signals and simulates 200–600 ms latency.

## What to explore

- Create, filter, paginate, and complete tasks; the mock starts with 47 tasks across three habits.
- Switch on **Flaky Retry** before completing a task to send the idempotent completion request twice.
- Re-fetch stats and invalidate the stats cache to see the cache-aside state.
- Watch the API budget and open **System Design** to inspect request method, endpoint, status, latency, cache status, remaining quota, and deduplication signals.
- Use the theme control to switch between dark and light mode.
