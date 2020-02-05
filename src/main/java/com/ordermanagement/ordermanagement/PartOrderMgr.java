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

import java.util.*;
import java.text.DateFormat;
import java.io.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import com.activant.aconnex.appsdk.*;
import com.activant.share.util.Base64; 
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
 *  Demonstrates each API through the PartOrderManager.
 */
public class PartOrderMgr {
    protected final transient Logger logger = Logger.getLogger(this.getClass().getName());
    private static String brokerPartnerId  = null;
    private static String buyPartnerId     = null;
    private static String sellPartnerId    = null;
    private static String sellPartnerId2   = null;
    private static String accountNumber    = null;
    static {
        ResourceBundle props = ResourceBundle.getBundle("AppSDKExamples", Locale.getDefault());
        brokerPartnerId = props.getString("broker.partner.id");
        buyPartnerId    = props.getString("svc.dealer.partner.id");
        sellPartnerId   = props.getString("platform.partner.id");
        sellPartnerId2  = props.getString("platform.partner.id2");
        accountNumber   = props.getString("account.number");
    }

    private PartOrderManager  partOrderManager = null;
    private XMLTemplateReader templateReader   = null;
    private boolean           do_twopass       = false;
    private String            resultsdir       = "./results";
    private String            resultsfilename  = "PartOrderMgr.csv";
    private String            resultsPath      = resultsdir + "/" + resultsfilename;
    private String            AppPath          = null;
    private FileOutputStream  os = null;
    private PrintStream       ps = null; 
    private String            today = null;
    private String            starttime = null;
    private int               seqnum = 0;
    private String            DistUser = null;
    private String            DistPass = null;
    private int               pause = 0;
    private int               delay = 0;
    private int               connect_timeout  = -1;
    private int               base_timeout     = -1;
    private int               var_base_timeout = -1;
    private int               var_line_count   = 25;
    
    public PartOrderMgr(String[] args) {
        boolean settimeouts = false;
        try {
            System.out.println();
            boolean do_all = XMLUtil.member("all", args) || args.length==0;
            boolean acxim      = XMLUtil.member("acxim", args);
            boolean noacxim    = XMLUtil.member("noacxim", args);
            boolean gotacxim  = (acxim || noacxim);
            boolean setcreds   = XMLUtil.member("setcreds", args);
            boolean do_cart    = do_all || XMLUtil.member("cart", args);
            boolean do_tireiq  = do_all || XMLUtil.member("tireiq", args);
            boolean do_tireor  = do_all || XMLUtil.member("tireor", args);
            boolean do_invoice = do_all || XMLUtil.member("invoice", args);
            boolean do_inquiry = do_all || XMLUtil.member("partInquiry", args);
            boolean do_porder  = do_all || XMLUtil.member("partOrder", args);
            boolean do_sorder  = do_all || XMLUtil.member("stockOrder", args);
            boolean do_preturn = do_all || XMLUtil.member("returnParts", args);
            boolean do_list    = do_all || XMLUtil.member("listOrders", args);
            boolean do_detail  = do_all || XMLUtil.member("orderDetail", args);
            boolean do_details = do_all || XMLUtil.member("orderDetails", args);
            boolean do_cover   = do_all || XMLUtil.member("productCoverage", args);
            boolean do_profile = do_all || XMLUtil.member("catalogProfile", args);
            boolean do_prodmap = do_all || XMLUtil.member("productLineMap", args);
            do_twopass         = XMLUtil.member("twoPass", args);
            String theRequest  = null;
            AppPath            = XMLUtil.memberAfter("apppath",args);
            String requestFile = XMLUtil.memberAfter("Request", args);
            String user        = XMLUtil.memberAfter("user",args);
            String pass        = XMLUtil.memberAfter("pass",args);
            DistUser           = XMLUtil.memberAfter("duser",args);
            DistPass           = XMLUtil.memberAfter("dpass",args);
            String orders      = XMLUtil.memberAfter("RetrieveOrders", args);
            String order       = XMLUtil.memberAfter("retrieveOrder", args);
            String brid        = XMLUtil.memberAfter("brid",args);
            String ids         = XMLUtil.memberAfter("ids", args);
            String bid         = XMLUtil.memberAfter("bid",args);
            String sid         = XMLUtil.memberAfter("sid",args);
            String sid2        = XMLUtil.memberAfter("sid2",args);
            String account     = XMLUtil.memberAfter("account",args);
            String countstr    = XMLUtil.memberAfter("count",args);
            String pausestr    = XMLUtil.memberAfter("pause",args);
            String delaystr    = XMLUtil.memberAfter("delay",args);
            String con_timeoutstr      = XMLUtil.memberAfter("con_timeout", args);
            String base_timeoutstr     = XMLUtil.memberAfter("base_timeout", args);
            String var_base_timeoutstr = XMLUtil.memberAfter("var_base_timeout", args);
            String var_line_countstr   = XMLUtil.memberAfter("var_line_count", args);
            if(brid!=null)    brokerPartnerId = brid;
            if(bid!=null)     buyPartnerId    = bid;
            if(sid!=null)     sellPartnerId   = sid;
            if(sid2!=null)    sellPartnerId2  = sid2;
            if(account!=null) accountNumber   = account;
            
            String optStr = (do_all?" all":"") +
                            (acxim?" acxim":"") +
                            (noacxim?" noacxim":"") +
                            (requestFile!=null?" RequestFile=" + requestFile:"") +
                            (user!=null?" user "+ user:"") +
                            (pass!=null?" pass "+ pass:"") +
                            (DistUser!=null?" duser "+ DistUser:"") +
                            (DistPass!=null?" dpass "+ DistPass:"") +
                            (setcreds?" setcreds":"") +
                            (brid!=null?" brid "+ brid:"") +
                            (bid!=null?" bid "+ bid:"") +
                            (sid!=null?" sid "+ sid:"") +
                            (sid2!=null?" sid2 "+ sid2:"") +
                            (account!=null?" account "+ account:"") +
                            (order!=null?" RetrieveOrder "+ orders:"") +
                            (orders!=null?" RetrieveOrders "+ orders:"") +
                            (ids!=null?" ids "+ ids:"") +
                            (countstr!=null?" count " + countstr : "") +
                            (pausestr!=null?" pause " + pausestr : "") +
                            (delaystr!=null?" delay " + delaystr : "") +
                            (con_timeoutstr!=null ? " con_timeout " + con_timeoutstr : "") +
                            (base_timeoutstr!=null ? " base_timeout " + base_timeoutstr : "") +
                            (var_base_timeoutstr!=null ? " var_base_timeout " + var_base_timeoutstr : "") +
                            (var_line_countstr!=null ? " var_line_count " + var_line_countstr : "") +
                            (do_cart?" cart":"") +
                            (do_tireiq?" tireiq":"") +
                            (do_tireor?" tireor":"") +
                            (do_invoice?" invoice":"") +
                            (do_inquiry?" partInquiry":"") +
                            (do_porder?" partOrder":"") + 
                            (do_sorder?" stockOrder":"") + 
                            (do_preturn?" returnParts":"") +
                            (do_list?" listOrders":"") +
                            (do_detail?" orderDetail":"") + 
                            (do_details?" orderDetails":"") + 
                            (do_cover?" productCoverage":"") + 
                            (do_prodmap?" productLineMap":"") + 
                            (do_profile?" catalogProfile":"") +
                            (do_twopass?" twoPass":"") +
                            (orders!=null?" retrieveOrders " + orders : "") +
                            (order!=null?" retrieveOrder " + order : "") +
                            (AppPath!=null?" apppath "+ AppPath:"");
            System.out.println("Options: " + optStr);
            logger.info("Options " + optStr);
            // Get the AppBuilder and pass in the Broker user and password if they
            // were given on the command line to override what was in the
            // AppSDK.properties file.
            AppBuilder appBuilder = null;
            if(user != null && pass != null) {
                logger.info("Creating AppBuilder with user and pass " + user + " " + pass);
                appBuilder = new AppBuilder(user,pass,null,AppPath);
            }
            else
                appBuilder = new AppBuilder(null,null,null,AppPath);

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
                    logger.info("calling appBuilder.setACXIM(" + acxim + ")");
                    appBuilder.setACXIM(acxim);
                }
            }
            partOrderManager = appBuilder.getPartOrderManager();
            // If a set of Distributed buyPartner Creds were provided then
            // call setCredentials on the partOrderManager so that it will be
            // using the Distributed BuyPartner creds.
            if(DistUser != null && DistPass != null) {
                logger.info("Calling setCredentials(" + DistUser + "," + DistPass + ")");
                partOrderManager.setCredentials(DistUser,DistPass);
            }

            if (settimeouts)
            {
                partOrderManager.setTimeouts(connect_timeout, base_timeout, var_base_timeout, var_line_count);
            }
            
            templateReader = new XMLTemplateReader(appBuilder);


            // Always override properties file with spec passed on command line
            boolean standalone = XMLUtil.member("standalone", args);
            boolean connected  = XMLUtil.member("connected", args);
            if (standalone) partOrderManager.setStandaloneMode(true);
            if (connected)  partOrderManager.setStandaloneMode(false);
            System.out.println("Running " + (partOrderManager.getStandaloneMode()?"standalone":"connected") + 
                               " via " + (appBuilder.getACXIM() ? "ACXIM" : "AConneX 1.0"));
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
                }
            }
            ArrayList SellIds = null;
            int       idindex = 0;
            if(ids != null) {
                // They gave us a list of sell partnerids to inquire into
                SellIds = new ArrayList();
                logger.info("ids=[" + ids + "]");
                if(ids.indexOf(',') >= 0) {
                    // the transactions were provided on the
                    // command line as a comma separated list.
                    String sids[] = ids.split("\\x2c");
                    logger.info("sids.length = " + sids.length);
                    for(int i=0;i<sids.length;i++) {
                        SellIds.add(sids[i]);
                    }
                }
            }

            
            int halfcount = 1;
            int count = 1;
            if(countstr != null) {
                try {
                    count = Integer.parseInt(countstr);
                } catch (Exception e) {}
            }
            // The pause here is for testing to see if we have a memory leak.
            // I want to do a bunch of iq and then pause to see
            // if the memory gets collected
            if(pausestr != null) {
                try {
                    pause = Integer.parseInt(pausestr);
                } catch (Exception e) {}
            }
            if(delaystr != null) {
                try {
                    delay = Integer.parseInt(delaystr);
                } catch (Exception e) {}
            }
            if(count > 1) {
                halfcount = count / 2;
            }
            // PartOrder API examples
            // Perform a Part Inquiry
            Runtime.getRuntime().gc();
            long freemem = Runtime.getRuntime().freeMemory();
            long totalmem = Runtime.getRuntime().totalMemory();

            if (do_cart) {
                cart(theRequest);
            }
            
            if (do_tireiq) {
                tireiq(theRequest);
            }
            
            if (do_tireor) {
                tireor(theRequest);
            }
            
            if (do_invoice) {
                invoice(theRequest);
            }
            
            if (do_inquiry) {
                System.out.println("sleeping " + pause + " seconds, free = " + freemem + " total = " + totalmem);
                logger.info("sleeping " + pause + " seconds, free = " + freemem + " total = " + totalmem);
                if(pause>0) {
                    try {
                        Thread.sleep(pause * 1000);
                    } catch (InterruptedException ex) { }
                }
                // do it once just to get size of memory after this call
                queryParts(theRequest);
                Runtime.getRuntime().gc();
                freemem = Runtime.getRuntime().freeMemory();
                totalmem = Runtime.getRuntime().totalMemory();
                System.out.println("after one iq free = " + freemem + " total = " + totalmem);
                logger.info("after one iq free = " + freemem + " total = " + totalmem);
                for(int i = 1; i< count; i++) {
                    if(SellIds != null) {
                        sellPartnerId = (String)SellIds.get(idindex);
                        idindex++;
                        if(idindex >= SellIds.size())
                            idindex = 0;
                    }
                    if(delay>0) {
                        try {
                            Thread.sleep(delay * 1000);
                        } catch (InterruptedException ex) { }
                    }
                    queryParts(theRequest);
                    if(pause>0 && i==halfcount) {
                        
                        freemem = Runtime.getRuntime().freeMemory();
                        totalmem = Runtime.getRuntime().totalMemory();
                        System.out.println("sleeping " + pause + " seconds, free = " + freemem + " total = " + totalmem);
                        logger.info("sleeping " + pause + " seconds, free = " + freemem + " total = " + totalmem);
                        Runtime.getRuntime().gc();
                        try {
                            Thread.sleep(pause * 1000);
                        } catch (InterruptedException ex) { }
                    }
                }
                freemem = Runtime.getRuntime().freeMemory();
                totalmem = Runtime.getRuntime().totalMemory();
                System.out.println("sleeping " + pause + " seconds, free = " + freemem + " total = " + totalmem);
                logger.info("sleeping " + pause + " seconds, free = " + freemem + " total = " + totalmem);
                Runtime.getRuntime().gc();
                if(pause>0) {
                    try {
                        Thread.sleep(pause * 1000);
                    } catch (InterruptedException ex) { }
                }
                freemem = Runtime.getRuntime().freeMemory();
                totalmem = Runtime.getRuntime().totalMemory();
                System.out.println("free = " + freemem + " total = " + totalmem);
                logger.info("free = " + freemem + " total = " + totalmem);
            }

            
            // Perform a Part Order
            // this will do a PartOrder if they asked for orderParts
            // or they asked for retrieveOrder. the trackingNumber 
            // of this part order will be used in the retrieveOrder call.
            String trackingNumber = null;
            if (do_porder || do_detail) {
                trackingNumber = orderParts(theRequest);
            }

            if(order != null) {
                // They asked to call retrieveOrder with the
                // transaction provided on the command line.
                do_detail = true;
                trackingNumber = order;
            }

            // Perform a Stock Order
            if (do_sorder) orderStock(theRequest);

            // Perform a Part Return
            if (do_preturn) returnParts(theRequest);

            // List orders
            // this will do listOrders if they asked for listOrders
            // or they asked for retrieveOrders
            // the array of transactionIds will then be used for
            // the retrieveOrders call
            List transactionIds = null;
            if (do_list || do_details)
                transactionIds = listOrders(theRequest);
            
            if(orders != null) {
                // They asked to call retrieveOrders with the
                // transactions provided on the command line.
                transactionIds = new ArrayList();
                do_details = true;
                if(orders.indexOf(',') >= 0) {
                    // the transactions were provided on the
                    // command line as a comma separated list.
                    String tracks[] = orders.split("\\x2c");

                    for(int i=0;i<tracks.length;i++) {
                        transactionIds.add(tracks[i]);
                    }
                }
                else {
                    // they either provided only one transaction
                    // or a filename containing the comma separated
                    // list
                    File f = new File(orders);
                    if(f.exists()) {
                        String forders = XMLUtil.readFile(f);
                        String tracks[] = forders.split("\\x2c");

                        for(int i=0;i<tracks.length;i++) {
                            transactionIds.add(tracks[i]);
                        }
                    }
                    else {
                        transactionIds.add(orders);
                    }
                }
            }

            // Order details
            if (do_details && transactionIds != null) {
                retrieveOrders(transactionIds,theRequest);
            }

            // Perform an Order Detail Request if we have a valid tracking number
            if (do_detail && trackingNumber != null) {
                if (partOrderManager.getStandaloneMode()) {
                    System.out.println("***");
                    System.out.println("*** Sorry, Order Detail is not yet implemented in standalone mode");
                    System.out.println("***");
                    System.out.println("");
                } else {
                    if(pause>0) {
                        System.out.println("pausing " + pause + " seconds before retrieving order");
                        try {
                            Thread.sleep(pause * 1000);
                        } catch (InterruptedException ex) { }
                    }
                    retrieveOrder(trackingNumber,theRequest); // Incorrect Response - Tracker #1004
                }
            }

            // Get the Product Coverage
            if (do_cover) retrieveProductCoverage(theRequest);

            // Get the Product Line Map
            if (do_prodmap) retrieveProductLineMap(theRequest);

            // Update a Catalog Profile
            if (do_profile) updateCatalogProfile(theRequest);

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

    // Attempt a Cart Inquiry Request.
    private void cart(String theRequest) throws Exception {
        XMLUtil.printHeading("Cart Request");
        String      trackingNumber = null;
        RequestInfo info           = null;
        String      request        = null;
        
        if(theRequest != null && theRequest.contains("<ACXCartInquiryRequest>"))
            request = theRequest;
        else {
            request = templateReader.readTemplate("ACXCartInquiryRequest");

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
                } else if ("CartInquiryRequest".equals(nodeName)) {
                    // Add value for ID
                    XMLUtil.setTagValue(document, node, "ID", "0");
                    // Add value for PartNum
                    XMLUtil.setTagValue(document, node, "PartNum", "22460");
                    // Add value for MfgCode
                    XMLUtil.setTagValue(document, node, "MfgCode", "WAL");
                    // Add value for Qty
                    XMLUtil.setTagValue(document, node, "Qty", "1");
                    // Add wildcard value for PONumber
                    XMLUtil.setTagValue(document, node, "UnitCost", "3.33");
                }
            }
            request = XMLUtil.documentToString(document);
        }
        XMLUtil.printXML("Cart Request:", request);

        
        try {
            // Query the part(s)
            String xmlResponse = partOrderManager.queryPartsInCart(request);
            info = partOrderManager.getRequestInfo();
            XMLUtil.printXML("Cart Response:", XMLUtil.prettifyString(xmlResponse));

            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
            writeResult(info, "cart"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo();
            writeResult(info, "cart"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);

            System.out.println("cart() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
            
        }
    }

    // Attempt a Tire Inquiry Request.
    private void tireiq(String theRequest) throws Exception {
        XMLUtil.printHeading("TireIQ Request");
        String      trackingNumber = null;
        RequestInfo info           = null;
        String      request        = null;
        
        if(theRequest != null && theRequest.contains("<ACXTireInquiryRequest>"))
            request = theRequest;
        else {
            request = templateReader.readTemplate("ACXTireInquiryRequest");

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
                } else if ("TireInquiryRequest".equals(nodeName)) {
                    // Add value for ID
                    XMLUtil.setTagValue(document, node, "ID", "0");
                    // Add value for TireSize->Width
                    XMLUtil.setTagValue(document, node, "Width", "225");
                    // Add value for TireSize->Ratio
                    XMLUtil.setTagValue(document, node, "Ratio", "65");
                    // Add value for TireSize->Diameter
                    XMLUtil.setTagValue(document, node, "Diameter", "17");
                    // Add value for Qty
                    XMLUtil.setTagValue(document, node, "Qty", "1");
                }
            }
            request = XMLUtil.documentToString(document);
        }
        XMLUtil.printXML("TireIQ Request:", request);

        
        try {
            // Query the part(s)
            String xmlResponse = partOrderManager.queryTires(request);
            info = partOrderManager.getRequestInfo();
            XMLUtil.printXML("TireIQ Response:", XMLUtil.prettifyString(xmlResponse));

            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
            writeResult(info, "tireiq"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo();
            writeResult(info, "tireiq"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);

            System.out.println("tireiq() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
            
        }
    }

    // Attempt a Tire Order Request.
    private void tireor(String theRequest) throws Exception {
        XMLUtil.printHeading("TireOR Request");
        String      trackingNumber = null;
        RequestInfo info           = null;
        String      request        = null;
        
        if(theRequest != null && theRequest.contains("<ACXTireOrderRequest>"))
            request = theRequest;
        else {
            request = templateReader.readTemplate("ACXTireOrderRequest");

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
                } else if ("TireOrderRequest".equals(nodeName)) {
                    // Add value for ID
                    XMLUtil.setTagValue(document, node, "ID", "0");
                    // Add value for PartNum
                    XMLUtil.setTagValue(document, node, "PartNum", "34657");
                    // Add value for MfgCode
                    XMLUtil.setTagValue(document, node, "MfgCode", "MICHELIN");
                    // Add value for Qty
                    XMLUtil.setTagValue(document, node, "Qty", "1");
                    DateFormat myformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                    myformat.setTimeZone(TimeZone.getTimeZone("GMT"));
                    String timestamp = myformat.format(new Date(System.currentTimeMillis()));
                    // Add value for TimePlaced
                    XMLUtil.setTagValue(document, node, "TimePlaced", timestamp);
                }
            }
            request = XMLUtil.documentToString(document);
        }
        XMLUtil.printXML("TireOR Request:", request);

        
        try {
            // Query the part(s)
            String xmlResponse = partOrderManager.orderTires(request);
            info = partOrderManager.getRequestInfo();
            XMLUtil.printXML("TireOR Response:", XMLUtil.prettifyString(xmlResponse));

            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
            writeResult(info, "tireor"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo();
            writeResult(info, "tireor"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);

            System.out.println("tireor() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
            
        }
    }

    // Attempt an invoice Request.
    private void invoice(String theRequest) throws Exception {
        XMLUtil.printHeading("Invoice Request");
        String      trackingNumber = null;
        RequestInfo info           = null;
        String      request        = null;
        
        if(theRequest != null && theRequest.contains("<ACXInvoiceRequest>"))
            request = theRequest;
        else {
            request = templateReader.readTemplate("ACXInvoiceRequest");

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
                } else if ("InvoiceRequest".equals(nodeName)) {
                    // Add value for ID
                    XMLUtil.setTagValue(document, node, "ID", "0");
                    // Add wildcard value for PONumber
                    XMLUtil.setTagValue(document, node, "PONumber", "*");
                }
            }
            request = XMLUtil.documentToString(document);
        }
        XMLUtil.printXML("Invoice Request:", request);

        
        try {
            // Query the part(s)
            String xmlResponse = partOrderManager.invoiceRequest(request);
            info = partOrderManager.getRequestInfo();
            XMLUtil.printXML("Invoice Response:", XMLUtil.prettifyString(xmlResponse));

            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
            writeResult(info, "invoice"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo();
            writeResult(info, "invoice"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);

            System.out.println("invoice() caught Exception: " + e);
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
    private void queryParts(String theRequest) throws Exception {
        XMLUtil.printHeading("Part Inquiry");
        String      trackingNumber = null;
        RequestInfo info           = null;
        String      request        = null;
        
        if(theRequest != null && theRequest.contains("<ACXPartInquiryRequest>"))
            request = theRequest;
        else {
            request = templateReader.readTemplate("ACXPartInquiryRequest");

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
            request = XMLUtil.documentToString(document);
        }
        XMLUtil.printXML("Part Inquiry Request:", request);

        
        try {
            // Query the part(s)
            String xmlResponse = partOrderManager.queryParts(request);
            info = partOrderManager.getRequestInfo();
            XMLUtil.printXML("Part Inquiry Response:", XMLUtil.prettifyString(xmlResponse));

            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
            writeResult(info, "queryParts"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo();
            writeResult(info, "queryParts"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);

            System.out.println("queryParts() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
            
        }
    }


    // Attempt a one pass part return. This is a one pass part
    // return because the attribute for the return receipt tag
    // is set to "No" within the template XML document.
    private void returnParts(String theRequest) throws Exception {
        XMLUtil.printHeading("Part Return");
        RequestInfo info           = null;
        String      xmlResponse    = null;
        String      trackingNumber = null;
        String      request        = null;
        
        if(theRequest != null && theRequest.contains("<ACXPartReturnRequest>"))
            request = theRequest;
        else {
            request = templateReader.readTemplate("ACXPartReturnRequest");

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
                } else if ("PartReturnRequest".equals(nodeName)) {
                    // Add value for ID
                    XMLUtil.setTagValue(document, node, "ID", "0");
                    // Add value for PartNum
                    XMLUtil.setTagValue(document, node, "PartNum", "22460");
                    // Add value for MfgCode
                    XMLUtil.setTagValue(document, node, "MfgCode", "WAL");
                    // Add value for Qty
                    XMLUtil.setTagValue(document, node, "Qty", "3");
                    // Add value for UnitCost
                    XMLUtil.setTagValue(document, node, "UnitCost", "4.50");
                    // Add value for BuyerDesc
                    XMLUtil.setTagValue(document, node, "BuyerDesc", "Mufflers");
                    // Add value for Msg
                    XMLUtil.setTagValue(document, node, "Msg", "Wrong parts ordered");
                    // Add value for ReturnCode
                    XMLUtil.setTagValue(document, node, "ReturnCode", "Wrong parts ordered");
                    // Add value for OriginalInvoice
                    XMLUtil.setTagValue(document, node, "OriginalInvoice", "12345");
                    // Add value for ReturnAuthorization
                    XMLUtil.setTagValue(document, node, "ReturnAuthorization", "54321");

                }
            }
            request = XMLUtil.documentToString(document);
        }
        XMLUtil.printXML("Part Return Request:", request);
        
        // send the part return
        try {
            xmlResponse = partOrderManager.returnParts(request);
            info = partOrderManager.getRequestInfo().copy();
            XMLUtil.printXML("Part Return Response:", XMLUtil.prettifyString(xmlResponse));

            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
            writeResult(info, "returnParts"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo().copy();
            writeResult(info, "returnParts"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);

            System.out.println("returnParts() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
            
        }
    }


    // Order Parts
    // This is a one pass (synchronous) request.
    // A two pass is asynchronous and would give a return receipt.
    private String orderParts(String theRequest) throws Exception {
        XMLUtil.printHeading("Part Order");
        RequestInfo info           = null;
        String      trackingNumber = null;
        String      request        = null;
        
        if(theRequest != null && theRequest.contains("<ACXPartOrderRequest>"))
            request = theRequest;
        else {
            request = templateReader.readTemplate("ACXPartOrderRequest");
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
                } else if ("PartOrderRequest".equals(nodeName)) {
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
            request = XMLUtil.documentToString(document);
        }
        XMLUtil.printXML("Part Order Request:", request);

        try {
            // Now place the order
            String xmlResponse = partOrderManager.orderParts(request);
            info = partOrderManager.getRequestInfo().copy();
            XMLUtil.printXML("Part Order Response:", XMLUtil.prettifyString(xmlResponse));

            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
            writeResult(info, "orderParts"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo().copy();
            writeResult(info, "orderParts"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);

            System.out.println("orderParts() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
            
        }

        return info.getTrackNum();    
    }

    // Get some transaction ids for the buyer
    private List listOrders(String theRequest) throws Exception {
        XMLUtil.printHeading("List Orders");
        RequestInfo info    = null;
        String      request = null;
        
        if(theRequest != null && theRequest.contains("<ACXOrderHistoryRequest>"))
            request = theRequest;
        else {
            request = templateReader.readTemplate("ACXOrderHistoryRequest");       
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);
            request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);

            // Set the begin and end date for orders to search for to today.
            long now = System.currentTimeMillis();
            //String date = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(now+86400000));
            String date = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date());
            request = XMLUtil.insertValueForTag(request, "</BeginDate>", date);
            request = XMLUtil.insertValueForTag(request, "</EndDate>", date);
        }
        
        XMLUtil.printXML("List Orders Request:", XMLUtil.prettifyString(request));
        if(pause>0) {
            System.out.println("pausing " + pause + " seconds before listing orders");
            try {
                Thread.sleep(pause * 1000);
            } catch (InterruptedException ex) { }
        }

        try {
            // Get the transactionIds
            String xmlResponse = partOrderManager.listOrders(request);
            info = partOrderManager.getRequestInfo().copy();
            XMLUtil.printXML("List Orders Response:", XMLUtil.prettifyString(xmlResponse));

            System.out.println("listOrders");
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
            List transactionIds = getTransactionIds(xmlResponse);        
            writeResult(info, "listOrders"/*testcase*/, "BrokerID Unknown", ""/*Account*/);
            return transactionIds;
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo().copy();
            writeResult(info, "listOrders"/*testcase*/, "BrokerID Unknown", ""/*Account*/);

            System.out.println("listOrders() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();

        }

        List transactionIds = new ArrayList();
        return transactionIds;
    }
    private List getTransactionIds(String xml) throws Exception {
        List list = new ArrayList();

        Document document = XMLUtil.createDocument(xml);
        NodeList nodes = document.getElementsByTagName("TransactionId");
        for (int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            list.add(XMLUtil.getTagValue(node));
        }
        return list;
    }

    // Get details for the specified transaction ids.
    private void retrieveOrders(List transactionIds,String theRequest) throws Exception {
        RequestInfo info    = null;
        String      request = null;
        
        if(theRequest != null && theRequest.contains("<ACXOrderDetailsRequest>")) {
            request = theRequest;
            try {
                XMLUtil.printXML("Order Details Request:", XMLUtil.prettifyString(request));

                String xmlResponse = partOrderManager.retrieveOrders(request);
                info = partOrderManager.getRequestInfo().copy();
                XMLUtil.printXML("Order Details Response:", XMLUtil.prettifyString(xmlResponse));

                System.out.println("retrieveOrders");
                System.out.println("  Total time:     " + info.getTotalTime() + " millis");
                System.out.println("  Gateway access: " + info.getGatewayAccessTime());
                System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
                System.out.println();
                writeResult(info, "retrieveOrders"/*testcase*/, "BrokerID Unknown", ""/*Account*/);
            } catch (Exception e) {
                info = partOrderManager.getRequestInfo().copy();
                writeResult(info, "retrieveOrders"/*testcase*/, "BrokerID Unknown", ""/*Account*/);

                System.out.println("retrieveOrders() caught Exception: " + e);
                System.out.println("ACXTrackNum: " + info.getTrackNum());
                System.out.println("  Total time:     " + info.getTotalTime() + " millis");
                System.out.println("  Gateway access: " + info.getGatewayAccessTime());
                System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
                System.out.println();
            
            }
            return;
        }
        for (int i=0; i < transactionIds.size();) {
            request = templateReader.readTemplate("ACXOrderDetailsRequest"); 
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);

            // send off a request of 10 at a time since that is the maximum
            // amount that we may supply.
            for (int j=0; j<10; j++) {
                request = XMLUtil.insertValueForTag(request,
                                                    "</OrderDetails>",
                                                    "<TransactionId>" + transactionIds.get(i) + "</TransactionId>");
                if (++i == transactionIds.size()) break;
            }

            try {
                XMLUtil.printXML("Order Details Request:", XMLUtil.prettifyString(request));

                String xmlResponse = partOrderManager.retrieveOrders(request);
                info = partOrderManager.getRequestInfo().copy();
                XMLUtil.printXML("Order Details Response:", XMLUtil.prettifyString(xmlResponse));

                System.out.println("retrieveOrders");
                System.out.println("  Total time:     " + info.getTotalTime() + " millis");
                System.out.println("  Gateway access: " + info.getGatewayAccessTime());
                System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
                System.out.println();
                writeResult(info, "retrieveOrders"/*testcase*/, "BrokerID Unknown", ""/*Account*/);
            } catch (Exception e) {
                info = partOrderManager.getRequestInfo().copy();
                writeResult(info, "retrieveOrders"/*testcase*/, "BrokerID Unknown", ""/*Account*/);

                System.out.println("retrieveOrders() caught Exception: " + e);
                System.out.println("ACXTrackNum: " + info.getTrackNum());
                System.out.println("  Total time:     " + info.getTotalTime() + " millis");
                System.out.println("  Gateway access: " + info.getGatewayAccessTime());
                System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
                System.out.println();
            
            }
        }    
    }

    private void orderStock(String theRequest) throws Exception {
        XMLUtil.printHeading("Order Stock");
        RequestInfo info    = null;
        String      request = null;
        
        if(theRequest != null && theRequest.contains("<ACXStockOrderRequest>"))
            request = theRequest;
        else {
            request = templateReader.readTemplate("ACXStockOrderRequest");        
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);        
            request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);        
            request = XMLUtil.insertValueForTag(request, "</CustNum>", accountNumber);        
        }
        
        XMLUtil.printXML("Order Stock Request:", XMLUtil.prettifyString(request));

        try {
            // Order the Stock
            String xmlResponse = partOrderManager.orderStock(request);
            info = partOrderManager.getRequestInfo().copy();
            XMLUtil.printXML("Order Stock Response:", XMLUtil.prettifyString(xmlResponse));

            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
            writeResult(info, "orderStock"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo().copy();
            writeResult(info, "orderStock"/*testcase*/, "BrokerID Unknown", accountNumber/*Account*/);

            System.out.println("orderStock() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
            
        }
    }


    // Perform an Order Detail Request
    private void retrieveOrder(String trackingNumber,String theRequest) throws Exception {
        XMLUtil.printHeading("Order Detail Request");
        boolean     pending = true;
        RequestInfo info    = null;
        String      request = null;
        
        if(theRequest != null && theRequest.contains("<ACXOrderDetailRequest>"))
            request = theRequest;
        else {
            request = templateReader.readTemplate("ACXOrderDetailRequest");        
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);       
            request = XMLUtil.insertValueForTag(request, "</ACXTrackNum>", trackingNumber);
        }
        XMLUtil.printXML("Order Detail Request:", XMLUtil.prettifyString(request));


        // Now use the tracking number to get the details
        try {
            String xmlResponse = partOrderManager.retrieveOrder(request);
            info = partOrderManager.getRequestInfo().copy();
            XMLUtil.printXML("Order Detail Response:", XMLUtil.prettifyString(xmlResponse));

            System.out.println("retrieveOrder");
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
            writeResult(info, "retrieveOrder"/*testcase*/, "BrokerID Unknown", ""/*Account*/);
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo().copy();
            writeResult(info, "retrieveOrder"/*testcase*/, "BrokerID Unknown", ""/*Account*/);

            System.out.println("retrieveOrder() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
    }

    // Poll at intervals of intervalSecs for a valid response to the  
    // request represented by trackNum for a total of waitSecs.
    private String retrievePendingResponse(String trackNum, 
                                           int waitSecs, 
                                           int intervalSecs) throws Exception 
    {
        RequestInfo info        = null;
        boolean     pending     = true;
        String      xmlResponse = null;
        long        startTm     = System.currentTimeMillis();
        long        endTm       = startTm + (waitSecs * 1000);

        // Use the tracking number to check for response
        String request = templateReader.readTemplate("ACXOrderDetailRequest");        
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);       
        request = XMLUtil.insertValueForTag(request, "</ACXTrackNum>", trackNum);
        XMLUtil.printXML("Checking for pending response for: " + trackNum, 
                         XMLUtil.prettifyString(request));

        while (System.currentTimeMillis() < endTm) {
            try {
                Thread.sleep(intervalSecs * 1000);
            } catch (InterruptedException ex) {
            }

            try {
                xmlResponse = partOrderManager.retrieveOrder(request);
                // Got some kind of non-error response. See if it is an ACXNotificationResponse.
                // check to see if this is a pending notification response
                // If it is a pending response then stay in this loop.
                // if it is not a pending response then it must be the
                // long-awaited response (which could be an ACXNotificationResponse)
                if(!AppSDKXMLUtil.IsPending(xmlResponse, info))
                    return xmlResponse;

                XMLUtil.printXML("Request " + trackNum + " is still pending:", 
                                 XMLUtil.prettifyString(xmlResponse));
            } catch (Exception e) {
                info = partOrderManager.getRequestInfo().copy();

                //TODO - maybe write result to file.
                
                System.out.println("retrievePendingResponse() caught Exception: " + e);
                System.out.println("ACXTrackNum: " + info.getTrackNum());
                System.out.println("  Total time:     " + info.getTotalTime() + " millis");
                System.out.println("  Gateway access: " + info.getGatewayAccessTime());
                System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
                System.out.println();
            }

        }

        System.out.println();
        System.out.println("*** Request Timeout ***");
        System.out.println("Response not received fpr " + trackNum + " after " + waitSecs + " seconds");
        System.out.println();
        return null;
    }

    // Get the seller's product coverage via a 2-pass request sequence.
    private void retrieveProductCoverage(String theRequest) throws Exception {
        XMLUtil.printHeading("Retrieve Product Coverage");
        String      trackNum = null;
        RequestInfo info     = null;
        String      request  = null;
        
        if(theRequest != null && theRequest.contains("<ACXProductCoverageRequest>")) {
            request = theRequest;
            // I should also set do_twopass here from ReturnReceipt attribute in request.
            // but I don't currently have an easy method to get an attribute.
            // It should have been set from the command line param.
        }
        else {
            request = templateReader.readTemplate("ACXProductCoverageRequest");
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", brokerPartnerId);        
            request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);               
            request = XMLUtil.insertValueForTag(request, "</CustNum>", "ignored"); 
            if(do_twopass) {
                // Set flag indicating this will be a 2-pass request
                request = XMLUtil.setAttrValue(request, "ReturnReceipt", "value", "Yes");   
            }
        }
        XMLUtil.printXML("Retrieve Product Coverage Request:", XMLUtil.prettifyString(request));
        long startTm = System.currentTimeMillis();
        try {
            String xmlResponse = partOrderManager.retrieveProductCoverage(request);
            if(do_twopass) {
                info = partOrderManager.getRequestInfo().copy();
                writeResult(info, "retrieveProductCoverage twopass"/*testcase*/, brokerPartnerId, ""/*Account*/);
                XMLUtil.printXML("Retrieve Product Coverage pass 1 response:", XMLUtil.prettifyString(xmlResponse));

                trackNum = XMLUtil.getTagValue("ACXTrackNum", xmlResponse);
                System.out.println("-- Waiting up to 3 minutes for response to: " + trackNum + " --");
                System.out.println();
                xmlResponse = retrievePendingResponse(trackNum, 180, 5);
                if (xmlResponse == null) return; // message already displayed
            }

            // should now have a valid Product Coverage Response or an
            // ACXNotificationResponse saying the platform does not support this type
            // of document.
            long totalTm = System.currentTimeMillis() - startTm;
            XMLUtil.printXML("Retrieve Product Coverage Response:", XMLUtil.prettifyString(xmlResponse));

            if(!do_twopass) {
                trackNum = XMLUtil.getTagValue("ACXTrackNum", xmlResponse);
                info = partOrderManager.getRequestInfo().copy();
            }

            System.out.println("ACXTrackNum: " + trackNum);
            System.out.println("  Total time:     " + totalTm + " millis");
            System.out.println();
            writeResult(info, "retrieveProductCoverage"/*testcase*/, brokerPartnerId, ""/*Account*/);
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo().copy();
            if(do_twopass)
                writeResult(info, "retrieveProductCoverage twopass"/*testcase*/, brokerPartnerId, ""/*Account*/);
            else
                writeResult(info, "retrieveProductCoverage"/*testcase*/, brokerPartnerId, ""/*Account*/);

            System.out.println("retrieveProductCoverage() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }

    }

    // Get the seller's product line map via a 2-pass request sequence.
    private void retrieveProductLineMap(String theRequest) throws Exception {
        XMLUtil.printHeading("Retrieve Product Line Map");
        String      trackNum = null;
        RequestInfo info     = null;
        String      request  = null;
        
        if(theRequest != null && theRequest.contains("<ACXProductLineMapRequest>")) {
            request = theRequest;
            // I should also set do_twopass here from ReturnReceipt attribute in request.
            // but I don't currently have an easy method to get an attribute.
            // It should have been set from the command line param.
        }
        else {
            request = templateReader.readTemplate("ACXProductLineMapRequest");
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", brokerPartnerId);        
            request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);               
            request = XMLUtil.insertValueForTag(request, "</CustNum>", "ignored"); 
            request = XMLUtil.insertValueForTag(request, "</MapToPartnerID>", sellPartnerId2);   
            if(do_twopass) {
                // Set flag indicating this will be a 2-pass request
                request = XMLUtil.setAttrValue(request, "ReturnReceipt", "value", "Yes");   
            }
        }

        XMLUtil.printXML("Retrieve Product Line Map Request:", XMLUtil.prettifyString(request));
        long startTm = System.currentTimeMillis();
        try {
            String xmlResponse = partOrderManager.retrieveProductLineMap(request);

            if(do_twopass) {
                info = partOrderManager.getRequestInfo().copy();
                writeResult(info, "retrieveProductLineMap twopass"/*testcase*/, brokerPartnerId, ""/*Account*/);
                XMLUtil.printXML("Retrieve Product Line Map pass 1 response:", XMLUtil.prettifyString(xmlResponse));
                trackNum = XMLUtil.getTagValue("ACXTrackNum", xmlResponse);
                System.out.println("-- Waiting up to 3 minutes for response to: " + trackNum + " --");
                System.out.println();

                xmlResponse = retrievePendingResponse(trackNum, 180, 5);
                if (xmlResponse == null) return; // message already displayed
            }

            // should now have a valid Product Line Map Response or an
            // ACXNotificationResponse saying the platform does not support this type
            // of document.
            long totalTm = System.currentTimeMillis() - startTm;
            XMLUtil.printXML("Retrieve Product Line Map Response:", XMLUtil.prettifyString(xmlResponse));

            if(!do_twopass) {
                trackNum = XMLUtil.getTagValue("ACXTrackNum", xmlResponse);
                info = partOrderManager.getRequestInfo().copy();
            }

            System.out.println("ACXTrackNum: " + trackNum);
            System.out.println("  Total time:     " + totalTm + " millis");
            System.out.println();
            writeResult(info, "retrieveProductLineMap"/*testcase*/, brokerPartnerId, ""/*Account*/);
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo().copy();
            if(do_twopass)
                writeResult(info, "retrieveProductLineMap twopass"/*testcase*/, brokerPartnerId, ""/*Account*/);
            else
                writeResult(info, "retrieveProductLineMap"/*testcase*/, brokerPartnerId, ""/*Account*/);

            System.out.println("retrieveProductLineMap() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
    }

    // Get the seller's catalog profile via a 2-pass request sequence.
    private void updateCatalogProfile(String theRequest) throws Exception {
        XMLUtil.printHeading("Update Catalog Profile");
        String      trackNum    = null;
        String      xmlResponse = null;
        RequestInfo info        = null;
        String      account     = accountNumber;
        String      defaultacct = "default";
        String      request     = null;
        
        if(theRequest != null && theRequest.contains("<ACXCatalogProfileRequest>")) {
            request = theRequest;
            account = XMLUtil.getTagValue("CustNum", request);
            // I should also set do_twopass here from ReturnReceipt attribute in request.
            // but I don't currently have an easy method to get an attribute.
            // It should have been set from the command line param.
        }
        else {
            // Get the buyPartner's MCL from the platform
            request = templateReader.readTemplate("ACXCatalogProfileRequest");        
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);        
            request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);        
            request = XMLUtil.insertValueForTag(request, "</CustNum>", account); 
            if(do_twopass) {
                // Set flag indicating this will be a 2-pass request
                request = XMLUtil.setAttrValue(request, "ReturnReceipt", "value", "Yes");   
            }
        }

        try {
            XMLUtil.printXML("Update Catalog Profile Request:", XMLUtil.prettifyString(request));
            xmlResponse = partOrderManager.updateCatalogProfile(request);

            if(do_twopass) {
                info = partOrderManager.getRequestInfo().copy();
                writeResult(info, "updateCatalogProfile twopass"/*testcase*/, brokerPartnerId, account/*Account*/);
                XMLUtil.printXML("Update Catalog Profile pass 1 response:", XMLUtil.prettifyString(xmlResponse));
                trackNum = XMLUtil.getTagValue("ACXTrackNum", xmlResponse);
                System.out.println("-- Waiting up to 3 minutes for response to: " + trackNum + " --");
                System.out.println();

                xmlResponse = retrievePendingResponse(trackNum, 180, 5);
                if (xmlResponse == null) return; // message already displayed
            }

            // Should now have a valid catalog profile response
            XMLUtil.printXML("Update Catalog Profile Response:", XMLUtil.prettifyString(xmlResponse));
            processMCL(xmlResponse, sellPartnerId, account);
            info = partOrderManager.getRequestInfo().copy();
            writeResult(info, "updateCatalogProfile"/*testcase*/, brokerPartnerId, account/*Account*/);

        } catch (Exception e) {
            info = partOrderManager.getRequestInfo().copy();
            if(do_twopass)
                writeResult(info, "updateCatalogProfile twopass"/*testcase*/, brokerPartnerId, account/*Account*/);
            else
                writeResult(info, "updateCatalogProfile"/*testcase*/, brokerPartnerId, account/*Account*/);

            System.out.println("updateCatalogProfile() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
        
        // if they gave us a request then we did the request above. return here
        // so we don't do more requests.
        if(theRequest != null && theRequest.contains("<ACXCatalogProfileRequest>"))
            return;

        try {
            // Get the default MCL from the platform
            request = templateReader.readTemplate("ACXCatalogProfileRequest");        
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", brokerPartnerId);        
            request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);        
            request = XMLUtil.insertValueForTag(request, "</CustNum>", defaultacct); 
            if(do_twopass) {
                // Set flag indicating this will be a 2-pass request
                request = XMLUtil.setAttrValue(request, "ReturnReceipt", "value", "Yes");   
            }

            XMLUtil.printXML("Update Catalog Profile Request:", XMLUtil.prettifyString(request));
            long startTm = System.currentTimeMillis();
            xmlResponse = partOrderManager.updateCatalogProfile(request);

            if(do_twopass) {
                info = partOrderManager.getRequestInfo().copy();
                writeResult(info, "updateCatalogProfile twopass"/*testcase*/, brokerPartnerId, defaultacct/*Account*/);
                XMLUtil.printXML("Update Catalog Profile pass 1 response:", XMLUtil.prettifyString(xmlResponse));
                trackNum = XMLUtil.getTagValue("ACXTrackNum", xmlResponse);
                System.out.println("-- Waiting up to 3 minutes for response to: " + trackNum + " --");
                System.out.println();

                xmlResponse = retrievePendingResponse(trackNum, 180, 5);
                if (xmlResponse == null) return; // message already displayed
            }
            long totalTm = System.currentTimeMillis() - startTm;

            // Should now have a valid catalog profile response
            XMLUtil.printXML("Update Catalog Profile Response:", XMLUtil.prettifyString(xmlResponse));
            processMCL(xmlResponse, sellPartnerId, defaultacct);

            if(!do_twopass) {
                trackNum = XMLUtil.getTagValue("ACXTrackNum", xmlResponse);
                info = partOrderManager.getRequestInfo().copy();
            }

            System.out.println("ACXTrackNum: " + trackNum);
            System.out.println("  Total time:     " + totalTm + " millis");
            System.out.println();
            writeResult(info, "updateCatalogProfile"/*testcase*/, brokerPartnerId, defaultacct/*Account*/);
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo().copy();
            if(do_twopass)
                writeResult(info, "updateCatalogProfile twopass"/*testcase*/, brokerPartnerId, defaultacct/*Account*/);
            else
                writeResult(info, "updateCatalogProfile"/*testcase*/, brokerPartnerId, defaultacct/*Account*/);

            System.out.println("updateCatalogProfile() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
    }
    void processMCL(String xmlResponse, String partnerID, String acct) throws Exception {
        // First get the Base64-encoded file data
        String data = XMLUtil.getTagValue("File", xmlResponse);
        if (data == null) {
            throw new Exception("Expected response to contain MCL file data");
        }

        // Decode into a byte array
        char[] charData = new char[data.length()];
        data.getChars(0, charData.length, charData, 0);
        byte[] rawData = Base64.decode(charData);

        // Manage file names
        String baseName = "." + File.separator + "MCL_" + partnerID +
                          (""==acct ? "" : "_" + acct);
        String b64FileName = baseName + "_base64.txt";
        String decodedFileName = baseName + "_decoded.dat";

        File f1 = new File(b64FileName);
        File f2 = new File(decodedFileName);
        f1.delete();
        f2.delete();

        // Write information to files
        XMLUtil.writeFile(f1, data);
        XMLUtil.writeFile(f2, rawData);

        System.out.println();
        System.out.println("Wrote base 64 data to " + b64FileName);
        System.out.println("Wrote decoded data to " + decodedFileName);
        System.out.println();
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
    
	/*
	 * public static void main (String[] args) { new PartOrderMgr(args); }
	 */

}

