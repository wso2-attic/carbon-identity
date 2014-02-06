/*
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.relyingparty.ui.client;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;
import org.apache.rampart.RampartMessageData;
import org.wso2.carbon.identity.base.IdentityBaseUtil;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.relyingparty.stub.RelyingPartyServiceStub;
import org.wso2.carbon.identity.relyingparty.stub.dto.ClaimDTO;
import org.wso2.carbon.identity.relyingparty.stub.dto.OpenIDAuthInfoDTO;
import org.wso2.carbon.identity.relyingparty.stub.dto.OpenIDDTO;
import org.wso2.carbon.identity.relyingparty.stub.dto.OpenIDSignInDTO;
import org.wso2.carbon.identity.relyingparty.ui.openid.OpenIDAuthenticationRequest;

import com.google.step2.Step2;

public class RelyingPartyServiceClient {

    private RelyingPartyServiceStub stub;
    private static final Log log = LogFactory.getLog(RelyingPartyServiceClient.class);

    /**
     * Instantiates RelyingPartyServiceClient
     * 
     * @param cookie
     *            For session management
     * @param backendServerURL
     *            URL of the back end server where UserRegistrationAdminService
     *            is running.
     * @param configCtx
     *            ConfigurationContext
     * @throws org.apache.axis2.AxisFault
     *             if error occurs when instantiating the stub
     */
    public RelyingPartyServiceClient(String cookie, String backendServerURL,
            ConfigurationContext configCtx) throws AxisFault {
        String serviceURL = backendServerURL + "RelyingPartyService";

        try {

            stub = new RelyingPartyServiceStub(configCtx, serviceURL);
            ServiceClient client = stub._getServiceClient();
            // Engage rampart as we are going to sign requests to Relying Party
            // Service
            client.engageModule("rampart");
            // Get a RampartConfig with default crypto information
            Policy rampartConfig = IdentityBaseUtil.getDefaultRampartConfig();
            Policy signOnly = IdentityBaseUtil.getSignOnlyPolicy();
            Policy mergedPolicy = signOnly.merge(rampartConfig);
            // Attach the RampartConfig policy to the client, rest of the
            // security policy is extracted from the WSDL and
            // and included in the RelyingPartyServiceStub
            Options option = client.getOptions();
            option.setProperty(RampartMessageData.KEY_RAMPART_POLICY, mergedPolicy);
            option.setManageSession(true);
            if (cookie != null) {
                option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING,
                        cookie);
            }
        } catch (Exception e) {
            handleException("Error initializing Relying Party Client", e);
        }
    }

   public String getCookie() {
        return (String) stub._getServiceClient().getServiceContext()
                .getProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING);
    }

    /**
     * @return
     * @throws AxisFault
     */
    public OpenIDAuthenticationRequest getOpenIDAuthInfo(HttpServletRequest request,
            HttpServletResponse response, String dialect) throws AxisFault {
        OpenIDAuthInfoDTO dto = null;
        OpenIDAuthenticationRequest authRequest = null;

        try {
            if(dialect == null) {
                dialect = IdentityConstants.OPENID_SREG_DIALECT;
            } 
            
            dto = stub.getOpenIDAuthInfo(dialect);
            authRequest = new OpenIDAuthenticationRequest(request, response);
            if (dto != null) {
                for (String requestType : dto.getRequestTypes()) {
                    authRequest.addRequestType(requestType);
                }
                for (String optionalClaim : dto.getOptionalClaims()) {
                    authRequest.addOptionalClaims(optionalClaim);
                }
                for (String reqClaim : dto.getRequiredClaims()) {
                    authRequest.addRequiredClaims(reqClaim);
                }
                authRequest.setRealm(dto.getRealm());
                authRequest.setRequestClaimsFromIdP(dto.getRequestClaimsFromIdP());
            }
        } catch (Exception e) {
            handleException(" Error while retrieving OpenID login info. " + e.getMessage(), e);
        }
        return authRequest;
    }

    /**
     * @param openID
     * @return
     * @throws AxisFault
     */
    public OpenIDSignInDTO signInWithOpenID(OpenIDDTO openID) throws AxisFault {
        try {
            return stub.signInWithOpenID(openID);
        } catch (Exception e) {
            handleException("Error while signing in. " + e.getMessage(), e);
        }
        return null;
    }

    public void signInGAppUser(HttpServletRequest request, HttpServletResponse response,
                               OpenIDDTO openId,
                               String gappDomainName) throws Exception {
        try {
            ClaimDTO[] claims = openId.getClaims();
            String username = null;
            for (ClaimDTO claim : claims) {
                if (claim.getClaimUri().equals(Step2.AxSchema.EMAIL.getUri())) {
                    username = claim.getClaimValue();
                }
            }
            ServiceClient client = stub._getServiceClient();
            Options options = client.getOptions();
            options.setManageSession(true);

            stub.signInGAppUser(openId, gappDomainName);
            request.setAttribute("gapp.openid.username", username);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception(e.getMessage(), e);
        }
    }

    /**
     * @param dto
     * @return
     * @throws AxisFault
     */
    public boolean addOpenIDToProfile(OpenIDDTO dto) throws AxisFault {
        try {
            return stub.addOpenIdToProfile(dto);
        } catch (Exception e) {
            handleException(
                    "Error occured when trying to associate the OpenId with the user profile.", e);
        }
        return false;
    }


    /**
     * Logs and wraps the given exception.
     * 
     * @param msg
     *            Error message
     * @param e
     *            Exception
     * @throws AxisFault
     *             which wraps the error
     */
    private void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }
}
