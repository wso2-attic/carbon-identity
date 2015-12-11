<%@ page import="org.apache.axiom.om.OMElement" %>
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

<%
    String savedDataApproved = "";
    String savedDataDisapproved = "";
    OMElement responseElement = (OMElement) request.getAttribute("taskOutput");

    if (responseElement != null) {
        if (responseElement.getFirstElement() != null) {
            String approved = responseElement.getFirstElement().getText();
            if ("APPROVED".equals(approved)) {
                savedDataApproved = "checked=\"true\"";
            } else if ("REJECTED".equals(approved)) {
                savedDataDisapproved = "checked=\"true\"";
            }
        }
    }
%>

<script type="text/javascript">
    createTaskOutput = function () {
        checked = "true";
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
