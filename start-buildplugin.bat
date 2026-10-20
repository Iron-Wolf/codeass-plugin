@echo off
setlocal

:: Set JAVA_HOME to IntelliJ's bundled JBR 21 to avoid older/32-bit system Java issues
set "JAVA_HOME=%userprofile%\AppData\Local\Programs\idea-2025.3.3.win\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ======================================================
echo  Launching IntelliJ IDEA Plugin Sandbox...
echo  JAVA_HOME: %JAVA_HOME%
echo ======================================================

call "%~dp0gradlew.bat" buildPlugin %*

endlocal
