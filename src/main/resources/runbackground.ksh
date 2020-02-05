# ****************************************************************************
# * 
# * Copyright (c) 2012 Activant Solutions Inc. All Rights Reserved. 
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
# * $Workfile:   runbackground.ksh  $
# * $Id$
# * 
# ****************************************************************************
# * 
# * runbackground.ksh - 
# *   Executes the BackGround Task used to send observations and Deposits.
# *   Must be called with the first argument set to the full or relative
# *   path of the lib directory where the SDK jar files can be found.
# * 
# ****************************************************************************

#-----------------------------------------------------------------------------
#  showusage - shows the proper usage for this script which runs the 
#              AConneXCL BackGroundTask.
#-----------------------------------------------------------------------------
showusage() {

     echo ".=================================================================
     echo ". runbackground.ksh - Runs the BackGround Task that sends Observations
     echo ".                     and Deposits to the Repository.
     echo ".
     echo ". Usage:  runbackground libdir
     echo ".
     echo ". Where libdir is full path or relative path of the directory
     echo ".              that holds the jar files. The relative path is
     echo ".              relative to the current working directory.
     echo ".
     echo ". Examples:
     echo ".   runbackground ../lib
     echo ".   runbackground /aposdk/java2.0.0.007/lib
     echo ".=================================================================
     echo " "

}

#----------------------
#       MAIN 
#----------------------
  
clear
# if no parameters show how to use this script
#echo "$#" 

if [ $# = 0 ] 
then
    showusage
    exit
fi


# detect that JAVA_HOME is set and reachable
if [ -z $JAVA_HOME ]
then
    echo "Please set JAVA_HOME environment variable"
    echo "to the root directory of a 1.3, 1.4 or 1.5 JDK installation"
    exit
else
    echo "Using $JAVA_HOME as JDK"
fi

# assign executable for running example applications
if [ -a $JAVA_HOME/bin/java ] 
then
    JAVAEXE=$JAVA_HOME/bin/java
else
    echo "Something wrong with JDK. Can't find $JAVA_HOME/bin/java"
    exit
fi

# if we get here then there was > 1 parameter and JAVA_HOME was detected 
# so take the first parameter and assign it
# the first arg must be the full or relative path of the lib directory
# where the jar files can be found.
LIBDIR=$1
# now shift parameter stack down
shift

# Loop until all parameters are used up
while [ "$1" != "" ]
do
    # Shift all the parameters down by one
    OTHER_ARGS="$OTHER_ARGS $1"
    shift
    
done

#echo "$LIBDIR"
#echo "$OTHER_ARGS"

EXAMPLECP=.:$LIBDIR:$LIBDIR/*
#for i in $LIBDIR/*.jar
#do
#  EXAMPLECP=$EXAMPLECP:$i
#done

EX_CMD="$JAVAEXE -cp $EXAMPLECP com.activant.aconnex.appsdk.BackGroundTask $OTHER_ARGS"

echo $EX_CMD

$EX_CMD

