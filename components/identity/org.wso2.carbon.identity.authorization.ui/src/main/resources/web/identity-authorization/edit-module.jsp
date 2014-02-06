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

<fmt:bundle
	basename="org.wso2.carbon.identity.authorization.ui.i18n.Resources">
	<carbon:breadcrumb label="identity.cusom.perm.heading.edit.app"
		resourceBundle="org.wso2.carbon.identity.authorization.ui.i18n.Resources"
		topPage="false" request="<%=request%>" />

	<auth:module config="<%=config%>" request="<%=request%>" action="1"
		moduleId="<%=Integer.parseInt(request.getParameter("id"))%>"></auth:module>

	<div id="middle">
		<h2>
			<fmt:message key="identity.cusom.perm.heading" />
		</h2>
		<h3>
			<fmt:message key="identity.cusom.perm.heading.edit.app" />
		</h3>
		<h4>${module.moduleName }</h4>
		<div id="workArea" style="padding-bottom: 70px; margin-top: 10px;">
			<c:choose>
				<c:when test="${!empty redirect }">
					<script type="text/javascript">
						function forward() {
							location.href = "\"" + $
							{
								redirect
							}
							+"\"";
						}

						forward();
					</script>

				</c:when>
				<c:otherwise>
					<form id="moduleEditForm"
						action="/carbon/identity-authorization/edit-module-controller.jsp"
						method="post">

						<input type="hidden" value="0" id="numberOfActions"
							name="numberOfActions" /> <input type="hidden" value="0"
							id="numberOfResources" name="numberOfResources" /> <input
							type="hidden" value="<%=request.getParameter("id")%>"
							name="moduleId">
						<table class="styledLeft" style="clear: both;">
							<thead>
								<tr>
									<th style="width: 12%"><fmt:message key="link.delete" /></th>
									<th colspan="1"><fmt:message key="reg.apps.actions" /></th>
								</tr>
							</thead>
							<tbody>
								<c:forEach items="${module.actions }" var="action"
									varStatus="state">
									<tr id="actionRow_${state.index}">
										<td><input type="checkbox" id="delete_${action}"
											name="delete_${state.index + 1}" value="${action }" /></td>
										<td>${action }</td>
									</tr>
								</c:forEach>

								<tr id="buttonPanel_action">
									<td colspan="2"><a id="addMoreAction_1"
										style="background-image: url(../admin/images/add.gif);"
										class="icon-link" href="javascript:void(0);"><fmt:message
												key="link.add.new.action" /></a></td>
								</tr>
							</tbody>
						</table>
						<table class="styledLeft" style="clear: both; margin-top: 20px;">
							<thead>
								<tr>
									<th style="width: 12%"><fmt:message key="link.delete" /></th>
									<th colspan="1"><fmt:message key="reg.apps.resources" /></th>
								</tr>
							</thead>
							<tbody>
								<c:forEach items="${module.resources }" var="res"
									varStatus="state">
									<tr id="resourceRow_${state.index}">
										<td><input type="checkbox" id="delete_${res.name}"
											name="delete_${res.id}" value="${res.id }" /></td>
										<td>${res.name }</td>
									</tr>
								</c:forEach>

								<tr id="buttonPanel_resource">
									<td colspan="2"><a id="addMoreResource_1"
										style="background-image: url(../admin/images/add.gif);"
										class="icon-link" href="javascript:void(0);"><fmt:message
												key="link.add.new.resource" /></a></td>
								</tr>
							</tbody>
						</table>

						<table>
							<tr id="buttonPanel">
								<td><input type="submit"
									value="<fmt:message key="link.finish" />" class="button" /> <input
									type="button" onclick="cancellAdding();"
									value="<fmt:message key="link.cancel" />" class="button" /></td>
							</tr>
						</table>

					</form>
					<table style="display: none;">
						<tr style="display: none;" id="newActionRow_0">
							<td><input type="checkbox" id="deleteNewAction_"
								name="deleteNewAction_" value="deleted" /></td>
							<td><input type="text" name="newAction_" /></td>
						</tr>

						<tr style="display: none;" id="newResourceRow_0">
							<td><input type="checkbox" id="deleteNewResource_"
								name="deleteNewResource_" value="deleted" /></td>
							<td><input type="text" name="newResource_" /></td>
						</tr>


					</table>
				</c:otherwise>
			</c:choose>



		</div>
	</div>
</fmt:bundle>