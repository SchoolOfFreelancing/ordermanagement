package com.ordermanagement.ordermanagement.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.springframework.context.annotation.Bean;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.activant.aconnex.appsdk.AppBuilder;
import com.activant.aconnex.appsdk.PartOrderManager;
import com.activant.aconnex.appsdk.RequestInfo;
import com.activant.share.util.Base64;
import com.ordermanagement.ordermanagement.service.partOrder.impl.PartOrderManagementServiceImpl;

/**
 * Created by Sheyan Sandaruwan on 2/1/2020.
 */
public class BeanCreation {

    public static String brokerPartnerId  = null;
    public static String buyPartnerId     = null;
    public static String sellPartnerId    = null;
    public static String sellPartnerId2   = null;
    public static String accountNumber    = null;
    static {

        ResourceBundle props = ResourceBundle.getBundle("AppSDKExamples", Locale.getDefault());
        brokerPartnerId = props.getString("broker.partner.id");
        buyPartnerId    = props.getString("svc.dealer.partner.id");
        sellPartnerId   = props.getString("platform.partner.id");
        sellPartnerId2  = props.getString("platform.partner.id2");
        accountNumber   = props.getString("account.number");
    }

    private PartOrderManager partOrderManager = null;
    private XMLTemplateReader templateReader   = null;
    private boolean           do_twopass       = false;

    @Bean
    public void bean1 (String[] args) {
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
                trackingNumber = new PartOrderManagementServiceImpl().orderParts();
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
                transactionIds = new PartOrderManagementServiceImpl().listOrders();

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
        catch (Exception sdkE) {
            System.err.println("** Gateway Exception thrown: \n");
            System.err.println(sdkE);
            System.err.println();
//            System.err.println("Response XML: " + sdkE.getResponse());
        }
        // The Standalone client has encountered an XML parsing error
        // Other, general AppSDKExceptions
        // This is a programming or environment error of some sort ...

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

    // Get details for the specified transaction ids.
    private void retrieveOrders(List transactionIds) throws Exception {
        for (int i=0; i < transactionIds.size();) {
            String request = templateReader.readTemplate("ACXOrderDetailsRequest");
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", BeanCreation.buyPartnerId);

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


    // Perform an Order Detail Request
    private void retrieveOrder(String trackingNumber) throws Exception {
        XMLUtil.printHeading("Order Detail Request");

        String request = templateReader.readTemplate("ACXOrderDetailRequest");
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", BeanCreation.buyPartnerId);
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

    // Get the seller's product coverage via a 2-pass request sequence.
    private void retrieveProductCoverage() throws Exception {
        String trackNum = null;
        XMLUtil.printHeading("Retrieve Product Coverage");

        String request = templateReader.readTemplate("ACXProductCoverageRequest");
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", BeanCreation.brokerPartnerId);
        request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", BeanCreation.sellPartnerId);
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
                xmlResponse = new PartOrderManagementServiceImpl().retrievePendingResponse(trackNum, 180, 5);
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
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", BeanCreation.brokerPartnerId);
        request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", BeanCreation.sellPartnerId);
        request = XMLUtil.insertValueForTag(request, "</CustNum>", "ignored");
        request = XMLUtil.insertValueForTag(request, "</MapToPartnerID>", BeanCreation.sellPartnerId2);
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

                xmlResponse = new PartOrderManagementServiceImpl().retrievePendingResponse(trackNum, 180, 5);
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
            System.out.println("  SDK overhead:   " +(info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
    }





    // Get the seller's catalog profile via a 2-pass request sequence.
    private void updateCatalogProfile() throws Exception {
        String trackNum = null;
        XMLUtil.printHeading("Update Catalog Profile");

        // Get the buyPartner's MCL from the platform
        String request = templateReader.readTemplate("ACXCatalogProfileRequest");
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", BeanCreation.buyPartnerId);
        request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", BeanCreation.sellPartnerId);
        request = XMLUtil.insertValueForTag(request, "</CustNum>", BeanCreation.accountNumber);
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

                xmlResponse = new PartOrderManagementServiceImpl().retrievePendingResponse(trackNum, 180, 5);
                if (xmlResponse == null) return; // message already displayed
            }

            // Should now have a valid catalog profile response
            XMLUtil.printXML("Update Catalog Profile Response:", XMLUtil.prettifyString(xmlResponse));
            processMCL(xmlResponse, BeanCreation.sellPartnerId, BeanCreation.accountNumber);


            // Get the default MCL from the platform
            String acct = "default";
            request = templateReader.readTemplate("ACXCatalogProfileRequest");
            request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", BeanCreation.brokerPartnerId);
            request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", BeanCreation.sellPartnerId);
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

                xmlResponse = new PartOrderManagementServiceImpl().retrievePendingResponse(trackNum, 180, 5);
                if (xmlResponse == null) return; // message already displayed
            }
            long totalTm = System.currentTimeMillis() - startTm;

            // Should now have a valid catalog profile response
            XMLUtil.printXML("Update Catalog Profile Response:", XMLUtil.prettifyString(xmlResponse));
            processMCL(xmlResponse, BeanCreation.sellPartnerId, BeanCreation.accountNumber);

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
}
