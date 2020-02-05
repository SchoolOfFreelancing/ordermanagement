# ****************************************************************************
# * 
# * Copyright (c) 2000-2001 Activant Solutions Inc. All Rights Reserved. 
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
# * $Workfile:   runexample.ksh  $
# * $Id$
# * 
# ****************************************************************************
# * 
# * runexample.ksh - 
# *   Executes an AConneX Application SDK example program.
# *   Should be called from the examples directory.
# *   This script relies on the JAVA_HOME env variable set to a valid JDK.
# * 
# ****************************************************************************

#-----------------------------------------------------------------------------
#  showusage - shows the proper usage for this script which runs the 
#              AConneX App SDK exmaples. 
#-----------------------------------------------------------------------------
showusage() {

     echo " Usage: runexample.ksh group_name function_name"
     echo ""
     echo " Groups and their functions are:  "
     echo " AdminManagement:"
     echo "     partnerTypes, listServices, listProviders, listAffiliations,"
     echo "     listAssociations, legalAgreement"
     echo " PartnerManagement:"
     echo "     getPartner, partnerStatus, checkPartner,"
     echo "     listPartners, getGroupPartners,"
     echo "     createPartner, updatePartner,"
     echo "     listRelationships, checkRelationship,"
     echo "     getRelationship, createRelationship,"
     echo "     updateRelationship, removeRelationship,"
     echo " PartOrderManagement:"
     echo "     partInquiry, partOrder, returnParts,  stockOrder,"
     echo "     listOrders, orderDetail, orderDetails,"
     echo "     productCoverage, productLineMap, catalogProfile"
     echo " "
     echo " Options that apply to any group are:"
     echo "  all          Runs all functions in the group (this is the default"
     echo "               if no functions are supplied)."
     echo "  standalone   Run in standalone mode using local test data for"
     echo "               responses. Overrides property file settings."
     echo "  connected    Run in connected mode requesting responses from the"
     echo "               AConneX Gateway. Overrides the property file setting"
     echo " "
     echo " Examples:"
     echo " runexample.ksh PartOrderManagement partInquiry partOrder standalone"
     echo " runexample.ksh AdminManagement all"
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
CLASS_NAME=$1
#echo $CLASS_NAME

# now shift parameter stack down
shift

# Loop until all parameters are used up
while [ "$1" != "" ]
do
    # Shift all the parameters down by one
    FUNC_NAME="$FUNC_NAME $1"
    shift
    
done

#echo "$CLASS_NAME"
#echo "$FUNC_NAME"

# simple check for the .class file(s) to see if they are compiled.
if [ -s "./com/activant/aconnex/appsdk/examples/$CLASS_NAME.class" ]
then 
    echo ""    
else
    echo "Can't find ./com/activant/aconnex/appsdk/examples/$CLASS_NAME.class"
    echo "Please run ./compileexample.ksh"
    exit
fi

EXAMPLECP=.:../lib/*
#for i in ../lib/*.jar
#do
#  EXAMPLECP=$EXAMPLECP:$i
#done

EX_CMD="$JAVAEXE -cp $EXAMPLECP com.activant.aconnex.appsdk.examples.$CLASS_NAME $FUNC_NAME"

echo $EX_CMD

$EX_CMD

