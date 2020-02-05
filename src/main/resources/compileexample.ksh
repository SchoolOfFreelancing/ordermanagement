#!/bin/ksh

# ****************************************************************************
# * 
# * Copyright (c) 2000-2004 Activant Solutions Inc. All Rights Reserved. 
# * 
# * ACTIVANT SOLUTIONS INC. MAKES NO REPRESENTATIONS OR WARRANTIES 
# * ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED,  
# * INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, 
# * FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. 
# * 
# * ACTIVANT SOLUTIONS INC. SHALL NOT BE LIABLE FOR ANY DAMAGES 
# * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING 
# * THIS SOFTWARE OR ITS DERIVATIVES. 
# * 
# * $Workfile:   compileexample.ksh $
# * $Id$
# * 
# ****************************************************************************
# * 
# * compileexample.ksh -
# *   Compiles the AppSDK Example programs
# *   Should be run in the examples directory.
# *   The variable JAVA_HOME must be set to a valid JDK (1.3) directory 
# * 
# ****************************************************************************

#-------------------------------------------------------------------
#  Check for JAVA_HOME and let the user know to set it
#  in order for the examples to compile.
#-------------------------------------------------------------------

if [ -z $JAVA_HOME ]
then 
    echo "Please set JAVA_HOME environment variable"  
    echo "to the root directory of a 1.3 JDK installation"
    exit
else
    echo "Using $JAVA_HOME as JDK"
fi

 
#-------------------------------------------------------------------
#   We assume the jdk exists by the env variable 
#   so set JAVAEXE=$JAVA_HOME/bin/java
#   and JAVAC=$JAVA_HOME/bin/javac.
#-------------------------------------------------------------------

if [ -a $JAVA_HOME/bin/java ]
then 
    JAVAEXE=$JAVA_HOME/bin/java
else
    echo "Something wrong with JDK, can't locate $JAVA_HOME/bin/java"
    exit
fi

if [ -a $JAVA_HOME/bin/javac ]
then
    JAVAC=$JAVA_HOME/bin/javac
else
    echo "Something wrong with JDK, can't locate $JAVA_HOME/bin/javac"
    exit
fi

clear
EXAMPLECP=.:../lib/appsdk.jar:../lib/xerces.jar:../lib/jsse.jar:../lib/jnet.jar:../lib/jcert.jar:../lib/Util.jar

echo $EXAMPLECP

EX_CMD="$JAVAC -classpath $EXAMPLECP ./com/activant/aconnex/appsdk/examples/*.java"


#echo $EX_CMD

$EX_CMD

echo "Done compiling Example apps"
