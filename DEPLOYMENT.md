# Deploying IntelliRoute (free tier)

Three services, two hosts, $0/month:

| Service | Host | URL after deploy |
|---|---|---|
| `apps/web` (Next.js UI) | Vercel | `https://<project>.vercel.app` |
| `services/api` (Spring Boot) | Render (free) | `https://intelliroute-api-XXXX.onrender.com` |
| `services/ai-service` (FastAPI) | Render (free) | `https://intelliroute-ai-XXXX.onrender.com` |

## 1. Backend — Render Blueprint

1. Sign up / sign in at render.com with GitHub (free — if a credit card is
   ever required, stop; free tier should not require one).
2. **New → Blueprint** → connect the `Zemukiros/intelliroute` repo.
   Render reads `render.yaml` and shows both services → **Apply**.
3. Wait for both to build (the Java build takes several minutes the first time).
4. Open the **intelliroute-ai** service and copy its public URL.
5. Open **intelliroute-api → Environment** and set:
   - `INTELLIROUTE_RANKING_BASEURL` = the URL from step 4 (no trailing slash)
   - (CORS comes in step 3 below.) Save — the service redeploys.
6. Verify: `https://<intelliroute-api URL>/actuator/health` → `{"status":"UP"}`.

## 2. Frontend — Vercel

1. vercel.com/new → Import `Zemukiros/intelliroute`.
2. **Root Directory: `apps/web`** (click Edit next to the root directory).
   Framework auto-detects Next.js.
3. Add environment variable:
   - `NEXT_PUBLIC_API_BASE_URL` = the intelliroute-api URL (no trailing slash)
4. **Deploy** → note the live URL.

## 3. Connect CORS

1. Render → **intelliroute-api → Environment**:
   - `INTELLIROUTE_CORS_ALLOWEDORIGINS` = your Vercel URL, e.g.
     `https://intelliroute-xxxx.vercel.app`
     (comma-separate multiple origins if Vercel gives several domains).
2. Save → redeploys. Done.

## 4. Verify end-to-end

- Open the Vercel URL → the graph loads and routes compute.
- Ask for a recommendation with a preference → ranked results appear.
  - First request after idle may show the degraded java-local-fallback badge
    while the free-tier ranker wakes (~30-60s); retry a minute later and the
    ranking service responds normally. This is expected free-tier behavior —
    and a live demonstration of the API's designed fallback path.

## Free-tier behavior

Render free services sleep after ~15 minutes idle. The first visit wakes
them: the API takes ~30-60s (Java), the ranker similar. The web UI's
offline/degraded states cover this window honestly.
