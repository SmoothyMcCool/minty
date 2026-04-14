@echo off
setlocal enabledelayedexpansion

:: ============================================================
:: Record script start time and resolve today's catalina log
:: ============================================================
for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set DATETIME=%%I

for /f "tokens=* delims=" %%T in ('powershell -Command "Get-Date -Format \"yyyy-MM-dd HH:mm:ss\""') do set SCRIPT_START_DATE=%%T
echo [INFO] Script started at %SCRIPT_START_DATE%

set CATALINA_LOG=D:\projects\Tomcat\logs\catalina.!DATETIME:~0,4!-!DATETIME:~4,2!-!DATETIME:~6,2!.log
echo [INFO] Log file: !CATALINA_LOG!

:: ============================================================
:: Step 1: Maven build
:: ============================================================
echo.
echo [1/5] Running Maven build...
cd /d "D:\projects\Minty\webapp\backend\solution"

call mvn clean package
if %ERRORLEVEL% neq 0 (
    echo.
    echo [FAILED] Maven build failed. Aborting.
    pause
    exit /b 1
)
echo [OK] Maven build passed.

:: ============================================================
:: Step 2: Wait for Tomcat to finish redeploying Minty.war
:: ============================================================
echo.
echo [2/5] Waiting for Tomcat to redeploy Minty.war...

set DEPLOY_MSG=HostConfig.deployWAR Deployment of web application archive
set TIMEOUT_SECS=120
set ELAPSED=0

:WAIT_LOOP
timeout /t 2 /nobreak >nul
set /a ELAPSED+=2

powershell -Command ^
  "$startTime = [datetime]'%SCRIPT_START_DATE%';" ^
  "$found = Get-Content '%CATALINA_LOG%' | Where-Object {" ^
  "  $_ -match '%DEPLOY_MSG%' -and" ^
  "  $_ -match '^(\d{2}-\w{3}-\d{4} \d{2}:\d{2}:\d{2})' -and" ^
  "  [datetime]::ParseExact($matches[1], 'dd-MMM-yyyy HH:mm:ss', $null) -gt $startTime" ^
  "};" ^
  "if ($found) { exit 0 } else { exit 1 }" >nul 2>&1

if !ERRORLEVEL! equ 0 (
    echo [OK] Tomcat redeployment complete.
    goto PLAYWRIGHT
)

if !ELAPSED! geq %TIMEOUT_SECS% (
    echo.
    echo [FAILED] Timed out waiting for Tomcat redeployment after %TIMEOUT_SECS% seconds.
    pause
    exit /b 1
)

echo Waiting... (!ELAPSED!s elapsed)
goto WAIT_LOOP

:PLAYWRIGHT
:: ============================================================
:: Step 3: Playwright tests
:: ============================================================
echo.
echo [3/5] Running Playwright tests...
cd /d "D:\projects\Minty\webapp\frontend\app\src"

call npx playwright test
if %ERRORLEVEL% neq 0 (
    echo.
    echo [FAILED] Playwright tests failed. Aborting.
    pause
    exit /b 1
)
echo [OK] Playwright tests passed.

:: ============================================================
:: Step 4: Clear Minty-GitHub (except .git)
:: ============================================================
echo.
echo [4/5] Clearing D:\projects\Minty-GitHub (keeping .git)...

for /d %%D in ("D:\projects\Minty-GitHub\*") do (
    if /i not "%%~nxD"==".git" rd /s /q "%%D"
)
for %%F in ("D:\projects\Minty-GitHub\*") do (
    del /q "%%F"
)
echo [OK] Minty-GitHub cleared.

:: ============================================================
:: Step 5: Copy files to Minty-GitHub
:: ============================================================
echo.
echo [5/5] Copying files to D:\projects\Minty-GitHub...

robocopy "D:\projects\Minty" "D:\projects\Minty-GitHub" /E /PURGE /XD "D:\projects\Minty\.git" /XD "D:\projects\Minty\.metadata" /XD "D:\projects\Minty\webapp\frontend\app\.angular"

:: Robocopy exit codes 0-7 are success (8+ are errors)
if %ERRORLEVEL% geq 8 (
    echo.
    echo [FAILED] File copy failed with error code %ERRORLEVEL%.
    pause
    exit /b 1
)

echo.
echo [DONE] All steps completed successfully.
pause
exit /b 0