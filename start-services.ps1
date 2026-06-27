Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  Starting TechNova Leave & Attendance Portal  " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# Check if targets exist
if (!(Test-Path "config-server/target/config-server-0.0.1-SNAPSHOT.jar")) {
    Write-Host "JAR files not found. Please build the project first using 'mvn clean package -DskipTests'" -ForegroundColor Red
    Exit
}

Write-Host "1. Starting Config Server (Port 8888)..." -ForegroundColor Yellow
Start-Process java -ArgumentList "-jar config-server/target/config-server-0.0.1-SNAPSHOT.jar" -WindowStyle Normal -PassThru
Start-Sleep -Seconds 8

Write-Host "2. Starting Eureka Service Registry (Port 8761)..." -ForegroundColor Yellow
Start-Process java -ArgumentList "-jar eureka-server/target/eureka-server-0.0.1-SNAPSHOT.jar" -WindowStyle Normal -PassThru
Start-Sleep -Seconds 8

Write-Host "3. Starting API Gateway (Port 8080)..." -ForegroundColor Yellow
Start-Process java -ArgumentList "-jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar" -WindowStyle Normal -PassThru

Write-Host "4. Starting Authentication Service (Port 8081)..." -ForegroundColor Yellow
Start-Process java -ArgumentList "-jar auth-service/target/auth-service-0.0.1-SNAPSHOT.jar" -WindowStyle Normal -PassThru

Write-Host "5. Starting Attendance Service (Port 8082)..." -ForegroundColor Yellow
Start-Process java -ArgumentList "-jar attendance-service/target/attendance-service-0.0.1-SNAPSHOT.jar" -WindowStyle Normal -PassThru

Write-Host "6. Starting Leave Service (Port 8083)..." -ForegroundColor Yellow
Start-Process java -ArgumentList "-jar leave-service/target/leave-service-0.0.1-SNAPSHOT.jar" -WindowStyle Normal -PassThru

Write-Host "7. Starting HR Report Service (Port 8084)..." -ForegroundColor Yellow
Start-Process java -ArgumentList "-jar hr-service/target/hr-service-0.0.1-SNAPSHOT.jar" -WindowStyle Normal -PassThru

Write-Host "8. Starting Notification Service (Port 8085)..." -ForegroundColor Yellow
Start-Process java -ArgumentList "-jar notification-service/target/notification-service-0.0.1-SNAPSHOT.jar" -WindowStyle Normal -PassThru

Write-Host ""
Write-Host "All microservices are starting up!" -ForegroundColor Green
Write-Host "You can access the API Gateway endpoints on http://localhost:8080" -ForegroundColor Green
Write-Host "Press Enter to exit this script wrapper..." -ForegroundColor Cyan
Read-Host
