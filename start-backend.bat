@echo off
set AI_PROVIDER=stub
set GEMINI_API_KEY=test-key
set HMAC_SECRET=change-me-in-production
set ADMIN_USERNAME=admin
set ADMIN_PASSWORD_HASH={bcrypt}$2a$10$8NNlBPU28aAmz520dwwSw.1emitHPCfYoQv9XyK0j6qWPSvHHCPHO
set POSTGRES_USER=assessment
set POSTGRES_PASSWORD=assessment

cd /d C:\Work\Projects\assessment
call gradlew.bat bootRun --args="--server.port=8081 --spring.datasource.url=jdbc:postgresql://localhost:5432/assessment --spring.datasource.username=assessment --spring.datasource.password=assessment --spring.liquibase.enabled=true"
