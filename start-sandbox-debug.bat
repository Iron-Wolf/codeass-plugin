@echo off
setlocal

:: Set JAVA_HOME to IntelliJ's bundled JBR 21 to avoid older/32-bit system Java issues
set "JAVA_HOME=%userprofile%\AppData\Local\Programs\idea-2025.3.3.win\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ======================================================
echo  Launching IntelliJ IDEA Plugin Sandbox with Debug JVM...
echo  Sandbox IDE will pause at start and listen on port 5005
echo ======================================================

call "%~dp0gradlew.bat" runIde --debug-jvm %*

endlocal
