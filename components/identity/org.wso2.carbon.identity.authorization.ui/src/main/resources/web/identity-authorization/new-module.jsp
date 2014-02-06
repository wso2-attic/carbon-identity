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
	<carbon:breadcrumb label="root.actions.newapp"
		resourceBundle="org.wso2.carbon.identity.authorization.ui.i18n.Resources"
		topPage="false" request="<%=request%>" />

	<div id="middle">
		<h2>
			<fmt:message key="identity.cusom.perm.heading" />
		</h2>
		<h3>
			<fmt:message key="identity.cusom.perm.heading.app.new.app" />
		</h3>
		<div id="workArea" style="padding-bottom: 70px; margin-top: 10px;">

			<form id="moduleNewForm"
				action="/carbon/identity-authorization/new-module-controller.jsp"
				method="post">

				<input type="hidden" value="0" id="numberOfActions"
					name="numberOfActions" /> <input type="hidden" value="0"
					id="numberOfResources" name="numberOfResources" />

				<table class="styledLeft" style="clear: both;">
					<thead>
						<tr>
							<th colspan="1"><fmt:message key="module.name" /></th>
						</tr>
					</thead>
					<tbody>
						<tr>
							<td><input type="text" name="moduleName"></td>
						</tr>
					</tbody>
				</table>

				<table class="styledLeft" style="clear: both; margin-top: 10px;">
					<tr>
						<td>

							<div>

								<table class="styledLeft" style="clear: both; margin-top: 10px;">
									<thead>
										<tr>
											<th colspan="1">
												<div style="float: left;">
													<fmt:message key="reg.apps.actions" />
												</div>
												<div class="sectionHelp"
													style="float: left; margin-left: 10px;">
													<fmt:message key="help.action" />
												</div>
											</th>
										</tr>
									</thead>
									<tbody>
										<tr id="newActionRow_0">
											<td><input type="text" name="newAction_0" /></td>
										</tr>

										<tr id="buttonPanel_action">
											<td colspan="2"><a id="addMoreAction_1"
												style="background-image: url(../admin/images/add.gif);"
												class="icon-link" href="javascript:void(0);"><fmt:message
														key="link.add.new.action" /></a></td>
										</tr>
									</tbody>
								</table>
							</div>
						</td>
					</tr>
					<tr>
						<td>
							<div>

								<table class="styledLeft" style="clear: both; margin-top: 10px;">
									<thead>
										<tr>
											<th colspan="1">
												<div style="float: left;">
													<fmt:message key="reg.apps.resources" />
												</div>

												<div class="sectionHelp"
													style="float: left; margin-left: 10px;">
													<fmt:message key="help.resource" />
												</div>
											</th>
										</tr>
									</thead>
									<tbody>
										<tr id="newResourceRow_0">
											<td><input type="text" name="newResource_0" /></td>
										</tr>

										<tr id="buttonPanel_resource">
											<td colspan="2"><a id="addMoreResource_1"
												style="background-image: url(../admin/images/add.gif);"
												class="icon-link" href="javascript:void(0);"><fmt:message
														key="link.add.new.resource" /></a></td>
										</tr>
									</tbody>
								</table>

							</div>


						</td>
					</tr>
					<tr>
						<td colspan="1"><input type="button" id="submitNewModule"
							value='<fmt:message key="link.finish" />' class="button" /> <input
							type="button" onclick="cancellAdding();"
							value='<fmt:message key="link.cancel" />' class="button" /></td>
					</tr>

				</table>


			</form>
		</div>
	</div>
</fmt:bundle>