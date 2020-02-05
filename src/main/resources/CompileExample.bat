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
@rem * $Workfile:   CompileExample.bat  $
@rem * $Id$
@rem * 
@rem ****************************************************************************
@rem * 
@rem * CompileExample.bat - 
@rem *   Compiles the AppSDK Example programs
@rem *   Should be called from a command line CD'd into the examples directory.
@rem *   The variable JAVA_HOME must be set to a valid JDK directory (e.g. c:\jdk1.3)
@rem * 
@rem ****************************************************************************

@echo off

REM setlocal  # too bad this doesn't work on Win98

set JAVAEXE=%JAVA_HOME%\bin\java.exe
%JAVAEXE% -version 2>&1 1>nul
if NOT ERRORLEVEL 1 goto run

echo **
echo ** Error: Cannot invoke Java executable '%JAVAEXE%'
echo ** Please ensure JAVA_HOME is a JDK directory.
echo **
goto end

:run
cls
set JAVACEXE=%JAVA_HOME%\bin\javac.exe
set EXAMPLECP=.;..\lib\appsdk.jar;..\lib\xerces.jar;..\lib\jsse.jar;..\lib\jnet.jar;..\lib\jcert.jar;..\lib\Util.jar

set EX_CMD=%JAVACEXE% -classpath %EXAMPLECP% .\com\activant\aconnex\appsdk\examples\*.java

echo %EX_CMD%
%EX_CMD%
goto end

:end
REM endlocal  # too bad this doesn't work on Win98




