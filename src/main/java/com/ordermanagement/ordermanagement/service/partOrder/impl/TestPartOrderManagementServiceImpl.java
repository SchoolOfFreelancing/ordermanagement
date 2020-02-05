package com.ordermanagement.ordermanagement.service.partOrder.impl;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.activant.aconnex.appsdk.AdminManager;
import com.activant.aconnex.appsdk.AppBuilder;
import com.activant.aconnex.appsdk.AppSDKXMLUtil;
import com.activant.aconnex.appsdk.PartOrderManager;
import com.activant.aconnex.appsdk.RequestInfo;
import com.ordermanagement.ordermanagement.AdminManagement;
import com.ordermanagement.ordermanagement.request.OrderPartsRequest;
import com.ordermanagement.ordermanagement.service.partOrder.TestPartOrderManagementService;
import com.ordermanagement.ordermanagement.util.BeanCreation;
import com.ordermanagement.ordermanagement.util.XMLTemplateReader;
import com.ordermanagement.ordermanagement.util.XMLUtil;

/**
 * Created by Sheyan Sandaruwan on 1/30/2020.
 */
@Service
@SuppressWarnings("unused")
public class TestPartOrderManagementServiceImpl implements TestPartOrderManagementService{



	@Autowired
	AdminManagement adminManagement;

	@Autowired
	XMLUtil xmlUtil;
	
    private PartOrderManager partOrderManager = null;
    private XMLTemplateReader templateReader   = null;
    private boolean           do_twopass       = false;


	private AdminManager adminManager = null;

    // Get some transaction ids for the buyer
    public List listOrders() throws Exception {

        
        XMLUtil.printHeading("List Orders");

        String request = templateReader.readTemplate("ACXOrderHistoryRequest");
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", BeanCreation.buyPartnerId);
        request = XMLUtil.insertValueForTag(request, "</SellPartnerID>", BeanCreation.sellPartnerId);

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




    @Override
    public List getTransactionIds(String xml) throws Exception {
        List list = new ArrayList();

        Document document = XMLUtil.createDocument(xml);
        NodeList nodes = document.getElementsByTagName("TransactionId");
        for (int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            list.add(XMLUtil.getTagValue(node));
        }
        return list;
    }

    // Poll at intervals of intervalSecs for a valid response to the
    // request represented by trackNum for a total of waitSecs.
    @Override
     public String retrievePendingResponse(String trackNum,
                                           int waitSecs,
                                           int intervalSecs) throws Exception
    {
        // Use the tracking number to check for response
        String request = templateReader.readTemplate("ACXOrderDetailRequest");
        request = XMLUtil.insertValueForTag(request, "</BuyPartnerID>", BeanCreation.buyPartnerId);
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

    @SuppressWarnings("static-access")
	@Override
    public String orderParts(OrderPartsRequest order) throws Exception {

        // Get the AppBuilder
        AppBuilder appBuilder = new AppBuilder();
        adminManager = appBuilder.getAdminManager();
        partOrderManager = appBuilder.getPartOrderManager();
        templateReader = new XMLTemplateReader(appBuilder);
        
        xmlUtil.printHeading("Part Order");

        String trackingNumber = null;
        String request = templateReader.readTemplate("ACXPartOrderRequest");
        Document document = xmlUtil.createDocument(request);
        Node root = document.getDocumentElement();
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String nodeName = node.getNodeName();
            if ("Envelope".equals(nodeName)) {
            	xmlUtil.setTagValue(document, node, "BuyPartnerID", BeanCreation.buyPartnerId);
            } else if ("RequestRouter".equals(nodeName)) {
                // Add value for SellPartnerID
            	xmlUtil.setTagValue(document, node, "SellPartnerID", BeanCreation.sellPartnerId);
                // Add value for CustNum
            	xmlUtil.setTagValue(document, node, "CustNum", BeanCreation.accountNumber);
            } else if ("PartOrderRequest".equals(nodeName)) {
                // Add value for ID
            	xmlUtil.setTagValue(document, node, "ID", "0");
                // Add value for PartNum
            	xmlUtil.setTagValue(document, node, "PartNum", "22460");
                // Add value for MfgCode
            	xmlUtil.setTagValue(document, node, "MfgCode", "WAL");
                // Add value for Qty
            	xmlUtil.setTagValue(document, node, "Qty", "3");
                // Add value for ShipCode
            	xmlUtil.setTagValue(document, node, "ShipCode", "0");
                // Add value for CatVehID
            	xmlUtil.setTagValue(document, node, "CatVehID", "123456");
                // Add value for Year
            	xmlUtil.setTagValue(document, node, "Year", "85");
                // Add value for Make
            	xmlUtil.setTagValue(document, node, "Make", "DODGE");
                // Add value for Model
            	xmlUtil.setTagValue(document, node, "Model", "ARIES");
                // Add value for Engine
            	xmlUtil.setTagValue(document, node, "Engine", "V6");
                // Add value for SpeclCond
            	xmlUtil.setTagValue(document, node, "SpeclCond", "No");
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
}
