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
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head>
<body>
<form name="frm" id="frm" method="post"
	action="multifactor_auth_infocard_accept.jsp"><input type="hidden"
	name="InfoCardSignin" value="Log in" /><br />
<input type="hidden" name="FromIdentityProvider" value="true" /><br />
<input type="hidden" name="AuthenticationMethod" value="infocard" /><br />
<OBJECT type="application/x-informationCard" name="xmlToken">
    <PARAM  Name="tokenType"
                        Value="http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1">

	<PARAM Name="requiredClaims"
		Value="http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname
		       http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname
		       http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress
		       http://wso2.org/claims/tenant
		       http://schemas.xmlsoap.org/ws/2005/05/identity/claims/privatepersonalidentifier">
	<PARAM Name='optionalClaims'
		value='http://schemas.xmlsoap.org/ws/2005/05/identity/claims/webpage'>
 </OBJECT></form>
<script language="JavaScript" type="text/JavaScript">
                        <!--
                        document.frm.submit();
                        -->
                </script>
</body>
</html>