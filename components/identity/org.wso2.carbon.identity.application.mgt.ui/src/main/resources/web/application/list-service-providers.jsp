<!--
~ Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
-->
<%@ page import="org.wso2.carbon.identity.application.common.model.xsd.*"%>
<%@page import="org.wso2.carbon.identity.application.mgt.ui.client.ApplicationManagementServiceClient"%>
<%@ page import="org.apache.axis2.context.ConfigurationContext"%>
<%@ page import="org.wso2.carbon.CarbonConstants"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@ page import="org.wso2.carbon.utils.ServerConstants"%>
<%@ page import="org.wso2.carbon.identity.application.mgt.ui.client.ApplicationManagementServiceClient" %>
<%@ page import="org.wso2.carbon.identity.application.mgt.ui.ApplicationBean" %>

<%@ page import="java.util.ResourceBundle"%>

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
	prefix="carbon"%>

<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../extensions/core/js/vui.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>

<jsp:include page="../dialog/display_messages.jsp" />

<fmt:bundle
	basename="org.wso2.carbon.identity.application.mgt.ui.i18n.Resources">
	<carbon:breadcrumb label="application.mgt"
		resourceBundle="org.wso2.carbon.identity.application.mgt.ui.i18n.Resources"
		topPage="true" request="<%=request%>" />

	<script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
	<script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
	<script type="text/javascript" src="../carbon/admin/js/main.js"></script>

	<div id="middle">

		<h2>
			<fmt:message key='title.list.service.providers' />
		</h2>

		<div id="workArea">
		
<script type="text/javascript">
		
	function removeItem(appid) {
		location.href = "remove-service-provider.jsp?appid="+ appid;
	}
</script>
		
		<%
					ApplicationBasicInfo[] applications = null;
					    String BUNDLE = "org.wso2.carbon.identity.application.mgt.ui.i18n.Resources";
					    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());
					    
						try {
					String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
					String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
					ConfigurationContext configContext = (ConfigurationContext) config.getServletContext()
							                                                                  .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
							
					ApplicationManagementServiceClient serviceClient = new ApplicationManagementServiceClient(cookie, backendServerURL, configContext);
					applications = serviceClient.getAllApplicationBasicInfo();

					} catch (Exception e) {
						String message = resourceBundle.getString("error.while.reading.app.info") + " : " + e.getMessage();
						CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request, e);
					}
				%>

            <div style="height:30px;">
                <a href="load-service-provider.jsp?spName=wso2carbon-local-sp" class="icon-link" style="background-image:url(images/local-sp.png);"><fmt:message key='local.sp'/></a>
            </div>
            <br/>
			<table style="width: 100%" class="styledLeft">
				<tbody>
					<tr>
					<td style="border:none !important">
						<!-- SAML SSO -->
						<table class="styledLeft" width="100%" id="ServiceProviders">
							<thead>
								<tr style="white-space: nowrap">
									<th style="width: 25%"><fmt:message
											key="field.service.provider.id" /></th>
									<th style="width: 45%"><fmt:message
											key="application.list.application.desc" /></th>
									<th style="width: 200px"><fmt:message
											key="application.list.application.action" /></th>
								</tr>
							</thead>
							<%
								if(applications != null && applications.length > 0) {
							%>
							<tbody>
							<%
								for(ApplicationBasicInfo app : applications) {
							%>
								<tr>
									<td><%=app.getApplicationName()%></td>
									<td><%=app.getDescription()%></td>
									<td style="width: 100px; white-space: nowrap;"><a
										title="Edit Service Providers" href="load-service-provider.jsp?spName=<%=app.getApplicationName()%>" class="icon-link"
										style="background-image: url(../admin/images/edit.gif)">Edit</a>

										<a title="Remove Service Providers" onclick="removeItem('<%=app.getApplicationName()%>');return false;" href="#" class="icon-link"
										style="background-image: url(../admin/images/delete.gif)">Delete
									</a></td>
								</tr>
								<% } %>
							</tbody>
							<% } else { %>
							<tbody>
							<tr><td colspan="3"><i>No Service Providers registered</i></td></tr>
							</tbody>
							<% } %>
						</table>
						</td>
					</tr>
				</tbody>
			</table>
			<carbon:paginator pageNumber="0"
                                      numberOfPages="1"
                                      page="list-apps.jsp"
                                      pageNumberParameterName="pageNumber"
                                      parameters="10"
                                      resourceBundle="org.wso2.carbon.identity.oauth.ui.i18n.Resources"
                                      prevKey="prev" nextKey="next"/>
		</div>
	</div>
</fmt:bundle>
