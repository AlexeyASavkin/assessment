@echo off
setlocal enabledelayedexpansion

set "ROOT=%~dp0"
cd /d "%ROOT%"

title Assessment - dev launcher

echo ==================================================
echo   Assessment Service - локальный запуск (dev)
echo   PostgreSQL (Docker) + Backend (Gradle) + Frontend (Vite)
echo ==================================================
echo.

rem ============ 1. Проверка зависимостей ============

where docker >nul 2>&1
if errorlevel 1 (
    echo [ОШИБКА] Docker не найден. Установите Docker Desktop: https://www.docker.com/products/docker-desktop/
    goto :end
)
docker info >nul 2>&1
if errorlevel 1 (
    echo [ОШИБКА] Docker не запущен. Запустите Docker Desktop и повторите.
    goto :end
)

where java >nul 2>&1
if errorlevel 1 (
    echo [ОШИБКА] Java не найдена. Нужна Java 25: https://adoptium.net/
    goto :end
)

where node >nul 2>&1
if errorlevel 1 (
    echo [ОШИБКА] Node.js не найден. Нужен Node 20+: https://nodejs.org/
    goto :end
)

rem ============ 2. Файл .env ============

if not exist "%ROOT%.env" (
    echo [i] .env не найден - создаю из .env.example...
    copy /y "%ROOT%.env.example" "%ROOT%.env" >nul
    echo [i] Отредактируйте .env: добавьте GEMINI_API_KEY, при необходимости смените ADMIN_* и HMAC_SECRET
)

rem ============ 3. Загрузка .env в окружение ============

for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%ROOT%.env") do (
    if not "%%a"=="" if not "%%b"=="" set "%%a=%%b"
)
rem docker-compose экранирует $ как $$ - возвращаем одиночный $ для локального запуска
set "ADMIN_PASSWORD_HASH=%ADMIN_PASSWORD_HASH:$$=$%"

rem ============ 4. PostgreSQL ============

echo [1/3] Запуск PostgreSQL (Docker)...
docker compose -f "%ROOT%compose.db.yml" up -d >nul 2>&1
if errorlevel 1 (
    echo [ОШИБКА] Не удалось запустить PostgreSQL через Docker Compose.
    goto :end
)

echo        Ожидание готовности базы данных...
set /a tries=0
:wait_db
set /a tries+=1
if %tries% gtr 30 goto db_timeout
set "PG_CID="
for /f "usebackq" %%c in (`docker compose -f "%ROOT%compose.db.yml" ps -q postgres 2^>nul`) do set "PG_CID=%%c"
if defined PG_CID (
    for /f "usebackq delims=" %%h in (`docker inspect --format "{{.State.Health.Status}}" !PG_CID! 2^>nul`) do (
        if "%%h"=="healthy" goto db_ready
    )
)
timeout /t 2 /nobreak >nul
goto wait_db
:db_timeout
echo [ОШИБКА] PostgreSQL не стал healthy за 60 секунд. Логи: docker compose -f compose.db.yml logs postgres
goto :end
:db_ready
echo        PostgreSQL готов.

rem ============ 5. Бэкенд ============

netstat -ano | findstr ":8080" | findstr "LISTENING" >nul 2>&1
if errorlevel 1 (
    echo [2/3] Запуск бэкенда - Spring Boot на http://localhost:8080...
    start "Assessment Backend" /D "%ROOT%" cmd /k "title Assessment Backend && call gradlew.bat bootRun"
) else (
    echo [2/3] Бэкенд уже работает на порту 8080 - пропускаю запуск.
)

rem ============ 6. Фронтенд ============

netstat -ano | findstr ":3000" | findstr "LISTENING" >nul 2>&1
if errorlevel 1 (
    if not exist "%ROOT%frontend\node_modules" (
        echo [i] Устанавливаю зависимости фронтенда - npm install...
        pushd "%ROOT%frontend"
        call npm install
        popd
    )
    echo [3/3] Запуск фронтенда - Vite на http://localhost:3000...
    start "Assessment Frontend" /D "%ROOT%frontend" cmd /k "title Assessment Frontend && npm run dev"
) else (
    echo [3/3] Фронтенд уже работает на порту 3000 - пропускаю запуск.
)

echo.
echo ==================================================
echo   Готово! Открытые окна:
echo     Бэкенд : http://localhost:8080
echo     Фронт  : http://localhost:3000  (только Chrome)
echo     Админ  : http://localhost:3000/admin
echo             (логин из .env: ADMIN_USERNAME / ADMIN_PASSWORD_HASH)
echo   Остановка: Ctrl+C в окнах бэкенда и фронтенда,
echo              затем: docker compose -f compose.db.yml down
echo ==================================================
echo.

:end
pause
endlocal