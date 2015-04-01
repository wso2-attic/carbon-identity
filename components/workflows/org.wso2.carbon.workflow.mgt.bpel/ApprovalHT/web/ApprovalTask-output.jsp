<%@ page import="org.apache.axiom.om.OMElement" %>
<%
    String approved = "No Value Assigned";
    String savedDataApproved = "";
    String savedDataDisapproved = "";
    OMElement responseElement = (OMElement) request.getAttribute("taskOutput");

    if (responseElement != null) {
        if (responseElement.getFirstElement() != null) {
            approved = responseElement.getFirstElement().getText();
            if (approved.equals("APPROVED")) {
                savedDataApproved = "checked=\"true\"";
            } else if (approved.equals("REJECTED")) {
                savedDataDisapproved = "checked=\"true\"";
            }
        }
    }
%>

<script type="text/javascript">
    createTaskOutput = function () {
        checked = "true"
        var outputVal = getCheckedRadio();
        if (outputVal == 'approve') {
            return '<sch:ApprovalCBData xmlns:sch="http://ht.bpel.mgt.workflow.identity.carbon.wso2.org/wsdl/schema"><approvalStatus>APPROVED</approvalStatus></sch:ApprovalCBData>';
        } else if (outputVal == 'disapprove') {
            return '<sch:ApprovalCBData xmlns:sch="http://ht.bpel.mgt.workflow.identity.carbon.wso2.org/wsdl/schema"><approvalStatus>REJECTED</approvalStatus></sch:ApprovalCBData>';
        }
    };

    getCheckedRadio = function () {
        var radioButtons = document.getElementsByName("responseRadio");
        for (var x = 0; x < radioButtons.length; x++) {
            if (radioButtons[x].checked) {
                return radioButtons[x].value;
            }
        }
    };
</script>

<p>

<form>
    <table border="0">
        <tr>
            <td>
                <input type="radio" name="responseRadio" id="responseRadio1" value="approve" <%=savedDataApproved%>/>
                Approve
                <input type="radio" name="responseRadio" id="responseRadio2"
                       value="disapprove" <%=savedDataDisapproved%>/> Disapprove
            </td>
        </tr>

    </table>
</form>
</p>
