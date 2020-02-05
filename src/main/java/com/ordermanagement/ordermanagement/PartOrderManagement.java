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

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.activant.aconnex.appsdk.AppBuilder;
import com.activant.aconnex.appsdk.AppSDKException;
import com.activant.aconnex.appsdk.AppSDKGatewayException;
import com.activant.aconnex.appsdk.AppSDKXMLParserException;
import com.activant.aconnex.appsdk.AppSDKXMLUtil;
import com.activant.aconnex.appsdk.PartOrderManager;
import com.activant.aconnex.appsdk.RequestInfo;
import com.activant.share.util.Base64; 

/**
 *  Demonstrates each API through the PartOrderManager.
 */
public class PartOrderManagement {
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

    public PartOrderManagement(String[] args) {
        try {
            // Get the AppBuilder
            AppBuilder appBuilder = new AppBuilder();
            partOrderManager = appBuilder.getPartOrderManager();
            templateReader = new XMLTemplateReader(appBuilder);

            System.out.println();
            boolean do_all = XMLUtil.member("all", args) || args.length==0;
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
            do_twopass = XMLUtil.member("twoPass", args);
            String orders = XMLUtil.memberAfter("RetrieveOrders", args);
            String order  = XMLUtil.memberAfter("retrieveOrder", args);
            String optStr = (do_all?" all":"") + 
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
                            (order!=null?" retrieveOrder " + order : "");
            System.out.println("Options: " + optStr);

            // Always override properties file with spec passed on command line
            boolean standalone = XMLUtil.member("standalone", args);
            boolean connected  = XMLUtil.member("connected", args);
            if (standalone) partOrderManager.setStandaloneMode(true);
            if (connected)  partOrderManager.setStandaloneMode(false);
            System.out.println("Running " + (partOrderManager.getStandaloneMode()?"standalone":"connected") + 
                               " via " + (appBuilder.getACXIM() ? "ACXIM" : "AConneX 1.0"));
            
            System.out.println();


            if (do_invoice) {
                invoice();
            }
            
            // Perform a Part Inquiry
            if (do_inquiry) queryParts();

            
            // Perform a Part Order
            // this will do a PartOrder if they asked for orderParts
            // or they asked for retrieveOrder. the trackingNumber 
            // of this part order will be used in the retrieveOrder call.
            String trackingNumber = null;
            if (do_porder || do_detail) {
                trackingNumber = orderParts();
            }

            if(order != null) {
                // They asked to call retrieveOrder with the
                // transaction provided on the command line.
                do_detail = true;
                trackingNumber = order;
            }

            // Perform a Stock Order
            if (do_sorder) orderStock();

            // Perform a Part Return
            if (do_preturn) returnParts();

            // List orders
            // this will do listOrders if they asked for listOrders
            // or they asked for retrieveOrders
            // the array of transactionIds will then be used for
            // the retrieveOrders call
            List transactionIds = null;
            if (do_list || do_details)
                transactionIds = listOrders();
            
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
                retrieveOrders(transactionIds);
            }

            // Perform an Order Detail Request if we have a valid tracking number
            if (do_detail && trackingNumber != null) {
                if (partOrderManager.getStandaloneMode()) {
                    System.out.println("***");
                    System.out.println("*** Sorry, Order Detail is not yet implemented in standalone mode");
                    System.out.println("***");
                    System.out.println("");
                } else {
                    retrieveOrder(trackingNumber); // Incorrect Response - Tracker #1004
                }
            }

            // Get the Product Coverage
            if (do_cover) retrieveProductCoverage();

            // Get the Product Line Map
            if (do_prodmap) retrieveProductLineMap();

            // Update a Catalog Profile
            if (do_profile) updateCatalogProfile();

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

    // Attempt an invoice Request.
    private void invoice() throws Exception {
        XMLUtil.printHeading("Invoice Request");
        RequestInfo info           = null;
        String      request        = null;
        
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
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo();

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
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo().copy();

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
    private void returnParts() throws Exception {
        XMLUtil.printHeading("Part Return");

        String trackingNumber = null;
        String request = templateReader.readTemplate("ACXPartReturnRequest");

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
        String acxPartReturnRequest = XMLUtil.documentToString(document);
        XMLUtil.printXML("Part Return Request:", acxPartReturnRequest);

        // send the part return
        String xmlResponse = null;
        try {
            xmlResponse = partOrderManager.returnParts(acxPartReturnRequest);
            RequestInfo info = partOrderManager.getRequestInfo().copy();
            XMLUtil.printXML("Part Return Response:", XMLUtil.prettifyString(xmlResponse));

            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        } catch (Exception e) {
            RequestInfo info = partOrderManager.getRequestInfo().copy();

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
    private String orderParts() throws Exception {
        XMLUtil.printHeading("Part Order");

        String trackingNumber = null;
        String request = templateReader.readTemplate("ACXPartOrderRequest");
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
        String acxPartOrderRequest = XMLUtil.documentToString(document);
        XMLUtil.printXML("Part Order Request:", acxPartOrderRequest);

        RequestInfo info = null;
        try {
            // Now place the order
            String xmlResponse = partOrderManager.orderParts(acxPartOrderRequest);
            info = partOrderManager.getRequestInfo().copy();
            XMLUtil.printXML("Part Order Response:", XMLUtil.prettifyString(xmlResponse));

            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo().copy();

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
    private List listOrders() throws Exception {
        XMLUtil.printHeading("List Orders");

        String request = templateReader.readTemplate("ACXOrderHistoryRequest");       
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);
        request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);

        // Set the begin and end date for orders to search for to today.
        long now = System.currentTimeMillis();
        //String date = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(now+86400000));
        String date = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date());
        request = XMLUtil.insertValueForTag(request, "</BeginDate>", date);
        request = XMLUtil.insertValueForTag(request, "</EndDate>", date);

        XMLUtil.printXML("List Orders Request:", XMLUtil.prettifyString(request));

        RequestInfo info = null;
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
            return transactionIds;
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo().copy();

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
    private void retrieveOrders(List transactionIds) throws Exception {
        for (int i=0; i < transactionIds.size();) {
            String request = templateReader.readTemplate("ACXOrderDetailsRequest"); 
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);

            // send off a request of 10 at a time since that is the maximum
            // amount that we may supply.
            for (int j=0; j<10; j++) {
                request = XMLUtil.insertValueForTag(request,
                                                    "</OrderDetails>",
                                                    "<TransactionId>" + transactionIds.get(i) + "</TransactionId>");
                if (++i == transactionIds.size()) break;
            }

            RequestInfo info = null;
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
            } catch (Exception e) {
                info = partOrderManager.getRequestInfo().copy();

                System.out.println("retrieveOrders() caught Exception: " + e);
                System.out.println("ACXTrackNum: " + info.getTrackNum());
                System.out.println("  Total time:     " + info.getTotalTime() + " millis");
                System.out.println("  Gateway access: " + info.getGatewayAccessTime());
                System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
                System.out.println();
            
            }
        }    
    }

    private void orderStock() throws Exception {
        XMLUtil.printHeading("Order Stock");

        String request = templateReader.readTemplate("ACXStockOrderRequest");        
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);        
        request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);        
        request = XMLUtil.insertValueForTag(request, "</CustNum>", accountNumber);        
        XMLUtil.printXML("Order Stock Request:", XMLUtil.prettifyString(request));

        RequestInfo info = null;
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
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo().copy();

            System.out.println("orderStock() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
            
        }
    }


    // Perform an Order Detail Request
    private void retrieveOrder(String trackingNumber) throws Exception {
        XMLUtil.printHeading("Order Detail Request");

        String request = templateReader.readTemplate("ACXOrderDetailRequest");        
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);       
        request = XMLUtil.insertValueForTag(request, "</ACXTrackNum>", trackingNumber);
        XMLUtil.printXML("Order Detail Request:", XMLUtil.prettifyString(request));


        // Now use the tracking number to get the details
        boolean pending = true;
        RequestInfo info = null;
        try {
            String xmlResponse = partOrderManager.retrieveOrder(request);
            info = partOrderManager.getRequestInfo().copy();
            XMLUtil.printXML("Order Detail Response:", XMLUtil.prettifyString(xmlResponse));

            System.out.println("retrieveOrder");
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo().copy();

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
        // Use the tracking number to check for response
        String request = templateReader.readTemplate("ACXOrderDetailRequest");        
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);       
        request = XMLUtil.insertValueForTag(request, "</ACXTrackNum>", trackNum);
        XMLUtil.printXML("Checking for pending response for: " + trackNum, 
                         XMLUtil.prettifyString(request));

        boolean pending = true;
        String xmlResponse = null;
        long startTm = System.currentTimeMillis();
        long endTm = startTm + (waitSecs * 1000);
        while (System.currentTimeMillis() < endTm) {
            try {
                Thread.sleep(intervalSecs * 1000);
            } catch (InterruptedException ex) {
            }

            RequestInfo info = null;
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
    private void retrieveProductCoverage() throws Exception {
        String trackNum = null;
        XMLUtil.printHeading("Retrieve Product Coverage");

        String request = templateReader.readTemplate("ACXProductCoverageRequest");
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", brokerPartnerId);        
        request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);               
        request = XMLUtil.insertValueForTag(request, "</CustNum>", "ignored"); 
        if(do_twopass) {
            // Set flag indicating this will be a 2-pass request
            request = XMLUtil.setAttrValue(request, "ReturnReceipt", "value", "Yes");   
        }

        XMLUtil.printXML("Retrieve Product Coverage Request:", XMLUtil.prettifyString(request));
        long startTm = System.currentTimeMillis();
        RequestInfo info = null;
        try {
            String xmlResponse = partOrderManager.retrieveProductCoverage(request);
            if(do_twopass) {
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
            }

            System.out.println("ACXTrackNum: " + trackNum);
            System.out.println("  Total time:     " + totalTm + " millis");
            System.out.println();
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo().copy();

            System.out.println("retrieveProductCoverage() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }

    }

    // Get the seller's product line map via a 2-pass request sequence.
    private void retrieveProductLineMap() throws Exception {
        String trackNum = null;
        XMLUtil.printHeading("Retrieve Product Line Map");

        String request = templateReader.readTemplate("ACXProductLineMapRequest");
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", brokerPartnerId);        
        request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);               
        request = XMLUtil.insertValueForTag(request, "</CustNum>", "ignored"); 
        request = XMLUtil.insertValueForTag(request, "</MapToPartnerID>", sellPartnerId2);   
        if(do_twopass) {
            // Set flag indicating this will be a 2-pass request
            request = XMLUtil.setAttrValue(request, "ReturnReceipt", "value", "Yes");   
        }

        XMLUtil.printXML("Retrieve Product Line Map Request:", XMLUtil.prettifyString(request));
        long startTm = System.currentTimeMillis();
        RequestInfo info = null;
        try {
            String xmlResponse = partOrderManager.retrieveProductLineMap(request);

            if(do_twopass) {
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
            }

            System.out.println("ACXTrackNum: " + trackNum);
            System.out.println("  Total time:     " + totalTm + " millis");
            System.out.println();
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo().copy();

            System.out.println("retrieveProductLineMap() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
    }

    // Get the seller's catalog profile via a 2-pass request sequence.
    private void updateCatalogProfile() throws Exception {
        String trackNum = null;
        XMLUtil.printHeading("Update Catalog Profile");

        // Get the buyPartner's MCL from the platform
        String request = templateReader.readTemplate("ACXCatalogProfileRequest");        
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", buyPartnerId);        
        request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);        
        request = XMLUtil.insertValueForTag(request, "</CustNum>", accountNumber); 
        if(do_twopass) {
            // Set flag indicating this will be a 2-pass request
            request = XMLUtil.setAttrValue(request, "ReturnReceipt", "value", "Yes");   
        }

        RequestInfo info = null;
        try {
            XMLUtil.printXML("Update Catalog Profile Request:", XMLUtil.prettifyString(request));
            String xmlResponse = partOrderManager.updateCatalogProfile(request);

            if(do_twopass) {
                XMLUtil.printXML("Update Catalog Profile pass 1 response:", XMLUtil.prettifyString(xmlResponse));
                trackNum = XMLUtil.getTagValue("ACXTrackNum", xmlResponse);
                System.out.println("-- Waiting up to 3 minutes for response to: " + trackNum + " --");
                System.out.println();

                xmlResponse = retrievePendingResponse(trackNum, 180, 5);
                if (xmlResponse == null) return; // message already displayed
            }

            // Should now have a valid catalog profile response
            XMLUtil.printXML("Update Catalog Profile Response:", XMLUtil.prettifyString(xmlResponse));
            processMCL(xmlResponse, sellPartnerId, accountNumber);


            // Get the default MCL from the platform
            String acct = "default";
            request = templateReader.readTemplate("ACXCatalogProfileRequest");        
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", brokerPartnerId);        
            request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", sellPartnerId);        
            request = XMLUtil.insertValueForTag(request, "</CustNum>", acct); 
            if(do_twopass) {
                // Set flag indicating this will be a 2-pass request
                request = XMLUtil.setAttrValue(request, "ReturnReceipt", "value", "Yes");   
            }

            XMLUtil.printXML("Update Catalog Profile Request:", XMLUtil.prettifyString(request));
            long startTm = System.currentTimeMillis();
            xmlResponse = partOrderManager.updateCatalogProfile(request);

            if(do_twopass) {
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
            processMCL(xmlResponse, sellPartnerId, accountNumber);

            if(!do_twopass) {
                trackNum = XMLUtil.getTagValue("ACXTrackNum", xmlResponse);
            }

            System.out.println("ACXTrackNum: " + trackNum);
            System.out.println("  Total time:     " + totalTm + " millis");
            System.out.println();
        } catch (Exception e) {
            info = partOrderManager.getRequestInfo().copy();

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

//    public static void main (String[] args) {
//        new PartOrderManagement(args);
//    }

}

