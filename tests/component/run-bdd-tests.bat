@echo off
chcp 65001 >nul
cd /d "%~dp0..\.."

echo ========================================
echo   BDD tests runner
echo ========================================

echo.
echo [1/7] Starting PostgreSQL...
docker compose up -d postgres
if %errorlevel% neq 0 (
    echo ERROR: Failed to start PostgreSQL
    exit /b 1
)

echo.
echo [2/7] Building backend...
call .\gradlew.bat bootJar -x test
if %errorlevel% neq 0 (
    echo ERROR: Backend build failed
    exit /b 1
)

echo.
echo [3/7] Stopping old backend on port 8081...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8081"') do (
    taskkill /F /PID %%a >nul 2>&1
)

echo.
echo [4/7] Starting backend...
set AI_PROVIDER=stub
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/assessment
set POSTGRES_USER=assessment
set POSTGRES_PASSWORD=assessment
set SERVER_PORT=8081
set HMAC_SECRET=change-me-in-production
set ADMIN_USERNAME=admin
set ADMIN_PASSWORD_HASH={bcrypt}$2a$10$8NNlBPU28aAmz520dwwSw.1emitHPCfYoQv9XyK0j6qWPSvHHCPHO
start "AssessmentBackend" /B java -jar build\libs\assessment-service-0.0.1-SNAPSHOT.jar

echo.
echo [5/7] Waiting for backend...
:wait
timeout /t 3 /nobreak >nul
curl -s -o nul http://localhost:8081/api/admin/competencies
if errorlevel 1 goto wait
echo Backend is ready!

echo.
echo [6/7] Running component tests...
call .\gradlew.bat -p tests\component test
set TEST_EXIT=%errorlevel%

echo.
echo [7/7] Generating Allure report...
call .\gradlew.bat -p tests\component allureReport -x test

echo.
echo Results:
echo   Allure report: tests\component\build\reports\allure-report\index.html
echo   Cucumber JSON: tests\component\build\reports\cucumber.json

if %TEST_EXIT% equ 0 (
    echo All tests passed.
) else (
    echo Some tests failed (exit code: %TEST_EXIT%).
)

exit /b %TEST_EXIT%
