<%--
  Created by IntelliJ IDEA.
  User: ishara
  Date: 4/5/15
  Time: 4:16 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.wso2.carbon.identity.application.authentication.endpoint.util.CharacterEncoder"%>


<html>
<head>
    <title></title>
    <%
        String authRequest = (String) request.getParameter("data");

    %>

    <%--<script type="text/javascript" src="js/u2f-api.js"></script>--%>
    <script type="text/javascript" src="chrome-extension://pfboblefjcgdjicmnffhdgionmgcdmne/u2f-api.js"></script>

    <script type="text/javascript">
        function next(){
            var sample = '<%=authRequest%>';
            var jsample = JSON.parse(sample);
            alert(jsample);
            setTimeout(function() {
                u2f.sign(jsample.authenticateRequests,
                        function(data) {
                            var form = document.getElementById('form');
                            var reg = document.getElementById('tokenResponse');
                            if(data.errorCode) {
                                alert("U2F failed with error: " + data.errorCode);
                                return;
                            }
                            reg.value=JSON.stringify(data);
                            alert('test authentication');
                            form.submit();
                        });
            }, 1000);
        }


    </script>

    <script type="text/javascript">

        var name = document.getElementById('tokenResponse').value;

        function sample()
        {
            alert("ishara");

        }
    </script>

    <script type="text/javascript">
        var request = ${data};
        var regRequest = document.getElementById('tokenResponse');
        setTimeout(function() {
            u2f.register(request.registerRequests, request.authenticateRequests,
                    function(data) {
                        var form = document.getElementById('form');
                        var reg = document.getElementById('tokenResponse');
                        if(data.errorCode) {
                            alert("U2F failed with error: " + data.errorCode);
                            return;
                        }
                        reg.value=JSON.stringify(data);
                        form.submit();
                    });
        }, 1000);
    </script>
</head>
<body onload='next();'>
<p>Touch your U2F token.</p>
<form method="POST" action="../commonauth" id="form" onsubmit="return false;">
    <input type="text" name="username" id="username" value="ishara"/>
    <input type="hidden" name="sessionDataKey" value='<%=CharacterEncoder.getSafeText(request.getParameter("sessionDataKey"))%>'/>
    <input type="hidden" name="tokenResponse" id="tokenResponse" value="tmp val"/>
    <%--<input type="text" value="create Account" class="button" onclick="next();" />--%>
    <%--<input type="text" value="Test Registration" class="button" onclick="sample();" />--%>
</form>
</body>
</html>