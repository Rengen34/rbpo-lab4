@echo off
setlocal
cd /d "%~dp0"

for /f "delims=" %%J in ('powershell -NoProfile -Command "[Environment]::GetEnvironmentVariable('JAVA_HOME','User')"') do set "JAVA_HOME=%%J"
if exist "%JAVA_HOME%\bin\java.exe" set "PATH=%JAVA_HOME%\bin;%PATH%"
where java >nul 2>&1
if errorlevel 1 (
    echo Java was not found. Install JDK 17 or newer, then try again.
    pause
    exit /b 1
)

if not defined RBPO_DB_OWNER_PASSWORD (
    for /f "delims=" %%P in ('powershell -NoProfile -Command "$s=Read-Host 'Database owner password' -AsSecureString; $p=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($s); try {[Runtime.InteropServices.Marshal]::PtrToStringBSTR($p)} finally {[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($p)}"') do set "RBPO_DB_OWNER_PASSWORD=%%P"
)
if not defined RBPO_DB_PASSWORD (
    for /f "delims=" %%P in ('powershell -NoProfile -Command "$s=Read-Host 'Application database password' -AsSecureString; $p=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($s); try {[Runtime.InteropServices.Marshal]::PtrToStringBSTR($p)} finally {[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($p)}"') do set "RBPO_DB_PASSWORD=%%P"
)
if not defined RBPO_MANAGER_PASSWORD (
    for /f "delims=" %%P in ('powershell -NoProfile -Command "$s=Read-Host 'Manager password' -AsSecureString; $p=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($s); try {[Runtime.InteropServices.Marshal]::PtrToStringBSTR($p)} finally {[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($p)}"') do set "RBPO_MANAGER_PASSWORD=%%P"
)

if not defined RBPO_DB_OWNER_PASSWORD exit /b 1
if not defined RBPO_DB_PASSWORD exit /b 1
if not defined RBPO_MANAGER_PASSWORD exit /b 1

echo Manager login: manager
echo First get a CSRF token: http://127.0.0.1:8080/api/auth/csrf
echo Use postman\lab4.postman_collection.json for the complete scenario.
echo.
call mvnw.cmd spring-boot:run
pause
