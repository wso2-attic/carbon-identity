<%--
~ Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
~
~  WSO2 Inc. licenses this file to you under the Apache License,
~  Version 2.0 (the "License"); you may not use this file except
~  in compliance with the License.
~   You may obtain a copy of the License at
~
~     http://www.apache.org/licenses/LICENSE-2.0
~
~  Unless required by applicable law or agreed to in writing,
~  software distributed under the License is distributed on an
~  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~  KIND, either express or implied.  See the License for the
~  specific language governing permissions and limitations
~  under the License.
--%>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.CharacterEncoder"%>


<html>
<head>
    <title></title>
    <%
//        String authRequest = CharacterEncoder.getSafeText(request.getParameter("data"));
        String authRequest = request.getParameter("data");

    %>

    <script type="text/javascript" src="js/u2f-api.js"></script>
    <%--<script type="text/javascript" src="chrome-extension://pfboblefjcgdjicmnffhdgionmgcdmne/u2f-api.js"></script>--%>

    <script type="text/javascript">
        function talkToDevice(){
            var authRequest = '<%=authRequest%>';
            var jsonAuthRequest = JSON.parse(authRequest);
            setTimeout(function() {
                u2f.sign(jsonAuthRequest.authenticateRequests,
                        function(data) {
                            var form = document.getElementById('form');
                            var reg = document.getElementById('tokenResponse');
                            reg.value = JSON.stringify(data);
                            form.submit();
                        });
            }, 1000);
        }


    </script>

</head>
<body onload='talkToDevice();'>
<p>Touch your U2F token.</p>
<form method="POST" action="../commonauth" id="form" onsubmit="return false;">
    <input type="hidden" name="sessionDataKey" value='<%=CharacterEncoder.getSafeText(request.getParameter("sessionDataKey"))%>'/>
    <input type="hidden" name="tokenResponse" id="tokenResponse" value="tmp val"/>
</form>
</body>
</html>