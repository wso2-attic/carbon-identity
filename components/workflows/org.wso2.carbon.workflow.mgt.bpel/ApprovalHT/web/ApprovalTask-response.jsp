<%@ page import="org.apache.axiom.om.OMElement" %>
<p>
        <%
        String approved = "Not yet available";

        OMElement responseElement = (OMElement) request.getAttribute("taskOutput");

        if (responseElement != null) {
           approved = responseElement.getFirstElement().getText();
        }
    	%>

<table border="0">
    <tr>
        <td>Status :</td>
        <td><%=approved%>
        </td>
    </tr>
</table>
</p>
