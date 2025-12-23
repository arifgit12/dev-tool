@echo off
REM ====== Configure your actual install paths ======
set "JAVA8=C:\Program Files\Java\jdk1.8.0_202"
set "JAVA11=C:\Program Files\Java\jdk-11"
set "JAVA17=C:\Program Files\Java\jdk-17"

if "%~1"=="" (
  echo Usage: switch-java 8 ^| 11 ^| 17
  exit /b 1
)

if "%~1"=="8" (
  set "NEWJAVA=%JAVA8%"
) else if "%~1"=="11" (
  set "NEWJAVA=%JAVA11%"
) else if "%~1"=="17" (
  set "NEWJAVA=%JAVA17%"
) else (
  echo Invalid option. Use 8, 11, or 17.
  exit /b 1
)

if not exist "%NEWJAVA%\bin\java.exe" (
  echo Not found: "%NEWJAVA%\bin\java.exe"
  exit /b 1
)

REM --- Set JAVA_HOME for this CMD session
set "JAVA_HOME=%NEWJAVA%"

REM --- Strip known Java entries from PATH
REM Remove Oracle javapath shims
set "PATH=%PATH:;C:\Program Files\Common Files\Oracle\Java\javapath;=;%"

REM Remove previous JDK bin entries
set "PATH=%PATH:;%JAVA8%\bin;=;%"
set "PATH=%PATH:;%JAVA11%\bin;=;%"
set "PATH=%PATH:;%JAVA17%\bin;=;%"

REM --- Prepend the new JDK bin
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo.
echo Switched JAVA_HOME = %JAVA_HOME%
for %%I in (java.exe) do echo Using java at: %%~$PATH:I
echo.
java -version