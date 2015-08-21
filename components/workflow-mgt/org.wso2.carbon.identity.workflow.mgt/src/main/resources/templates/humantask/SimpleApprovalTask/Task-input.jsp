<%--
  ~ Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
  ~
  ~ WSO2 Inc. licenses this file to you under the Apache License,
  ~ Version 2.0 (the "License"); you may not use this file except
  ~ in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  --%>

<%@ page import="org.apache.axiom.om.OMAttribute" %>
<%@ page import="org.apache.axiom.om.OMElement" %>
<%@ page import="javax.xml.namespace.QName" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Map" %>
<p>
        <%
        Map<String,String> values = new HashMap<String, String>();

        OMElement requestElement = (OMElement) request.getAttribute("taskInput");
        String ns = "http://ht.bpel.mgt.workflow.identity.carbon.wso2.org/wsdl/schema";

        if (requestElement != null) {
            OMElement parametersList = requestElement.getFirstChildWithName(new QName("parametersList"));
            Iterator iterator = parametersList.getChildElements();

            while (iterator.hasNext()){
                OMElement paramElement = (OMElement) iterator.next();
                OMAttribute itemName = paramElement.getAttribute(new QName(ns,"itemName"));
                OMElement valueElement = paramElement.getFirstChildWithName(new QName(ns,"itemValue"));
                if(itemName!=null && itemName.getAttributeValue()!=null && valueElement!=null){
                    String paramValue = valueElement.getText();
                    if(paramValue==null){
                        paramValue = "";
                    }
                    if(paramValue.endsWith(",")){
                        paramValue = paramValue.substring(0,paramValue.length()-1);
                    }
                    values.put(itemName.getAttributeValue(),paramValue);
                }
            }
        }
    %>

<table border="0">
    <%
        for (Map.Entry<String, String> inputParamEntry : values.entrySet()) {
    %>
    <tr>
        <td><%=inputParamEntry.getKey()%>
        </td>
        <td><%=inputParamEntry.getValue()%>
        </td>
    </tr>
    <%
        }
    %>
</table>

</p>


