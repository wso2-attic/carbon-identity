<!--
 ~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 ~
 ~ WSO2 Inc. licenses this file to you under the Apache License,
 ~ Version 2.0 (the "License"); you may not use this file except
 ~ in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~    http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 -->
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="java.lang.Exception" %>
<%@ page import="org.wso2.carbon.identity.authorization.core.dto.xsd.PaginatedRoleDTO" %>
<%@ page import="org.wso2.carbon.identity.authorization.ui.IdentityAuthorizationClient" %>


<%
    String forwardTo = null;
    PaginatedRoleDTO paginatedRoleDTO = null;
    String[] roleNames = null;
    
    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

    String BUNDLE = "org.wso2.carbon.identity.authorization.ui.i18n.Resources";
	ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());

    int numberOfPages = 0;
    String pageNumber = request.getParameter("pageNumber");
    if (pageNumber == null) {
        pageNumber = "0";
    }
    int pageNumberInt = 0;
    try {
        pageNumberInt = Integer.parseInt(pageNumber);
    } catch (NumberFormatException ignored) {
    }

    String roleSearchString = request.getParameter("roleSearchString");
    if (roleSearchString == null) {
        roleSearchString = "";
    } else {
        roleSearchString = roleSearchString.trim();
    }

    String paginationValue = "roleSearchString=" + roleSearchString;

    try {
        IdentityAuthorizationClient client = new IdentityAuthorizationClient(cookie, serverURL, configContext);
        paginatedRoleDTO = client.getRoleList(roleSearchString, pageNumberInt);

        roleNames = paginatedRoleDTO.getRoleNames();
        numberOfPages = paginatedRoleDTO.getNumberOfPages();
    } catch (Exception e) {
    	String message = resourceBundle.getString("error.while.loading.policy");
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request, e);
        forwardTo = "../admin/error.jsp";
%>

<script type="text/javascript">
    function forward() {
        location.href = "<%=forwardTo%>";
    }
</script>

<script type="text/javascript">
    forward();
</script>
<%
    }
%>

<fmt:bundle basename="org.wso2.carbon.identity.authorization.ui.i18n.Resources">
    <carbon:breadcrumb
            label="ent.policies"
            resourceBundle="org.wso2.carbon.identity.authorization.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>

<script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
<script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
<script type="text/javascript" src="../carbon/admin/js/main.js"></script>
<script type="text/javascript">

    var allPolicesSelected = false;


    function selectRoles(){

        var selected = false;
        if (document.roleForm.roleNames[0] != null) { // there is more than 1 policy
            for (var j = 0; j < document.roleForm.roleNames.length; j++) {
                selected = document.roleForm.roleNames[j].checked;
                if (selected) break;
            }
        } else if (document.roleForm.roleNames != null) { // only 1 policy
            selected = document.roleForm.roleNames.checked;
        }
        if (!selected) {
            CARBON.showInfoDialog('<fmt:message key="select.roles.to.be.added"/>');
            return;
        }
        if (allPolicesSelected) {
            CARBON.showConfirmationDialog("<fmt:message key="publish.all.policies.prompt"/>",function() {
                document.roleForm.action = "permission-configure.jsp";
                document.roleForm.submit();
            });
        } else {
            CARBON.showConfirmationDialog("<fmt:message key="publish.services.on.page.prompt"/>",function() {
                document.roleForm.action = "permission-configure.jsp";
                document.roleForm.submit();
            });
        }
    }

    function publishAllPolicies() {
        CARBON.showConfirmationDialog("<fmt:message key="publish.all.policies.prompt"/>",function() {
            location.href = "permission-configure.jsp";
        });
    }

    function publishPolicy(policy) {
        location.href = "permission-configure.jsp?policyid=" + policy;
    }

    function selectAllInThisPage(isSelected) {
        allPolicesSelected = false;
        if (document.roleForm.roleNames != null &&
            document.roleForm.roleNames[0] != null) { // there is more than 1 service
            if (isSelected) {
                for (var j = 0; j < document.roleForm.roleNames.length; j++) {
                    document.roleForm.roleNames[j].checked = true;
                }
            } else {
                for (j = 0; j < document.roleForm.roleNames.length; j++) {
                    document.roleForm.roleNames[j].checked = false;
                }
            }
        } else if (document.roleForm.roleNames != null) { // only 1 service
            document.roleForm.roleNames.checked = isSelected;
        }
        return false;
    }

    function selectAllInAllPages() {
        selectAllInThisPage(true);
        allPolicesSelected = true;
        return false;
    }

    function resetVars() {
        allPolicesSelected = false;

        var isSelected = false;
        if (document.roleForm.roleNames[0] != null) { // there is more than 1 service
            for (var j = 0; j < document.roleForm.policies.length; j++) {
                if (document.roleForm.roleNames[j].checked) {
                    isSelected = true;
                }
            }
        } else if (document.roleForm.roleNames != null) { // only 1 service
            if (document.roleForm.roleNames.checked) {
                isSelected = true;
            }
        }
        return false;
    }

    function searchServices() {
        document.searchForm.submit();
    }


</script>

<div id="middle">
    <h2><fmt:message key='add.roles'/></h2>
    <div id="workArea">

    <form action="permission-roles.jsp" name="searchForm">
        <table id="searchTable" name="searchTable" class="styledLeft" style="border:0;
                                                !important margin-top:10px;margin-bottom:10px;">
            <tr>
            <td>
                <table style="border:0; !important">
                    <tbody>
                    <tr style="border:0; !important">
                        <td style="border:0; !important">
                            <fmt:message key="search.roles"/>
                            <input type="text" name="roleSearchString"
                                   value="<%= roleSearchString != null? roleSearchString :""%>"/>
                        </td>
                        <td style="border:0; !important">
                             <a class="icon-link" href="#" style="background-image: url(images/search.gif);"
                                   onclick="searchServices(); return false;"
                                   alt="<fmt:message key="search"/>"></a>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </td>
            </tr>
        </table>
    </form>

    <table style="margin-top:10px;margin-bottom:10px">
        <tbody>
        <tr>
            <td>
                <a style="cursor: pointer;" onclick="selectAllInThisPage(true);return false;" href="#"><fmt:message key="selectAllInPage"/></a>
                &nbsp;<b>|</b>&nbsp;</td><td><a style="cursor: pointer;" onclick="selectAllInThisPage(false);return false;" href="#"><fmt:message key="selectNone"/></a>
            </td>
        </tr>
        <tr>
            <td>
                <a onclick="selectRoles();return false;"  href="#" class="icon-link"
                   style="background-image: url(images/publish.gif);" ><fmt:message key="add.authorization.selected"/></a>
            </td>
            <%--<td>--%>
                <%--<a onclick="publishAllPolicies();return false;"  class="icon-link" href="#"--%>
                   <%--style="background-image: url(images/publish-all.gif);" ><fmt:message key="add.authorization.all"/></a>--%>
            <%--</td>--%>
        </tr>
        </tbody>
    </table>

    <form action="" name="roleForm" method="post">
        <table>
            <tbody>
            <%
            if (roleNames != null) {
                for (int i = 0; i < roleNames.length; i++) {
                    if(roleNames[i] != null){
            %>
            <tr>

                <td width="10px" style="text-align:center; !important">
                    <input type="checkbox" name="roleNames"
                           value="<%=roleNames[i]%>"
                           onclick="resetVars()" class="chkBox" />
                </td>

                <td> <%=roleNames[i]%> </td>
            </tr>
            <%
                    } 
                }
            }
            %>
            </tbody>
        </table>
    </form>
    <carbon:paginator pageNumber="<%=pageNumberInt%>" numberOfPages="<%=numberOfPages%>"
                      page="index.jsp" pageNumberParameterName="pageNumber" parameters="<%=paginationValue%>"
                      resourceBundle="org.wso2.carbon.identity.authorization.ui.i18n.Resources"
                      prevKey="prev" nextKey="next"/>
        </div>
    </div>
</fmt:bundle>
