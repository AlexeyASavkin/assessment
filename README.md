# Сервис оценки компетенций

## Описание задачи

Сервис предназначен для оценки компетенций сотрудников с помощью голосового интервью в браузере.

- **Администратор** через веб-интерфейс (`/admin`) или REST API управляет компетенциями, разделами, темами, сотрудниками и создаёт для каждого сотрудника одноразовую пригласительную ссылку.
- **Сотрудник** получает ссылку, открывает её в Google Chrome, последовательно отвечает на вопросы из банка, используя голосовой ввод (`SpeechRecognition API`), при необходимости редактирует распознанный текст и отправляет ответ.
- **LLM (OpenCode / GigaChat / OpenRouter / Gemini 2.0 Flash)** оценивает ответы по шкале 0–5. Для слабых ответов (≤ 2 балла) задаётся один уточняющий вопрос с переоценкой.
- **По завершении сессии** формируется итоговый отчёт с результатом «Пройден / Не пройден».

## Стек

| Компонент | Технология |
|-----------|-----------|
| Бэкенд | Java 25, Spring Boot 4.1.0, Spring Security 7, Spring Data JPA, JOOQ |
| LLM | Spring AI 2.0.0 + OpenCode (default, DeepSeek V4 Flash), GigaChat, OpenRouter, Gemini 2.0 Flash, stub |
| База данных | PostgreSQL 18 |
| Миграции | Liquibase |
| Rate limiting | Resilience4j |
| Сборка | Gradle 9.6.1 |
| Фронтенд | React 19, TypeScript, Vite |
| Тестирование (unit) | JUnit 5, Mockito |
| Тестирование (component/BDD) | Cucumber 7.21, JUnit 5, OkHttp 4, Allure 2.30 |
| Деплой | Docker Compose |
| AI-агентный харнесс | OpenCode + oh-my-openagent (модель GLM-5.2) |

## Разработка с AI-агентным харнессом

Проект разрабатывается с помощью AI-агентного окружения:

- **OpenCode** — агентный харнесс (CLI) для разработки: чтение кода, генерация правок, запуск команд, работа с git.
- **oh-my-openagent** — плагин оркестрации для OpenCode: субагенты с ролями, делегирование задач и контроль качества. Конфигурация — `~/.omo/omo.jsonc` (включая fallback-модели).
- **Основная модель** — `GLM-5.2` через провайдер OpenCode; переключение моделей — в конфиге харнесса.

## Архитектура

Бэкенд построен по гексагональной архитектуре (порты и адаптеры) с тремя bounded context в одном Gradle-модуле:

- **`ai`** (Контекст 1: LLM) — порты `LlmScoringPort`, `LlmQuestionGenerationPort`, `LlmFollowUpPort` и их адаптеры: `SpringAi*Adapter` (реальные провайдеры) и `Stub*Adapter` (тесты, фиксированные ответы). Конфигурация провайдеров — `ai/config/ChatClientConfig.java`.
- **`assessment`** (Контекст 2: session flow сотрудника) — иммутабельные domain-модели (`AssessmentSession`, `Attempt`, `AssessmentResult`), use case'ы в `application/` (`GetQuestionUseCase`, `SubmitAnswerUseCase`, `GetReportUseCase`, `InviteEmployeeUseCase`), outbound-порты в `port/out/`, тонкий контроллер `adapter/in/EmployeeWebAdapter`.
- **`management`** (Контекст 3: админский CRUD) — use case'ы в `application/` (`CompetencyCrudUseCase`, `SectionCrudUseCase`, `TopicCrudUseCase`, `EmployeeCrudUseCase`, `TokenManagementUseCase`, `QuestionBankManagementUseCase`, `AiSettingsUseCase`, `ApplicationManagementUseCase`), outbound-порты в `port/out/`, тонкий контроллер `adapter/in/AdminWebAdapter`.

**Правила зависимостей:** контроллеры не импортируют репозитории и JPA-сущности; domain-модели не зависят от Spring/JPA/Lombok; use case'ы зависят от портов, но не от адаптеров. JPA-сущности (`entity/`) и Spring Data репозитории (`repository/`) — внутренняя деталь outbound-адаптеров.

## Локальный запуск

### Требования

- Java 25
- Docker / Docker Compose
- (Опционально) Node.js 20+ для локальной разработки фронтенда
- Ключ `OPENCODE_API_KEY` нужен только для генерации вопросов и оценки ответов; без него приложение запустится, но LLM-функции будут возвращать ошибку.

### Быстрый запуск одной командой (Windows)

```bat
start-dev.cmd
```

Скрипт поднимает весь dev-стек в трёх окнах:

1. **PostgreSQL** — `docker compose -f compose.db.yml up -d`, ожидание статуса `healthy` (до 60 с).
2. **Бэкенд** — `gradlew.bat bootRun` в отдельном окне, `http://localhost:8080`.
3. **Фронтенд** — `npm install` (только если нет `node_modules`) и `npm run dev` в отдельном окне, `http://localhost:3000`.

Дополнительно скрипт:

- проверяет наличие Docker, Java 25 и Node.js;
- создаёт `.env` из `.env.example`, если его нет, и загружает переменные окружения в процесс бэкенда (в т.ч. разэкранирует `$$` → `$` в `ADMIN_PASSWORD_HASH`);
- **пропускает запуск компонента, если его порт уже занят**: бэкенд на `8080`, фронтенд на `3000` — скрипт можно перезапускать в любой момент, он поднимет только недостающее;
- остановка: `Ctrl+C` в окнах бэкенда и фронтенда, затем `docker compose -f compose.db.yml down`.

### Запуск базы данных

```bash
cp .env.example .env
# отредактируй .env и добавь OPENCODE_API_KEY
docker compose up -d postgres
```

### Запуск бэкенда

```bash
./gradlew bootRun
```

Приложение стартует на `http://localhost:8080`.

### Запуск фронтенда

```bash
cd frontend
npm install
npm run dev
```

Фронтенд доступен на `http://localhost:3000`.

### Полный запуск через Docker Compose

```bash
cp .env.example .env
# отредактируй .env и добавь OPENCODE_API_KEY и ADMIN_USERNAME / ADMIN_PASSWORD_HASH
docker compose up --build
```

Сервис `backend` в `docker-compose.yml` запускается с профилем `prod` (`SPRING_PROFILES_ACTIVE: prod`). Профиль `prod` **не стартует** (fail-fast, `IllegalStateException` при запуске), если в `.env` не заданы реальные секреты:

- `HMAC_SECRET` всё ещё равен значению по умолчанию `change-me-in-production`;
- `ADMIN_PASSWORD_HASH` содержит хеш пароля по умолчанию из `.env.example`;
- `ADMIN_USERNAME` пуст.

Перед `docker compose up` обязательно замени в `.env` значения `HMAC_SECRET`, `ADMIN_USERNAME` и `ADMIN_PASSWORD_HASH` на свои (см. `.env.example` — комментарии к этим переменным).

## Тестирование

### Unit-тесты

170 unit-тестов в `src/test/` (запуск: `./gradlew test`):

| Файл | Сколько | Что проверяет |
|------|---------|---------------|
| `AdminUserDetailsServiceTest` | 5 | Загрузка admin-пользователя из переменных окружения |
| `HmacTokenValidatorTest` | 9 | Генерация и валидация HMAC-токенов |
| `AiProviderServiceTest` | 20 | Переключение провайдеров, API-ключи, промпты |
| `LlmJsonParserTest` | 24 | Извлечение значений из JSON-ответов LLM |
| `SessionLlmRateLimiterTest` | 3 | Персональный rate limiter сессии (Resilience4j) |
| `AssessmentSessionTest`, `AttemptTest`, `AssessmentResultTest`, `InviteTokenTest` | 14 | Иммутабельные domain-модели assessment-контекста |
| `InviteEmployeeUseCaseImplTest`, `GetQuestionUseCaseImplTest`, `SubmitAnswerUseCaseImplTest`, `GetReportUseCaseImplTest` | 36 | Use case'ы session flow сотрудника |
| `AttemptScoringExecutorTest`, `SessionQuestionPickerTest` | 17 | Оценка ответов и выбор вопросов |
| `CompetencyCrudUseCaseImplTest`, `SectionCrudUseCaseImplTest`, `TopicCrudUseCaseImplTest` | 11 | CRUD компетенций, разделов и тем |
| `EmployeeCrudUseCaseImplTest`, `TokenManagementUseCaseImplTest` | 14 | CRUD сотрудников и пригласительные токены |
| `QuestionBankManagementUseCaseImplTest`, `AiSettingsUseCaseImplTest`, `ApplicationManagementUseCaseImplTest` | 17 | Банк вопросов, настройки AI, управление приложением |

### Component (BDD) тесты

Интеграционные тесты в `tests/component/` проверяют API через HTTP, без мокирования — полноценный black-box подход с реальным запуском приложения.

**Стек тестов:** Cucumber 7.21 + JUnit 5 + OkHttp 4 + Allure 2.30 (`allure-cucumber7-jvm`).

**5 feature-файлов, 27 сценариев:**

| Файл | Что проверяет |
|------|---------------|
| `admin_auth.feature` | Вход админа (успех/неверный пароль), доступ без авторизации ко всем админским ресурсам |
| `competencies.feature` | CRUD компетенций, разделов и тем |
| `employees.feature` | CRUD сотрудников, генерация пригласительной ссылки, открытие ссылки |
| `employee_session.feature` | Получение вопроса, отправка ответа, уточняющие вопросы для слабых ответов, завершение сессии, отклонение повторного открытия использованной ссылки (403), невалидный токен, изоляция сессий сотрудников (IDOR) |
| `report.feature` | Отчёт по завершённой сессии, отказ для активной, админский отчёт |

### Запуск тестов

**Полный pipeline одной командой** (поднимает PostgreSQL, бэкенд, гоняет тесты, генерирует Allure-отчёт):

```bash
tests\component\run-bdd-tests.bat
```

**Пошагово:**

```bash
# 1. Запустить PostgreSQL
docker compose up -d postgres

# 2. Запустить бэкенд с stub AI-провайдером (без LLM)
start-backend.bat

# 3. В отдельном терминале запустить тесты
./gradlew -p tests/component test

# 4. Сгенерировать Allure-отчёт
./gradlew -p tests/component allureReport
# Отчёт: tests/component/build/reports/allure-report/index.html
```

Для тестов используется отдельный файл конфигурации `tests/component/src/test/resources/config/test-admin.properties` с credentials `admin / TestAdminPass!` — они отличаются от значений в `.env`. При запуске через `start-backend.bat` пароль автоматически подставляется через `ADMIN_PASSWORD_HASH`.

**Stub AI-провайдер:** при `AI_PROVIDER=stub` все LLM-вызовы возвращают заранее заданные ответы без внешних API. Оценка ответов детерминированная по содержимому: ответ со словом «слабый» получает 1 балл (триггерит уточняющий вопрос), остальные — 4 балла. Генерация вопросов возвращает уникальные тексты для каждой темы (вопрос N). Позволяет тестировать логику приложения без внешних API.

## Конфигурация

Основные параметры в `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/assessment
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yml

assessment:
  security:
    hmac-secret: ${HMAC_SECRET}
    session-cookie-name: SESSION_EMPLOYEE
    token-expiry-hours: 72
  question:
    max-questions-per-session: 20
  ai:
    active-provider: ${AI_PROVIDER:opencode}
```

### LLM-провайдеры и модели

Активный провайдер задаётся переменной `AI_PROVIDER` (по умолчанию `opencode`) и переключается в рантайме через раздел «Настройки ИИ» в админ-панели. API-ключ читается из окружения (`OPENCODE_API_KEY` и т.д.).

| Провайдер | Модель | Назначение |
|-----------|--------|------------|
| `opencode` (default) | `opencode-go/deepseek-v4-flash` | Основной провайдер. OpenAI-совместимый API шлюза OpenCode (`https://opencode.ai/zen/v1`), платная модель DeepSeek V4 Flash. Ключ: `OPENCODE_API_KEY` |
| `gigachat` | `GigaChat 2` | Российская модель Сбера. Ключ: `GIGACHAT_API_KEY` |
| `openrouter` | `openai/gpt-4o` | Агрегатор моделей (OpenAI, Anthropic и др.). Ключ: `OPENROUTER_API_KEY` |
| `gemini` | `Gemini 2.0 Flash` | Облачная модель Google. Ключ: `GEMINI_API_KEY` |
| `stub` | — | Заглушка для тестов без внешних API. Возвращает фиксированные ответы LLM |

## Как работает сервис

### Подготовка оценки

1. Админ создаёт компетенцию.
2. Внутри компетенции создаёт разделы и темы.
3. Создаёт сотрудника и генерирует ему одноразовую ссылку.
4. При необходимости загружает вопросы в банк вопросов или генерирует их через LLM.

### Прохождение оценки сотрудником

1. Сотрудник открывает ссылку вида `/api/employee/invite/{token}`.
2. Сервер проверяет токен (случайный 256-битный, в БД хранится только его SHA-256-хеш), создаёт сессию и устанавливает cookie `SESSION_EMPLOYEE`.
3. Происходит редирект на страницу сессии `/session/{sessionId}`.
4. Ссылка одноразовая: повторное открытие уже использованной ссылки отклоняется с HTTP 403. Продолжить сессию можно только по cookie `SESSION_EMPLOYEE`.
5. Фронтенд получает текущий вопрос (`GET /api/employee/sessions/{id}/questions`).
6. Сотрудник отвечает голосом, редактирует транскрипт и отправляет ответ (`POST /api/employee/sessions/{id}/answers`).
7. Сервер оценивает ответ через LLM, сохраняет результат и возвращает следующий вопрос или признак завершения. Оценка основных вопросов выполняется асинхронно для ускорения UX.
8. Когда тема исчерпана, сервер синхронно дооценивает все ответы темы и ищет кандидатов на уточняющий вопрос (основные ответы с оценкой ≤ 2). При наличии кандидата генерируется уточняющий вопрос. После ответа на уточнение переоценивается основной ответ с учётом уточнения.
9. Когда все темы пройдены, сессия переходит в статус `COMPLETED`, сотрудник попадает на страницу отчёта.

### Расчёт результата

- Средняя оценка по теме считается только по основным вопросам (`followup_depth = 0`) и только по валидным оценкам (`valid_judge = true`).
- `avg >= 3.0` → тема пройдена
- иначе → тема не пройдена
- Общий результат — «Пройден», если средний балл по всем темам ≥ 3.0.

## API

### Аутентификация администратора

Админ использует стандартную Spring Security form-login:

```bash
curl -X POST http://localhost:8080/api/admin/login \
  -d "username=admin&password=admin" \
  -c cookies.txt
```

Логин и пароль задаются через переменные окружения `ADMIN_USERNAME` и `ADMIN_PASSWORD_HASH` (см. `.env`). В `.env.example` по умолчанию `admin / admin` (BCrypt-хеш в `ADMIN_PASSWORD_HASH`). Для BDD-тестов используется другой пароль — `TestAdminPass!` (см. секцию «Тестирование»).

### Админ API

Все запросы требуют аутентификации администратора.

#### Компетенции

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/admin/competencies` | Создать компетенцию |
| GET | `/api/admin/competencies` | Список компетенций |
| GET | `/api/admin/competencies/{id}` | Получить компетенцию |
| PUT | `/api/admin/competencies/{id}` | Обновить компетенцию |
| DELETE | `/api/admin/competencies/{id}` | Удалить компетенцию |

Пример создания компетенции:

```bash
curl -X POST http://localhost:8080/api/admin/competencies \
  -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"name": "Java", "description": "Оценка знаний Java"}'
```

#### Разделы

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/admin/competencies/{competencyId}/sections` | Добавить раздел |
| GET | `/api/admin/competencies/{competencyId}/sections` | Список разделов компетенции |
| PUT | `/api/admin/sections/{id}` | Обновить раздел |
| DELETE | `/api/admin/sections/{id}` | Удалить раздел |

#### Темы

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/admin/sections/{sectionId}/topics` | Добавить тему |
| GET | `/api/admin/sections/{sectionId}/topics` | Список тем раздела |
| PUT | `/api/admin/topics/{id}` | Обновить тему |
| DELETE | `/api/admin/topics/{id}` | Удалить тему |

#### Сотрудники

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/admin/employees` | Создать сотрудника |
| GET | `/api/admin/employees` | Список сотрудников |
| GET | `/api/admin/employees/{id}` | Получить сотрудника |
| PUT | `/api/admin/employees/{id}` | Обновить сотрудника |

#### Пригласительные ссылки

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/admin/employees/{employeeId}/invite` | Сгенерировать одноразовую ссылку |
| GET | `/api/admin/tokens` | Список выданных токенов |

Пример генерации ссылки:

```bash
curl -X POST http://localhost:8080/api/admin/employees/{employeeId}/invite \
  -b cookies.txt
```

Ответ: `/api/employee/invite/{token}`

### API сотрудника

Сотрудник не вводит логин и пароль. Доступ осуществляется через cookie `SESSION_EMPLOYEE`, которая устанавливается при открытии пригласительной ссылки.

#### Открытие пригласительной ссылки

```bash
curl -L http://localhost:8080/api/employee/invite/{token} \
  -c employee_cookies.txt
```

Ссылка одноразовая: при первом открытии сервер создаёт сессию и отвечает редиректом (302) на `/session/{sessionId}`. Повторное открытие уже использованной ссылки возвращает **HTTP 403** — сессия продолжается по cookie `SESSION_EMPLOYEE`.

#### Сессия

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/employee/sessions` | Получить или создать сессию по cookie |

#### Вопросы и ответы

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/api/employee/sessions/{sessionId}/questions` | Получить текущий вопрос |
| POST | `/api/employee/sessions/{sessionId}/answers` | Отправить ответ |

Пример получения вопроса:

```bash
curl http://localhost:8080/api/employee/sessions/{sessionId}/questions \
  -b employee_cookies.txt
```

Пример отправки ответа:

```bash
curl -X POST http://localhost:8080/api/employee/sessions/{sessionId}/answers \
  -b employee_cookies.txt \
  -H "Content-Type: application/json" \
  -d '{
    "questionAttemptId": "uuid",
    "finalTranscript": "Я знаю Java."
  }
```

Ответ:

```json
{
  "nextQuestionId": "uuid или null",
  "completed": false,
  "isFollowUp": false,
  "followupParentId": null
}
```

#### Отчёт

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/api/employee/sessions/{sessionId}/report` | Получить итоговый отчёт |

Пример:

```bash
curl http://localhost:8080/api/employee/sessions/{sessionId}/report \
  -b employee_cookies.txt
```

Ответ:

```json
{
  "sessionId": "uuid",
  "employeeName": "Иванов Иван",
  "competencies": [
    {
      "topicId": "uuid",
      "topicName": "Stream API",
      "sectionName": "Java Core",
      "competencyName": "Java",
      "averageScore": 4.50,
      "passed": true,
      "followUpScores": [4.0],
      "feedbacks": ["Отличное понимание Stream API"]
    }
  ],
  "passed": true,
  "overallRecommendation": "Сотрудник демонстрирует высокий уровень компетенций.",
  "attempts": [
    {
      "attemptId": "uuid",
      "questionText": "Расскажите о Stream API",
      "finalTranscript": "Stream API — это...",
      "score": 4.5,
      "baseScore": null,
      "validJudge": true,
      "followupDepth": 0,
      "followupParentId": null,
      "feedback": "Хорошее понимание",
      "topicName": "Stream API",
      "createdAt": "2026-07-26T20:00:00"
    }
  ]
}
```

## Структура базы данных

| Таблица | Назначение |
|---------|-----------|
| `competencies` | Компетенции |
| `sections` | Разделы внутри компетенций |
| `topics` | Темы внутри разделов |
| `criteria` | Критерии оценки внутри компетенций |
| `criteria_levels` | Уровни критериев |
| `employees` | Сотрудники |
| `employee_competencies` | Связь сотрудников с компетенциями |
| `sessions` | Сессии оценки |
| `assessment_invite_tokens` | Одноразовые пригласительные токены |
| `question_attempts` | Вопросы, ответы и оценки (включая уточняющие) |
| `question_banks` | Банк вопросов (сгенерированных или добавленных вручную) |
| `ai_settings` | Настройки AI-провайдеров и промптов |

### Демо-данные

При первой миграции БД (changeset `015-seed-demo-data`) сервис автоматически наполняется демо-данными для предварительной настройки и примеров:

- **Компетенции**: «Java-разработка», «SQL и базы данных», «Frontend-разработка (React)»
- **Разделы и темы**: Java Core (Stream API, Коллекции), Spring Framework (Spring Boot, Spring Data JPA), Основы SQL (SELECT и JOIN, Индексы и оптимизация), React Core (Компоненты и props, Хуки)
- **Банк вопросов**: 16 примеров вопросов с уровнями сложности `JUNIOR` / `MIDDLE` / `SENIOR` для каждой темы

Демо-данные можно удалить или изменить через админ-панель или REST API.

## Известные ограничения

- Голосовой ввод работает только в Google Chrome.
- Нет серверного распознавания речи и хранения аудиофайлов.
- Нет поддержки Firefox, Safari и мобильных браузеров.
- Один LLM-вызов на ответ (OpenCode, GigaChat, OpenRouter или Gemini). Rate limiter предотвращает превышение лимитов.
