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
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"  prefix="carbon" %>


<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../extensions/core/js/vui.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>

<jsp:include page="../dialog/display_messages.jsp"/>


<fmt:bundle
        basename="org.wso2.carbon.identity.scim.ui.i18n.Resources">
    <carbon:breadcrumb label="add.new.provider"
                       resourceBundle="org.wso2.carbon.identity.scim.ui.i18n.Resources"
                       topPage="false" request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>

    <div id="middle">
        <h2><fmt:message key='add.new.provider'/></h2>

        <div id="workArea">
            <script type="text/javascript">
                function validate() {
                    var value = document.getElementsByName("providerId")[0].value;
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="provider.is.required"/>');
                        return false;
                    }
                    value = document.getElementsByName("username")[0].value;
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="username.required"/>');
                        return false;
                    }
                    value = document.getElementsByName("password")[0].value;
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="password.required"/>');
                        return false;
                    }
                    value = document.getElementsByName("userURL")[0].value;
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="user.url.is.required"/>');
                        return false;
                    }
                    document.addProviderForm.submit();
                }
            </script>

            <form method="post" name="addProviderForm" action="add-finish.jsp"
                  target="_self">
                <table style="width: 100%" class="styledLeft">
                    <thead>
                    <tr>
                        <th><fmt:message key='new.provider'/></th>
                    </tr>
                    </thead>
                    <tbody>
		    <tr>
			<td class="formRow">
				<table class="normal" >
		                    <tr>
		                        <td class="leftCol-small"><fmt:message key='provider.id'/><span class="required">*</span> </td>
		                        <td><input class="text-box-big" id="providerId" name="providerId"
		                                   type="text" /></td>
		                    </tr>
		                    <tr>
		                        <td class="leftCol-small"><fmt:message key='user.name'/><span class="required">*</span></td>
		                        <td><input class="text-box-big" id="username" name="username"
		                                   type="text" /></td>
		                    </tr>
                            <tr>
		                        <td class="leftCol-small"><fmt:message key='password'/><span class="required">*</span></td>
		                        <td><input class="text-box-big" id="password" name="password"
		                                   type="password" /></td>
		                    </tr>
                            <tr>
		                        <td class="leftCol-small"><fmt:message key='user.endpoint.url'/><span class="required">*</span></td>
		                        <td><input class="text-box-big" id="userURL" name="userURL"
		                                   type="text" /></td>
		                    </tr>
                            <tr>
		                        <td class="leftCol-small"><fmt:message key='group.endpoint.url'/></td>
		                        <td><input class="text-box-big" id="groupURL" name="groupURL"
		                                   type="text" /></td>
		                    </tr>
				</table>
			</td>
		    </tr>
                    <tr>
                        <td class="buttonRow" >
                            <input name="addProvider" type="button" class="button" value="<fmt:message key='add'/>" onclick="validate();"/>
                            <input type="button" class="button"
                                       onclick="javascript:location.href='index.jsp?region=region1&item=oauth_menu&ordinal=0'"
                                   value="<fmt:message key='cancel'/>"/></td>
                    </tr>
                    </tbody>
                </table>

            </form>
        </div>
    </div>
</fmt:bundle>

