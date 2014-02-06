/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/


package org.wso2.carbon.identity.sso.saml.tomcat.agent;

import javax.servlet.FilterConfig;

public class SSOConfigs {

	private static String ssoLoginPage;
	private static String homePage;
	private static String logoutPage;
	private static String issuerId;
	private static String idpUrl;
	private static String attributeIndex;
	private static String consumerUrl;
	private static String subjectNameAttributeId;
    private static String trustStore;
    private static String trustStorePassword;
    private static String idPCertAlias;
    private static String errorPage;
    private static int serverVersion = 4;


	public static void initConfigs(FilterConfig fConfigs){
		ssoLoginPage = fConfigs.getInitParameter("SSOLoginPage");
		homePage = fConfigs.getInitParameter("HomePage");
		errorPage = fConfigs.getInitParameter("ErrorPage");
		logoutPage = fConfigs.getInitParameter("LogoutPage");
		consumerUrl = fConfigs.getInitParameter("ConsumerUrl");
		idpUrl = fConfigs.getInitParameter("IdpUrl");
		issuerId = fConfigs.getInitParameter("Issuer");
		attributeIndex = fConfigs.getInitParameter("AttributeConsumingServiceIndex");
		subjectNameAttributeId = fConfigs.getInitParameter("SubjectNameAttributeId");
        trustStore = fConfigs.getInitParameter("TrustStore");
        trustStorePassword = fConfigs.getInitParameter("TrustStorePassword");
        idPCertAlias = fConfigs.getInitParameter("IDPCertAlias");
        idPCertAlias = fConfigs.getInitParameter("IDPCertAlias");
        String serverVersionString = fConfigs.getInitParameter("ServerVersion");
        if(serverVersionString != null && serverVersionString.startsWith("3")){
            serverVersion = 3 ;
        }
	}

	public static String getSsoLoginPage() {
		return ssoLoginPage;
	}
	
	public static String getHomePage() {
		return homePage;
	}
	
	public static String getLogoutPage() {
		return logoutPage;
	}

    public static String getIssuerId() {
        return issuerId;
    }

    public static String getIdpUrl() {
        return idpUrl;
    }

    public static String getAttributeIndex() {
        return attributeIndex;
    }

    public static String getConsumerUrl() {
        return consumerUrl;
    }

    public static String getSubjectNameAttributeId() {
        if(subjectNameAttributeId == null || subjectNameAttributeId.trim().length() == 0){
            subjectNameAttributeId = "Subject";
        }
        return subjectNameAttributeId;
    }

    public static String getTrustStore() {
        return trustStore;
    }

    public static String getTrustStorePassword() {
        return trustStorePassword;
    }

    public static String getIdPCertAlias() {
        return idPCertAlias;
    }

    public static String getErrorPage() {
        return errorPage;
    }

    public static int getServerVersion() {
        return serverVersion;
    }
}
