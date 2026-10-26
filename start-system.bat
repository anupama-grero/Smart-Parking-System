@echo off
setlocal

set "APP_DIR=%~dp0server\server"
set "BASE_URL=http://localhost:8080"
set "STATUS_URL=%BASE_URL%/api/parking/status"

if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
)
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
echo Configure ESP32_WIFI_BASE_URL=http://^<ESP32-LAN-IP^> and
echo ESP32_GATE_CONTROL_TOKEN before starting this script to enable gate control.
echo Configure the firmware SERVER_URL with this computer's LAN address and port 8080.
echo Current computer LAN address:
powershell -NoProfile -Command "$configurations = Get-NetIPConfiguration; foreach ($configuration in $configurations) { if ($configuration.IPv4DefaultGateway) { $configuration.IPv4Address.IPAddress; break } }"
echo Reserve a stable LAN IP for the ESP32 and set ESP32_WIFI_BASE_URL to that address.
echo.
pause
exit /b 0

:backend_timeout
echo Backend did not become ready. Check the Smart Parking Backend window for errors.
pause
exit /b 1
