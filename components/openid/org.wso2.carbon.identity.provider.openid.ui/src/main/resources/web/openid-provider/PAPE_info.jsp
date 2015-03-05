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
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.openid4java.message.ParameterList" %>

<script type="text/javascript" src="global-params.js"></script>
<%
    String phishingRes = (String) session.getAttribute("papePhishingResistance");
    String multiFactorAuth = (String) session.getAttribute("multiFactorAuth");
    String inforCardBasedMultiFactorAuth = (String) session.getAttribute("infoCardBasedMultiFacotrAuth");
    String xmppBasedMultiFactorAuth = (String) session.getAttribute("xmppBasedMultiFacotrAuth");
    boolean isPhishinResistant = false;
    boolean isXmppBasedMFA = false;
    boolean isInfocardBasedMFA = false;

    if (phishingRes.equals("true")) {
        isPhishinResistant = true;
    }

    if (multiFactorAuth.equals("true")) {
        if (inforCardBasedMultiFactorAuth.equals("true")) {
            isInfocardBasedMFA = true;
        }

        if (xmppBasedMultiFactorAuth.equals("true")) {
            isXmppBasedMFA = true;
        }
    }

    ParameterList requestp = (ParameterList) session
            .getAttribute("parameterlist");
    String openidrealm = requestp.hasParameter("openid.realm") ? requestp
            .getParameterValue("openid.realm")
            : null;
    String openidreturnto = requestp.hasParameter("openid.return_to") ? requestp
            .getParameterValue("openid.return_to")
            : null;
    String openidclaimedid = requestp.hasParameter("openid.claimed_id") ? requestp
            .getParameterValue("openid.claimed_id")
            : null;
    String openididentity = requestp.hasParameter("openid.identity") ? requestp
            .getParameterValue("openid.identity")
            : null;
    String site = (String) (openidrealm == null ? openidreturnto
            : openidrealm);
    session.setAttribute("openId", openididentity);
%>

<fmt:bundle basename="org.wso2.carbon.identity.provider.openid.ui.i18n.Resources">
    <carbon:breadcrumb
            label="pape.info"
            resourceBundle="org.wso2.carbon.identity.provider.openid.ui.i18n.Resources"
            topPage="false"
            request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>
    <script type="text/javascript">
        function submitPhishingResistanceform()
        {
            document.phishingResistanceInfoCardAuthForm.submit();
        }
    </script>
    <script type="text/javascript">
        function submitMultifactorform()
        {
            document.multifactorInfoCardAuthForm.submit();
        }
    </script>

    <div id="middle">
        <h2 id="identity"><fmt:message key='pape.information'/></h2>

        <div id="workArea">

            <%if (isPhishinResistant) {%>
            <img src="images/openid-input.gif" align="top" style="margin-right:5px;"/> <a style="font-size:13px"
                                                                                          href="<%=openididentity%>"><%=openididentity%>
        </a>

            <div id="loginbox" class="identity-box" style="text-align:left !important;padding:10px; height:120px;">

                <table style="float:left">
                    <tr>
                        <td>
                            <form name="phishingResistanceInfoCardAuthForm" action="multifactor_auth_infocard.jsp"
                                  method="POST">

                                <img src="images/infocard-logo.png" border="0" align="left"
                                     onclick="submitPhishingResistanceform();" style="cursor:pointer">
                            </form>
                        </td>
                        <td>
								<span style="line-height:25px;"><fmt:message key='phishing.message1'/><br/>
								<fmt:message key='phishing.message2'/></span>
                        </td>
                    </tr>
                </table>
                <span style="clear:both"/>

            </div>
            <%} else if (isInfocardBasedMFA) {%>
            <img src="images/openid-input.gif" align="top" style="margin-right:5px;"/><a
                href="<%=openididentity%>"><%=openididentity%>
        </a>

            <div id="loginbox" class="identity-box" style="text-align:left;height:120px">
                <table style="float:left">
                    <tr>

                        <td style="text-align:center !important">
                            <form name="multifactorInfoCardAuthForm"
                                  action="PAPE_multi_factor_auth_infocard.jsp"
                                  method="POST">

                                <img src="images/infocard-logo.png" border="0"
                                     onclick="submitMultifactorform();" align="left" style="cursor:pointer"/>

                            </form>
                        </td>
                        <td>
                                                <span style="line-height:25px;"><fmt:message key='multifactor.auth.message1'/><br/>
                                                <fmt:message key='multifactor.auth.message2'/>
                                                <% if (isXmppBasedMFA) {%>
                                                <fmt:message key='multifactor.auth.message3'/><fmt:message key='multifactor.auth.message4'/>
                                                <%}%>
								</span>
                        </td>
                    </tr>

                </table>
            </div>

            <%} else if (isXmppBasedMFA) {%>


            <img src="images/openid-input.gif" align="top" style="margin-right:5px;"/><a
                href="<%=openididentity%>"><%=openididentity%>
        </a>

            <div id="loginbox" class="identity-box" style="text-align:left;">
                <form action="openid_profile_view.jsp" method="post">
                    <input type="hidden" id='openid' name='openid' value="<%=openididentity%>"/>

                    <div style="line-height:25px;">
                        <fmt:message key='xmpp.message1'/>
                        <a href="<%=openididentity%>"><%=openididentity%> </a>
                        <fmt:message key='xmpp.message2'/>
                        <a href="<%=openidreturnto%>"><%=openidreturnto%> </a>
                        <fmt:message key='xmpp.message3'/>
                    </div>

                    <input type="password" id='password' name="password"
                           size='30' style="margin-top:10px;"/>
                    <br/><br/>
                    <input type="submit" value="<fmt:message key='login'/>" class="button">
                </form>
            </div>

            <%}%>
        </div>
    </div>
</fmt:bundle>