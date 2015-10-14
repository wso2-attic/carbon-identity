<%@ page
        import="org.wso2.carbon.identity.application.authentication.endpoint.util.UserRegistrationAdminServiceClient" %>
<!--
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
-->
<%

  UserRegistrationAdminServiceClient registrationClient = new UserRegistrationAdminServiceClient();

  String userName = request.getParameter("reg-username");
  String firstName = request.getParameter("reg-username");
  String lastName = request.getParameter("reg-username");
  String password = request.getParameter("reg-username");
  String email = request.getParameter("reg-username");

  registrationClient.addUser();

%>
