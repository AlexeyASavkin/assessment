# Сервис оценки компетенций

## Описание задачи

Сервис предназначен для оценки компетенций сотрудников с помощью голосового интервью в браузере.

- **Администратор** через веб-интерфейс (`/admin`) или REST API управляет компетенциями, критериями, уровнями требований, сотрудниками и создаёт для каждого сотрудника одноразовую пригласительную ссылку.
- **Сотрудник** получает ссылку, открывает её в Google Chrome, последовательно отвечает на вопросы, используя голосовой ввод (`SpeechRecognition API`), при необходимости редактирует распознанный текст и отправляет ответ.
- **LLM (Gemini 2.0 Flash)** генерирует вопросы по критериям, формирует до одного уточняющего вопроса на каждый основной и оценивает ответы по шкале 0–5 с уровнем уверенности и рекомендацией.
- **По завершении сессии** формируется итоговый отчёт с расчётным уровнем Junior / Middle / Senior.

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

Фронтенд доступен на `http://localhost:5173`.

### Полный запуск через Docker Compose

```bash
cp .env.example .env
# раскомментируй сервисы backend и frontend в docker-compose.yml, если нужно
docker compose up --build
```

## Конфигурация

Основные параметры в `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/assessment
    username: assessment
    password: assessment
  ai:
    google:
      genai:
        api-key: ${GEMINI_API_KEY:}
        chat:
          options:
            model: gemini-2.0-flash

assessment:
  security:
    hmac-secret: ${HMAC_SECRET:change-me-in-production}
    session-cookie-name: SESSION_EMPLOYEE
    token-expiry-hours: 72
  question:
    max-followups-per-main: 1
    max-questions-per-session: 20
  rate-limiter:
    max-requests-per-minute: 15
```

## Как работает сервис

### Подготовка оценки

1. Админ создаёт компетенцию.
2. Внутри компетенции создаёт критерии.
3. Для каждого критерия добавляет уровни требований: `JUNIOR`, `MIDDLE`, `SENIOR`.
4. Создаёт сотрудника и генерирует ему одноразовую ссылку.

### Прохождение оценки сотрудником

1. Сотрудник открывает ссылку вида `/api/employee/invite/{token}`.
2. Сервер проверяет HMAC-подпись токена, создаёт сессию и устанавливает cookie `SESSION_EMPLOYEE`.
3. Происходит редирект на страницу сессии `/session/{sessionId}`.
4. Фронтенд получает текущий вопрос (`GET /api/employee/sessions/{id}/questions`).
5. Сотрудник отвечает голосом, редактирует транскрипт и отправляет ответ (`POST /api/employee/sessions/{id}/answers`).
6. Сервер оценивает ответ через Gemini, сохраняет результат и возвращает следующий вопрос или признак завершения.
7. После ответа на основной вопрос может быть задан один уточняющий вопрос.
8. Когда все критерии пройдены, сессия переходит в статус `COMPLETED`, сотрудник попадает на страницу отчёта.

### Расчёт уровня

- Средняя оценка по критерию считается только по основным вопросам (`followup_depth = 0`) и только по валидным оценкам (`valid_judge = true`).
- `avg >= 4.3` → `SENIOR`
- `avg >= 3.5` → `MIDDLE`
- иначе → `JUNIOR`
- Композитный уровень — тот же расчёт по среднему баллу всех критериев.

## API

### Аутентификация администратора

Админ использует стандартную Spring Security form-login:

```bash
curl -X POST http://localhost:8080/api/admin/login \
  -d "username=admin&password=admin" \
  -c cookies.txt
```

Логин и пароль по умолчанию: `admin / admin`.

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

#### Критерии

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/admin/competencies/{competencyId}/criteria` | Добавить критерий |
| GET | `/api/admin/competencies/{competencyId}/criteria` | Список критериев компетенции |
| PUT | `/api/admin/criteria/{id}` | Обновить критерий |
| DELETE | `/api/admin/criteria/{id}` | Удалить критерий |

#### Уровни требований

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/admin/criteria/{criteriaId}/levels` | Добавить уровень требований |
| GET | `/api/admin/criteria/{criteriaId}/levels` | Список уровней |
| PUT | `/api/admin/criteria/levels/{id}` | Обновить уровень |
| DELETE | `/api/admin/criteria/levels/{id}` | Удалить уровень |

Пример добавления уровня:

```bash
curl -X POST http://localhost:8080/api/admin/criteria/{criteriaId}/levels \
  -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"level": "MIDDLE", "requirements": "Понимает Stream API и коллекции"}'
```

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
    "rawTranscript": "я знаю джаву",
    "finalTranscript": "Я знаю Java."
  }'
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
      "criteriaId": "uuid",
      "criteriaName": "Stream API",
      "competencyName": "Java",
      "averageScore": 4.50,
      "achievedLevel": "SENIOR",
      "followUpScores": [4.0],
      "feedbacks": ["Отличное понимание Stream API"]
    }
  ],
  "compositeLevel": "SENIOR",
  "overallRecommendation": "Сотрудник демонстрирует высокий уровень компетенций. Рекомендуется к повышению."
}
```

## Структура базы данных

| Таблица | Назначение |
|---------|-----------|
| `competencies` | Компетенции |
| `criteria` | Критерии внутри компетенций |
| `criteria_levels` | Уровни требований (JUNIOR / MIDDLE / SENIOR) |
| `employees` | Сотрудники |
| `sessions` | Сессии оценки |
| `assessment_invite_tokens` | Одноразовые пригласительные токены |
| `question_attempts` | Вопросы, ответы и оценки |

## Известные ограничения

- Голосовой ввод работает только в Google Chrome.
- Один LLM (Gemini 2.0 Flash) выполняет и генерацию вопросов, и оценку ответов.
- Нет серверного распознавания речи и хранения аудиофайлов.
- Нет поддержки Firefox, Safari и мобильных браузеров.
