@echo off
setlocal EnableExtensions

rem Yantra Windows release-build setup.
rem Usage:
rem   build-release.bat          Builds an installable release APK.
rem   build-release.bat apk      Builds an installable release APK.
rem   build-release.bat bundle   Builds a Play Store Android App Bundle.

set "PROJECT_ROOT=%~dp0"
cd /d "%PROJECT_ROOT%"

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: Android Studio Java was not found at:
    echo   %JAVA_HOME%
    exit /b 1
)

if not exist "%ANDROID_HOME%" (
    echo ERROR: Android SDK was not found at:
    echo   %ANDROID_HOME%
    exit /b 1
)

if not exist "%PROJECT_ROOT%gradlew.bat" (
    echo ERROR: gradlew.bat was not found in:
    echo   %PROJECT_ROOT%
    exit /b 1
)

if not exist "%GRADLE_USER_HOME%\gradle.properties" (
    echo ERROR: Signing properties were not found at:
    echo   %GRADLE_USER_HOME%\gradle.properties
    exit /b 1
)

for %%P in (YANTRA_KEYSTORE YANTRA_KEYSTORE_PASSWORD YANTRA_KEY_ALIAS YANTRA_KEY_PASSWORD) do (
    findstr /B /C:"%%P=" "%GRADLE_USER_HOME%\gradle.properties" >nul
    if errorlevel 1 (
        echo ERROR: %%P is missing from %GRADLE_USER_HOME%\gradle.properties
        exit /b 1
    )
)

for /F "tokens=1,* delims==" %%A in ('findstr /B /C:"YANTRA_KEYSTORE=" "%GRADLE_USER_HOME%\gradle.properties"') do set "YANTRA_KEYSTORE_PATH=%%B"
if not exist "%YANTRA_KEYSTORE_PATH%" (
    echo ERROR: The YANTRA_KEYSTORE file does not exist:
    echo   %YANTRA_KEYSTORE_PATH%
    exit /b 1
)

set "SDK_FORWARD=%ANDROID_HOME:\=/%"
> "%PROJECT_ROOT%local.properties" echo sdk.dir=%SDK_FORWARD%

set "BUILD_KIND=%~1"
if "%BUILD_KIND%"=="" set "BUILD_KIND=apk"

if /I "%BUILD_KIND%"=="apk" (
    set "GRADLE_TASK=:app:assembleRelease"
    set "OUTPUT_PATH=app\build\outputs\apk\release\app-release.apk"
) else if /I "%BUILD_KIND%"=="bundle" (
    set "GRADLE_TASK=:app:bundleRelease"
    set "OUTPUT_PATH=app\build\outputs\bundle\release\app-release.aab"
) else (
    echo ERROR: Unknown build type "%BUILD_KIND%". Use apk or bundle.
    exit /b 1
)

echo.
echo Yantra release environment is ready.
echo   Java:  %JAVA_HOME%
echo   SDK:   %ANDROID_HOME%
echo   Gradle user home: %GRADLE_USER_HOME%
echo   Build: %BUILD_KIND%
echo.

call "%PROJECT_ROOT%gradlew.bat" --stop >nul 2>&1
call "%PROJECT_ROOT%gradlew.bat" %GRADLE_TASK% --rerun-tasks --no-daemon
if errorlevel 1 (
    echo.
    echo ERROR: Yantra release build failed.
    exit /b 1
)

echo.
echo BUILD SUCCESSFUL
echo Output: %PROJECT_ROOT%%OUTPUT_PATH%
exit /b 0
