/*
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
package com.ordermanagement.ordermanagement;

import com.activant.aconnex.appsdk.*;

import java.io.*;
import java.util.*;


/**
 * Class that knows how to read in an AConneX request XML template
 * body from a file and prepend appropriate XML header information
 * (XML version and DOCTYPE info).
 */
public class XMLTemplateReader {

    private AppBuilder   appbuilder = null;

    public XMLTemplateReader(AppBuilder appbuilder) throws Exception {
        // settings are available from the AppBuilder
        this.appbuilder = appbuilder;
    }

    public String readTemplate(String request) throws Exception {
        return readTemplate("xml" + File.separator, request);    
    }    

    public String readTemplate(String path, String requestName) throws Exception {
        StringBuffer buffer = new StringBuffer();
        FileInputStream file = null;
        try {
            file = new FileInputStream(path + requestName + ".xml");
            BufferedReader br = new BufferedReader(new InputStreamReader(file));

            // First append the required header stiff (XML version and DOCTYPE info)
            buffer.append(appbuilder.getXmlVersionHeader());
            buffer.append("<!DOCTYPE ");
            buffer.append(requestName);
            buffer.append(" SYSTEM  \"");
            buffer.append(appbuilder.getDtdLocation() + "/");
            buffer.append(requestName);
            buffer.append(appbuilder.getDtdVersionSuffix());
            buffer.append(".dtd\">");

            // could also just have done this...
            // buffer.append(appbuilder.makeRequestHeader(requestName));

            String line = null;
            while ((line = br.readLine()) != null) {
                buffer.append(line);
            }           
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                file.close();
            } catch (IOException ioe) {
            }
        }
        return buffer.toString();
    }        

}    
