# AGENTS.md — Assessment Service

## What this repo is

Java/Spring Boot backend + React frontend for employee competency assessment via voice interviews in the browser. LLM generates questions and scores answers. Admins manage competencies/criteria/employees via web UI or REST API. Employees receive one-time invite links and answer questions using Chrome's SpeechRecognition API.

## Stack & versions

- **Backend**: Java 25, Spring Boot 4.1.0, Spring Security 7, Spring Data JPA, JOOQ, Spring AI 2.0.0
- **LLM providers**: OpenCode (default), Sber GigaChat, OpenRouter, Google Gemini 2.0 Flash, plus a `stub` provider for tests (5 total; switchable at runtime via `AI_PROVIDER` env / `AiSettings` table)
- **Database**: PostgreSQL 18, migrations via Liquibase
- **Rate limiting**: Resilience4j
- **Build**: Gradle 9.6.1 (wrapper), Java toolchain 25
- **Frontend**: React 19, TypeScript 5.7, Vite 6, react-router-dom 7
- **Deploy**: Docker Compose

## Must-know commands

### Backend

```bash
# Run (requires PostgreSQL running locally or via Docker)
./gradlew bootRun

# Build jar
./gradlew bootJar

# Unit tests (170 tests across 23 files in src/test/)
./gradlew test

# Component (BDD) tests + Allure report — full pipeline
tests\component\run-bdd-tests.bat

# Or step-by-step:
./gradlew -p tests/component test          # 27 Cucumber scenarios via JUnit Platform
./gradlew -p tests/component allureReport  # generates build/reports/allure-report/index.html
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
# Start PostgreSQL via Docker (только БД, без бэкенда и фронтенда)
cp .env.example .env  # then edit OPENCODE_API_KEY
docker compose -f compose.db.yml up -d
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
- **Liquibase master**: `src/main/resources/db/changelog/db.changelog-master.yml`

## Architecture notes

### Hexagonal structure (порты и адаптеры)

Бэкенд переведён на гексагональную архитектуру: 3 bounded context в одном Gradle-модуле, пакетное разделение. Контроллеры — тонкие driving adapters, бизнес-логика — в use case за портами, персистентность и LLM — за outbound-адаптерами.

```
com.assessment/
├── entity/      (JPA-сущности — персистентная модель, используются адаптерами)
├── repository/  (Spring Data JPA репозитории — внутренняя деталь адаптеров)
├── service/     (легаси-сервисы: AiProviderService, ReportService, QuestionGeneratorService, QuestionSelector)
├── security/    (HmacTokenValidator, EmployeeTokenService, AdminUserDetailsService)
├── config/      (SecurityConfig, Resilience4jConfig, RoutingChatModel, StubChatModel, RateLimitingChatModelDecorator)
├── ai/          (Контекст 1: LLM)
│   ├── domain/      (ScoreResult, QuestionResult, FollowUpResult, PromptTemplate)
│   ├── port/        (LlmScoringPort, LlmQuestionGenerationPort, LlmFollowUpPort)
│   ├── adapter/     (SpringAi*Adapter, Stub*Adapter)
│   └── config/      (ChatClientConfig, AiAdapterConfig)
├── assessment/  (Контекст 2: session flow сотрудника)
│   ├── domain/      (AssessmentSession, Attempt, AssessmentResult, SessionStatus, InviteToken — иммутабельные, без Spring/JPA)
│   ├── application/ (GetQuestionUseCase, SubmitAnswerUseCase, GetReportUseCase, InviteEmployeeUseCase + impl, AttemptScoringExecutor, SessionQuestionPicker)
│   ├── port/out/    (SessionRepositoryPort, AttemptRepositoryPort, QuestionBankRepositoryPort, InviteTokenRepositoryPort, TopicQueryPort)
│   └── adapter/     (in/EmployeeWebAdapter, out/Jpa*RepositoryAdapter)
└── management/  (Контекст 3: админский CRUD)
    ├── application/ (CompetencyCrudUseCase, SectionCrudUseCase, TopicCrudUseCase, EmployeeCrudUseCase, TokenManagementUseCase, QuestionBankManagementUseCase, AiSettingsUseCase, ApplicationManagementUseCase + impl)
    ├── port/out/    (CompetencyRepositoryPort, SectionRepositoryPort, TopicRepositoryPort, EmployeeRepositoryPort, TokenRepositoryPort, QuestionBankRepositoryPort, SessionRepositoryPort)
    └── adapter/     (in/AdminWebAdapter, out/*JpaAdapter)
```

**Правила зависимостей:**
- `adapter/in` (контроллеры) НЕ импортируют `repository.*` и `entity.*` — только use case интерфейсы и DTO-мапперы
- `domain` НЕ импортирует `jakarta.persistence.*`, `org.springframework.*`, `lombok.*`
- use case зависят от портов и легаси-сервисов, но не от адаптеров
- Порты — интерфейсы, адаптеры — их реализации (`@Component` / `@Service`)
- Имена management-адаптеров (`CompetencyJpaAdapter` и т.п.) отличаются от assessment-адаптеров (`JpaCompetencyRepositoryAdapter`), чтобы не было конфликта имён бинов Spring

### Security model

- **Admin**: Spring Security form-login at `/api/admin/login`. Credentials via env vars `ADMIN_USERNAME` / `ADMIN_PASSWORD_HASH`; default seed in `.env.example` uses `admin / admin`. All `/api/admin/**` require `ADMIN` role.
- **Employee**: No password. Access via HMAC-signed invite token at `/api/employee/invite/{token}`. Server validates token, creates session, sets cookie `SESSION_EMPLOYEE`. All subsequent employee API calls use this cookie.
- **CSRF is disabled** (`SecurityConfig`)

### AI provider switching

Spring AI auto-configurations for Gemini, GigaChat and OpenAI are **explicitly excluded** in `AssessmentApplication.java` (`@SpringBootApplication(exclude = {...})`). They are loaded manually via `ai/config/ChatClientConfig.java` (bean assembly: provider `ChatModel` beans → `RoutingChatModel` → `ChatClient` / primary `ChatModel`). Active provider is controlled by env var `AI_PROVIDER` (`opencode` | `gigachat` | `openrouter` | `gemini` | `stub`, default `opencode`). Validated hard-coded in `AiProviderService.setActiveProvider`. LLM-вызовы идут через AI-адаптеры (`ai/adapter/SpringAi*Adapter`, `Stub*Adapter`), которые используют `ChatModel` + `AiProviderService` (промпты).

### Rate limiting

Resilience4j (15 requests/minute, 10s timeout, `Resilience4jConfig.java`): LLM-вызовы сотрудников ограничены персональными bucket'ами на сессию (`config/SessionLlmRateLimiter.java`, имя лимитера `session:<sessionId>`), админская генерация вопросов — общим bucket'ом `geminiApi` (`service/QuestionSelector.java`). При превышении лимита `RequestNotPermitted` → HTTP 429 с `Retry-After` (`config/GlobalExceptionHandler.java`).

### Frontend constraints

- **Chrome-only**: `App.tsx` checks `navigator.userAgent` for Chrome and blocks other browsers because SpeechRecognition API is required.
- **Dev proxy**: Vite config proxies `/api` to `http://localhost:8080`. Dev server port is 3000.
- **No tests, no linter, no formatter** configured for frontend.

## Database

- PostgreSQL 18. Default local credentials: `assessment / assessment`, database `assessment`.
- Migrations are **Liquibase YAML** in `src/main/resources/db/changelog/changes/`. Do not use Hibernate DDL auto; `ddl-auto: none` is set.
- Tables: `competencies`, `sections`, `topics`, `criteria`, `criteria_levels`, `employees`, `employee_competencies`, `sessions`, `assessment_invite_tokens`, `question_attempts`, `ai_settings`, `question_banks`, `admin_users`.

## Environment variables

| Variable | Required | Default | Purpose |
|----------|----------|---------|---------|
| `OPENCODE_API_KEY` | Yes for LLM | — | OpenCode API key (DeepSeek, Grok, GLM etc.) |
| `GIGACHAT_API_KEY` | If using GigaChat | — | GigaChat API key |
| `OPENROUTER_API_KEY` | If using OpenRouter | — | OpenRouter API key (aggregates multiple models) |
| `GEMINI_API_KEY` | If using Gemini | — | Gemini API key |
| `AI_PROVIDER` | No | `opencode` | `opencode` \| `gigachat` \| `openrouter` \| `gemini` \| `stub` |
| `HMAC_SECRET` | No | `change-me-in-production` | HMAC signing for invite tokens |
| `ADMIN_USERNAME` | Yes (seed) | — | Initial admin username (Liquibase seed) |
| `ADMIN_PASSWORD_HASH` | Yes (seed) | — | Initial admin password hash (BCrypt, Liquibase seed) |
| `POSTGRES_USER` | Yes | — | PostgreSQL username |
| `POSTGRES_PASSWORD` | Yes | — | PostgreSQL password |
| `SERVER_PORT` | No | `8080` | Backend HTTP port (tests use 8081) |
| `SPRING_DATASOURCE_URL` | No | `jdbc:postgresql://localhost:5432/assessment` | JDBC URL override |

## Build / runtime quirks

- `gradle.properties` sets `org.gradle.jvmargs=--enable-native-access=ALL-UNNAMED` required for Java 25.
- Dockerfile uses Gradle 9.6.1 for build stage; wrapper is also 9.6.1 (upgraded from 8.14 — Gradle 8.14's bundled Groovy 3.0.24 cannot compile build scripts on Java 25, "Unsupported class file major version 69").
- Backend exposes port 8080. Frontend dev server 3000, production Docker serves on 80 via nginx.
- Frontend nginx config proxies `/api/` to `backend:8080` in Docker network.

## Testing

### Backend tests

- **Unit tests**: 170 tests across 23 files in `src/test/` (JUnit 5 + Mockito, no Spring context).
  - Legacy: `AdminUserDetailsServiceTest` (5), `HmacTokenValidatorTest` (9), `AiProviderServiceTest` (20), `LlmJsonParserTest` (24)
  - `config` (1): `SessionLlmRateLimiterTest` (3)
  - `assessment/domain` (4): `AssessmentSessionTest` (3), `AttemptTest` (4), `AssessmentResultTest` (3), `InviteTokenTest` (4)
  - `assessment/application` (6): `InviteEmployeeUseCaseImplTest` (6), `GetQuestionUseCaseImplTest` (7), `SubmitAnswerUseCaseImplTest` (16), `GetReportUseCaseImplTest` (7), `AttemptScoringExecutorTest` (4), `SessionQuestionPickerTest` (13)
  - `management/application` (8): `CompetencyCrudUseCaseImplTest` (3), `SectionCrudUseCaseImplTest` (4), `TopicCrudUseCaseImplTest` (4), `EmployeeCrudUseCaseImplTest` (11), `TokenManagementUseCaseImplTest` (3), `QuestionBankManagementUseCaseImplTest` (9), `AiSettingsUseCaseImplTest` (1), `ApplicationManagementUseCaseImplTest` (7)
- Backend uses JUnit Platform (`./gradlew test`).
- Use `@DisplayName` with a Russian description for unit tests.
- Use camelCase for test method names (no underscores).

### Component (BDD) tests

- **Location**: `tests/component/` (standalone Gradle project; NOT included in root `settings.gradle`, won't run during `./gradlew build`).
- **Approach**: black-box — HTTP via OkHttp, no mocking, real backend instance on port 8081 with `AI_PROVIDER=stub`.
- **Stack**: Cucumber 7.21 + JUnit Platform Suite + OkHttp 4 + Allure 2.30 (`allure-cucumber7-jvm` adapter).
- **5 feature-files, 27 scenarios**: `admin_auth`, `competencies`, `employees`, `employee_session`, `report`.
- **Test credentials**: `admin / TestAdminPass!` (different from `.env`; see `tests/component/src/test/resources/config/test-admin.properties`).
- **Allure**: `allureReport` task downloads Allure CLI on demand (cached in `build/allure-cli/`) and generates `build/reports/allure-report/index.html`.
- **Frontend**: No tests, no linter, no formatter configured.

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
- `ai` — AiProviderService, AI-адаптеры (SpringAi*/Stub*), промпты
- `scoring` — скоринг, LlmJsonParser
- `report` — ReportService, AssessmentResult
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
- One LLM call per answer (OpenCode or GigaChat). Rate limiter prevents abuse.

## Reference docs

- `README.md` — full API examples and DB schema description.
- `docker-compose.yml` — service topology.
- `src/main/resources/application.yml` — runtime config.
