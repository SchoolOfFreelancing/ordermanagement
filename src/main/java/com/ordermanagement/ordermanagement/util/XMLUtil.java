package com.ordermanagement.ordermanagement.util;/*
 * Copyright (c) 2000-2004 Activant Solutions Inc.  All Rights Reserved.
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.activant.share.util.StringUtil;

@Service
public class XMLUtil {

    private static final String DTD_LOAD_FEATURE =
            "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    public static Document createDocument(String xmlDoc) throws Exception {
        return createDocument(xmlDoc, /*validate*/false);
    }
    public static Document createDocument(String xmlDoc, boolean validate) throws Exception {
        StringReader sr = new StringReader(xmlDoc);
        InputSource input = new InputSource(sr);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(validate);
        try {
            if (validate == false)
                dbf.setAttribute(DTD_LOAD_FEATURE, "false");
        } catch (Exception e) {
            //System.err.println("Feature " + DTD_LOAD_FEATURE + " not available");
        }
        DocumentBuilder db = dbf.newDocumentBuilder();
        if (validate) {
            // Validating. Provide an ErrorHandler interface that
            // throws a SAXException on the first error encountered.
            db.setErrorHandler(new DefaultHandler() {
                // ErrorHandler interface - just throw the exception
                public void error(SAXParseException e) throws SAXException {
                    throw e;
                }
                public void fatalError(SAXParseException e) throws SAXException {
                    throw e;
                }
                public void warning(SAXParseException e) throws SAXException {
                    throw e;
                }
            });
        } else {
            // Not validating. Provide an EntityResolver in order to
            // short-circuit external DTD loading.
            db.setEntityResolver(new DefaultHandler() {
                // EntityResolver interface - Return an empty input source
                // for the external entity
                public InputSource resolveEntity(String publicId, String systemId) {
                    return new InputSource(new StringReader(""));
                }
            });
        }

        Document doc = db.parse(input);
        doc.normalize();
        return doc;
    }
    public static String documentToString(Document document) throws Exception {
        StringWriter stringOut = new StringWriter();
        OutputFormat fmt = new OutputFormat(document);
        fmt.setPreserveSpace(false);
        fmt.setIndenting(true);
        fmt.setIndent(2);
        XMLSerializer serial = new XMLSerializer(stringOut, fmt);
        serial.asDOMSerializer();
        serial.serialize(document);
        return stringOut.toString();
    }

    public static String prettifyString(String string) {
        string = StringUtil.trim(string);
        try {
            return documentToString(createDocument(string));
        } catch (Exception e) {
            return string;
        }
    }

    public static void printXML(String title, String xml) {
        System.out.println();
        System.out.println(title);
        System.out.println();
        System.out.println(xml);
    }
    public static void printXML(String title, Document doc) throws Exception {
        printXML(title, XMLUtil.documentToString(doc));
    }

    public static void traverseTree(Node node) {
        int nodeType = node.getNodeType();

        switch (nodeType) {
            case Node.ELEMENT_NODE:
                displayNodeAndAttributes(node);
                break;

            case Node.CDATA_SECTION_NODE:
            case Node.TEXT_NODE:
                System.out.println("Text Node - " + node.getNodeValue());
                break;
        }
    }

    public static void displayNodeAndAttributes(Node node) {
        String nodeName = node.getNodeName();
        System.out.println("<" + nodeName + ">");

        NamedNodeMap attributes = node.getAttributes();
        Node tempAttrib = null;
        for (int i=0; i<attributes.getLength(); i++) {
            tempAttrib = attributes.item(i);
            System.out.println(tempAttrib.getNodeName() + "=" + tempAttrib.getNodeValue());
        }

        // perform recursion for each child node
        if (node.hasChildNodes()) {
            NodeList children = node.getChildNodes();
            for (int i=0; i<children.getLength(); i++) {
                traverseTree(children.item(i));

            }
        }
        System.out.println("</" + nodeName + ">");
    }

    public static Node findNode(Node node, String name) {
        if (node == null || name == null) return null;

        Node theNode = null;
        if (node.getNodeName().equals(name)) {
            theNode = node;
        } else {
            if (node.hasChildNodes()) {
                NodeList list = node.getChildNodes();
                for (int i=0; i<list.getLength(); i++) {
                    Node n = findNode(list.item(i), name);
                    if (n != null && n.getNodeName().equals(name)) {
                        theNode = n;
                        break;
                    }
                }
            }
        }
        return theNode;
    }

    public static String getTagValue(Node node) {
        if (node == null || node.getNodeType() != Node.ELEMENT_NODE) return null;
        return node.getFirstChild().getNodeValue();
    }

    public static void setTagValue(Document document, Node node, String value) {
        if (node != null && document != null && node.getNodeType() == Node.ELEMENT_NODE) {
            if (node.getFirstChild() != null) {
                node.getFirstChild().setNodeValue(value);
            } else {
                node.appendChild(document.createTextNode(value));
            }
        }
    }
    public static void setTagValue(Document document, Node node, String tagName, String value) {
        Node tmpNode = XMLUtil.findNode(node, tagName);
        setTagValue(document, tmpNode, value);
    }

    public static String setAttrValue(String xml, String tagName,
                                      String atName, String atVal) {
        if (xml==null || atName==null) return null;
        if (atVal==null) atVal = "";

        String tagStr = "<" + tagName;
        String attrStr = atName + "=";
        int posTag1 = xml.indexOf(tagStr);
        if (posTag1 < 0) return null;
        int posTag2 = xml.indexOf(">", posTag1);
        if (posTag2 < 0) return null;

        int posAttr1 = xml.indexOf(attrStr, posTag1);
        if (posAttr1 < 0 || posAttr1 > posTag2) return null;

        char quoteChar = xml.charAt(posAttr1+attrStr.length());
        if (quoteChar != '\'' && quoteChar != '"') return null;

        int posAttr2 = xml.indexOf(quoteChar, posAttr1+attrStr.length()+1);
        if (posAttr2 < 0 || posAttr2 > posTag2) return null;

        return xml.substring(0, posAttr1) + attrStr + quoteChar + atVal +
                xml.substring(posAttr2);
    }

    // pass end-tag
    public static String insertValueForTag(String xmlDoc, String endTag, String value) {
        StringBuffer buffer = new StringBuffer(xmlDoc);
        int index = xmlDoc.indexOf(endTag);
        buffer.insert(index, value);
        return buffer.toString();
    }

    public static String getTagValue(String tagName, String xml) {
        if (xml==null || tagName==null) return null;

        String startTag = "<" + tagName + ">";
        String endTag   = "</" + tagName + ">";
        int pos1 = xml.indexOf(startTag);
        int pos2 = xml.indexOf(endTag);
        if (pos1 < 0 || pos2 < 0) return null;
        return StringUtil.trimOrNull(xml.substring(pos1+startTag.length(), pos2));
    }

    public static void printHeading(String heading) {
        System.out.println("-----------------------------------");
        System.out.println("-- " + heading);
        System.out.println("-----------------------------------");
    }

    // Returns true if the target string appears in the args list.
    public static boolean member(String target, String[] args) {
        if (args==null || target==null) {
            return false;
        }
        for (int ix=0; ix<args.length; ix++) {
            if (target.equalsIgnoreCase(args[ix])) {
                return true;
            }
        }
        return false;
    }

    // Returns the arg after the target string specified.
    public static String memberAfter(String target, String[] args) {
        if (args==null || target==null) {
            return null;
        }
        for (int ix=0; ix<args.length; ix++) {
            if (target.equalsIgnoreCase(args[ix])) {
                if(ix+1 < args.length)
                    return args[ix+1];
                else
                    return null;
            }
        }
        return null;
    }


    /** Reads characters from the specified file into a string. */
    static public String readFile(File f) throws IOException, FileNotFoundException
    {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
        int len = (int)f.length();
        byte[] data = new byte[len];
        in.read(data, 0, len);
        return new String(data);
    }

    /** Writes a string to the specified file. */
    static public void writeFile(File f, String str) throws IOException
    {
        byte[] data = str.getBytes();
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
        out.write(data);
        out.close();
    }

    /** Writes a string to the specified file. */
    static public void writeFile(File f, byte[] data) throws IOException
    {
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
        out.write(data);
        out.close();
    }
}    
