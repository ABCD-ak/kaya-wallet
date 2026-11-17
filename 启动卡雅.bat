@echo off
title Kaya Launcher
echo ========================================
echo   Kaya Wallet - Quick Launcher
echo ========================================
echo.

curl -s -f http://localhost:8080/api/health >nul 2>&1
if not errorlevel 1 goto backend_ok
echo [1/2] Starting backend (Spring Boot :8080) ...
start "Kaya-Backend" cmd /k "cd /d D:\code\11.12\kaya-backend && mvn.cmd jetty:run"
goto backend_done
:backend_ok
echo [OK] Backend already running on :8080
:backend_done

curl -s -f http://localhost:5173 >nul 2>&1
if not errorlevel 1 goto frontend_ok
echo [2/2] Starting frontend (Vite :5173) ...
start "Kaya-Frontend" cmd /k "cd /d D:\code\11.12\app && npm.cmd run dev"
goto frontend_done
:frontend_ok
echo [OK] Frontend already running on :5173
:frontend_done

echo.
echo Waiting for services to be ready ...
echo.

set /a count=0
:wait_loop
ping -n 4 127.0.0.1 >nul
set /a count+=1
curl -s -f http://localhost:8080/api/health >nul 2>&1
if not errorlevel 1 goto ready
if %count% geq 20 goto timeout
goto wait_loop

:ready
echo Backend ready, opening browser ...
start "" http://localhost:5173
echo.
echo ========================================
echo   Done!
echo   Frontend: http://localhost:5173
echo   Backend:  http://localhost:8080/api
echo ========================================
echo.
pause
exit /b

:timeout
echo Timeout. Please check backend window.
start "" http://localhost:5173
pause
exit /b
