<%--
  ~ Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
  ~ KIND, either express or implied. See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  --%>

<%@page import="org.apache.axis2.context.ConfigurationContext"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.base.MultitenantConstants" %>
<%@ page import="org.wso2.carbon.identity.sts.mgt.stub.service.util.xsd.TrustedServiceData" %>


<%@page import="org.wso2.carbon.identity.sts.mgt.ui.client.CarbonSTSClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.CarbonUtils" %>
<%@ page import="org.wso2.carbon.utils.NetworkUtils" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.owasp.encoder.Encode" %>


<jsp:include page="../dialog/display_messages.jsp"/>

<fmt:bundle basename="org.wso2.carbon.identity.sts.mgt.ui.i18n.Resources">
<carbon:breadcrumb
        label="sts.configuration"
        resourceBundle="org.wso2.carbon.identity.sts.mgt.ui.i18n.Resources"
        topPage="true"
        request="<%=request%>"/>

<script type="text/javascript" src="global-params.js"></script>
<script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
<script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
<script type="text/javascript" src="../carbon/admin/js/main.js"></script>

<script type="text/javascript">
    function doValidation() {
        var fld = document.getElementsByName("endpointaddrs")[0];
        var value = fld.value;
        if (value.length == 0) {
            CARBON.showWarningDialog("<fmt:message key='enter.valid.endpoint.address'/>", null, null);
            return false;
        }

        value = value.replace(/^\s+/, "");
        if (value.length == 0) {
            CARBON.showWarningDialog("<fmt:message key='enter.valid.endpoint.address'/>", null, null);
            return false;
        }
        return true;
    }
    function doValidationOnClaims() {
        var fld = document.getElementById("realmName");
        var value = fld.value;
        value = value.replace(/^\s+/, "");
        if (value.length == 0) {
            CARBON.showWarningDialog("<fmt:message key='enter.valid.realm.name'/>", null, null);
            return false;
        }
        return true;
    }

    function itemRemove(hostName) {
        CARBON.showConfirmationDialog("<fmt:message key='remove.message1'/>" + hostName + "<fmt:message key='remove.message2'/>",
                                      function() {
                                          location.href = "remove-trusted-service.jsp?endpointaddrs=" + hostName;
                                      }, null);
    }
    function removePassiveSTSTrustService(realm) {
        CARBON.showConfirmationDialog("<fmt:message key='remove.message1'/>" + realm + "<fmt:message key='remove.message2'/>",
                                      function() {
                                          location.href = "remove-passive-sts-trusted-service.jsp?realmName=" + realm;
                                      }, null);
    }

</script>

<%
    TrustedServiceData[] services = null;
    String[] aliases = null;
    CarbonSTSClient sts = null;
    String address = null;
    String keyAlias = null;
    String cookie = null;
    String serverUrl = null;
    String spName = request.getParameter("spName");
    String action = request.getParameter("spAction");
    String spAudience = request.getParameter("spAudience");


    ConfigurationContext configurationContext = (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    try {
    	
    	String tenantDomain = (String) session.getAttribute(MultitenantConstants.TENANT_DOMAIN);
    	String tenantContext =  "";

    	
    	if (! MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equalsIgnoreCase(tenantDomain)){
        	 tenantContext =  MultitenantConstants.TENANT_AWARE_URL_PREFIX + "/" + tenantDomain + "/";

    	}
       	
    	
        if(CarbonUtils.isRunningOnLocalTransportMode()){
            String mgtTransport = CarbonUtils.getManagementTransport();
            int mgtTransportPort =  CarbonUtils.getTransportPort(configurationContext,mgtTransport);
            String ip = NetworkUtils.getLocalHostname();
            serverUrl = mgtTransport + "://" + ip + ":" + mgtTransportPort + "/services/"+ tenantContext + "wso2carbon-sts";
        }else{
            serverUrl = CarbonUIUtil.getServerURL(config.getServletContext(), session) + tenantContext + "wso2carbon-sts";
        }
        cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        sts = new CarbonSTSClient(config, session, cookie);

        address = (String) request.getParameter("endpointaddrs");
        keyAlias = (String) request.getParameter("alias");
        session.setAttribute("returnToPath", "../generic-sts/sts.jsp");
        session.setAttribute("cancelLink", "../generic-sts/sts.jsp");
        session.setAttribute("backLink", "../generic-sts/sts.jsp");

        try {
            sts.addTrustedService(address, keyAlias);
            if (spName != null && action!=null && "returnToSp".equals(action) ) {
            	
            	boolean qpplicationComponentFound = CarbonUIUtil.isContextRegistered(config, "/application/");
            	if (qpplicationComponentFound) {
%> 
<script>
   location.href = '../application/configure-service-provider.jsp?action=update&display=serviceName&spName=<%=Encode.forUriComponent(spName)%>&serviceName=<%=Encode.forUriComponent(address)%>';
</script>
<% 
            }
            }
        } catch (Exception e) {
        }
        aliases = sts.getAliasFromPrimaryKeystore();
        services = sts.getTrustedServices();
        
        if (action != null && "spEdit".equals(action)) {
        	if (services != null && services.length > 0) {
                for (TrustedServiceData service : services) {
                	if (service!=null && service.getServiceAddress().equals(spAudience)){
                		keyAlias = service.getCertAlias();
                		break;
                	}
                }
        	}
        }
        
    } catch (Exception e) {
%>
<script>
    <jsp:forward page="../admin/error.jsp?<%=e.getMessage()%>"/>
</script>
<%
        return;
    }
%>

<div id="middle">
    <h2><fmt:message key="sts.configuration"/></h2>

    <div id="workArea">
       <%if(spName==null) {%>
        <table>
            <tr>
                <td>
                    <div style="height:30px;">
                        <a href="javascript:document.location.href='../securityconfig/index.jsp?serviceName=wso2carbon-sts'"
                           class="icon-link"
                           style="background-image:url(images/configure.gif);"><fmt:message
                                key='apply.security.policy'/></a>
                    </div>
                </td>
                <td>
                    <div style="height:30px;">
                        <a href="javascript:document.location.href='<%=serverUrl+"?wsdl"%>'"
                           class="icon-link"
                           style="background-image:url(images/sts.gif);"><%=serverUrl%>
                        </a>
                    </div>
                </td>
                <td>
                    <div style="height:30px;">
                        <a href="javascript:document.location.href='passive-sts.jsp'"
                           class="icon-link"
                           style="background-image:url(images/sts.gif);">Passive STS Configuration
                        </a>
                    </div>
                </td>
            </tr>
        </table>
        <form>
            <table class="styledLeft" width="100%" id="trustedServices">
                <thead>
                <tr>
                    <th width="40%"><fmt:message key="sts.service.endpoint"/></th>
                    <th colspan="2"><fmt:message key="sts.certificate.alias"/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    if (services != null && services.length > 0) {
                        for (TrustedServiceData service : services) {
                %>
                <tr>
                    <td width="40%"><%=Encode.forHtmlContent(service.getServiceAddress())%>
                    </td>
                    <td width="40%">&nbsp;&nbsp;<%=Encode.forHtmlContent(service.getCertAlias())%>
                    </td>
                    <td width="20%">
                        <a title="Remove Trusted RP"
                           onclick="itemRemove('<%=Encode.forJavaScriptAttribute(service.getServiceAddress())%>');return false;"
                           href="#" class="icon-link"
                           style="background-image:url(../admin/images/delete.gif)">Delete
                        </a>
                    </td>
                </tr>
                <%
                    }
                } else {
                %>
                <tr>
                    <td colspan="3"><i><fmt:message key="sts.trusted.services.not.found"/></i></td>
                </tr>
                <% } %>
                </tbody>
            </table>
            <script type="text/javascript">
                alternateTableRows('trustedServices', 'tableEvenRow', 'tableOddRow');
            </script>
        </form>

        <br/>
        <%} %>

        <form method="get" action="sts.jsp" name="trustedservice" onsubmit="return doValidation();">
            <table class="styledLeft" width="100%">
                <thead>
                <tr>
                    <th><fmt:message key="sts.trusted.services.new"/></th>
                    
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td class="formRow">
                        <table class="normal" cellspacing="0">
                            <tr>
                                <td><fmt:message key="sts.endpoint.address"/><font
                                        color="red">*</font></td>
                            <%if (spAudience!=null) { %>
                             <td><input type="text" id="endpointaddrs" name="endpointaddrs"
                                           class="text-box-big" value="<%=Encode.forHtmlAttribute(spAudience)%>"  /></td>
                            <% } else { %>
                             <td><input type="text" id="endpointaddrs" name="endpointaddrs"
                                           class="text-box-big"/></td>
                            <% } %>
                               
                            </tr>
                            <tr>
                                <td><fmt:message key="sts.certificate.alias"/>
                                <input type="hidden" name="spName" value="<%=Encode.forHtmlAttribute(spName)%>" >
                                <input type="hidden" name="spAction" value="returnToSp" >
                                
                                </td>
                                <td>
                                    <select id="alias" name="alias">
                                        <%
                                            if (aliases != null) {
                                                for (String alias : aliases) {
                                                    if (alias != null) {
                                                    	if (keyAlias != null && keyAlias.equals(alias)){
                                        %>
                                                          <option value="<%=Encode.forHtmlAttribute(alias)%>" selected="selected"><%=Encode.forHtmlContent(alias)%></option>
                                        
                                                       <%} else { %>
                                                          <option value="<%=Encode.forHtmlAttribute(alias)%>"><%=Encode.forHtmlContent(alias)%></option>
                                        <%
                                                         }
                                                     }
                                                 }
                                            }
                                        %>
                                    </select>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
                <tr>
                    <td class="buttonRow">
                        <input class="button" type="submit"
                               value="<fmt:message key="sts.apply.caption"/>"/>
                    </td>
                </tr>
                </tbody>
            </table>
        </form>
    </div>
</div>
</fmt:bundle>