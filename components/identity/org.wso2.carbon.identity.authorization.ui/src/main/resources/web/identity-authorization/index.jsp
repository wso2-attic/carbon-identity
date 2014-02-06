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

<%@ page isELIgnored="false"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="auth" uri="tld/identity-authoization.tld"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
	prefix="carbon"%>


<script type="text/javascript" src="js/cust_permissions.js"></script>

<fmt:bundle basename="org.wso2.carbon.identity.authorization.ui.i18n.Resources" >
<carbon:breadcrumb label=""
		resourceBundle="org.wso2.carbon.identity.authorization.ui.i18n.Resources"
		topPage="true"
		request="<%=request%>" />
		
<%@include file="messages.jsp"%>
<div id="middle">

	<h2><fmt:message key="identity.cusom.perm.heading" /> </h2>
	<div id="workArea" style="padding-bottom: 70px; margin-top: 10px;">
		<auth:module config="<%=config%>" request="<%=request%>" action="1"></auth:module>
		<table class="styledLeft" style="width: 55%; clear: both;">
			<thead>
				<tr>
					<th><fmt:message key="reg.apps" /></th>
					<th><fmt:message key="reg.apps.actions" /></th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${modules }" var="module">
					<tr>
						<td id="${module.moduleId }">${module.moduleName }</td>

						<td><a
							style="background-image: url(../admin/images/edit.gif);"
							class="icon-link"
							href="javascript:document.location.href='edit-module.jsp?id=${module.moduleId }'"><fmt:message key="link.edit" /></a>

							<a style="background-image: url(../admin/images/delete.gif);"
							class="icon-link"
							href="javascript:document.location.href='delete-module-controller.jsp?id=${module.moduleId }'"><fmt:message key="link.delete" /></a>
						</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
		<a style="background-image: url(../admin/images/add.gif);"
			class="icon-link"
			href="javascript:document.location.href='new-module.jsp?extuser=false&region=region1&item=authorization_menu'"><fmt:message key="root.actions.newapp" /></a><a
			style="background-image: url(../admin/images/add.gif);"
			class="icon-link"
			href="javascript:document.location.href='add-permissions.jsp?extuser=false&region=region1&item=authorization_menu'"><fmt:message key="root.actions.newperm" /></a> <a
			style="background-image: url(../admin/images/view.gif);"
			class="icon-link"
			href="javascript:document.location.href='view-permissions.jsp?extuser=false&region=region1&item=authorization_menu'"><fmt:message key="root.actions.search.perm" /></a>


	</div>
</div>
</fmt:bundle>