/*
 *
 * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * /
 */

package org.wso2.carbon.identity.uma;

public final class UMAConstants {

    public static final String UMA_AUTHORIZATION_API_SCOPE = "uma_authorization";
    public static final String UMA_PROTECTION_API_SCOPE = "uma_protection";

    public static final String UMA_PERMISSION_TICKET = "ticket";
    public static final String UMA_RPT = "rpt";

    public static final String UMA_ERROR = "error";
    public static final String UMA_ERROR_DETAILS = "error_details";
    public static final String UMA_ERROR_DESCRIPTION = "error_description";
    public static final String UMA_ERROR_URI = "error_uri";


    public static final String AUTHENTICAION_CONTEXT = "authentication_context";
    public static final String REQUIRED_AUTHENTICAION_CONTEXT_REFERENCE = "required_acr";


    public static final String REQUESTING_PARTY_CLAIMS = "requesting_party_claims";
    public static final String REQUIRED_CLAIMS = "required_claims";
    public static final String REDIRECT_USER = "redirect_user";


    // constants related to claims gathering flows
    public static final String CLAIM_TOKEN_FORMAT = "format";
    public static final String CLAIM_TOKEN = "token";

    // claim gathering via redirect
    public static final String CLIENT_ID = "client_id";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String STATE = "state";

    public static final String AUTHORIZATION_STATE = "authorization_state";


    public static class UmaErrors{

        public static final String INVALID_TICKET = "invalid_ticket";
        public static final String EXPIRED_TICKET = "expired_ticket";
        public static final String NOT_AUTHORIZED = "not_authorized";
        public static final String NEED_INFO = "need_info";
        public static final String REQUEST_SUBMITTED = "request_submitted";
        public static final String INVALID_REQUEST = "invalid_request";
        public static final String SERVER_ERROR = "server_error";
        public static final String TEMPORARILY_UNAVAILABLE = "temporarily_unavailable";


        // The client is free to return to the RPT endpoint to seek authorization data once again.
        public static final String CLAIMS_SUBMITTED = "claims_submitted";

        private UmaErrors(){}
    }


    public static class UMARequiredClaimsAttributes{
        public static final String CLAIM_NAME = "name";
        public static final String CLAIM_FRIENDLY_NAME = "friendly_name";
        public static final String CLAIM_TYPE = "claim_type";
        public static final String CLAIM_TOKEN_FORMAT = "claim_token_format";
        public static final String CLAIM_ISSUER = "issuer";

        private UMARequiredClaimsAttributes(){}
    }


    // UMA 1.0 endpoints
    public static class UMAEndpoints {
        public static final String UMA_AUTHORIZATION_URL = "/rpt";
        public static final String UMA_PROTECTION_URL = "/protect";

        private UMAEndpoints(){

        }
    }


    public static class UMAErrorCodes{
        public static final int UMA_INVALID_REQUEST_CODE = 400;
        public static final int UMA_EXPIRED_TICKET_CODE = 400;
        public static final int UMA_NOT_AUTHORIZED_CODE = 403;
        public static final int UMA_NEED_INFO_CODE = 403;

        private UMAErrorCodes(){}
    }

    // Resource Set Registation API Constants
    public class OAuthResourceSetRegistration {

        public static final String RESOURCE_SET_NAME = "name";
        public static final String RESOURCE_SET_TYPE = "type";
        public static final String RESOURCE_SET_URI = "uri";
        public static final String RESOURCE_SET_ICON_URI = "icon_uri";
        public static final String RESOURCE_SET_SCOPES = "scopes";
        public static final String RESOURCE_SET_ID = "_id";

        public static final String RESOURCE_REG_RESPONSE_POLICY_URI = "user_access_policy_uri";
        public static final String ERR_RESOURCE_SET_NOT_FOUND = "not_found";

        private OAuthResourceSetRegistration(){}
    }


    // OAuth introspection API constants
    public static class OAuthIntrospectConstants {

        public static final String TOKEN = "token";
        public static final String TOKEN_TYPE_HINT = "token_type_hint";


        public static final String RESP_FIELD_ACTIVE = "active";
        public static final String RESP_FIELD_SCOPE = "scope";
        public static final String RESP_FIELD_CLIENT_ID = "client_id";
        public static final String RESP_FIELD_USER_ID = "user_id";
        public static final String RESP_FIELD_TOKEN_TYPE = "token_type";

        public static final String RESP_FIELD_EXP = "exp";
        public static final String RESP_FIELD_IAT = "iat";
        public static final String RESP_FIELD_NBF = "nbf";
        public static final String RESP_FIELD_SUB = "sub";
        public static final String RESP_FIELD_AUD = "aud";
        public static final String RESP_FIELD_ISS = "iss";
        public static final String RESP_FIELD_JTI = "jti";

        private OAuthIntrospectConstants(){}
    }



    private UMAConstants(){
    }

}
