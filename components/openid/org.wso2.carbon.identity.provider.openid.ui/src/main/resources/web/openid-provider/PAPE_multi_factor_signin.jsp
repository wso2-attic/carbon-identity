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
<%@ page import="org.openid4java.message.ParameterList" %>

<script type="text/javascript" src="global-params.js"></script>
<%
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
    String xmppBasedMultiFactorAuth = (String) session.getAttribute("xmppBasedMultiFacotrAuth");
    boolean isXmppBasedMultifactorAuthEnabled = (xmppBasedMultiFactorAuth == null || xmppBasedMultiFactorAuth
            .equals("false")) ? false : true ;
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

     <div id="middle">
        <h2 id="identity"><fmt:message key='pape.information'/></h2>

        <div id="workArea">
         <img src="images/openid-input.gif" align="top" style="margin-right:5px;"/><a
                href="<%=openididentity%>"><%=openididentity%>
        </a>

            <div id="loginbox" class="identity-box" style="text-align:left;">
                <form action="openid_profile_view.jsp" method="post">
                    <input type="hidden" id='openid' name='openid' value="<%=openididentity%>"/>

                    <div style="line-height:25px;">
                        <fmt:message key='openid.message1'/>
                        <a href="<%=openididentity%>"><%=openididentity%></a>
                        <fmt:message key='openid.message2'/>
                        <a href="<%=openidreturnto%>"><%=openidreturnto%>
                    </a>
                        .<%if(isXmppBasedMultifactorAuthEnabled){%>
                        <fmt:message key='openid.message3'/>                        
                        <%}%>
                    </div>

                    <input type="password" id='password' name="password"
                           size='30' style="margin-top:10px;"/>
                    <br/><br/>
                    <input type="submit" value="<fmt:message key='login'/>" class="button">

                </form>
            </div>


        </div>
    </div>
</fmt:bundle>