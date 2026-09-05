@echo off
echo ==============================================
echo  Roads Navigation System - Full Startup
echo ==============================================

echo.
echo [1/3] Killing any lingering Python/Java processes...
taskkill /F /IM python.exe /T >nul 2>&1
taskkill /F /IM java.exe /T >nul 2>&1
ping -n 2 127.0.0.1 >nul

echo [1/3] Done. Ports cleared.

echo.
echo [2/3] Starting Python AI Service on port 8000...
start "AI Service (Port 8000)" cmd /k "cd /d %~dp0 && python ai_service/app.py"
ping -n 4 127.0.0.1 >nul

echo [2/3] AI Service starting...

echo.
echo [3/3] Starting Spring Boot Backend on port 8081...
start "Spring Boot Backend (Port 8081)" cmd /k "cd /d %~dp0backend && .\mvnw.cmd spring-boot:run"

echo.
echo ==============================================
echo  All servers are starting in separate windows.
echo.
echo  AI Service:  http://localhost:8000/health
echo  Backend API: http://localhost:8081/api/simulations
echo  Frontend:    http://localhost:3000  (run npm start separately)
echo ==============================================
pause
