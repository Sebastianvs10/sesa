@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup script for Windows
@REM ----------------------------------------------------------------------------

@echo off
if "%HOME%" == "" set "HOME=%HOMEDRIVE%%HOMEPATH%"

set ERROR_CODE=0
set MAVEN_PROJECTBASEDIR=%~dp0
if not "%MAVEN_PROJECTBASEDIR%" == "" goto endDetectBaseDir
set MAVEN_PROJECTBASEDIR=%CD%
:endDetectBaseDir
if "%MAVEN_PROJECTBASEDIR:~-1%" == "\" set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"

set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

set DOWNLOAD_URL="https://repo.maven.apache.org/maven2/io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar"
if exist "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties" (
  for /F "tokens=1,2 delims==" %%A in ("%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties") do (
    if "%%A"=="wrapperUrl" set DOWNLOAD_URL=%%B
  )
)

if not exist %WRAPPER_JAR% (
  if not "%MVNW_REPOURL%" == "" set DOWNLOAD_URL="%MVNW_REPOURL%/io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar"
  echo Downloading Maven Wrapper...
  powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile %WRAPPER_JAR% -UseBasicParsing}"
  if exist %WRAPPER_JAR% echo Maven Wrapper downloaded.
)

set MAVEN_CMD_LINE_ARGS=%*
set JAVACMD=java
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set JAVACMD="%JAVA_HOME%\bin\java.exe"
%JAVACMD% -classpath %WRAPPER_JAR% "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %WRAPPER_LAUNCHER% %MAVEN_CONFIG% %*
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
exit /B %ERROR_CODE%
