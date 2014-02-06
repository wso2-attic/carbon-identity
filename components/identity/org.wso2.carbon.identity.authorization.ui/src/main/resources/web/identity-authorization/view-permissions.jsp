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

<script type="text/javascript" src="js/util.js"></script>
<script type="text/javascript" src="js/cust_permissions.js"></script>
<fmt:bundle basename="org.wso2.carbon.identity.authorization.ui.i18n.Resources" >
<carbon:breadcrumb
		label="root.actions.search.perm"
		resourceBundle="org.wso2.carbon.identity.authorization.ui.i18n.Resources"
		topPage="false"
		request="<%=request%>" />

<div id="middle">
	<%@include file="messages.jsp"%>

	<h2><fmt:message key="identity.cusom.perm.heading" /></h2>
	<h3><fmt:message key="identity.cusom.perm.heading.view.perm" /></h3>
	<div id="workArea" style="padding-bottom: 70px; margin-top: 10px;">
		<span><fmt:message key="name.search.cri" /></span>
		<form id="viewPermissionsForm"
			action="/carbon/identity-authorization/view-permissions-controller.jsp"
			method="post">
			<input type="hidden" name="operation" id="operation" value="1" />
			<table class="styledLeft" style="width: 50%">
				<tr>
					<td><fmt:message key="name.application" /></td>
					<td><select name="application">
							<c:forEach items="${sessionScope.modules }" var="module">
								<option
									<c:if test="${not empty permissionRequest and permissionRequest.module eq  module.moduleName}">selected="selected"</c:if>
									value="${module.moduleName }">${module.moduleName }</option>
							</c:forEach>
					</select></td>
				</tr>
				<tr>
					<td><fmt:message key="name.role" /></td>
					<td><input
						<c:if
						test="${not empty permissionRequest and not empty permissionRequest.subject}">value="${permissionRequest.subject }"</c:if>
						type="text" name="role" /></td>
				</tr>
				<tr>
					<td><fmt:message key="name.resource" /></td>
					<td><input
						<c:if
						test="${not empty permissionRequest and not empty permissionRequest.resource}">value="${permissionRequest.resource }"</c:if>
						type="text" name="resource" /></td>
				</tr>

				<tr id="buttonPanel">
					<td colspan="2"><input type="button" onclick="submitPermissionsSearchReq();" value="<fmt:message key="link.search" />"
						class="button" /></td>
				</tr>
			</table>




			<table class="styledLeft"
				style="width: 50%; clear: both; margin-top: 20px;">
				<thead>
					<tr>
						<th style="width: 5%"><input type="checkbox" id="selectAll" />
						</th>
						<th><fmt:message key="name.application" /></th>
						<th><fmt:message key="name.resource" /></th>
						<th><fmt:message key="name.role" /></th>
						<th><fmt:message key="name.action" /></th>
						<th><fmt:message key="name.autho" /></th>
					</tr>
				</thead>
				<tbody>
					<c:set var="currentPage"
						value="<%=request.getParameter("p") != null
			                                           ? Integer.parseInt(request.getParameter("p"))
			                                           : 1%>" />
					<auth:paging pageNumber="${currentPage}" pageSize="10"
						dataSet="${permissionsResult.permissions }">
						<tr id="permRow_${data.subjectPermissionId}">
							<td><input type="checkbox"
								id="select_${data.subjectPermissionId}"
								name="select_${data.subjectPermissionId}"
								value="${data.subjectPermissionId }" /></td>
							<td>${permissionsResult.moduleName }</td>
							<td>${data.resourceId }</td>
							<td>${data.subject }</td>
							<td>${data.action }</td>
							<td>${data.authorized }</td>
						</tr>

					</auth:paging>
					<tr>
						<td colspan="6">
							<ul>
								<c:forEach items="${pages }" var="page">
									<c:choose>
										<c:when test="${currentPage eq page }">
											<li
												style="display: inline-block; text-align: center; padding: 4px; background-color: #FF9880"><a
												href="view-permissions.jsp?p=${page}">${page }</a></li>

										</c:when>
										<c:otherwise>
											<li
												style="display: inline-block; text-align: center; padding: 4px; background-color: #F5DEB3"><a
												href="view-permissions.jsp?p=${page}">${page }</a></li>
										</c:otherwise>
									</c:choose>
								</c:forEach>
							</ul>
						</td>
					</tr>

					<tr id="buttonPanel">
						<td colspan="6"><a
							style="background-image: url(../admin/images/add.gif);"
							class="icon-link"
							href="javascript:document.location.href='add-permissions.jsp?extuser=false&region=region1&item=authorization_menu'"><fmt:message key="root.actions.newperm" /></a>
							<input type="button" value='<fmt:message key="link.delete" />'
							class="button" onclick="deletePermissions();" /> <input
							type="button" onclick="cancellAdding();" value="<fmt:message key="link.cancel" />"
							class="button" /></td>
					</tr>
				</tbody>
			</table>
			<input type="hidden" name="deleted" id="deleted" value="" />
		</form>




	</div>
</div>
</fmt:bundle>