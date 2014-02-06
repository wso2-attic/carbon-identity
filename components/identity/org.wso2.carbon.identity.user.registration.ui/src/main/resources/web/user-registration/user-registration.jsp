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
<%@page import="org.wso2.carbon.identity.user.registration.stub.dto.PasswordRegExDTO"%>
<%@page import="org.wso2.carbon.ui.util.CharacterEncoder"%>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.user.registration.ui.UserRegistrationConstants" %>
<%@ page import="org.wso2.carbon.identity.user.registration.ui.client.UserRegistrationClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>


<%@page import="org.wso2.carbon.user.core.UserCoreConstants" %>
<%@page import="org.wso2.carbon.identity.base.IdentityConstants" %>

<%@page import="java.util.ResourceBundle" %>
<%@ page import="java.net.URLDecoder" %>
<jsp:include page="../dialog/display_messages.jsp"/>

<%@ page import="org.wso2.carbon.identity.user.registration.stub.dto.UserFieldDTO" %>
<link media="all" type="text/css" rel="stylesheet" href="css/registration.css"/>

<%
    String cssLocation = request.getParameter("css");
    PasswordRegExDTO[] regExes = null;

    if ("null".equals(cssLocation)) {
        cssLocation = null;
    }

    if (cssLocation != null) {
        cssLocation = URLDecoder.decode(cssLocation, "UTF-8");
    }

    String pageTitle = request.getParameter("title");

    String forwardPage = request.getParameter("forwardPage");
    if (forwardPage != null) {
        forwardPage = URLDecoder.decode(forwardPage, "UTF-8");
    }

    UserFieldDTO[] userFields = null;
    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);    

    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext()
                    .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

    String forwardTo = "index.jsp?region=region1&item=user_registration_menu&ordinal=0";
    if (forwardPage != null) {
        forwardTo = forwardPage;
    }

    String BUNDLE = "org.wso2.carbon.identity.user.registration.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());

    //retrieving the claims from the OpenID
    String username = "";

    if (session.getAttribute("nickname") != null) {
        username = (String) session.getAttribute("nickname");
    }

    try {
        UserRegistrationClient client =
                new UserRegistrationClient(cookie, serverURL, configContext);
        if (session.getAttribute("openId") == null) {
            userFields = client.readUserFieldsForUserRegistration(
                    UserCoreConstants.DEFAULT_CARBON_DIALECT);
        } else {
            userFields =
                    client.readUserFieldsForUserRegistration(IdentityConstants.OPENID_SREG_DIALECT);
        }
        userFields = client.getOrderedUserFields(userFields);
        regExes = client.getPasswordRegularExpressions();
    } catch (Exception e) {
        String message = resourceBundle.getString("error.while.loading.user.registration.fields");
        session.removeAttribute("openId");
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);         
%>
<script type="text/javascript">
    function forward() {
        location.href = "<%=forwardTo%>";
    }
</script>

<script type="text/javascript">
    forward();
</script>
<%
    }
%>

<fmt:bundle basename="org.wso2.carbon.identity.user.registration.ui.i18n.Resources">
<%
    if (cssLocation == null) {
%>
<carbon:breadcrumb
        label="username.password"
        resourceBundle="org.wso2.carbon.identity.user.registration.ui.i18n.Resources"
        topPage="false"
        request="<%=request%>"/>
<%
} else {
%>
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
    <meta http-equiv="content-type" content="text/html;charset=utf-8"/>
    <title><%=pageTitle%>
    </title>

    <link href="../admin/css/global.css" rel="stylesheet" type="text/css" media="all"/>
    <link href="../dialog/css/jqueryui/jqueryui-themeroller.css" rel="stylesheet" type="text/css"
          media="all"/>
    <link href="../dialog/css/dialog.css" rel="stylesheet" type="text/css" media="all"/>

    <link href="<%=cssLocation%>" rel="stylesheet" type="text/css"
          media="all"/>

    <link rel="icon" href="../admin/images/favicon.ico" type="image/x-icon"/>
    <link rel="shortcut icon" href="../admin/images/favicon.ico" type="image/x-icon"/>

    <script type="text/javascript" src="../admin/js/jquery.js"></script>

    <script type="text/javascript" src="../admin/js/jquery.form.js"></script>
    <script type="text/javascript" src="../dialog/js/jqueryui/jquery-ui.min.js"></script>
    <script type="text/javascript" src="../admin/js/main.js"></script>

    <script type="text/javascript" src="../admin/js/WSRequest.js"></script>
    <script type="text/javascript" src="../admin/js/cookies.js"></script>
</head>

<body>
<div id="dcontainer"></div>
<script type="text/javascript" src="../dialog/js/dialog.js"></script>

<!--This is the link panel of the portal page-->
<div id="link-panel">
    <div class="left-logo">
        <a class="header-home" href="../../portal">
            <img width="179" height="28" src="images/1px.gif"/>
        </a>
    </div>
</div>
<div id="dcontainer"></div>
<script type="text/javascript" src="../dialog/js/dialog.js"></script>
<%
    }
%>


<script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
<script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
<script type="text/javascript" src="../carbon/admin/js/main.js"></script>

<div id="middle">

<%
    if (cssLocation == null) {
%>
<h2><fmt:message key="signup.with.username.and.password"/></h2>
<%
    }
%>

<div id="workArea">
<script type="text/javascript">
    function validate() {
        var value = document.getElementsByName("<%=UserRegistrationConstants.PARAM_USERNAME%>")[0].value;
        if (value == '') {
            CARBON.showWarningDialog('<fmt:message key="user.name.is.required"/>');
            return false;
        } else if (value.length > 20) {
    		CARBON.showWarningDialog('<fmt:message key="user.name.is.too.long"/>');
        	return false;
    	}
        
        var domains = document.getElementsByName("<%=UserRegistrationConstants.PARAM_DOMAINNAME%>")[0];
        var domain =  domains.options[domains.selectedIndex].value;

        value =
        document.getElementsByName("<%=UserRegistrationConstants.PARAM_PASSWORD%>")[0].value;
        var password = value;
        if (value == '') {
            CARBON.showWarningDialog('<fmt:message key="password.is.required"/>');
            return false;
        } else if (value.length > 30) {
    		CARBON.showWarningDialog('<fmt:message key="password.is.too.long"/>');
        	return false;
    	}

        value =
        document.getElementsByName("<%=UserRegistrationConstants.PARAM_RETYPED_PASSWORD%>")[0].value;
        var repassword = value;
        if (value == '') {
            CARBON.showWarningDialog('<fmt:message key="retyped.password.is.required"/>');
            return false;
        }

        if (repassword != password) {
            CARBON.showWarningDialog('<fmt:message key="password.mismatch"/>');
            return false;
        }

    	var selectedDomainValue = domain;
		var pwd = "pwd_";

		var passwordRegExElm = document.getElementById(pwd +selectedDomainValue);

		if (passwordRegExElm!=null) {
	   		passwordRegEx = document.getElementById(pwd +selectedDomainValue).value;
		} else {
			passwordRegEx = document.getElementById("pwd_primary_null").value;
		}
		
		if (passwordRegEx!=null) {
		
		 	var passReg = new RegExp(passwordRegEx);
	     	var valid = passReg.test(value);
	     	if (value != '' && !valid) {
	            CARBON.showWarningDialog("Password strength does not meet with the required criteria.");
	            return false;
	     	}
       }
	

    <%if (userFields!=null) {
for (int i=0; i< userFields.length;i++) { %>
        value = document.getElementsByName("<%=userFields[i].getClaimUri()%>")[0].value;
    <% if (userFields[i].getRequired()) {%>
        if (value == '') {
            CARBON.showWarningDialog("<%=userFields[i].getFieldName()%>" +
                                     " <fmt:message key="is.required"/>");
            return false;
        }
        
        if (value != null && value.length > 30) {
    		CARBON.showWarningDialog(' <fmt:message key="value.is.too.long"/>');
        	return false;
    	}

    <%}
    
    
    if(userFields[i].getRegEx() != null){
    %>
        var reg = new RegExp("<%=userFields[i].getRegEx() %>");
        var valid = reg.test(value);
        if (value != '' && !valid) {
            CARBON.showWarningDialog("<%=userFields[i].getFieldName()%>" +
                                     " <fmt:message key="is.not.valid"/>");
            return false;
        }
    <%}%>

    var unsafeCharPattern = /[<>`\"]/;
    var elements = document.getElementsByTagName("input");
    for(i = 0; i < elements.length; i++){
        if((elements[i].type === 'text' || elements[i].type === 'password') && 
           elements[i].value != null && elements[i].value.match(unsafeCharPattern) != null){
            CARBON.showWarningDialog("<fmt:message key="unsafe.char.validation.msg"/>");
            return false;
        }
    }

    <%} }%>

        document.registrationform.submit();
    }
</script>

<%
    String user = null;
    user = (String) session.getAttribute("logged-user");
    if (user != null) {
%>
<div id="middle">
    <h2><fmt:message key='sign.up'/></h2>

    <div id="workArea">
        <fmt:message key='signout.message'/>
    </div>
</div>
<%} else { %>
<form method="post" name="registrationform" action="add_user.jsp" target="_self">

<%

if (regExes!=null) {
	
	for (int i=0; i < regExes.length;i++) {
		if (i==0) {
%>
<input type="hidden"  id="pwd_primary_null" name="pwd_primary_null" value=<%=regExes[i].getRegEx()%>>   

<% 
		}
%>
 <input type="hidden"  id="pwd_<%=regExes[i].getDomainName().toUpperCase()%>" name="pwd_<%=regExes[i].getDomainName().toUpperCase()%>" value=<%=regExes[i].getRegEx()%>>    

<% 
		
	}
}

%>


<%
if(forwardPage!=null){
    %>
    <input type="hidden" name="forwardPage" value="<%=forwardPage%>"/>
    <%
}
%>
    <table style="width: 100%" class="styledLeft">
        <thead>
            <tr>
                <th colspan="2"><fmt:message key='user.registration'/></th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="leftCol-small"><fmt:message key='domain.name'/><span
                        class="required">*</span></td>
                <td><select id="<%=UserRegistrationConstants.PARAM_DOMAINNAME%>" name="<%=UserRegistrationConstants.PARAM_DOMAINNAME%>">
                    <%
                        for(int j=0; j<regExes.length; j++) {
                            String domainName= regExes[j].getDomainName().toUpperCase();
                    %>
                    <option value="<%=domainName%>"><%=domainName%></option>
                    <%
                        }
                    %>
                </select></td>
            </tr>
            <tr>
                <td class="leftCol-small"><fmt:message key='user.name'/><span
                        class="required">*</span></td>
                <td><input class="text-box-big" id="<%=UserRegistrationConstants.PARAM_USERNAME%>"
                           name="<%=UserRegistrationConstants.PARAM_USERNAME%>" type="text"
                           value="<%=username%>"></td>
            </tr>
            <tr>
                <td class="leftCol-small"><fmt:message key='password'/><span
                        class="required">*</span></td>
                <td><input class="text-box-big" id="<%=UserRegistrationConstants.PARAM_PASSWORD%>"
                           name="<%=UserRegistrationConstants.PARAM_PASSWORD%>" type="password">
                </td>
            </tr>
            <tr>
                <td class="leftCol-small"><fmt:message key='retype.password'/><span
                        class="required">*</span></td>
                <td><input class="text-box-big"
                           id="<%=UserRegistrationConstants.PARAM_RETYPED_PASSWORD%>"
                           name="<%=UserRegistrationConstants.PARAM_RETYPED_PASSWORD%>"
                           type="password"></td>
            </tr>

            <% if (userFields != null) {
                for (int i = 0; i < userFields.length; i++) {

                    if (session.getAttribute(userFields[i].getClaimUri()) == null) {
                        session.setAttribute(userFields[i].getClaimUri(), "");
                    }
            %>
            <tr>
                <td class="leftCol-small"><%=CharacterEncoder.getSafeText(userFields[i].getFieldName())%>
                    <% if (userFields[i].getRequired()) {%>
                    <span class="required">*</span>
                    <%}%></td>
                <td><input class="text-box-big" id="<%=userFields[i].getClaimUri()%>"
                           name="<%=userFields[i].getClaimUri()%>" type="text"
                           value="<%=(String)session.getAttribute(userFields[i].getClaimUri())%>">
                </td>
            </tr>
            <%
                    }
                }
            %>

            <tr>
                <td class="buttonRow" colspan="2">
                    <input name="adduser" type="button" class="button"
                           value="<fmt:message key='submit'/>"
                           onclick="validate();"/>
                    <input type="button" class="button"
                           onclick="javascript:location.href='<%=forwardTo%>'"
                           value="<fmt:message key='cancel'/>"/>
                </td>
            </tr>
        </tbody>
    </table>

</form>
<% } %>
</div>
</div>
</fmt:bundle>
<%
    if (cssLocation != null) {
        // Closing HTML page tags.
%>
<div class="footer-content">
    <div class="copyright">&copy; 2008 - 2009 WSO2 Inc. All Rights Reserved.</div>
</div>
</body>
</html>
<%
    }
%>
