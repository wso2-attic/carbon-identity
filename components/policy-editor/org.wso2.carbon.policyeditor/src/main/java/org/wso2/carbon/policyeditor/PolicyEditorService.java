/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.policyeditor;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;


public class PolicyEditorService {

    private static final Log log = LogFactory.getLog(PolicyEditorService.class);

    // The location of the XSD file resources
    private static final String ORG_WSO2_CARBON_POLICYEDITOR_XSD =
            "/org/wso2/carbon/policyeditor/xsd/";


    /**
     * Retrieves a Policy document from a given URL
     *
     * @param policyURL
     * @return A CDATA Wrapped Policy document if found
     * @throws AxisFault
     */
    public String getPolicyDoc(String policyURL) throws AxisFault {
        String policy = "";

        // Open a stream to the policy file using the URL.
        try {
            URL url = new URL(policyURL);

            InputStream in = url.openStream();
            BufferedReader dis =
                    new BufferedReader(new InputStreamReader(in));
            StringBuilder fBuf = new StringBuilder();

            String line = "";
            while ((line = dis.readLine()) != null) {
                fBuf.append(line).append("\n");
            }
            in.close();

            policy = fBuf.toString();
        }
        catch (IOException e) {
            throw new AxisFault(e.getMessage());
        }

        return "<![CDATA[" + policy + "]]>";
    }


    /**
     * Retrieves content from a named schema file bundled as a resource.
     *
     * @param fileName
     * @return
     * @throws AxisFault
     */
    public String getSchema(String fileName) throws AxisFault {
        String schema = "";

        StringBuffer fBuf = null;
        try {
            InputStream in = PolicyEditorService.class.getResourceAsStream(
                    ORG_WSO2_CARBON_POLICYEDITOR_XSD + fileName);

            BufferedReader dis =
                    new BufferedReader(new InputStreamReader(in));
            fBuf = new StringBuffer();

            String line = "";
            while ((line = dis.readLine()) != null) {
                fBuf.append(line).append("\n");
            }
            in.close();

            schema = fBuf.toString();
        } catch (IOException e) {
            throw new AxisFault(e.getMessage());
        }

        return "<![CDATA[" + schema + "]]>";
    }

    /**
     * Returns a list of bundled shema (XSD) file names
     *
     * @return A file name list
     * @throws AxisFault
     */
    public String getAvailableSchemas() throws AxisFault {
        String fileList = "";

        StringBuffer fBuf = null;
        try {
            InputStream in = PolicyEditorService.class.getResourceAsStream(
                    ORG_WSO2_CARBON_POLICYEDITOR_XSD + "policies.xml");

            BufferedReader dis =
                    new BufferedReader(new InputStreamReader(in));
            fBuf = new StringBuffer();

            String line = "";
            while ((line = dis.readLine()) != null) {
                fBuf.append(line).append("\n");
            }
            in.close();

            fileList = fBuf.toString();
        } catch (IOException e) {
            throw new AxisFault(e.getMessage());
        }

        return "<![CDATA[" + fileList + "]]>";
    }


    /**
     * Formats a given unformatted XML string
     *
     * @param xml
     * @return A CDATA wrapped, formatted XML String
     */
    public String formatXML(String xml) {

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document xmlDoc = docBuilder.parse(new ByteArrayInputStream(xml.getBytes()));

            OutputFormat format = new OutputFormat(xmlDoc);
            format.setLineWidth(0);
            format.setIndenting(true);
            format.setIndent(2);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            XMLSerializer serializer = new XMLSerializer(baos, format);
            serializer.serialize(xmlDoc);

            xml = baos.toString();

        } catch (Exception e) {
            log.error(e);
        }

        return "<![CDATA[" + xml + "]]>";
    }

}
