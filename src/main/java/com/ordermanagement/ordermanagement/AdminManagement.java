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

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.activant.aconnex.appsdk.AdminManager;
import com.activant.aconnex.appsdk.AppBuilder;
import com.activant.aconnex.appsdk.AppSDKException;
import com.activant.aconnex.appsdk.AppSDKGatewayException;
import com.activant.aconnex.appsdk.AppSDKXMLParserException;
import com.activant.aconnex.appsdk.RequestInfo;
import com.ordermanagement.ordermanagement.util.XMLTemplateReader; 


/**
 *  Demonstrates each API through the AdminManager.
 */
@Service
public class AdminManagement {
	
    private AdminManager adminManager = null;
    private XMLTemplateReader templateReader = null;

    // list that will contain each partner number and name in a String array
    private List partners = null;
    //AdminManager adminManager, XMLTemplateReader templateReader
    public AdminManagement(String[] args) {
//    	
//    	this.adminManager = adminManager;
//    	this.templateReader = templateReader;
        try {

            // Get the AppBuilder
            AppBuilder appBuilder = new AppBuilder();
            adminManager = appBuilder.getAdminManager();
            templateReader = new XMLTemplateReader(appBuilder);

            System.out.println();
            boolean do_all = XMLUtil.member("all", args) || args.length==0;
            boolean do_svcs    = do_all || XMLUtil.member("listServices", args);
            boolean do_prov    = do_all || XMLUtil.member("listProviders", args);
            boolean do_assoc   = do_all || XMLUtil.member("listAssociations", args);
            boolean do_agree   = do_all || XMLUtil.member("legalAgreement", args);
            boolean do_memb    = do_all || XMLUtil.member("listAffiliations", args);
            boolean do_types   = do_all || XMLUtil.member("partnerTypes", args) 
                                 || do_svcs || do_prov || do_assoc || do_memb; 
            String optStr = (do_all?" all":"") +
                            (do_types?" partnerTypes":"") + 
                            (do_svcs?" listServices":"") + 
                            (do_prov?" listProviders":"") + 
                            (do_assoc?" listAssociations":"") +
                            (do_memb?" listAffiliations":"") +
                            (do_agree?" legalAgreement":"");  
            System.out.println("Options: " + optStr);

            // We have no standalone data files for these requests so force to
            // connected mode for now.
            adminManager.setStandaloneMode(false);
            System.out.println("Running " + (adminManager.getStandaloneMode()?"standalone":"connected"));
            System.out.println();

            if (do_agree) getLegalAgreement();

            // lookup all partner types, then create a list of them.
            // must do this request since partner types used in most other requests.
            if (do_types) listPartnerTypes();

            if (do_svcs)  listApplicationServices();
            if (do_prov)  listBusinessSystemProviders();
            if (do_assoc) listStaticAssociations();
            if (do_memb)  listMemberAffiliations();
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
        } catch (Exception e) {
            info = adminManager.getRequestInfo().copy();

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

    private void listApplicationServices() throws AppSDKException, Exception {
        XMLUtil.printHeading("List Application Services");
        RequestInfo info = null;

        // look up services for each partner
        for (int i = 0; i < partners.size(); ++i) {
            String[] partner = (String[]) partners.get(i);
            String partnerType = partner[0];
            String partnerName = partner[1];

            // Add the partner type number and name to the xml template request
            String request = templateReader.readTemplate("ACXApplicationServicesListRequest");
            request = XMLUtil.insertValueForTag(request, "</PartnerTypeNumber>", partnerType);
            request = XMLUtil.insertValueForTag(request, "</PartnerTypeName>", partnerName);            

            XMLUtil.printXML("List Application Services Request for partner type " + partnerType + " " + partnerName + ":",
                             XMLUtil.prettifyString(request));

            try {
                // call gateway with XML request
                String xmlResponse = adminManager.listApplicationServices(request);

                XMLUtil.printXML("Appliation Services Response for partner type " + partnerType + " " + partnerName + ":", 
                                 XMLUtil.prettifyString(xmlResponse));
            } catch (Exception e) {
                info = adminManager.getRequestInfo().copy();

                System.out.println("listApplicationServices() caught Exception: " + e);
                System.out.println("ACXTrackNum: " + info.getTrackNum());
                System.out.println("  Total time:     " + info.getTotalTime() + " millis");
                System.out.println("  Gateway access: " + info.getGatewayAccessTime());
                System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
                System.out.println();
            }
        }
    }  

    private void listBusinessSystemProviders() throws AppSDKException, Exception {
        XMLUtil.printHeading("List Business System Providers");
        RequestInfo info = null;

        // look up services for each partner     
        for (int i = 0; i < partners.size(); ++i) {
            String[] partner = (String[]) partners.get(i);
            String partnerType = partner[0];
            String partnerName = partner[1];

            // Add the partner type number and name to the xml template request
            String request = templateReader.readTemplate("ACXBusinessSystemProvidersListRequest");       
            request = XMLUtil.insertValueForTag(request, "</PartnerTypeNumber>", partnerType);
            request = XMLUtil.insertValueForTag(request, "</PartnerTypeName>", partnerName); 

            XMLUtil.printXML("List Business System Providers Request for partner type " + partnerType + " " + partnerName + ":",
                             XMLUtil.prettifyString(request));

            try {
                // call gateway with XML request
                String xmlResponse = adminManager.listBusinessSystemProviders(request);

                XMLUtil.printXML("List Business System Providers Response for partner type " + partnerType + " " + partnerName + ":", 
                                 XMLUtil.prettifyString(xmlResponse));
            } catch (Exception e) {
                info = adminManager.getRequestInfo().copy();

                System.out.println("listBusinessSystemProviders() caught Exception: " + e);
                System.out.println("ACXTrackNum: " + info.getTrackNum());
                System.out.println("  Total time:     " + info.getTotalTime() + " millis");
                System.out.println("  Gateway access: " + info.getGatewayAccessTime());
                System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
                System.out.println();
            }
        }
    }

    private void listStaticAssociations() throws AppSDKException, Exception {
        XMLUtil.printHeading("List Static Associations");
        RequestInfo info = null;

        // look up services for each partner
        for (int i = 0; i < partners.size(); ++i) {
            String[] partner = (String[]) partners.get(i);
            String partnerType = partner[0];
            String partnerName = partner[1];

            // Add the partner type number and name to the xml template request
            String request = templateReader.readTemplate("ACXStaticAssociationsListRequest");
            request = XMLUtil.insertValueForTag(request, "</PartnerTypeNumber>", partnerType);
            request = XMLUtil.insertValueForTag(request, "</PartnerTypeName>", partnerName);            

            XMLUtil.printXML("List Static Associations Request for partner type " + partnerType + " " + partnerName + ":",
                             XMLUtil.prettifyString(request));

            try {
                // call gateway with XML request
                String xmlResponse = adminManager.listStaticAssociations(request);

                XMLUtil.printXML("List Static Associations Response for partner type " + partnerType + " " + partnerName + ":", 
                                 XMLUtil.prettifyString(xmlResponse));
            } catch (Exception e) {
                info = adminManager.getRequestInfo().copy();

                System.out.println("listStaticAssociations() caught Exception: " + e);
                System.out.println("ACXTrackNum: " + info.getTrackNum());
                System.out.println("  Total time:     " + info.getTotalTime() + " millis");
                System.out.println("  Gateway access: " + info.getGatewayAccessTime());
                System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
                System.out.println();
            }
            }
    }    

    private void listMemberAffiliations() throws AppSDKException, Exception {
        XMLUtil.printHeading("List Member Affiliations");
        RequestInfo info = null;

        // look up member affiliations for each partner type
        for (int i = 0; i < partners.size(); ++i) {
            String[] partner = (String[]) partners.get(i);
            String partnerType = partner[0];
            String partnerName = partner[1];

            // Add the partner type number and name to the xml template request
            String request = templateReader.readTemplate("ACXMemberAffiliationsListRequest");
            request = XMLUtil.insertValueForTag(request, "</PartnerTypeNumber>", partnerType);
            request = XMLUtil.insertValueForTag(request, "</PartnerTypeName>", partnerName);            

            XMLUtil.printXML("List Member Affiliations Request for partner type " + partnerType + " " + partnerName + ":",
                             XMLUtil.prettifyString(request));

            try {
                // call gateway with XML request
                String xmlResponse = adminManager.listMemberAffiliations(request);

                XMLUtil.printXML("List Member Affiliations partner type " + partnerType + " " + partnerName + ":", 
                                 XMLUtil.prettifyString(xmlResponse));
            } catch (Exception e) {
                info = adminManager.getRequestInfo().copy();

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
        } catch (Exception e) {
            info = adminManager.getRequestInfo().copy();

            System.out.println("getLegalAgreement() caught Exception: " + e);
            System.out.println("ACXTrackNum: " + info.getTrackNum());
            System.out.println("  Total time:     " + info.getTotalTime() + " millis");
            System.out.println("  Gateway access: " + info.getGatewayAccessTime());
            System.out.println("  SDK overhead:   " + (info.getTotalTime() - info.getGatewayAccessTime()));
            System.out.println();
        }
    }



}    
