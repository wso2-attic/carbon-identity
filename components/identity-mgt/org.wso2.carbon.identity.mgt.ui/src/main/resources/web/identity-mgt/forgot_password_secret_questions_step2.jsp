<!--
~ Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.mgt.ui.IdentityManagementClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.identity.mgt.stub.dto.UserChallengesDTO" %>
<%@ page import="org.wso2.carbon.identity.mgt.stub.beans.VerificationBean" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<carbon:jsi18n
        resourceBundle="org.wso2.carbon.identity.mgt.ui.i18n.Resources"
        request="<%=request%>"/>

<fmt:bundle basename="org.wso2.carbon.identity.mgt.ui.i18n.Resources">
    <%
        String userName = request.getParameter("userName");
        String userKey = request.getParameter("userKey");
        String question = request.getParameter("question");
        String answer = request.getParameter("answer");
        UserChallengesDTO currentChallenge = (UserChallengesDTO) session.
                                    getAttribute(IdentityManagementClient.USER_CHALLENGE_QUESTION);
        try {
            String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(),
                    session);
            ConfigurationContext configContext = (ConfigurationContext) config
                    .getServletContext()
                    .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
            IdentityManagementClient client =
                    new IdentityManagementClient(backendServerURL, configContext) ;
            VerificationBean bean = client.verifyChallengeQuestion(userName, userKey, question, answer);
            if(bean != null && bean.getVerified() && bean.getKey() != null &&
                                                            bean.getKey().trim().length() > 0) {
                userKey = bean.getKey();
            }  else {
    %>
            <script type="text/javascript">
                location.href = "fail_password_reset.jsp";
            </script>
    <%
            }
        } catch (Exception e) {
    %>
            <script type="text/javascript">
                location.href = "fail_password_reset.jsp";
            </script>
    <%
        }
    %>

    <script type="text/javascript">
        function cancel(){
            location.href = "../admin/login.jsp";
        }
    </script>
    
    <div id="middle">
        <h2><fmt:message key="secret.information"/></h2>
    </div>

<%
    if(currentChallenge != null){
%>
    <form action="forgot_password_secret_questions_final.jsp"
                                                            id="userChallenge"  method="post">
    <table>
        <tbody>
        <tr>
            <td><fmt:message key="secret.question"/></td>
            <td><%=currentChallenge.getQuestion()%></td>
        </tr>
        <tr>
            <td>
                <input type="hidden" tabindex="2" name="question" id="question"
                                                    value="<%=currentChallenge.getQuestion()%>"/>
            </td>
        </tr>
        <tr>
            <td><fmt:message key="secret.answer"/></td>
            <td>
                <input type="text" tabindex="2" name="answer" id="answer" />
            </td>
        </tr>
        <tr>
            <td><input type="hidden"  name="userName" id="userName"
                       value="<%=userName%>" /></td>
            <td><input type="hidden"  name="userKey" id="userKey"
                       value="<%=userKey%>" /></td>
        </tr>
        <tr>
            <td>
                <input type="button"  value="Cancel" onclick="cancel()"/>
                <input type="submit"  value="Next"/>
            </td>
        </tr>
        </tbody>
    </table>
    </form>
<%
    } else {
%>
    <script type="text/javascript">
        location.href = "fail _password_reset.jsp";
    </script>    
<%
    }
%>
</fmt:bundle>

