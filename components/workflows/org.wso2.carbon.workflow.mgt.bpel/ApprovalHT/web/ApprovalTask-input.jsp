<%@ page import="org.apache.axiom.om.OMElement" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
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
            Iterator iterator = requestElement.getChildrenWithName(new QName(ns,"inputParam"));
            while (iterator.hasNext()){
                OMElement paramElement = (OMElement) iterator.next();
                String paramName = paramElement.getAttributeValue(new QName("paramName"));
                OMElement valueElement = paramElement.getFirstChildWithName(new QName("value"));
                String paramValue = "Not Available";
                if(valueElement !=null){
                    paramValue = valueElement.getText();
                }
                if(StringUtils.isNotBlank(paramName)){
                    values.put(paramName,paramValue);
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
