/*
 * Copyright (c) 2001-2004 Activant Solutions Inc.  All Rights Reserved.
 *
 * ACTIVANT SOLUTIONS INC.  MAKES NO REPRESENTATIONS OR WARRANTIES 
 * ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. 
 *
 * ACTIVANT SOLUTIONS INC. SHALL NOT BE LIABLE FOR ANY DAMAGES 
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING 
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * $Id$
 */
package com.ordermanagement.ordermanagement;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;
import com.activant.aconnex.appsdk.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.Logger;

/**
 *  Demonstrates each API through the AdminManager.
 */
public class AdminMgr {
    protected final transient Logger logger = Logger.getLogger(this.getClass().getName());
    private AdminManager      adminManager = null;
    private XMLTemplateReader templateReader = null;
    private String            resultsdir      = "./results";
    private String            resultsfilename = "adminMgr.csv";
    private String            resultsPath     = resultsdir + "/" + resultsfilename;
    private FileOutputStream  os = null;
    private PrintStream       ps = null; 
    private String            today = null;
    private String            starttime = null;
    private int               seqnum = 0;
    private String            DistUser = null;
    private String            DistPass = null;
    private int               connect_timeout  = -1;
    private int               base_timeout     = -1;
    private int               var_base_timeout = -1;
    private int               var_line_count   = 25;    
    
    // list that will contain each partner number and name in a String array
    private List partners = null;

    public AdminMgr(String[] args) {
        boolean settimeouts = false;
        try {
            System.out.println();
            boolean do_all = XMLUtil.member("all", args) || args.length==0;
            boolean acxim      = XMLUtil.member("acxim", args);
            boolean noacxim    = XMLUtil.member("noacxim", args);
            boolean gotacxim  = (acxim || noacxim);
            boolean setcreds   = XMLUtil.member("setcreds", args);
            boolean do_svcs    = do_all || XMLUtil.member("listServices", args);
            boolean do_prov    = do_all || XMLUtil.member("listProviders", args);
            boolean do_assoc   = do_all || XMLUtil.member("listAssociations", args);
            boolean do_agree   = do_all || XMLUtil.member("legalAgreement", args);
            boolean do_memb    = do_all || XMLUtil.member("listAffiliations", args);
            boolean do_types   = do_all || XMLUtil.member("partnerTypes", args) 
                                 || do_svcs || do_prov || do_assoc || do_memb; 
            String con_timeoutstr      = XMLUtil.memberAfter("con_timeout", args);
            String base_timeoutstr     = XMLUtil.memberAfter("base_timeout", args);
            String var_base_timeoutstr = XMLUtil.memberAfter("var_base_timeout", args);
            String var_line_countstr   = XMLUtil.memberAfter("var_line_count", args);
            String theRequest  = null;
            String requestFile = XMLUtil.memberAfter("Request", args);
            String user        = XMLUtil.memberAfter("user",args);
            String pass        = XMLUtil.memberAfter("pass",args);
            DistUser           = XMLUtil.memberAfter("duser",args);
            DistPass           = XMLUtil.memberAfter("dpass",args);
            
            String optStr = (do_all?" all":"") +
                            (acxim?" acxim":"") +
                            (noacxim?" noacxim":"") +
                            (theRequest!=null?" RequestFile=" + theRequest:"") +
                            (user!=null?" user "+ user:"") +
                            (pass!=null?" pass "+ pass:"") +
                            (DistUser!=null?" duser "+ DistUser:"") +
                            (DistPass!=null?" dpass "+ DistPass:"") +
                            (setcreds?" setcreds":"") +
                            (con_timeoutstr!=null ? " con_timeout " + con_timeoutstr : "") +
                            (base_timeoutstr!=null ? " base_timeout " + base_timeoutstr : "") +
                            (var_base_timeoutstr!=null ? " var_base_timeout " + var_base_timeoutstr : "") +
                            (var_line_countstr!=null ? " var_line_count " + var_line_countstr : "") +
                            (do_types?" partnerTypes":"") + 
                            (do_svcs?" listServices":"") + 
                            (do_prov?" listProviders":"") + 
                            (do_assoc?" listAssociations":"") +
                            (do_memb?" listAffiliations":"") +
                            (do_agree?" legalAgreement":"");  
            System.out.println("Options: " + optStr);

            // Get the AppBuilder and pass in the Broker user and password if they
            // were given on the command line to override what was in the
            // AppSDK.properties file.
            AppBuilder appBuilder = null;
            if(user != null && pass != null)
                appBuilder = new AppBuilder(user,pass);
            else
                appBuilder = new AppBuilder();

            connect_timeout  = appBuilder.getConnect_timeout();
            base_timeout     = appBuilder.getBase_timeout();
            var_base_timeout = appBuilder.getVar_base_timeout();
            var_line_count   = appBuilder.getVar_line_count();

            if (con_timeoutstr != null)
            {
                connect_timeout = Integer.parseInt(con_timeoutstr);
                settimeouts = true;
            }
            if (base_timeoutstr != null)
            {
                base_timeout = Integer.parseInt(base_timeoutstr);
                settimeouts = true;
            }
            if (var_base_timeoutstr != null)
            {
                var_base_timeout = Integer.parseInt(var_base_timeoutstr);
                settimeouts = true;
            }
            if (var_line_countstr != null)
            {
                var_line_count = Integer.parseInt(var_line_countstr);
                settimeouts = true;
            }
                            
            // if we got an acxim setting on the command line then
            // if the acxim setting in the appBuilder is not the same as acxim
            // then we need to set the acxim setting in the appBuilder.
            if(gotacxim) {
                if((acxim & !appBuilder.getACXIM()) || (!acxim && appBuilder.getACXIM())) {
                    appBuilder.setACXIM(acxim);
                }
            }
            adminManager = appBuilder.getAdminManager();
            templateReader = new XMLTemplateReader(appBuilder);

            // setcreds is used to test the use of a user and pass set with
            // setCredentials(). DistUser and DistPass are not necessarily
            // Distributed buy partner creds. They could be Centralized broker creds as well.
            if(setcreds && DistUser != null && DistPass != null) {
                logger.info("Calling setCredentials(" + DistUser + "," + DistPass + ")");
                adminManager.setCredentials(DistUser,DistPass);
            }

            if (settimeouts)
            {
                adminManager.setTimeouts(connect_timeout, base_timeout, var_base_timeout, var_line_count);
            }
            // We have no standalone data files for these requests so force to
            // connected mode for now.
            adminManager.setStandaloneMode(false);
            System.out.println("Running " + (adminManager.getStandaloneMode()?"standalone":"connected"));
            System.out.println();

            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy'-'MM'-'dd");
            SimpleDateFormat timeFormatter = new SimpleDateFormat("kk'_'mm'_'ss");
            starttime = timeFormatter.format(new Date());
            today     = dateFormatter.format(new Date());

            File   dir = new File(resultsdir);
            if(!dir.exists()) // create the results directory if need be.
                dir.mkdir();
            dir = new File(today);
            if(!dir.exists()) // create the directory for today to hold xml req and resp files
                dir.mkdir();
  
            File   csvFile  = new File(resultsPath);
            logger.info("csv filepath is " + csvFile.getAbsolutePath());
            boolean append = csvFile.exists();
            try { 
                logger.info(" opening " + resultsPath + " with append = " + append);
                os = new FileOutputStream( resultsPath, append );
                ps = new PrintStream(os); 
  
                // Add the column label header line if new file 
                if (!append) {  
                    String myHeader = "TestCase,Status,Milliseconds,ConfNum,TrackNum,Date,Broker PID," +
                                      "Buyer PID,Seller PID,Account,ExceptionCode,ExceptionMsg," +
                                      "Requestpath,Responsepath,Exceptionpath";
                    ps.println(myHeader); 
                };

            } catch  (IOException ioe) { 
                   logger.info ("Error:  Unexpected IOException writing " + resultsPath + "\n", ioe);
            } catch  (Exception e)     {
                   logger.info ("Error:  Unexpected exception writing " + resultsPath + "\n", e);
            }; 

            // They provided the name of a file that contains the request to send
            // so lets read it in to pass on to the method.
            // This option was added to be able to send a very targeted request
            // instead of relying on the setings done in this code.
            // so this option should only be used with one method at at time
            if(requestFile != null) {
                File f = new File(requestFile);
                if(f.exists()) {
                    theRequest = XMLUtil.readFile(f);
                }
            }
            
           // Admin API examples
           if (do_agree) getLegalAgreement();

            // lookup all partner types, then create a list of them.
            // must do this request since partner types used in most other requests.
            if (do_types) listPartnerTypes();

            if (do_svcs)  listApplicationServices(theRequest);
            if (do_prov)  listBusinessSystemProviders(theRequest);
            if (do_assoc) listStaticAssociations(theRequest);
            if (do_memb)  listMemberAffiliations(theRequest);
        }
        // The AConneX Gateway returned an error response 
        catch (AppSDKGatewayException sdkE) {
            System.err.println("** Gateway Exception thrown: \n");
            System.err.println(sdkE);
            System.err.println();
            System.err.println("Response XML: " + sdkE.getResponse());            
        }
        // The Standalone client has encountered an XML parsing error  
        catch (AppSDKXMLParserException sdkE) {
            System.err.println("** Standalone mode XML parsing Exception thrown: \n");
            System.err.println(sdkE);
            System.err.println();
            System.err.println("XML: " + sdkE.getDocument());            
        }
        // Other, general AppSDKExceptions 
        catch (AppSDKException sdkE) {
            System.err.println("** AppSDK Exception thrown: \n");
            System.err.println(sdkE);
        }
        // This is a programming or environment error of some sort ... 
        catch (Exception e) {
            System.err.println(e);
            System.err.println();
            e.printStackTrace(System.err);
        }
    }

    private void listPartnerTypes() throws AppSDKException, Exception {
        XMLUtil.printHeading("List Partner Types");
        RequestInfo info   = null;
        String xmlResponse = null;
        try {
            // call gateway with XML request - no request document needed
            xmlResponse = adminManager.listPartnerTypes();

            XMLUtil.printXML("List Partner Types Response:", 
            XMLUtil.prettifyString(xmlResponse));
            info = adminManager.getRequestInfo().copy();
            writeResult(info, "listPartnerTypes"/*testcase*/, "BrokerID unknown", ""/*Account*/);
        } catch (Exception e) {
            info = adminManager.getRequestInfo().copy();
            writeResult(info, "listPartnerTypes"/*testcase*/, "BrokerID unknown", ""/*Account*/);

            System.out.println("listPartnerTypes() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }

        // now set up our local list of partner types;
        // this is used by all the other requests
        partners = parsePartners(xmlResponse);
    }
    // Create a List of 2-dim String arrays with the partner number and name. 
    private List parsePartners(String xml) throws Exception {
        List list = new ArrayList();
        List values = new ArrayList();        
        Document document = XMLUtil.createDocument(xml);
        NodeList nodes = document.getElementsByTagName("PartnerTypeNumber");
        for (int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i).getFirstChild();
            String partner = node.getNodeValue();
            values.add(partner);
        }

        NodeList nodes1 = document.getElementsByTagName("PartnerTypeName");
        for (int i=0; i<nodes1.getLength(); i++) {
            Node node = nodes1.item(i).getFirstChild();
            String partnerName = node.getNodeValue();
            String[] s = new String[2];
            s[0] = (String)values.get(i);
            s[1] = partnerName;
            list.add(s);
        }        
        return list;
    }

    private void listApplicationServices(String theRequest) throws AppSDKException, Exception {
        XMLUtil.printHeading("List Application Services");
        RequestInfo info        = null;
        String      partnerType = null;
        String      partnerName = null;
        String      request     = null;
        
        if(theRequest != null && theRequest.contains("<ACXApplicationServicesListRequest>")) {
            request = theRequest;
            partnerType = XMLUtil.getTagValue("PartnerTypeNumber", request);
            partnerName = XMLUtil.getTagValue("PartnerTypeName", request);
            XMLUtil.printXML("List Application Services Request for partner type " + partnerType + " " + partnerName + ":",
                             XMLUtil.prettifyString(request));

            try {
                // call gateway with XML request
                String xmlResponse = adminManager.listApplicationServices(request);

                XMLUtil.printXML("Appliation Services Response for partner type " + partnerType + " " + partnerName + ":", 
                                 XMLUtil.prettifyString(xmlResponse));
                info = adminManager.getRequestInfo().copy();
                writeResult(info, "listApplicationServices"/*testcase*/, "BrokerID unknown", ""/*Account*/);
            } catch (Exception e) {
                info = adminManager.getRequestInfo().copy();
                writeResult(info, "listApplicationServices"/*testcase*/, "BrokerID unknown", ""/*Account*/);

                System.out.println("listApplicationServices() caught Exception: " + e);
                System.out.println("ACXTrackNum: " + info.getTrackNum());
                System.out.println("  Total time:     " + info.getTotalTime() + " millis");
                System.out.println("  Gateway access: " + info.getGatewayAccessTime());
                System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
                System.out.println();
            }
            return;
        }
        // look up services for each partner
        for (int i = 0; i < partners.size(); ++i) {
            String[] partner = (String[]) partners.get(i);
            partnerType = partner[0];
            partnerName = partner[1];

            // Add the partner type number and name to the xml template request
            request = templateReader.readTemplate("ACXApplicationServicesListRequest");
            request = XMLUtil.insertValueForTag(request, "</PartnerTypeNumber>", partnerType);
            request = XMLUtil.insertValueForTag(request, "</PartnerTypeName>", partnerName);            

            XMLUtil.printXML("List Application Services Request for partner type " + partnerType + " " + partnerName + ":",
                             XMLUtil.prettifyString(request));

            try {
                // call gateway with XML request
                String xmlResponse = adminManager.listApplicationServices(request);

                XMLUtil.printXML("Appliation Services Response for partner type " + partnerType + " " + partnerName + ":", 
                                 XMLUtil.prettifyString(xmlResponse));
                info = adminManager.getRequestInfo().copy();
                writeResult(info, "listApplicationServices"/*testcase*/, "BrokerID unknown", ""/*Account*/);
            } catch (Exception e) {
                info = adminManager.getRequestInfo().copy();
                writeResult(info, "listApplicationServices"/*testcase*/, "BrokerID unknown", ""/*Account*/);

                System.out.println("listApplicationServices() caught Exception: " + e);
                System.out.println("ACXTrackNum: " + info.getTrackNum());
                System.out.println("  Total time:     " + info.getTotalTime() + " millis");
                System.out.println("  Gateway access: " + info.getGatewayAccessTime());
                System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
                System.out.println();
            }
        }
    }  

    private void listBusinessSystemProviders(String theRequest) throws AppSDKException, Exception {
        XMLUtil.printHeading("List Business System Providers");
        RequestInfo info        = null;
        String      partnerType = null;
        String      partnerName = null;
        String      request     = null;

        if(theRequest != null && theRequest.contains("<ACXBusinessSystemProvidersListRequest>")) {
            request = theRequest;
            partnerType = XMLUtil.getTagValue("PartnerTypeNumber", request);
            partnerName = XMLUtil.getTagValue("PartnerTypeName", request);
            XMLUtil.printXML("List Application Services Request for partner type " + partnerType + " " + partnerName + ":",
                             XMLUtil.prettifyString(request));

            try {
                // call gateway with XML request
                String xmlResponse = adminManager.listBusinessSystemProviders(request);

                XMLUtil.printXML("List Business System Providers Response for partner type " + partnerType + " " + partnerName + ":", 
                                 XMLUtil.prettifyString(xmlResponse));
                info = adminManager.getRequestInfo().copy();
                writeResult(info, "listBusinessSystemProviders"/*testcase*/, "BrokerID unknown", ""/*Account*/);
            } catch (Exception e) {
                info = adminManager.getRequestInfo().copy();
                writeResult(info, "listBusinessSystemProviders"/*testcase*/, "BrokerID unknown", ""/*Account*/);

                System.out.println("listBusinessSystemProviders() caught Exception: " + e);
                System.out.println("ACXTrackNum: " + info.getTrackNum());
                System.out.println("  Total time:     " + info.getTotalTime() + " millis");
                System.out.println("  Gateway access: " + info.getGatewayAccessTime());
                System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
                System.out.println();
            }
            return;
        }
        // look up services for each partner     
        for (int i = 0; i < partners.size(); ++i) {
            String[] partner = (String[]) partners.get(i);
            partnerType = partner[0];
            partnerName = partner[1];

            // Add the partner type number and name to the xml template request
            request = templateReader.readTemplate("ACXBusinessSystemProvidersListRequest");       
            request = XMLUtil.insertValueForTag(request, "</PartnerTypeNumber>", partnerType);
            request = XMLUtil.insertValueForTag(request, "</PartnerTypeName>", partnerName); 

            XMLUtil.printXML("List Business System Providers Request for partner type " + partnerType + " " + partnerName + ":",
                             XMLUtil.prettifyString(request));

            try {
                // call gateway with XML request
                String xmlResponse = adminManager.listBusinessSystemProviders(request);

                XMLUtil.printXML("List Business System Providers Response for partner type " + partnerType + " " + partnerName + ":", 
                                 XMLUtil.prettifyString(xmlResponse));
                info = adminManager.getRequestInfo().copy();
                writeResult(info, "listBusinessSystemProviders"/*testcase*/, "BrokerID unknown", ""/*Account*/);
            } catch (Exception e) {
                info = adminManager.getRequestInfo().copy();
                writeResult(info, "listBusinessSystemProviders"/*testcase*/, "BrokerID unknown", ""/*Account*/);

                System.out.println("listBusinessSystemProviders() caught Exception: " + e);
                System.out.println("ACXTrackNum: " + info.getTrackNum());
                System.out.println("  Total time:     " + info.getTotalTime() + " millis");
                System.out.println("  Gateway access: " + info.getGatewayAccessTime());
                System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
                System.out.println();
            }
        }
    }

    private void listStaticAssociations(String theRequest) throws AppSDKException, Exception {
        XMLUtil.printHeading("List Static Associations");
        RequestInfo info        = null;
        String      partnerType = null;
        String      partnerName = null;
        String      request     = null;
        
        if(theRequest != null && theRequest.contains("<ACXStaticAssociationsListRequest>")) {
            request = theRequest;
            partnerType = XMLUtil.getTagValue("PartnerTypeNumber", request);
            partnerName = XMLUtil.getTagValue("PartnerTypeName", request);
            XMLUtil.printXML("List Application Services Request for partner type " + partnerType + " " + partnerName + ":",
                             XMLUtil.prettifyString(request));

            try {
                // call gateway with XML request
                String xmlResponse = adminManager.listStaticAssociations(request);

                XMLUtil.printXML("List Static Associations Response for partner type " + partnerType + " " + partnerName + ":", 
                                 XMLUtil.prettifyString(xmlResponse));
                info = adminManager.getRequestInfo().copy();
                writeResult(info, "listStaticAssociations"/*testcase*/, "BrokerID unknown", ""/*Account*/);
            } catch (Exception e) {
                info = adminManager.getRequestInfo().copy();
                writeResult(info, "listStaticAssociations"/*testcase*/, "BrokerID unknown", ""/*Account*/);

                System.out.println("listStaticAssociations() caught Exception: " + e);
                System.out.println("ACXTrackNum: " + info.getTrackNum());
                System.out.println("  Total time:     " + info.getTotalTime() + " millis");
                System.out.println("  Gateway access: " + info.getGatewayAccessTime());
                System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
                System.out.println();
            }
            return;
        }

        // look up services for each partner
        for (int i = 0; i < partners.size(); ++i) {
            String[] partner = (String[]) partners.get(i);
            partnerType = partner[0];
            partnerName = partner[1];

            // Add the partner type number and name to the xml template request
            request = templateReader.readTemplate("ACXStaticAssociationsListRequest");
            request = XMLUtil.insertValueForTag(request, "</PartnerTypeNumber>", partnerType);
            request = XMLUtil.insertValueForTag(request, "</PartnerTypeName>", partnerName);            

            XMLUtil.printXML("List Static Associations Request for partner type " + partnerType + " " + partnerName + ":",
                             XMLUtil.prettifyString(request));

            try {
                // call gateway with XML request
                String xmlResponse = adminManager.listStaticAssociations(request);

                XMLUtil.printXML("List Static Associations Response for partner type " + partnerType + " " + partnerName + ":", 
                                 XMLUtil.prettifyString(xmlResponse));
                info = adminManager.getRequestInfo().copy();
                writeResult(info, "listStaticAssociations"/*testcase*/, "BrokerID unknown", ""/*Account*/);
            } catch (Exception e) {
                info = adminManager.getRequestInfo().copy();
                writeResult(info, "listStaticAssociations"/*testcase*/, "BrokerID unknown", ""/*Account*/);

                System.out.println("listStaticAssociations() caught Exception: " + e);
                System.out.println("ACXTrackNum: " + info.getTrackNum());
                System.out.println("  Total time:     " + info.getTotalTime() + " millis");
                System.out.println("  Gateway access: " + info.getGatewayAccessTime());
                System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
                System.out.println();
            }
        }
    }    

    private void listMemberAffiliations(String theRequest) throws AppSDKException, Exception {
        XMLUtil.printHeading("List Member Affiliations");
        RequestInfo info        = null;
        String      partnerType = null;
        String      partnerName = null;
        String      request     = null;
        
        if(theRequest != null && theRequest.contains("<ACXMemberAffiliationsListRequest>")) {
            request = theRequest;
            partnerType = XMLUtil.getTagValue("PartnerTypeNumber", request);
            partnerName = XMLUtil.getTagValue("PartnerTypeName", request);
            XMLUtil.printXML("List Application Services Request for partner type " + partnerType + " " + partnerName + ":",
                             XMLUtil.prettifyString(request));

            try {
                // call gateway with XML request
                String xmlResponse = adminManager.listMemberAffiliations(request);

                XMLUtil.printXML("List Member Affiliations partner type " + partnerType + " " + partnerName + ":", 
                                 XMLUtil.prettifyString(xmlResponse));
                info = adminManager.getRequestInfo().copy();
                writeResult(info, "listMemberAffiliations"/*testcase*/, "BrokerID unknown", ""/*Account*/);
            } catch (Exception e) {
                info = adminManager.getRequestInfo().copy();
                writeResult(info, "listMemberAffiliations"/*testcase*/, "BrokerID unknown", ""/*Account*/);

                System.out.println("listMemberAffiliations() caught Exception: " + e);
                System.out.println("ACXTrackNum: " + info.getTrackNum());
                System.out.println("  Total time:     " + info.getTotalTime() + " millis");
                System.out.println("  Gateway access: " + info.getGatewayAccessTime());
                System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
                System.out.println();
            }
            return;
        }

        // look up member affiliations for each partner type
        for (int i = 0; i < partners.size(); ++i) {
            String[] partner = (String[]) partners.get(i);
            partnerType = partner[0];
            partnerName = partner[1];

            // Add the partner type number and name to the xml template request
            request = templateReader.readTemplate("ACXMemberAffiliationsListRequest");
            request = XMLUtil.insertValueForTag(request, "</PartnerTypeNumber>", partnerType);
            request = XMLUtil.insertValueForTag(request, "</PartnerTypeName>", partnerName);            

            XMLUtil.printXML("List Member Affiliations Request for partner type " + partnerType + " " + partnerName + ":",
                             XMLUtil.prettifyString(request));

            try {
                // call gateway with XML request
                String xmlResponse = adminManager.listMemberAffiliations(request);

                XMLUtil.printXML("List Member Affiliations partner type " + partnerType + " " + partnerName + ":", 
                                 XMLUtil.prettifyString(xmlResponse));
                info = adminManager.getRequestInfo().copy();
                writeResult(info, "listMemberAffiliations"/*testcase*/, "BrokerID unknown", ""/*Account*/);
            } catch (Exception e) {
                info = adminManager.getRequestInfo().copy();
                writeResult(info, "listMemberAffiliations"/*testcase*/, "BrokerID unknown", ""/*Account*/);

                System.out.println("listMemberAffiliations() caught Exception: " + e);
                System.out.println("ACXTrackNum: " + info.getTrackNum());
                System.out.println("  Total time:     " + info.getTotalTime() + " millis");
                System.out.println("  Gateway access: " + info.getGatewayAccessTime());
                System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
                System.out.println();
            }
        }
    }    

    
    
    private void getLegalAgreement() throws AppSDKException, Exception {
        XMLUtil.printHeading("Get Legal Agreement");
        RequestInfo info = null;

        try {
            // call gateway with XML request - no request document needed
            String xmlResponse = adminManager.retrieveLegalAgreement();

            XMLUtil.printXML("Get Legal Agreement Response:", 
                             XMLUtil.prettifyString(xmlResponse));
            info = adminManager.getRequestInfo().copy();
            writeResult(info, "getLegalAgreement"/*testcase*/, "BrokerID unknown", ""/*Account*/);
        } catch (Exception e) {
            info = adminManager.getRequestInfo().copy();
            writeResult(info, "getLegalAgreement"/*testcase*/, "BrokerID unknown", ""/*Account*/);

            System.out.println("getLegalAgreement() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
    }

    private void writeResult(RequestInfo info, String testcase, String BrokerID, String Account) {
    /*
    "TestCase"; "Status"; // Pass/Fail "Milliseconds"; "ConfNum"; "TrackNum";
    "Date"; "Broker"; "Broker PID"; "Buyer"; "Buyer PID"; "Seller"; "Seller PID";
    "Account"; "ExceptionCode"; "ExceptionType"; "ExceptionMsg";
    "Request"; // file path to XML request
    "Response"; // file path to XML response
    "Exception"; // file path to exception text
         * new OrderResult(tc.getName(),status,String.valueOf(txml.getTotalTime()),
                                   myConfNum, myTrackNum, txml.getDateStamp(),broker.getName(),
                                   broker.getPID(),buyer.getName(),buyer.getBuyerPID(),
                                   seller.getName(),seller.getSellerPID(),myAccount,
                                   myCode,myType,myMsgString,txml.getRequestFile(),
                                   txml.getResponseFile(),txml.getExceptionFile() );
         * OrderResult(String testCase, String status, String milliseconds,
                       String confNum,String trackNum,String date,String broker,
                       String brokerPID,String buyer,String buyerPID,String seller,
                       String sellerPID,String account,String exceptionCode,
                       String exceptionType,String exceptionMsg,String request,
                       String response,String exception) {
    */
        String     reqfilepath  = "";
        String     respfilepath = "";
        String     excfilepath  = "";
        if(info.getRequest() != null) {
            // write request to today/starttime_seq_testcase.xml
            try {
                reqfilepath = today + "/" + starttime + "_" + String.format("%04d", seqnum) + "_" +  testcase + ".req.xml";
                File       req      = new File(reqfilepath);
                FileWriter fwr      = new FileWriter(req);
                fwr.write(info.getRequest());
                fwr.close();
                reqfilepath = req.getAbsolutePath(); // get the full path so we can record it.
            } catch (FileNotFoundException fne) {
                logger.error("Failed to open " + reqfilepath + " for writing. " + fne);
            } catch (IOException ioe) {
                logger.error("Failed to write to " + reqfilepath + ". " + ioe);
            }
        }
        if(info.getResponse() != null) {
            // write request to today/starttime_seq_testcase.xml
            try {
                respfilepath = today + "/" + starttime + "_" + String.format("%04d", seqnum) + "_" + testcase + ".rsp.xml";
                File       resp     = new File(respfilepath);
                FileWriter fwr      = new FileWriter(resp);
                fwr.write(info.getResponse());
                fwr.close();
                respfilepath = resp.getAbsolutePath(); // get the full path so we can record it.
            } catch (FileNotFoundException fne) {
                logger.error("Failed to open " + respfilepath + " for writing. " + fne);
            } catch (IOException ioe) {
                logger.error("Failed to write to " + respfilepath + ". " + ioe);
            }
        }
        if(info.getException() != null) {
            // write request to today/starttime_seq_testcase.txt
            try {
                excfilepath = today + "/" + starttime + "_" + String.format("%04d", seqnum) + "_" + testcase + ".exc.txt";
                File       exc      = new File(excfilepath);
                FileWriter fwr      = new FileWriter(exc);
                fwr.write(info.getException().toString());
                fwr.close();
                excfilepath = exc.getAbsolutePath(); // get the full path so we can record it.
            } catch (FileNotFoundException fne) {
                logger.error("Failed to open " + excfilepath + " for writing. " + fne);
            } catch (IOException ioe) {
                logger.error("Failed to write to " + excfilepath + ". " + ioe);
            }
        }
        seqnum++;
        if(ps!=null) {
            try {
                StringBuffer buf = new StringBuffer();
                buf.append(encodeValue(testcase));
                buf.append(",");
                if(info.getStatus() != 0)
                    buf.append(encodeValue("Fail"));
                else
                    buf.append(encodeValue("Pass"));
                buf.append(",");
                buf.append(encodeValue(String.valueOf(info.getTotalTime())));
                buf.append(",");
                buf.append(encodeValue("confnum"));
                buf.append(",");
                buf.append(encodeValue(info.getACXTrackNum()));
                buf.append(",");
                Date             date          = new Date(info.getMethodStartTime());
                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy'-'MM'-'dd'_'kk'_'mm'_'ss'.'SSS");
                String           dateStamp     = dateFormatter.format(date);
                buf.append(encodeValue(dateStamp));
                buf.append(",");
                buf.append(encodeValue(BrokerID));
                buf.append(",");
                buf.append(encodeValue(info.getBuyID()));
                buf.append(",");
                buf.append(encodeValue(info.getSellID()));
                buf.append(",");
                buf.append(encodeValue(Account));
                buf.append(",");
                buf.append(encodeValue(String.valueOf(info.getStatus())));
                buf.append(",");
                buf.append(encodeValue(info.getMsg()));
                buf.append(",");
                buf.append(encodeValue(reqfilepath));
                buf.append(",");
                buf.append(encodeValue(respfilepath));
                buf.append(",");
                buf.append(encodeValue(excfilepath));
                buf.append(",");
                
                ps.println(buf.toString()); 
            } catch  (Exception e)     {
                   logger.info ("Error:  Unexpected exception writing " + resultsPath + "\n", e);
            }; 
        }
    }

    private String encodeValue(String value) {
        // Replace double quotes or commas with substitute chars
        // System.out.println("DEBUG: colV = " + value);
        if (value == null) value = "";
        if (value.length() > 0) { // encode real commas and double quotes to avoid .csv column confusion
            String tmpString = value.replaceAll(",","&comma;");
            value = tmpString.replaceAll("\"","&doublequote;");
            tmpString = value.replaceAll("\n","&newline;");
            value = tmpString;
        }
        return "\"" + value + "\"";

    }


}    
