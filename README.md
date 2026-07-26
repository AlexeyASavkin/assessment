# Сервис оценки компетенций

## Описание задачи

Сервис предназначен для оценки компетенций сотрудников с помощью голосового интервью в браузере.

- **Администратор** через веб-интерфейс (`/admin`) или REST API управляет компетенциями, разделами, темами, сотрудниками и создаёт для каждого сотрудника одноразовую пригласительную ссылку.
- **Сотрудник** получает ссылку, открывает её в Google Chrome, последовательно отвечает на вопросы из банка, используя голосовой ввод (`SpeechRecognition API`), при необходимости редактирует распознанный текст и отправляет ответ.
- **LLM (Gemini 2.0 Flash / GigaChat / OpenRouter)** оценивает ответы по шкале 0–5. Для слабых ответов (≤ 2 балла) задаётся один уточняющий вопрос с переоценкой.
- **По завершении сессии** формируется итоговый отчёт с результатом «Пройден / Не пройден».

## Стек

| Компонент | Технология |
|-----------|-----------|
| Бэкенд | Java 25, Spring Boot 4.1.0, Spring Security 7, Spring Data JPA, JOOQ |
| LLM | Spring AI 2.0.0 + Google Gemini 2.0 Flash |
| База данных | PostgreSQL 18 |
| Миграции | Liquibase |
| Rate limiting | Resilience4j |
| Сборка | Gradle 8.14 / 9.6.1 |
| Фронтенд | React 19, TypeScript, Vite |
| Деплой | Docker Compose |

## Локальный запуск

### Требования

- Java 25
- Docker / Docker Compose
- (Опционально) Node.js 20+ для локальной разработки фронтенда
- Ключ `GEMINI_API_KEY` нужен только для генерации вопросов и оценки ответов; без него приложение запустится, но LLM-функции будут возвращать ошибку.

### Запуск базы данных

```bash
cp .env.example .env
# отредактируй .env и добавь GEMINI_API_KEY
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
# отредактируй .env и добавь GEMINI_API_KEY и ADMIN_USERNAME / ADMIN_PASSWORD_HASH
docker compose up --build
```

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
    parameters:
      adminUsername: ${ADMIN_USERNAME}
      adminPasswordHash: ${ADMIN_PASSWORD_HASH}

assessment:
  security:
    hmac-secret: ${HMAC_SECRET}
    session-cookie-name: SESSION_EMPLOYEE
    token-expiry-hours: 72
  question:
    max-questions-per-session: 20
  rate-limiter:
    max-requests-per-minute: 15
  ai:
    active-provider: ${AI_PROVIDER:gemini}
```

## Как работает сервис

### Подготовка оценки

1. Админ создаёт компетенцию.
2. Внутри компетенции создаёт разделы и темы.
3. Создаёт сотрудника и генерирует ему одноразовую ссылку.
4. При необходимости загружает вопросы в банк вопросов или генерирует их через LLM.

### Прохождение оценки сотрудником

1. Сотрудник открывает ссылку вида `/api/employee/invite/{token}`.
2. Сервер проверяет HMAC-подпись токена, создаёт сессию и устанавливает cookie `SESSION_EMPLOYEE`.
3. Происходит редирект на страницу сессии `/session/{sessionId}`.
4. Фронтенд получает текущий вопрос (`GET /api/employee/sessions/{id}/questions`).
5. Сотрудник отвечает голосом, редактирует транскрипт и отправляет ответ (`POST /api/employee/sessions/{id}/answers`).
6. Сервер оценивает ответ через LLM, сохраняет результат и возвращает следующий вопрос или признак завершения. Оценка основных вопросов выполняется асинхронно для ускорения UX.
7. Когда тема исчерпана, сервер синхронно дооценивает все ответы темы и ищет кандидатов на уточняющий вопрос (основные ответы с оценкой ≤ 2). При наличии кандидата генерируется уточняющий вопрос. После ответа на уточнение переоценивается основной ответ с учётом уточнения.
8. Когда все темы пройдены, сессия переходит в статус `COMPLETED`, сотрудник попадает на страницу отчёта.

### Расчёт результата

- Средняя оценка по теме считается только по основным вопросам (`followup_depth = 0`) и только по валидным оценкам (`valid_judge = true`).
- `avg >= 3.5` → тема пройдена
- иначе → тема не пройдена
- Общий результат — «Пройден», если пройдены все темы.

## API

### Аутентификация администратора

Админ использует стандартную Spring Security form-login:

```bash
curl -X POST http://localhost:8080/api/admin/login \
  -d "username=admin&password=admin" \
  -c cookies.txt
```

Логин и пароль задаются через переменные окружения `ADMIN_USERNAME` и `ADMIN_PASSWORD_HASH` (см. `.env`). По умолчанию: `admin / admin`.

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
| `employees` | Сотрудники |
| `sessions` | Сессии оценки |
| `assessment_invite_tokens` | Одноразовые пригласительные токены |
| `question_attempts` | Вопросы, ответы и оценки (включая уточняющие) |
| `question_banks` | Банк вопросов (сгенерированных или добавленных вручную) |
| `ai_settings` | Настройки AI-провайдеров и промптов |

## Известные ограничения

- Голосовой ввод работает только в Google Chrome.
- Нет серверного распознавания речи и хранения аудиофайлов.
- Нет поддержки Firefox, Safari и мобильных браузеров.
- Один LLM-вызов на ответ (Gemini или GigaChat). Rate limiter предотвращает превышение лимитов.
