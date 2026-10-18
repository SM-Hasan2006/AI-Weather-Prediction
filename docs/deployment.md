# Deployment guide (not executed – no hosting credentials were available)

Never claim deployment worked until you have opened each URL below and seen the correct response.

## 1. Managed MySQL
Create a MySQL 8 instance (e.g. a cloud provider's managed MySQL). Note host, port, database, user, password. Enforce TLS if offered and
change the JDBC URL to `useSSL=true` accordingly. Flyway creates the tables on the first backend start.

## 2. ML service (Python)
* Train first (`bash scripts/train_model.sh`) and make `ml-service/models/*.joblib` + `metadata.json` available to the host (build step or persistent disk; they are git-ignored).
* Start command: `uvicorn app.main:app --host 0.0.0.0 --port $PORT`; health check path `/health`.
* Environment: `ML_CORS_ALLOWED_ORIGINS` (backend origin), `ML_SERVICE_API_KEY` (long random string).

## 3. Spring Boot backend (Java 21)
* Build: `cd backend && mvn -DskipTests package`; run `java -jar target/ai-weather-backend-1.0.0.jar`; health check `/actuator/health`.
* Environment: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ALLOWED_ORIGINS` (your frontend https origin only), `ML_SERVICE_URL`, `ML_SERVICE_API_KEY` (same as above),
  `ADMIN_USERNAME`, `ADMIN_PASSWORD_BCRYPT`, `FORWARD_HEADERS_STRATEGY=native` (behind a proxy, so rate limiting sees real client IPs).
* Serve only over HTTPS (platform TLS); admin credentials travel in HTTP Basic headers and must never go over plain HTTP.

## 4. Frontend (Vercel / Netlify / any static host)
* Root `frontend/`, build `npm run build`, output `dist/`, env `VITE_API_BASE_URL=https://<backend-host>/api/v1`.
* Add a single-page-app rewrite of all paths to `/index.html` (React Router).

## 5. Verify
```bash
BACKEND_URL=https://<backend> ML_URL=https://<ml> FRONTEND_URL=https://<frontend> bash scripts/smoke_test.sh
```

## Free-tier cautions
Free web services often sleep after inactivity (first request can take 30–60 s, and the ML/Spring services may time out the first call),
have limited RAM (a JVM may need ≥512 MB), and free databases may expire or limit connections. Open-Meteo's free API is non-commercial with request limits;
commercial use needs a paid plan. Map tiles from openstreetmap.org are not meant for heavy traffic. Logging: Spring logs to stdout (collected by the host); credentials and tokens are never logged.
