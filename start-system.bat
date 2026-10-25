@echo off
setlocal

set "APP_DIR=%~dp0server\server"
set "BASE_URL=http://localhost:8080"
set "STATUS_URL=%BASE_URL%/api/parking/status"

if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
)
if not defined ESP32_SERIAL_PORT set "ESP32_SERIAL_PORT=COM3"
if not defined ESP32_GATE_CONTROL_TOKEN set "ESP32_GATE_CONTROL_TOKEN=guard123"

if not exist "%APP_DIR%\pom.xml" (
    echo Smart Parking backend was not found at "%APP_DIR%".
    pause
    exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
    echo Java is not installed or is missing from PATH.
    pause
    exit /b 1
)

powershell -NoProfile -Command "try { $response = Invoke-WebRequest -Uri '%STATUS_URL%' -TimeoutSec 2 -UseBasicParsing; if ($response.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1
if errorlevel 1 (
    echo Starting Smart Parking backend...
    start "Smart Parking Backend" /D "%APP_DIR%" cmd /k "mvnw.cmd spring-boot:run"
) else (
    echo Smart Parking backend is already running.
)

set /a ATTEMPTS=0
:wait_for_backend
powershell -NoProfile -Command "try { $response = Invoke-WebRequest -Uri '%STATUS_URL%' -TimeoutSec 2 -UseBasicParsing; if ($response.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1
if not errorlevel 1 goto backend_ready
set /a ATTEMPTS+=1
if %ATTEMPTS% geq 60 goto backend_timeout
timeout /t 2 /nobreak >nul
goto wait_for_backend

:backend_ready
echo Backend is ready.
start "" "%BASE_URL%/driver"
start "" "%BASE_URL%/security-guard"
echo.
echo Driver dashboard: %BASE_URL%/driver
echo Security dashboard: %BASE_URL%/security-guard
echo The unchanged four-slot firmware has a fixed SERVER_URL:
echo http://192.168.29.59:8080
echo The ESP32 must be able to reach that address on port 8080.
echo Current computer LAN address:
powershell -NoProfile -Command "$configurations = Get-NetIPConfiguration; foreach ($configuration in $configurations) { if ($configuration.IPv4DefaultGateway) { $configuration.IPv4Address.IPAddress; break } }"
echo Do not edit the firmware. Use an appropriate static IP reservation or network route if the addresses differ.
echo.
pause
exit /b 0

:backend_timeout
echo Backend did not become ready. Check the Smart Parking Backend window for errors.
pause
exit /b 1
