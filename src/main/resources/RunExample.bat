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
@rem * $Workfile:   RunExample.bat  $
@rem * $Id$
@rem * 
@rem ****************************************************************************
@rem * 
@rem * RunExample.bat - 
@rem *   Executes an AConneX Application SDK example program.
@rem *   Should be called from a command line CD'd into the examples directory.
@rem *   See RunExampleHelp.txt for usage documentation.
@rem * 
@rem ****************************************************************************

@echo off

REM setlocal  # too bad this doesn't work on Win98
if "%1"=="" goto usage

set CLASS_NAME=%1
shift
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
set CP_LIB=%EXAMPLEDIR%\..\lib
set AUX_CP=.;%CP_LIB%
for %%i in ("%CP_LIB%\*.jar") do call "%EXAMPLEDIR%\lcp.bat" %%i

@rem set EXAMPLECP=.;..\lib\appsdk.jar
set EXAMPLECP=%AUX_CP%

set EX_CMD=%JAVAEXE% -cp %EXAMPLECP% com.activant.aconnex.appsdk.examples.%CLASS_NAME% %OTHER_ARGS%

echo %EX_CMD%
%EX_CMD%
goto end

:usage
type RunExampleHelp.txt
goto end

:end
REM endlocal  # too bad this doesn't work on Win98



