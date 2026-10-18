@echo off
setlocal
cd /d "%~dp0"
java -version
if errorlevel 1 (
  echo ERROR: a JDK 25 is required on the PATH ^(for example Eclipse Temurin 25^).
  pause
  exit /b 1
)
rem The wrapper downloads Gradle 9.7.1 the first time; no need to install Gradle.
call gradlew.bat clean build --stacktrace
if errorlevel 1 (
  echo The build failed. Check the error, especially Java and mixins.
  pause
  exit /b 1
)
echo Done: the .jar is in the release folder.
pause
