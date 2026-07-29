@echo off
set AI_PROVIDER=stub
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/assessment
set POSTGRES_USER=assessment
set POSTGRES_PASSWORD=assessment
set SERVER_PORT=8081
set HMAC_SECRET=change-me-in-production
set ADMIN_USERNAME=admin
set ADMIN_PASSWORD_HASH={bcrypt}$2a$10$8NNlBPU28aAmz520dwwSw.1emitHPCfYoQv9XyK0j6qWPSvHHCPHO
start "AssessmentTest" /B java -jar build\libs\assessment-service-0.0.1-SNAPSHOT.jar
