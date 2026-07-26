# AGENTS.md — Assessment Service

## What this repo is

Java/Spring Boot backend + React frontend for employee competency assessment via voice interviews in the browser. LLM generates questions and scores answers. Admins manage competencies/criteria/employees via web UI or REST API. Employees receive one-time invite links and answer questions using Chrome's SpeechRecognition API.

## Stack & versions

- **Backend**: Java 25, Spring Boot 4.1.0, Spring Security 7, Spring Data JPA, JOOQ, Spring AI 2.0.0
- **LLM providers**: Google Gemini 2.0 Flash (default) + Sber GigaChat (switchable)
- **Database**: PostgreSQL 18, migrations via Liquibase
- **Rate limiting**: Resilience4j
- **Build**: Gradle 8.14 (wrapper), Java toolchain 25
- **Frontend**: React 19, TypeScript 5.7, Vite 6, react-router-dom 7
- **Deploy**: Docker Compose

## Must-know commands

### Backend

```bash
# Run (requires PostgreSQL running locally or via Docker)
./gradlew bootRun

# Build jar
./gradlew bootJar

# Run tests (JUnit Platform; note: no tests currently exist in the repo)
./gradlew test
```

### Frontend

```bash
cd frontend
npm install
npm run dev      # dev server on http://localhost:3000, proxies /api → localhost:8080
npm run build    # tsc && vite build → dist/
npm run preview
```

### Database

```bash
# Start PostgreSQL via Docker
cp .env.example .env  # then edit GEMINI_API_KEY
docker compose up -d postgres
```

### Full Docker Compose

```bash
cp .env.example .env
# NOTE: backend and frontend services in docker-compose.yml are NOT commented out,
# but README suggests uncommenting them if needed. They are present and will start.
docker compose up --build
```

## Entry points

- **Backend main**: `src/main/java/com/assessment/AssessmentApplication.java`
- **Frontend main**: `frontend/src/main.tsx`
- **Frontend routes**: `frontend/src/App.tsx`
- **Spring config**: `src/main/resources/application.yml`
- **Liquibase master**: `src/main/resources/db/changelog/db.changelog-master.xml`

## Architecture notes

### Security model

- **Admin**: Spring Security form-login at `/api/admin/login`. Default credentials: `admin / admin`. All `/api/admin/**` require `ADMIN` role.
- **Employee**: No password. Access via HMAC-signed invite token at `/api/employee/invite/{token}`. Server validates token, creates session, sets cookie `SESSION_EMPLOYEE`. All subsequent employee API calls use this cookie.
- **CSRF is disabled** (`SecurityConfig`)

### AI provider switching

Spring AI auto-configurations for both Gemini and GigaChat are **explicitly excluded** in `AssessmentApplication.java` (`@SpringBootApplication(exclude = {...})`). They are loaded manually via `ChatClientConfig`. Active provider is controlled by env var `AI_PROVIDER` (`gemini` or `gigachat`, default `gemini`).

### Rate limiting

Resilience4j rate limiter `geminiApi` configured for 15 requests/minute, 10s timeout. Config in `application.yml` and `Resilience4jConfig.java`.

### Frontend constraints

- **Chrome-only**: `App.tsx` checks `navigator.userAgent` for Chrome and blocks other browsers because SpeechRecognition API is required.
- **Dev proxy**: Vite config proxies `/api` to `http://localhost:8080`. Dev server port is 3000.
- **No tests, no linter, no formatter** configured for frontend.

## Database

- PostgreSQL 18. Default local credentials: `assessment / assessment`, database `assessment`.
- Migrations are **Liquibase XML** in `src/main/resources/db/changelog/changes/`. Do not use Hibernate DDL auto; `ddl-auto: none` is set.
- Tables: `competencies`, `criteria`, `criteria_levels`, `employees`, `sessions`, `assessment_invite_tokens`, `question_attempts`, `ai_settings`, `question_banks`.

## Environment variables

| Variable | Required | Default | Purpose |
|----------|----------|---------|---------|
| `GEMINI_API_KEY` | Yes for LLM | — | Gemini API key |
| `GIGACHAT_API_KEY` | If using GigaChat | — | GigaChat API key |
| `AI_PROVIDER` | No | `gemini` | `gemini` or `gigachat` |
| `HMAC_SECRET` | No | `change-me-in-production` | HMAC signing for invite tokens |

## Build / runtime quirks

- `gradle.properties` sets `org.gradle.jvmargs=--enable-native-access=ALL-UNNAMED` required for Java 25.
- Dockerfile uses Gradle 9.6.1 for build stage, but wrapper is 8.14.
- Backend exposes port 8080. Frontend dev server 3000, production Docker serves on 80 via nginx.
- Frontend nginx config proxies `/api/` to `backend:8080` in Docker network.

## Testing

- **No backend tests** exist currently (`src/test/` is empty).
- **No frontend tests** configured.
- When adding tests, backend uses JUnit Platform (`./gradlew test`).

## Code style / conventions

- No enforced linter or formatter for Java or TypeScript.
- Backend uses Lombok (`@Data`, etc.) extensively.
- Frontend `tsconfig.json` has `strict: true`, `noUnusedLocals: true`, `noUnusedParameters: true`.
- Repo language is Russian (UI strings, some comments, README). Keep Russian user-facing strings when editing frontend.

### Git commit conventions

Format — Conventional Commits with Russian description:

```
type(scope): краткое описание на русском
```

**Types (`type`):**
- `feat` — new feature
- `fix` — bug fix
- `refactor` — refactoring without behavior change
- `chore` — tech debt, dead code removal, tooling
- `style` — formatting, CSS, whitespace (not semantics)
- `test` — adding or fixing tests
- `docs` — documentation
- `db` — DB migration (when scope is omitted)

**Scopes (`scope`):**
- `api` — controllers, endpoints
- `frontend` / `ui` — React components, styles
- `admin` — admin panel
- `config` — configuration, env, application.yml
- `ai` — AiProviderService, FollowUpService, prompts
- `scoring` — scoring, LlmJsonParser
- `report` — ReportService
- `db` / `entity` — Liquibase, entities
- `security` — auth, tokens

**Rules:**
- Description in past tense answering "what was done": «добавлен», «поправлен», «вынесен», «удалён».
- No trailing period.
- Omit scope if it's obvious from context (`feat: добавить...`).
- One commit per change. Do not mix `feat` and `fix` in the same commit.

Examples:

```
feat(api): добавить триггер уточняющих вопросов в submitAnswer
fix(config): вернуть max-questions-per-session
refactor(scoring): вынести LlmJsonParser в отдельный util
feat(frontend): показывать Loader при ожидании оценки
chore: удалить мёртвые config-ключи
db: добавить колонку base_score в question_attempts
```

## Known limitations

- Voice input only works in Google Chrome.
- No server-side speech recognition; browser handles it.
- No mobile browser support.
- One LLM call per answer (Gemini or GigaChat). Rate limiter prevents abuse.

## Reference docs

- `README.md` — full API examples and DB schema description.
- `docker-compose.yml` — service topology.
- `src/main/resources/application.yml` — runtime config.
