@rem ****************************************************************************
@rem * 
@rem * Copyright (c) 2000-2004 Activant Solutions Inc. All Rights Reserved. 
@rem * 
@rem * ACTIVANT SOLUTIONS INC. MAKES NO REPRESENTATIONS OR WARRANTIES 
@rem * ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED,  
@rem * INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, 
@rem * FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. 
@rem * 
@rem * ACTIVANT SOLUTIONS INC. SHALL NOT BE LIABLE FOR ANY DAMAGES 
@rem * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING 
@rem * THIS SOFTWARE OR ITS DERIVATIVES. 
@rem * 
@rem * $Workfile:   RunBackGround.bat  $
@rem * $Id$
@rem * 
@rem ****************************************************************************
@rem * 
@rem * RunBackGround.bat - 
@rem *   Executes the BackGround Task used to send observations and Deposits.
@rem *   Must be called with the first argument set to the full or relative
@rem *   path of the lib directory where the SDK jar files can be found.
@rem * 
@rem ****************************************************************************

@echo off

@setlocal
if "%1"=="" goto usage

@REM the first arg must be the full or relative path of the lib directory
@REM where the jar files can be found.
set LIBDIR=%1

set OTHER_ARGS=

:setupArgs
if %1a==a goto doneArgs
set OTHER_ARGS=%OTHER_ARGS% %1
shift
goto setupArgs

:doneArgs

set JAVAEXE="%JAVA_HOME%\bin\java.exe"
%JAVAEXE% -version 2>&1 1>nul
if NOT ERRORLEVEL 1 goto run

echo **
echo ** Error: Cannot invoke Java executable '%JAVAEXE%'
echo ** Please ensure JAVA_HOME is a JDK directory.
echo **
goto end

:run
cls

if "%EXAMPLEDIR%"=="" set EXAMPLEDIR=.
if not exist "%EXAMPLEDIR%\lcp.bat" goto nolcpbat

@rem set CP_LIB=%EXAMPLEDIR%\..\lib
set CP_LIB=%LIBDIR%
set AUX_CP=.;%CP_LIB%
for %%i in ("%CP_LIB%\*.jar") do call "%EXAMPLEDIR%\lcp.bat" %%i

set EXAMPLECP=%AUX_CP%

set EX_CMD=%JAVAEXE% -cp %EXAMPLECP% com.activant.aconnex.appsdk.BackGroundTask %OTHER_ARGS%

echo %EX_CMD%
%EX_CMD%
goto end

:nolcpbat
echo.
echo lcp.bat not found in current working directory.
echo.

:usage
type RunBackGroundHelp.txt
goto end

:end
@endlocal



