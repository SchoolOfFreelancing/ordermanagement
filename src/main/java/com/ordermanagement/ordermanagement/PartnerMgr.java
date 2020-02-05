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
import org.xml.sax.*; 
import com.activant.aconnex.appsdk.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.Logger;

/**
 *  Demonstrates each API through the PartnerManager.
 */
public class PartnerMgr {
    protected final transient Logger logger = Logger.getLogger(this.getClass().getName());
    private static String brokerPartnerId    = null;
    private static String buyPartnerId = null;
    private static String sellPartnerId  = null;
    private static String sellPartnerId2 = null;
    private static String accountNumber      = null;
    private static String legalVersion       = null;
    private static String appServiceNum      = null;
    private static String partnerTypeNum     = null;
    private static String groupTypeNum       = null;
    static {
        ResourceBundle props = ResourceBundle.getBundle("AppSDKExamples", Locale.getDefault());
        brokerPartnerId    = props.getString("broker.partner.id");
        buyPartnerId = props.getString("svc.dealer.partner.id");
        sellPartnerId  = props.getString("platform.partner.id");
        sellPartnerId2 = props.getString("platform.partner.id2");
        accountNumber      = props.getString("account.number");
        appServiceNum      = props.getString("app.service.number");
        partnerTypeNum     = "100"; // Service Dealer
        groupTypeNum       = "100"; // System Group
    }

    private PartnerManager    partnerManager   = null;
    private PartOrderManager  partOrderManager = null;
    private AdminManager      adminManager     = null;
    private XMLTemplateReader templateReader   = null;
    private String            resultsdir       = "./results";
    private String            resultsfilename  = "PartnerMgr.csv";
    private String            resultsPath      = resultsdir + "/" + resultsfilename;
    private FileOutputStream  os = null;
    private PrintStream       ps = null; 
    private String            today = null;
    private String            starttime = null;
    private int               seqnum = 0;
    private String            User     = null;
    private String            Pass     = null;
    private String            DistUser = null;
    private String            DistPass = null;
    private boolean           DistBroker = false;
    private String            NewPIDUser = null;
    private int               connect_timeout  = -1;
    private int               base_timeout     = -1;
    private int               var_base_timeout = -1;
    private int               var_line_count   = 25;    
    
//    public static void main (String[] args) {
//        new PartnerMgr(args);
//    }

    public PartnerMgr(String[] args) {
        boolean settimeouts = false;
        try {
            System.out.println();
            boolean do_all = XMLUtil.member("all", args) || args.length==0;
            boolean acxim      = XMLUtil.member("acxim", args);
            boolean noacxim    = XMLUtil.member("noacxim", args);
            boolean gotacxim  = (acxim || noacxim);
            boolean setcreds   = XMLUtil.member("setcreds", args);
            boolean do_inquiry = do_all || XMLUtil.member("partInquiry", args);
            boolean do_parts   = do_all || XMLUtil.member("listPartners", args);
            boolean do_stat    = do_all || XMLUtil.member("partnerStatus", args);
            boolean do_retrievepart = do_all || XMLUtil.member("retrievePartner", args);
            boolean do_isPartnerValid  = do_all || XMLUtil.member("isPartnerValid", args);
            boolean do_group   = do_all || XMLUtil.member("retrieveGroupPartners", args);
            boolean do_mkpart  = do_all || XMLUtil.member("createPartner", args);
            boolean do_uppart  = do_all || XMLUtil.member("updatePartner", args);
            boolean do_rels    = do_all || XMLUtil.member("listRelationships", args);
            boolean do_isrel   = do_all || XMLUtil.member("isTradingRelationshipValid", args);
            boolean do_retrieverel  = do_all || XMLUtil.member("retrieveRelationship", args);
            boolean do_mkrel   = do_all || XMLUtil.member("createRelationship", args);
            boolean do_uprel   = do_all || XMLUtil.member("updateRelationship", args);
            boolean do_rmrel   = do_all || XMLUtil.member("removeRelationship", args);
            String con_timeoutstr      = XMLUtil.memberAfter("con_timeout", args);
            String base_timeoutstr     = XMLUtil.memberAfter("base_timeout", args);
            String var_base_timeoutstr = XMLUtil.memberAfter("var_base_timeout", args);
            String var_line_countstr   = XMLUtil.memberAfter("var_line_count", args);
            boolean setadmin           = XMLUtil.member("setadmin", args);
            boolean setporder          = XMLUtil.member("setporder", args);
            boolean setpmgr            = XMLUtil.member("setpmgr", args);
            String theRequest  = null;
            String requestFile = XMLUtil.memberAfter("Request", args);
            User               = XMLUtil.memberAfter("user",args);
            Pass               = XMLUtil.memberAfter("pass",args);
            DistUser           = XMLUtil.memberAfter("duser",args);
            DistPass           = XMLUtil.memberAfter("dpass",args);
            DistBroker         = XMLUtil.member("DistBroker", args);
            String brid        = XMLUtil.memberAfter("brid",args);
            String bid         = XMLUtil.memberAfter("bid",args);
            String sid         = XMLUtil.memberAfter("sid",args);
            String sid2        = XMLUtil.memberAfter("sid2",args);
            String account     = XMLUtil.memberAfter("account",args);
            if(brid!=null)    brokerPartnerId = brid;
            if(bid!=null)     buyPartnerId    = bid;
            if(sid!=null)     sellPartnerId   = sid;
            if(sid2!=null)    sellPartnerId2  = sid2;
            if(account!=null) accountNumber   = account;
            

            String optStr = (do_all?" all":"") +
                            (acxim?" acxim":"") +
                            (noacxim?" noacxim":"") +
                            (requestFile!=null?" RequestFile=" + requestFile:"") +
                            (User!=null?" user "+ User:"") +
                            (Pass!=null?" pass "+ Pass:"") +
                            (DistUser!=null?" duser "+ DistUser:"") +
                            (DistPass!=null?" dpass "+ DistPass:"") +
                            (DistBroker?" DistBroker":"") +
                            (setcreds?" setcreds":"") +
                            (brid!=null?" brid "+ brid:"") +
                            (bid!=null?" bid "+ bid:"") +
                            (sid!=null?" sid "+ sid:"") +
                            (sid2!=null?" sid2 "+ sid2:"") +
                            (account!=null?" account "+ account:"") +
                            (con_timeoutstr!=null ? " con_timeout " + con_timeoutstr : "") +
                            (base_timeoutstr!=null ? " base_timeout " + base_timeoutstr : "") +
                            (var_base_timeoutstr!=null ? " var_base_timeout " + var_base_timeoutstr : "") +
                            (var_line_countstr!=null ? " var_line_count " + var_line_countstr : "") +
                            (do_inquiry?" partInquiry":"") +
                            (do_parts?" listPartners":"") + 
                            (do_stat?" partnerStatus":"") +
                            (do_isPartnerValid?" isPartnerValid":"") +
                            (do_retrievepart?" retrievePartner":"") +
                            (do_group?" retrieveGroupPartners":"") +
                            (do_mkpart?" createPartner":"") + 
                            (do_uppart?" updatePartner":"") + 
                            (do_rels?" listRelationships":"") + 
                            (do_isrel?" isTradingRelationshipValid":"") + 
                            (do_retrieverel?" retrieveRelationship":"") +
                            (do_mkrel?" createRelationship":"") + 
                            (do_uprel?" updateRelationship":"") + 
                            (do_rmrel?" removeRelationship":"");  
            System.out.println("Options: " + optStr);

            // Get the AppBuilder and pass in the user and password if they
            // were given on the command line to override what was in the
            // AppSDK.properties file.
            AppBuilder appBuilder = null;
            if(User != null && Pass != null)
                appBuilder = new AppBuilder(User,Pass);
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
                            
            if(bid != null) {
                // they provided a buypartnerid on the command line so we need to 
                // make the settings object use it instead of what was in the AppSDK.properties file.
                appBuilder.setBuyPartnerID(bid);
            }
            // if we got an acxim setting on the command line then
            // if the acxim setting in the appBuilder is not the same as acxim
            // then we need to set the acxim setting in the appBuilder.
            if(gotacxim) {
                if((acxim & !appBuilder.getACXIM()) || (!acxim && appBuilder.getACXIM())) {
                    appBuilder.setACXIM(acxim);
                }
            }
            partnerManager   = appBuilder.getPartnerManager();
            adminManager     = appBuilder.getAdminManager();
            partOrderManager = appBuilder.getPartOrderManager();
            // If a set of Distributed buyPartner Creds were provided then
            // call setCredentials on the partOrderManager so that it will be
            // using the Distributed BuyPartner creds.
            if(DistUser != null && DistPass != null)
                partOrderManager.setCredentials(DistUser,DistPass);
            
            // setcreds is used to test the use of a user and pass set with
            // setCredentials(). DistUser and DistPass are not necessarily
            // Distributed buy partner creds. They could be Centralized broker creds as well.
            if(setcreds && DistUser != null && DistPass != null) {
                logger.info("Calling setCredentials(" + DistUser + "," + DistPass + ")");
                partnerManager.setCredentials(DistUser,DistPass);
                adminManager.setCredentials(DistUser,DistPass);
            }
            if (settimeouts)
            {
                if(setporder)
                    partOrderManager.setTimeouts(connect_timeout, base_timeout, var_base_timeout, var_line_count);
                if (setadmin)
                    adminManager.setTimeouts(connect_timeout, base_timeout, var_base_timeout, var_line_count);
                if (setpmgr)
                    partnerManager.setTimeouts(connect_timeout, base_timeout, var_base_timeout, var_line_count);
            }
            templateReader   = new XMLTemplateReader(appBuilder);

            // We have no standalone data files for these requests so force to
            // connected mode for now.

            partnerManager.setStandaloneMode(false);
            System.out.println("Running " + (partnerManager.getStandaloneMode()?"standalone":"connected"));
            System.out.println();
            
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy'-'MM'-'dd");
            SimpleDateFormat timeFormatter = new SimpleDateFormat("kk'_'mm'_'ss");
            starttime = timeFormatter.format(new Date());
            today     = dateFormatter.format(new Date());

            File   dir = new File(resultsdir);
            if(!dir.exists())
                dir.mkdir();
            dir = new File(today);
            if(!dir.exists()) // create the directory for today to hold xml req and resp files
                dir.mkdir();
            
            File   csvFile  = new File(resultsfilename);
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
                } else {
                    System.out.println("File: " + requestFile + " does not exist");
                }
            }
            
            // Partner API examples
            if (do_parts)          listPartners(theRequest);
            if (do_stat)           retrievePartnerStatus(theRequest);
            if (do_retrievepart)   retrievePartner(brokerPartnerId,sellPartnerId,theRequest);
            if (do_isPartnerValid) isPartnerValid(theRequest);
            if (do_group)          retrieveGroupPartners(theRequest);

            // Only update newly created partners so that we don't
            // modify the Gateway's built-in partner info.
            String newPartnerId = null;
            
            if (do_mkpart || (do_uppart && theRequest == null) || do_inquiry) {
                newPartnerId = createPartner(theRequest);
            }
            if (do_uppart && theRequest != null) {
                updatePartner(newPartnerId,theRequest);
            }
            if (do_uppart && newPartnerId != null) {
                updatePartner(newPartnerId,theRequest);
            }

            // do an inquiry into sell partner using newly created
            // buypartner but first create a trade rel for it
            // using the main account number that should be valid on the 
            // platform so the inquiry will succeed.
            if(do_inquiry) {
                createTradingRelationshipmainacct(newPartnerId);
                queryParts();
            }
            

            // Trading Relationship API examples
            if (do_rels)        listTradingRelationships(theRequest);
            if (do_isrel)       isTradingRelationshipValid(theRequest);
            if (do_retrieverel) retrieveRelationship(theRequest);

            // Only update newly created relationships so that we don't
            // modify the Gateway's built-in relationship info.
            String newAcctNum = null;
            if (do_mkrel || (do_uprel && theRequest == null) || (do_rmrel && theRequest == null)) {
                newAcctNum = createTradingRelationship(theRequest);
            }
            if (do_uprel && theRequest != null) {
                updateTradingRelationship(newAcctNum,theRequest); 
            }
            if (do_uprel && newAcctNum != null) {
                newAcctNum = updateTradingRelationship(newAcctNum,theRequest); 
            }
            if (do_rmrel && theRequest != null) {
                removeTradingRelationship(newAcctNum,theRequest);
            }
            if (do_rmrel && newAcctNum != null) {
                removeTradingRelationship(newAcctNum,theRequest);
            }
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

    private void listPartners(String theRequest) throws Exception {
        XMLUtil.printHeading("List Partners");
        RequestInfo info    = null;
        String      request = null;
        
        if(theRequest != null && theRequest.contains("<ACXPartnersListRequest>"))
            request = theRequest;
        else {
            request = templateReader.readTemplate("ACXPartnersListRequest");

            // swap out with our buy partnerId
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", brokerPartnerId);        
            // in this example, we will use the name filter with a '*' wildcard
            request = XMLUtil.insertValueForTag(request, "</Name>", "A*");
        }

        XMLUtil.printXML("List Partners Request:", XMLUtil.prettifyString(request));

        try {
            String xmlResponse = partnerManager.listPartners(request);
            info = partnerManager.getRequestInfo().copy();

            XMLUtil.printXML("List Partners Response:", XMLUtil.prettifyString(xmlResponse));
            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "listPartners"/*testcase*/, "BrokerID Unknown", ""/*Account*/);
        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "listPartners"/*testcase*/, "BrokerID Unknown", ""/*Account*/);

            System.out.println("listPartners() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
    }

    private void retrievePartnerStatus(String theRequest) throws Exception {
        XMLUtil.printHeading("Retrieve Partner Status");
        RequestInfo info    = null;
        String      request = null;

        if(theRequest != null && theRequest.contains("<ACXPartnerStatusRequest>"))
            request = theRequest;
        else {
            request = templateReader.readTemplate("ACXPartnerStatusRequest");    
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", brokerPartnerId);        
            request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);      
        }

        XMLUtil.printXML("Retrieve Partner Status Request:", XMLUtil.prettifyString(request));

        try {
            String xmlResponse = partnerManager.retrievePartnerStatus(request);
            info = partnerManager.getRequestInfo().copy();

            XMLUtil.printXML("Retrieve Partner Status Response:", XMLUtil.prettifyString(xmlResponse));        
            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "retrievePartnerStatus"/*testcase*/, "BrokerID Unknown", ""/*Account*/);
        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "retrievePartnerStatus"/*testcase*/, "BrokerID Unknown", ""/*Account*/);

            System.out.println("retrievePartnerStatus() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
    }

    private void retrieveGroupPartners(String theRequest) throws Exception {
        XMLUtil.printHeading("Retrieve Group Partners");
        RequestInfo info    = null;
        String      request = null;

        if(theRequest != null && theRequest.contains("<ACXGroupPartnersRequest>"))
            request = theRequest;
        else {
            request = templateReader.readTemplate("ACXGroupPartnersRequest");    
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", brokerPartnerId); 

            request = XMLUtil.insertValueForTag(request, "</GroupPartnerID>", sellPartnerId);      
            request = XMLUtil.insertValueForTag(request, "</GroupTypeNumber>", groupTypeNum);      
            request = XMLUtil.insertValueForTag(request, "</GroupTypeName>", "ignored");      
        }
        XMLUtil.printXML("Retrieve Group Partners Request:", XMLUtil.prettifyString(request));

        try {
            String xmlResponse = partnerManager.retrieveGroupPartners(request);
            info = partnerManager.getRequestInfo().copy();

            XMLUtil.printXML("Retrieve Group Partners Response:", XMLUtil.prettifyString(xmlResponse));        
            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "retrieveGroupPartners"/*testcase*/, "BrokerID Unknown", ""/*Account*/);
        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "retrieveGroupPartners"/*testcase*/, "BrokerID Unknown", ""/*Account*/);

            System.out.println("retrieveGroupPartners() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
    }      

    // Helper to get the current legal agreement version
    private String getLegalVersion() throws Exception {
        RequestInfo info = null;
        // see if we have already requested it
        if (legalVersion != null) return legalVersion;

        try {
            // call gateway with XML request - no request document needed
            String xmlResponse = adminManager.retrieveLegalAgreement();
            legalVersion = XMLUtil.getTagValue("Version", xmlResponse);
            info = adminManager.getRequestInfo().copy();
            writeResult(info, "getLegalVersion"/*testcase*/, "BrokerID Unknown", ""/*Account*/);
        } catch (Exception e) {
            info = adminManager.getRequestInfo().copy();
            writeResult(info, "getLegalVersion"/*testcase*/, "BrokerID Unknown", ""/*Account*/);

            System.out.println("getLegalVersion() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }

        return legalVersion;
    }
    private String createPartner(String theRequest) throws Exception {
        XMLUtil.printHeading("Create Partner");
        RequestInfo info    = null;
        String      request = null;
        String      userid  = null;

        if(theRequest != null && theRequest.contains("<ACXPartnerCreationRequest>"))
            request = theRequest;
        else {
            request = templateReader.readTemplate("ACXPartnerCreationRequest");        
            Document document = XMLUtil.createDocument(request);        
            Node root = document.getDocumentElement();               

            NodeList nodes = root.getChildNodes();
            for (int i=0; i<nodes.getLength(); i++) {
                Node node = nodes.item(i);
                String nodeName = node.getNodeName();
                //System.out.println("NodeName - " + nodeName);
                if ("LegalVersion".equals(nodeName)) {
                    XMLUtil.setTagValue(document, node, getLegalVersion());
                } else
                    if ("PartnerType".equals(nodeName)) {
                    XMLUtil.setTagValue(document, node, "PartnerTypeNumber", partnerTypeNum);
                    XMLUtil.setTagValue(document, node, "PartnerTypeName", "ignored");
                } else
                    if ("ApplicationService".equals(nodeName)) {
                    XMLUtil.setTagValue(document, node, "AppServiceNumber", appServiceNum);
                    XMLUtil.setTagValue(document, node, "AppServiceName", "ignored");                
                } else
                    if ("PartnerUpdateProfile".equals(nodeName)) {
                    // Add value for Name
                    String name = "New Partner " + String.valueOf(Math.abs(new Random().nextInt()));                
                    XMLUtil.setTagValue(document, node, "Name", name);                
                    // Add value for phone                
                    XMLUtil.setTagValue(document, node, "Phone", "555-9999");                
                    // Add value for address - street
                    XMLUtil.setTagValue(document, node, "Street", "partner address line 1");                
                    // Add value for address - city
                    XMLUtil.setTagValue(document, node, "City", "Austin");                
                    // Add value for address - State
                    XMLUtil.setTagValue(document, node, "State", "TX");                
                    // Add value for address - country
                    XMLUtil.setTagValue(document, node, "Country", "USA");                
                    // Add value for address - PostalID
                    XMLUtil.setTagValue(document, node, "PostalID", "78737");                                
                } else
                    if ("BillingProfile".equals(nodeName)) {
                    // Add value for BillingProfile - street
                    XMLUtil.setTagValue(document, node, "Street", "my address");                
                    // Add value for BillingProfile - city
                    XMLUtil.setTagValue(document, node, "City", "Austin");                
                    // Add value for BillingProfile - State
                    XMLUtil.setTagValue(document, node, "State", "TX");                
                    // Add value for BillingProfile - country
                    XMLUtil.setTagValue(document, node, "Country", "USA");                
                    // Add value for BillingProfile - PostalID
                    XMLUtil.setTagValue(document, node, "PostalID", "78739");                
                } else
                    if ("UserProfile".equals(nodeName)) {
                    // Add value for UserProfile - ID                
                    //XMLUtil.setTagValue(document, node, "ID", "99887766545");
                    int num = Math.abs(new Random().nextInt());
                    NewPIDUser = "NEWPID" + String.valueOf(num);
                    XMLUtil.setTagValue(document, node, "ID", NewPIDUser);                
                    // Add value for Person - FirstName
                    XMLUtil.setTagValue(document, node, "FirstName", "Calvin");                
                    // Add value for Person - LastName
                    XMLUtil.setTagValue(document, node, "LastName", "Hobbs");                
                    // Add value for Person - Phone
                    XMLUtil.setTagValue(document, node, "Phone", "512-895-7899");                
                    // Add value for Person - Email
                    XMLUtil.setTagValue(document, node, "Email", "calvin@activant.com");                
                    // Add value for Password
                    XMLUtil.setTagValue(document, node, "Password", "mypassword");                
                    // Add value for PasswordHint
                    //XMLUtil.setTagValue(document, node, "PasswordHint", "i made it, i should know it");               
                }
            }               
            request = XMLUtil.documentToString(document);
        }
        XMLUtil.printXML("Create Partner Request:", request);

        String xmlResponse = null;
        String partnerID   = null;
        try {
            xmlResponse = partnerManager.createPartner(request);
            info = partnerManager.getRequestInfo().copy();

            XMLUtil.printXML("Create Partner Response:", XMLUtil.prettifyString(xmlResponse));

            partnerID = XMLUtil.getTagValue("PartnerID", xmlResponse);
            System.out.println();
            System.out.println("Partner " + partnerID + " successfully created");
            System.out.println();

            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "createPartner"/*testcase*/, "BrokerID Unknown", ""/*Account*/);
            
            String buypartner = brokerPartnerId;
            // If they passed us a Distributed user and password then we have
            // to change the partnerManager to use them when calling retrievePartner()
            // because Distributed Broker creds are not allowed to call this method.
            if(DistBroker) {
                partnerManager.setCredentials(NewPIDUser,"mypassword");
                buypartner = partnerID;
            }

            // use retrievePartner() to verify the partner was created.
            request = templateReader.readTemplate("ACXPartnerRequest"); 
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buypartner);
            request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", partnerID);

            XMLUtil.printXML("Retrieve Partner Request After Create:", XMLUtil.prettifyString(request));

            try {
                xmlResponse = partnerManager.retrievePartner(request);
                info = partnerManager.getRequestInfo().copy();

                XMLUtil.printXML("Retrieve Partner After Create Response:", XMLUtil.prettifyString(xmlResponse));
                System.out.println("Request time:     " + info.getTotalTime() + " millis");
                System.out.println();
                writeResult(info, "VerifyCreatePartner"/*testcase*/, "BrokerID Unknown", ""/*Account*/);
            } catch (Exception e) {
                info = partnerManager.getRequestInfo().copy();
                writeResult(info, "VerifyCreatePartner"/*testcase*/, "BrokerID Unknown", ""/*Account*/);

                System.out.println("retrievePartner() caught Exception: " + e);
                System.out.println("ACXTrackNum: " + info.getTrackNum());
                System.out.println("  Total time:     " + info.getTotalTime() + " millis");
                System.out.println("  Gateway access: " + info.getGatewayAccessTime());
                System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
                System.out.println();
            }
            // change the user and pass back to the Broker creds in case there are other
            // tests that are going to be run after this.
            if(DistBroker)
                partnerManager.setCredentials(User,Pass);

        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "createPartner"/*testcase*/, "BrokerID Unknown", ""/*Account*/);

            System.out.println("createPartner() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
        return partnerID;
    }

    private void retrievePartner(String buypartner, String sellpartner,String theRequest) throws Exception {
        XMLUtil.printHeading("Retrieve Partner");
        RequestInfo info    = null;
        String      request = null;

        if(theRequest != null && theRequest.contains("<ACXPartnerRequest>"))
            request = theRequest;
        else {
            request = templateReader.readTemplate("ACXPartnerRequest"); 
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buypartner);
            request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellpartner);
        }

        XMLUtil.printXML("Retrieve Partner Request:", XMLUtil.prettifyString(request));

        try {
            String xmlResponse = partnerManager.retrievePartner(request);
            info = partnerManager.getRequestInfo().copy();

            XMLUtil.printXML("Retrieve Partner Response:", XMLUtil.prettifyString(xmlResponse));
            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "retrievePartner"/*testcase*/, "BrokerID Unknown", ""/*Account*/);
        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "retrievePartner"/*testcase*/, "BrokerID Unknown", ""/*Account*/);

            System.out.println("retrievePartner() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
    }

    private void isPartnerValid(String theRequest) throws Exception {
        XMLUtil.printHeading("Verify Partner");
        RequestInfo info    = null;
        boolean     valid   = false;
        String      request = null;

        if(theRequest != null && theRequest.contains("<ACXPartnerValidRequest>")) {
            request = theRequest;
            buyPartnerId = XMLUtil.getTagValue("SellPartnerID", request);
        }
        else {
            //This service dealer partner ID should be valid
            request = templateReader.readTemplate("ACXPartnerValidRequest"); 
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", brokerPartnerId);
            request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", buyPartnerId);
        }
        XMLUtil.printXML("Verify Partner Request:", XMLUtil.prettifyString(request));

        try {
            valid = partnerManager.isPartnerValid(request);
            info = partnerManager.getRequestInfo().copy();

            System.out.println("Partner " + buyPartnerId + (valid?" is":" is not") + " valid");   
            System.out.println();
            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "isPartnerValid"/*testcase*/, "BrokerID Unknown", ""/*Account*/);
        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "isPartnerValid"/*testcase*/, "BrokerID Unknown", ""/*Account*/);

            System.out.println("isPartnerValid() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }

        // if they gave us a request then we did the request above. return here
        // so we don't do more requests.
        if(theRequest != null && theRequest.contains("<ACXPartnerValidRequest>"))
            return;

        //This JCON platform partner ID should be valid
        request = templateReader.readTemplate("ACXPartnerValidRequest"); 
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", brokerPartnerId);
        request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);

        XMLUtil.printXML("Verify Partner Request:", XMLUtil.prettifyString(request));

        try {
            valid = partnerManager.isPartnerValid(request);
            info = partnerManager.getRequestInfo().copy();
            System.out.println("Partner " + sellPartnerId + (valid?" is":" is not") + " valid");   
            System.out.println();
            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "isPartnerValid"/*testcase*/, "BrokerID Unknown", ""/*Account*/);
        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "isPartnerValid"/*testcase*/, "BrokerID Unknown", ""/*Account*/);

            System.out.println("isPartnerValid() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }

        //This ADIS platform partner ID should be valid
        request = templateReader.readTemplate("ACXPartnerValidRequest"); 
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", brokerPartnerId);
        request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId2);

        XMLUtil.printXML("Verify Partner Request:", XMLUtil.prettifyString(request));

        try {
            valid = partnerManager.isPartnerValid(request);
            info = partnerManager.getRequestInfo().copy();
            System.out.println("Partner " + sellPartnerId2 + (valid?" is":" is not") + " valid");   
            System.out.println();
            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "isPartnerValid"/*testcase*/, "BrokerID Unknown", ""/*Account*/);
        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "isPartnerValid"/*testcase*/, "BrokerID Unknown", ""/*Account*/);

            System.out.println("isPartnerValid() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }

        // This one should not be valid
        String otherSellId = "1234567890";
        request = templateReader.readTemplate("ACXPartnerValidRequest"); 
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", brokerPartnerId);
        request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", otherSellId);

        XMLUtil.printXML("Verify Partner Request:", XMLUtil.prettifyString(request));

        try {
            valid = partnerManager.isPartnerValid(request);
            info = partnerManager.getRequestInfo().copy();
            System.out.println("Partner " + otherSellId + (valid?" is":" is not") + " valid");   
            System.out.println();
            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "isPartnerValid"/*testcase*/, "BrokerID Unknown", ""/*Account*/);
        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "isPartnerValid"/*testcase*/, "BrokerID Unknown", ""/*Account*/);

            System.out.println("isPartnerValid() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
    }

    private void updatePartner(String partnerIdToUpdate,String theRequest) throws Exception {
        XMLUtil.printHeading("Update Partner");
        RequestInfo info    = null;
        String      request = null;

        if(theRequest != null && theRequest.contains("<ACXPartnerUpdateRequest>")) {
            request = theRequest;
            partnerIdToUpdate = XMLUtil.getTagValue("BuyPartnerID", request);
        }
        else {
            request = templateReader.readTemplate("ACXPartnerUpdateRequest");
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", partnerIdToUpdate);        
            String name = "Updated partner " + String.valueOf(Math.abs(new Random().nextInt())); 
            request = XMLUtil.insertValueForTag(request, "</Name>", name);
            request = XMLUtil.insertValueForTag(request, "</Phone>", "892-1234");
            request = XMLUtil.insertValueForTag(request, "</Fax>", "892-5678");
            request = XMLUtil.insertValueForTag(request, "</Country>", "USA");
        }
        
        XMLUtil.printXML("Update Partner Request:", XMLUtil.prettifyString(request));

        // If they passed us a Distributed user and password then we have
        // to change the partnerManager to use them when calling updatePartner()
        // because Distributed Broker creds are not allowed to call this method.
        if(DistBroker)
            partnerManager.setCredentials(NewPIDUser,"mypassword");

        try {
            // call to update - if no exception thrown, then it was successful
            partnerManager.updatePartner(request);
            info = partnerManager.getRequestInfo().copy();

            System.out.println("Update Partner successful for " + partnerIdToUpdate);
            System.out.println();
            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "updatePartner"/*testcase*/, "BrokerID Unknown", ""/*Account*/);
        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "updatePartner"/*testcase*/, "BrokerID Unknown", ""/*Account*/);

            System.out.println("updatePartner() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
        // change the user and pass back to the Broker creds in case there are other
        // tests that are going to be run after this.
        if(DistBroker)
            partnerManager.setCredentials(User,Pass);
    }

    private void listTradingRelationships(String theRequest) throws Exception {
        XMLUtil.printHeading("List Trading Relationships");
        RequestInfo info    = null;
        String      request = null;

        if(theRequest != null && theRequest.contains("<ACXTradingRelationshipsListRequest>"))
            request = theRequest;
        else {
            request = templateReader.readTemplate("ACXTradingRelationshipsListRequest");      
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);        
            request = XMLUtil.insertValueForTag(request, "</AppServiceNumber>", appServiceNum);
            request = XMLUtil.insertValueForTag(request, "</AppServiceName>", "ignored");       
        }
        
        XMLUtil.printXML("List Trading Relationships Request:", XMLUtil.prettifyString(request));

        try {
            String xmlResponse = partnerManager.listTradingRelationships(request);
            info = partnerManager.getRequestInfo().copy();

            XMLUtil.printXML("List Trading Relationships Response:", XMLUtil.prettifyString(xmlResponse));        
            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "listTradingRelationships"/*testcase*/, "BrokerID Unknown", ""/*Account*/);
        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "listTradingRelationships"/*testcase*/, "BrokerID Unknown", ""/*Account*/);

            System.out.println("listTradingRelationships() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
    }

    private void isTradingRelationshipValid(String theRequest) throws Exception {
        XMLUtil.printHeading("Check Trading Relationship");
        RequestInfo info    = null;
        boolean     isValid = false;
        String      request = null;

        if(theRequest != null && theRequest.contains("<ACXTradingRelationshipValidRequest>")) {
            request = theRequest;
            buyPartnerId = XMLUtil.getTagValue("BuyPartnerID", request);
            sellPartnerId = XMLUtil.getTagValue("SellPartnerID", request);
            accountNumber = XMLUtil.getTagValue("AccountNum", request);
            }
        else {
            // This one between built-in service dealer and JCON should be valid
            request = templateReader.readTemplate("ACXTradingRelationshipValidRequest");       
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);        
            request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);
            request = XMLUtil.insertValueForTag(request, "</AppServiceNumber>", appServiceNum);
            request = XMLUtil.insertValueForTag(request, "</AccountNum>", accountNumber);        
        }
        XMLUtil.printXML("Check Trading Relationship Request:", XMLUtil.prettifyString(request));

        try {
            isValid = partnerManager.isTradingRelationshipValid(request);
            info = partnerManager.getRequestInfo().copy();

            System.out.println("Trading Relationship " + accountNumber + " between " 
                               + buyPartnerId + " and " + sellPartnerId + 
                               (isValid?" is":" is not") + " valid"); 
            System.out.println();
            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "isTradingRelationshipValid"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);
        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "isTradingRelationshipValid"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);

            System.out.println("isTradingRelationshipValid() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }

        // if they gave us a request then we did the request above. return here
        // so we don't do more requests.
        if(theRequest != null && theRequest.contains("<ACXTradingRelationshipValidRequest>"))
            return;

        // This one between built-in service dealer and ADIS should also be valid
        request = templateReader.readTemplate("ACXTradingRelationshipValidRequest");       
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);        
        request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId2);
        request = XMLUtil.insertValueForTag(request, "</AppServiceNumber>", appServiceNum);
        request = XMLUtil.insertValueForTag(request, "</AccountNum>", accountNumber);        

        XMLUtil.printXML("Check Trading Relationship Request:", XMLUtil.prettifyString(request));

        try {
            isValid = partnerManager.isTradingRelationshipValid(request);
            info = partnerManager.getRequestInfo().copy();
            System.out.println("Trading Relationship " + accountNumber + " between " 
                               + buyPartnerId + " and " + sellPartnerId2 + 
                               (isValid?" is":" is not") + " valid"); 
            System.out.println();
            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "isTradingRelationshipValid"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);
        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "isTradingRelationshipValid"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);

            System.out.println("isTradingRelationshipValid() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }

        // This one should not be valid
        String otherAcctNum = "XX099";
        request = templateReader.readTemplate("ACXTradingRelationshipValidRequest");       
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);        
        request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);
        request = XMLUtil.insertValueForTag(request, "</AppServiceNumber>", appServiceNum);
        request = XMLUtil.insertValueForTag(request, "</AccountNum>", otherAcctNum);        

        XMLUtil.printXML("Check Trading Relationship Request:", XMLUtil.prettifyString(request));

        try {
            isValid = partnerManager.isTradingRelationshipValid(request);
            info = partnerManager.getRequestInfo().copy();
            System.out.println("Trading Relationship " + otherAcctNum + " between " 
                               + buyPartnerId + " and " + sellPartnerId + 
                               (isValid?" is":" is not") + " valid"); 
            System.out.println();
            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "isTradingRelationshipValid"/*testcase*/, "BrokerID Unknown", otherAcctNum/*Account*/);
        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "isTradingRelationshipValid"/*testcase*/, "BrokerID Unknown", otherAcctNum/*Account*/);

            System.out.println("isTradingRelationshipValid() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
    }

    private void retrieveRelationship(String theRequest) throws Exception {
        XMLUtil.printHeading("Retrieve Trading Relationship");
        RequestInfo info = null;
        String      request = null;

        if(theRequest != null && theRequest.contains("<ACXTradingRelationshipRequest>")) {
            request = theRequest;
            accountNumber = XMLUtil.getTagValue("AccountNum", request);
        }
        else {
            request = templateReader.readTemplate("ACXTradingRelationshipRequest");       
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);        
            request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);
            request = XMLUtil.insertValueForTag(request, "</AppServiceNumber>", appServiceNum);
            request = XMLUtil.insertValueForTag(request, "</AppServiceName>", "ignored");
            request = XMLUtil.insertValueForTag(request, "</AccountNum>", accountNumber);        
        }
        
        XMLUtil.printXML("Retrieve Trading Relationship Request:", XMLUtil.prettifyString(request));
        String xmlResponse = null;
        try {
            xmlResponse = partnerManager.retrieveTradingRelationship(request);            
            info = partnerManager.getRequestInfo().copy();

            XMLUtil.printXML("Retrieve Trading Relationship Response:", XMLUtil.prettifyString(xmlResponse));
            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "retrieveRelationship"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);
        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "retrieveRelationship"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);

            System.out.println("retreiveRelationship() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }

        // if they gave us a request then we did the request above. return here
        // so we don't do more requests.
        if(theRequest != null && theRequest.contains("<ACXTradingRelationshipRequest>"))
            return;

        request = templateReader.readTemplate("ACXTradingRelationshipRequest");       
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);        
        request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId2);
        request = XMLUtil.insertValueForTag(request, "</AppServiceNumber>", appServiceNum);
        request = XMLUtil.insertValueForTag(request, "</AppServiceName>", "ignored");
        request = XMLUtil.insertValueForTag(request, "</AccountNum>", accountNumber);        

        XMLUtil.printXML("Retrieve Trading Relationship Request:", XMLUtil.prettifyString(request));

        try {
            xmlResponse = partnerManager.retrieveTradingRelationship(request);            
            info = partnerManager.getRequestInfo().copy();

            XMLUtil.printXML("Retrieve Trading Relationship Response:", XMLUtil.prettifyString(xmlResponse));
            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "retrieveRelationship"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);
        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "retrieveRelationship"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);

            System.out.println("retreiveRelationship() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
    }

    // returns the newly created AccountNum
    private String createTradingRelationship(String theRequest) throws Exception {
        XMLUtil.printHeading("Create Trading Relationship");
        String      newAccountNum = null;
        RequestInfo info          = null;
        String      request       = null;
        
        if(theRequest != null && theRequest.contains("<ACXTradingRelationshipCreationRequest>")) {
            request = theRequest;
            buyPartnerId = XMLUtil.getTagValue("BuyPartnerID", request);
            sellPartnerId = XMLUtil.getTagValue("SellPartnerID", request);
            newAccountNum = XMLUtil.getTagValue("AccountNum", request);
        }
        else {
            request = templateReader.readTemplate("ACXTradingRelationshipCreationRequest");
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);        
            request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);
            request = XMLUtil.insertValueForTag(request, "</AppServiceNumber>", appServiceNum);
            request = XMLUtil.insertValueForTag(request, "</AppServiceName>", "ignored");

            // Account number will need to be dynamic
            newAccountNum = String.valueOf("Acct" + Math.abs(new Random().nextInt()));
            request = XMLUtil.insertValueForTag(request, "</AccountNum>", newAccountNum);
        }
        
        XMLUtil.printXML("Create Trading Relationship Request:", XMLUtil.prettifyString(request));

        try {
            partnerManager.createTradingRelationship(request);
            info = partnerManager.getRequestInfo().copy();

            System.out.println("Create Trading Relationship successful for " + buyPartnerId + "/" + sellPartnerId);
            System.out.println("New AccountNum is " + newAccountNum);
            System.out.println();

            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "createTradingRelationship"/*testcase*/, "BrokerID Unknown", newAccountNum/*Account*/);
        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "createTradingRelationship"/*testcase*/, "BrokerID Unknown", newAccountNum/*Account*/);

            System.out.println("createTradingRelationship() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
        return newAccountNum;
    }

    // creates trade rel between new buyPartnerId and sellPartnerId with main account num
    private void createTradingRelationshipmainacct(String newbuyid) throws Exception {
        XMLUtil.printHeading("Create Trading Relationship");
        RequestInfo info = null;

        String request = templateReader.readTemplate("ACXTradingRelationshipCreationRequest");
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", newbuyid);        
        request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);
        request = XMLUtil.insertValueForTag(request, "</AppServiceNumber>", appServiceNum);
        request = XMLUtil.insertValueForTag(request, "</AppServiceName>", "ignored");

        request = XMLUtil.insertValueForTag(request, "</AccountNum>", accountNumber);

        XMLUtil.printXML("Create Trading Relationship Request:", XMLUtil.prettifyString(request));

        try {
            partnerManager.createTradingRelationship(request);
            info = partnerManager.getRequestInfo().copy();

            System.out.println("Create Trading Relationship successful for " + newbuyid + "/" + sellPartnerId);
            System.out.println("AccountNum is " + accountNumber);
            System.out.println();

            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "createTradingRelationship"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);
        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "createTradingRelationship"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);

            System.out.println("createTradingRelationship() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
        return;
    }

    // returns the newly updated AccountNum
    private String updateTradingRelationship(String acctNum,String theRequest) throws Exception {
        XMLUtil.printHeading("Update Trading Relationship");        
        String      newAccountNum = null;
        RequestInfo info          = null;
        String      request       = null;

        if(theRequest != null && theRequest.contains("<ACXTradingRelationshipUpdateRequest>")) {
            request = theRequest;
            buyPartnerId = XMLUtil.getTagValue("BuyPartnerID", request);
            sellPartnerId = XMLUtil.getTagValue("SellPartnerID", request);
            newAccountNum = XMLUtil.getTagValue("NewAccountNum", request);
        }
        else {
            request = templateReader.readTemplate("ACXTradingRelationshipUpdateRequest");
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);        
            request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);
            request = XMLUtil.insertValueForTag(request, "</AppServiceNumber>", appServiceNum);
            request = XMLUtil.insertValueForTag(request, "</AppServiceName>", "ignored");
            request = XMLUtil.insertValueForTag(request, "</AccountNum>", acctNum);        
        
            // account number is the only updatable field.
            newAccountNum = String.valueOf("Acct" + Math.abs(new Random().nextInt()));
            request = XMLUtil.insertValueForTag(request, "</NewAccountNum>", newAccountNum);        
        }

        XMLUtil.printXML("Update Trading Relationship Request:", XMLUtil.prettifyString(request));

        try {
            partnerManager.updateTradingRelationship(request);
            info = partnerManager.getRequestInfo().copy();

            System.out.println("Update Trading Relationship successful for " + buyPartnerId + "/" + sellPartnerId);
            System.out.println("Updated AccountNum is " + newAccountNum);
            System.out.println();

            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "updateTradingRelationship"/*testcase*/, "BrokerID Unknown", acctNum/*Account*/);
        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "updateTradingRelationship"/*testcase*/, "BrokerID Unknown", acctNum/*Account*/);

            System.out.println("updateTradingRelationship() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
        return newAccountNum;
    }  

    private void removeTradingRelationship(String accountNumToRemove,String theRequest) throws Exception {
        XMLUtil.printHeading("Remove Trading Relationship");
        RequestInfo info = null;
        String      request = null;

        if(theRequest != null && theRequest.contains("<ACXTradingRelationshipRemovalRequest>")) {
            request = theRequest;
            buyPartnerId = XMLUtil.getTagValue("BuyPartnerID", request);
            sellPartnerId = XMLUtil.getTagValue("SellPartnerID", request);
            accountNumToRemove = XMLUtil.getTagValue("AccountNum", request);
        }
        else {
            request = templateReader.readTemplate("ACXTradingRelationshipRemovalRequest");      
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);
            request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);        
            request = XMLUtil.insertValueForTag(request, "</AppServiceNumber>", appServiceNum);
            request = XMLUtil.insertValueForTag(request, "</AppServiceName>", "ignored");
            request = XMLUtil.insertValueForTag(request, "</AccountNum>", accountNumToRemove);        
        }
        
        XMLUtil.printXML("Remove Trading Relationship Request:", XMLUtil.prettifyString(request));

        try {
            partnerManager.removeTradingRelationship(request);
            info = partnerManager.getRequestInfo().copy();

            System.out.println("Remove Trading Relationship successful for " + buyPartnerId + "/" + sellPartnerId);
            System.out.println("AccountNum " + accountNumToRemove);         
            System.out.println();

            System.out.println("Request time:     " + info.getTotalTime() + " millis");
            System.out.println();
            writeResult(info, "removeTradingRelationship"/*testcase*/, "BrokerID Unknown", accountNumToRemove/*Account*/);
        } catch (Exception e) {
            info = partnerManager.getRequestInfo().copy();
            writeResult(info, "removeTradingRelationship"/*testcase*/, "BrokerID Unknown", accountNumToRemove/*Account*/);

            System.out.println("removeTradingRelationship() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
    }    
    
    // Attempt a one pass part inquiry. This is a one pass part
    // inquiry because the attribute for the return receipt tag
    // is set to "No" within the template XML document.
    private void queryParts() throws Exception {
        XMLUtil.printHeading("Part Inquiry");

        String trackingNumber = null;
        String request = templateReader.readTemplate("ACXPartInquiryRequest");

        Document document = XMLUtil.createDocument(request);
        Node root = document.getDocumentElement();
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String nodeName = node.getNodeName();
            if ("Envelope".equals(nodeName)) {
                XMLUtil.setTagValue(document, node, "BuyPartnerID", buyPartnerId);                
            } else if ("RequestRouter".equals(nodeName)) {
                // Add value for SellPartnerID
                XMLUtil.setTagValue(document, node, "SellPartnerID", sellPartnerId);
                // Add value for CustNum
                XMLUtil.setTagValue(document, node, "CustNum", accountNumber);
            } else if ("PartInquiryRequest".equals(nodeName)) {
                // Add value for ID
                XMLUtil.setTagValue(document, node, "ID", "0");
                // Add value for PartNum
                XMLUtil.setTagValue(document, node, "PartNum", "22460");
                // Add value for MfgCode
                XMLUtil.setTagValue(document, node, "MfgCode", "WAL");
                // Add value for Qty
                XMLUtil.setTagValue(document, node, "Qty", "3");
                // Add value for ShipCode
                XMLUtil.setTagValue(document, node, "ShipCode", "0");
                // Add value for CatVehID
                XMLUtil.setTagValue(document, node, "CatVehID", "123456");
                // Add value for Year
                XMLUtil.setTagValue(document, node, "Year", "85");                 
                // Add value for Make
                XMLUtil.setTagValue(document, node, "Make", "DODGE");
                // Add value for Model
                XMLUtil.setTagValue(document, node, "Model", "ARIES");
                // Add value for Engine
                XMLUtil.setTagValue(document, node, "Engine", "V6");
                // Add value for SpeclCond
                XMLUtil.setTagValue(document, node, "SpeclCond", "No");
            }
        }
        String acxPartInquiryRequest = XMLUtil.documentToString(document);
        XMLUtil.printXML("Part Inquiry Request:", acxPartInquiryRequest);

        RequestInfo info = null;
        
        try {
            // Query the part(s)
            String xmlResponse = partOrderManager.queryParts(acxPartInquiryRequest);
            info = partOrderManager.getRequestInfo().copy();
            XMLUtil.printXML("Part Inquiry Response:", XMLUtil.prettifyString(xmlResponse));

            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
            writeResult(info, "queryParts"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo().copy();
            writeResult(info, "queryParts"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);

            System.out.println("queryParts() caught Exception: " + e);
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
