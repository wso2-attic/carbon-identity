<!--
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
~ KIND, either express or implied. See the License for the
~ specific language governing permissions and limitations
~ under the License.
-->
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
           prefix="carbon" %>
<%@page import="org.wso2.carbon.utils.ServerConstants" %>
<%@page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@page import="org.apache.axis2.context.ConfigurationContext" %>
<%@page import="org.wso2.carbon.CarbonConstants" %>
<%@page import="java.lang.Exception" %>
<%@page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../extensions/core/js/vui.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>

<jsp:include page="../dialog/display_messages.jsp"/>

<%
    String username = request.getParameter("username");

    if (username == null) {
        username = (String) request.getSession().getAttribute("logged-user");
    }
%>

<%@page import="org.wso2.carbon.user.core.UserCoreConstants" %>
<fmt:bundle
        basename="org.wso2.carbon.i18n.mgt.ui.i18n.Resources">
    <carbon:breadcrumb label="email.add"
                       resourceBundle="org.wso2.carbon.identity.user.profile.ui.i18n.Resources"
                       topPage="true" request="<%=request%>"/>


    <script type="text/javascript" src="extensions/js/vui.js"></script>
    <script type="text/javascript" src="../extensions/core/js/vui.js"></script>
    <script type="text/javascript" src="../admin/js/main.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>


    <div id="middle">
        <h2><fmt:message key='email.template.management'/></h2>

        <div id="workArea">


            <script type="text/javascript">

                function validate() {

                    var value = document.getElementsByName("emailType")[0].value;
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="email.template.type.is.required"/>');
                        return false;
                    } else if (value.length > 50) {
                        CARBON.showWarningDialog('<fmt:message key="email.template.type.is.too.long"/>');
                        return false;
                    }

                    var value = document.getElementsByName("emailSubject")[0].value;
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="email.template.subject.is.required"/>');
                        return false;
                    } else if (value.length > 50) {
                        CARBON.showWarningDialog('<fmt:message key="email.template.subject.is.too.long"/>');
                        return false;
                    }


                    var value = document.getElementsByName("emailBody")[0].value;
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="email.template.body.is.required"/>');
                        return false;
                    }

                    var value = document.getElementsByName("emailFooter")[0].value;
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="email.template.footer.is.required"/>');
                        return false;
                    }
                    document.addemailtemplate.submit();
                }

            </script>

            <form name="addemailtemplate" action="add-email-template-submit.jsp" method="post">
                <table style="width: 100%" class="styledLeft">
                    <thead>
                    <tr>
                        <th colspan="2"><fmt:message key='add.new.emailtemplate.details'/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td class="formRow">
                            <table class="normal" cellspacing="0">
                                <tr>
                                    <td class="leftCol-small"><fmt:message key='email.template.type'/><font color="red">*</font>
                                    </td>
                                    <td class="leftCol-big"><input type="text" name="emailType" id="emailType"
                                                                   class="text-box-big"/></td>
                                </tr>

                                <tr>
                                    <td class="leftCol-med labelField"><fmt:message key="email.template.locale"/></td>
                                    <td><select id="emailLocale" name="emailLocale" class="leftCol-med">
                                        <option value="en">English</option>
                                        <option value="es">Spanish</option>
                                        <option value="ja">Japanese</option>
                                        <option value="fr">French</option>
                                    </select></td>
                                </tr>
                                <tr>
                                    <td class="leftCol-small"><fmt:message key='email.template.subject'/><font
                                            color="red">*</font></td>
                                    <td><input type="text" name="emailSubject" id="emailSubject" class="text-box-big"
                                               style="width:500px" ;/></td>
                                </tr>
                                <tr>
                                    <td class="leftCol-small"><fmt:message key='email.template.body'/><font color="red">*</font>
                                    </td>
                                    <td><textarea name="emailBody" id="emailBody" class="text-box-big"
                                                  style="width: 500px; height: 170px;"></textarea></td>
                                </tr>
                                <tr>
                                    <td class="leftCol-small"><fmt:message key='email.template.footer'/><font
                                            color="red">*</font></td>
                                    <td><textarea name="emailFooter" id="emailFooter" class="text-box-big"
                                                  style="width: 265px; height: 87px;"></textarea></td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2" class="buttonRow">
                            <input type="button" value="<fmt:message key='email.add'/>" class="button"
                                   onclick="validate();"/>
                        </td>
                    </tr>

                    </tbody>
                </table>
            </form>
        </div>
    </div>
</fmt:bundle>
