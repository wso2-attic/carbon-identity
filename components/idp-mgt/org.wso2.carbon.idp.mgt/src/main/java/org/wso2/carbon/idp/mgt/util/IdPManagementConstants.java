/*
 *Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.carbon.idp.mgt.util;

public class IdPManagementConstants {

    public static class SQLQueries {

        public static final String GET_RESIDENT_IDP_SQL = "SELECT SP_IDP_HOME_REALM_ID, SP_IDP_OPENID_REALM, "
                + "SP_IDP_ENTITY_ID, SP_IDP_PASSIVE_STS_REALM FROM SP_IDP_RESIDENT WHERE SP_TENANT_ID=?";

        public static final String ADD_RESIDENT_IDP_SQL = "INSERT INTO SP_IDP_RESIDENT (SP_IDP_HOME_REALM_ID, "
                + "SP_IDP_OPENID_REALM, SP_IDP_ENTITY_ID, SP_IDP_PASSIVE_STS_REALM, SP_TENANT_ID) VALUES (?, ?, ?, ?, ?)";

        public static final String UPDATE_RESIDENT_IDP_SQL = "UPDATE SP_IDP_RESIDENT SET SP_IDP_HOME_REALM_ID=?, "
                + "SP_IDP_OPENID_REALM=?, SP_IDP_ENTITY_ID=?, SP_IDP_PASSIVE_STS_REALM=? WHERE SP_TENANT_ID=?";

        public static final String GET_IDPS_SQL = "SELECT SP_IDP_NAME, SP_IDP_PRIMARY, SP_IDP_HOME_REALM_ID "
                + "FROM SP_IDP WHERE SP_TENANT_ID=?";

        public static final String GET_IDP_BY_NAME_SQL = "SELECT SP_IDP_ID, SP_IDP_PRIMARY, SP_IDP_HOME_REALM_ID, "
                + "SP_IDP_THUMBPRINT, SP_IDP_TOKEN_EP_ALIAS, SP_IDP_INBOUND_PROVISIONING_ENABLED, SP_IDP_INBOUND_PROVISIONING_USER_STORE_ID, "
                + "SP_IDP_USER_CLAIM_URI, SP_IDP_ROLE_CLAIM_URI,SP_IDP_DEFAULT_AUTHENTICATOR_NAME,SP_IDP_DEFAULT_PRO_CONNECTOR_NAME FROM SP_IDP WHERE SP_TENANT_ID=? AND SP_IDP_NAME=?";

        public static final String GET_IDP_ID_BY_NAME_SQL = "SELECT SP_IDP_ID "
                + "FROM SP_IDP WHERE SP_TENANT_ID=? AND SP_IDP_NAME=?";

        public static final String GET_IDP_OPENID_SQL = "SELECT SP_IDP_OPENID_ENABLED, "
                + "SP_IDP_OPENID_URL,SP_IDP_OPENID_UID_IN_CLAIMS FROM SP_IDP_OPENID WHERE SP_IDP_ID=?";

        public static final String GET_IDP_SAMLSSO_SQL = "SELECT SP_IDP_SAMLSSO_ENABLED, "
                + "SP_IDP_IDP_ENTITY_ID, SP_IDP_SP_ENTITY_ID, SP_IDP_SSO_URL, "
                + "SP_IDP_AUTHN_REQ_SIGNED, SP_IDP_LOGOUT_ENABLED, SP_IDP_LOGOUT_URL, SP_IDP_LOGOUT_REQ_SIGNED, "
                + "SP_IDP_AUTHN_RES_SIGNED, SP_IDP_SAMLSSO_UID_IN_CLAIMS FROM SP_IDP_SAMLSSO WHERE SP_IDP_ID=?";

        public static final String GET_IDP_OIDC_SQL = "SELECT SP_IDP_OIDC_ENABLED,"
                + "SP_IDP_CONSUMER_KEY, SP_IDP_CONSUMER_SECRET, SP_IDP_AUTHZ_URL, SP_IDP_TOKEN_URL, "
                + "SP_IDP_IS_OIDC_UID_IN_CLAIMS FROM SP_IDP_OIDC WHERE SP_IDP_ID=?";

        public static final String GET_IDP_FB_AUTH_SQL = "SELECT SP_IDP_FB_AUTH_ENABLED,"
                + "SP_IDP_FB_CLIENT_ID, SP_IDP_FB_CLIENT_SECRET, SP_IDP_IS_FB_UID_IN_CLAIMS FROM SP_IDP_FB_AUTH WHERE SP_IDP_ID=?";

        public static final String GET_IDP_PASSIVE_STS_SQL = "SELECT SP_IDP_PASSIVE_STS_ENABLED, "
                + "SP_IDP_PASSIVE_STS_REALM, SP_IDP_PASSIVE_STS_URL, "
                + "SP_IDP_PSVE_STS_UID_IN_CLAIMS FROM SP_IDP_PASSIVE_STS WHERE SP_IDP_ID=?";

        public static final String GET_IDP_PROVISIONING_CONFIGS_SQL = "SELECT SP_IDP_PROV_CONNECTOR_TYPE, "
                + "SP_IDP_PROV_CONFIG_KEY, SP_IDP_PROV_CONFIG_VALUE, "
                + "SP_IDP_PROV_CONFIG_IS_SECRET FROM SP_IDP_PROVISIONING_CONFIGS WHERE SP_IDP_ID=?";

        public static final String DELETE_PROVISIONING_CONNECTORS = "DELETE FROM SP_IDP_PROVISIONING_CONFIGS WHERE SP_IDP_ID=?";

        public static final String GET_IDP_NAME_BY_REALM_ID_SQL = "DELETE SP_IDP_NAME WHERE SP_TENANT_ID=? AND SP_IDP_HOME_REALM_ID=?";

        public static final String GET_IDP_CLAIM_MAPPINGS_SQL = "SELECT SP_IDP_CLAIMS.SP_IDP_CLAIM, SP_IDP_CLAIM_MAPPINGS.SP_LOCAL_CLAIM "
                + "FROM SP_IDP_CLAIM_MAPPINGS INNER JOIN SP_IDP_CLAIMS ON SP_IDP_CLAIM_MAPPINGS.SP_IDP_CLAIM_ID= SP_IDP_CLAIMS.SP_IDP_CLAIM_ID WHERE SP_IDP_CLAIMS.SP_IDP_ID=?";

        public static final String GET_IDP_ROLE_MAPPINGS_SQL = "SELECT SP_IDP_ROLE_MAPPINGS.SP_USER_STORE_ID, SP_IDP_ROLE_MAPPINGS.SP_LOCAL_ROLE, SP_IDP_ROLES.SP_IDP_ROLE "
                + "FROM SP_IDP_ROLE_MAPPINGS  INNER JOIN SP_IDP_ROLES ON SP_IDP_ROLE_MAPPINGS.SP_IDP_ROLE_ID=SP_IDP_ROLES.SP_IDP_ROLE_ID "
                + "WHERE SP_IDP_ROLES.SP_IDP_ID=?";

        public static final String UPDATE_IDP_SQL = "UPDATE SP_IDP SET SP_IDP_NAME=?, SP_IDP_PRIMARY=?, "
                + "SP_IDP_HOME_REALM_ID=?, SP_IDP_THUMBPRINT=?, SP_IDP_TOKEN_EP_ALIAS=?, SP_IDP_INBOUND_PROVISIONING_ENABLED=?, "
                + "SP_IDP_INBOUND_PROVISIONING_USER_STORE_ID=?,SP_IDP_USER_CLAIM_URI=?, SP_IDP_ROLE_CLAIM_URI=?, SP_IDP_DEFAULT_AUTHENTICATOR_NAME=?,SP_IDP_DEFAULT_PRO_CONNECTOR_NAME=? WHERE SP_TENANT_ID=? AND SP_IDP_NAME=?";

        public static final String UPDATE_IDP_OPENID_SQL = "UPDATE SP_IDP_OPENID SET SP_IDP_OPENID_ENABLED=?, "
                + "SP_IDP_OPENID_URL=?,SP_IDP_OPENID_UID_IN_CLAIMS=? WHERE SP_IDP_ID=?";

        public static final String UPDATE_IDP_SAMLSSO_SQL = "UPDATE SP_IDP_SAMLSSO SET SP_IDP_SAMLSSO_ENABLED=?, "
                + "SP_IDP_IDP_ENTITY_ID=?, SP_IDP_SP_ENTITY_ID=?, SP_IDP_SSO_URL=?, "
                + "SP_IDP_AUTHN_REQ_SIGNED=?, SP_IDP_LOGOUT_ENABLED=?, SP_IDP_LOGOUT_URL=?, SP_IDP_LOGOUT_REQ_SIGNED=?, "
                + "SP_IDP_AUTHN_RES_SIGNED=?, SP_IDP_SAMLSSO_UID_IN_CLAIMS=? WHERE SP_IDP_ID=?";

        public static final String UPDATE_IDP_OIDC_SQL = "UPDATE SP_IDP_OIDC SET SP_IDP_OIDC_ENABLED=?, "
                + "SP_IDP_CONSUMER_KEY=?, SP_IDP_CONSUMER_SECRET=?, SP_IDP_AUTHZ_URL=?, "
                + "SP_IDP_TOKEN_URL=?, SP_IDP_IS_OIDC_UID_IN_CLAIMS=? WHERE SP_IDP_ID=?";

        public static final String UPDATE_IDP_FB_AUTH_SQL = "UPDATE SP_IDP_FB_AUTH SET SP_IDP_FB_AUTH_ENABLED=?, "
                + "SP_IDP_FB_CLIENT_ID=?, SP_IDP_FB_CLIENT_SECRET=?, "
                + "SP_IDP_IS_FB_UID_IN_CLAIMS=? WHERE SP_IDP_ID=?";

        public static final String UPDATE_IDP_PASSIVE_STS_SQL = "UPDATE SP_IDP_PASSIVE_STS SET "
                + "SP_IDP_PASSIVE_STS_ENABLED=?,SP_IDP_PASSIVE_STS_REALM=?, "
                + "SP_IDP_PASSIVE_STS_URL=?, SP_IDP_PSVE_STS_UID_IN_CLAIMS=? WHERE SP_IDP_ID=?";

        public static final String ADD_IDP_CLAIMS_SQL = "INSERT INTO SP_IDP_CLAIMS ( SP_IDP_ID, SP_IDP_CLAIM ) "
                + "VALUES (?, ?)";

        public static final String DELETE_IDP_CLAIMS_SQL = "DELETE FROM SP_IDP_CLAIMS "
                + "WHERE (SP_IDP_ID=? AND SP_IDP_CLAIM=?)";

        public static final String UPDATE_IDP_CLAIMS_SQL = "UPDATE SP_IDP_CLAIMS SET SP_IDP_CLAIM=?, "
                + "WHERE (SP_IDP_ID=? AND SP_IDP_CLAIM=?)";

        public static final String GET_IDP_CLAIMS_SQL = "SELECT SP_IDP_CLAIM_ID, SP_IDP_CLAIM FROM SP_IDP_CLAIMS WHERE SP_IDP_ID=?";

        public static final String GET_USER_ROLE_CLAIMS_SQL = "SELECT SP_IDP_USER_CLAIM_URI, SP_IDP_ROLE_CLAIM_URI FROM SP_IDP WHERE SP_IDP_ID=?";

        public static final String DELETE_IDP_CLAIM_MAPPINGS_SQL = "DELETE FROM SP_IDP_CLAIM_MAPPINGS "
                + "WHERE (SP_IDP_CLAIM_ID=? AND SP_TENANT_ID=? AND SP_LOCAL_CLAIM=?)";

        public static final String ADD_IDP_CLAIM_MAPPINGS_SQL = "INSERT INTO SP_IDP_CLAIM_MAPPINGS "
                + "(SP_IDP_CLAIM_ID, SP_TENANT_ID, SP_LOCAL_CLAIM) VALUES (?, ?, ?)";

        public static final String ADD_IDP_ROLES_SQL = "INSERT INTO SP_IDP_ROLES (SP_IDP_ID, SP_IDP_ROLE) "
                + "VALUES (?, ?)";

        public static final String DELETE_IDP_ROLES_SQL = "DELETE FROM SP_IDP_ROLES "
                + "WHERE (SP_IDP_ID=? AND SP_IDP_ROLE=?)";

        public static final String UPDATE_IDP_ROLES_SQL = "UPDATE SP_IDP_ROLES SET SP_IDP_ROLE=? "
                + "WHERE (SP_IDP_ID=? AND SP_IDP_ROLE=?)";

        public static final String GET_IDP_ROLES_SQL = "SELECT SP_IDP_ROLE_ID, SP_IDP_ROLE  FROM SP_IDP_ROLES "
                + "WHERE SP_IDP_ID=?";

        public static final String DELETE_IDP_ROLE_MAPPINGS_SQL = "DELETE FROM SP_IDP_ROLE_MAPPINGS "
                + "WHERE (SP_IDP_ROLE_ID=? AND SP_TENANT_ID=? AND SP_USER_STORE_ID = ? AND SP_LOCAL_ROLE=?)";

        public static final String ADD_IDP_ROLE_MAPPINGS_SQL = "INSERT INTO SP_IDP_ROLE_MAPPINGS "
                + "(SP_IDP_ROLE_ID, SP_TENANT_ID, SP_USER_STORE_ID, SP_LOCAL_ROLE) VALUES (?, ?, ?, ?)";

        public static final String ADD_IDP_SQL = "INSERT INTO SP_IDP (SP_TENANT_ID, SP_IDP_NAME, SP_IDP_PRIMARY, "
                + "SP_IDP_HOME_REALM_ID, SP_IDP_THUMBPRINT, SP_IDP_TOKEN_EP_ALIAS, SP_IDP_INBOUND_PROVISIONING_ENABLED, "
                + "SP_IDP_INBOUND_PROVISIONING_USER_STORE_ID, SP_IDP_USER_CLAIM_URI, SP_IDP_ROLE_CLAIM_URI,SP_IDP_DEFAULT_AUTHENTICATOR_NAME,SP_IDP_DEFAULT_PRO_CONNECTOR_NAME) VALUES (?, ?, ?,?,?, ?, ?, ?, ?, ?,?,?)";

        public static final String ADD_IDP_OPENID_SQL = "INSERT INTO SP_IDP_OPENID (SP_IDP_ID, SP_IDP_OPENID_ENABLED, "
                + "SP_IDP_OPENID_URL,SP_IDP_OPENID_UID_IN_CLAIMS) " + "VALUES (?, ?, ?,?)";

        public static final String ADD_IDP_SAMLSSO_SQL = "INSERT INTO SP_IDP_SAMLSSO (SP_IDP_ID, SP_IDP_SAMLSSO_ENABLED, "
                + "SP_IDP_IDP_ENTITY_ID, SP_IDP_SP_ENTITY_ID, SP_IDP_SSO_URL, "
                + "SP_IDP_AUTHN_REQ_SIGNED, SP_IDP_LOGOUT_ENABLED, SP_IDP_LOGOUT_URL, SP_IDP_LOGOUT_REQ_SIGNED, "
                + "SP_IDP_AUTHN_RES_SIGNED, SP_IDP_SAMLSSO_UID_IN_CLAIMS) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        public static final String ADD_IDP_OIDC_SQL = "INSERT INTO SP_IDP_OIDC (SP_IDP_ID, SP_IDP_OIDC_ENABLED, "
                + "SP_IDP_CONSUMER_KEY, SP_IDP_CONSUMER_SECRET, SP_IDP_AUTHZ_URL, "
                + "SP_IDP_TOKEN_URL, SP_IDP_IS_OIDC_UID_IN_CLAIMS) VALUES (?, ?, ?, ?, ?, ?, ?)";

        public static final String ADD_IDP_FB_AUTH_SQL = "INSERT INTO SP_IDP_FB_AUTH (SP_IDP_ID, SP_IDP_FB_AUTH_ENABLED, "
                + "SP_IDP_FB_CLIENT_ID, SP_IDP_FB_CLIENT_SECRET,  SP_IDP_IS_FB_UID_IN_CLAIMS ) "
                + "VALUES (?, ?, ?, ?, ?)";

        public static final String ADD_IDP_PASSIVE_STS_SQL = "INSERT INTO SP_IDP_PASSIVE_STS (SP_IDP_ID, "
                + "SP_IDP_PASSIVE_STS_ENABLED, SP_IDP_PASSIVE_STS_REALM, "
                + "SP_IDP_PASSIVE_STS_URL, SP_IDP_PSVE_STS_UID_IN_CLAIMS) VALUES (?, ?, ?, ?, ?)";

        public static final String ADD_IDP_PROVISIONING_CONFIG_SQL = "INSERT INTO SP_IDP_PROVISIONING_CONFIGS (SP_IDP_ID, "
                + "SP_IDP_PROV_CONNECTOR_TYPE, SP_IDP_PROV_CONFIG_KEY, SP_IDP_PROV_CONFIG_VALUE, "
                + "SP_IDP_PROV_CONFIG_IS_SECRET) VALUES (?, ?, ?, ?, ?)";

        public static final String UPDATE_IDP_PROVISIONING_CONFIG_SQL = "UPDATE SP_IDP_PROVISIONING_CONFIGS SET "
                + "SP_IDP_PROV_CONNECTOR_TYPE=?, SP_IDP_PROV_CONFIG_KEY=? SP_IDP_PROV_CONFIG_VALUE=?, "
                + "SP_IDP_PROV_CONFIG_IS_SECRET = ? WHERE SP_IDP_ID=?";

        public static final String DELETE_IDP_SQL = "DELETE FROM SP_IDP WHERE (SP_TENANT_ID=? AND SP_IDP_NAME=?)";

        public static final String GET_IDP_ROW_ID_SQL = "SELECT SP_IDP_ID FROM SP_IDP "
                + "WHERE (SP_TENANT_ID=? AND SP_IDP_NAME=?)";

        public static final String GET_PRIMARY_IDP_SQL = "SELECT SP_IDP_NAME, SP_IDP_HOME_REALM_ID "
                + "FROM SP_IDP WHERE (SP_TENANT_ID=? AND SP_IDP_PRIMARY=?)";

        public static final String SWITCH_IDP_PRIMARY_SQL = "UPDATE SP_IDP SET SP_IDP_PRIMARY=? "
                + "WHERE (SP_TENANT_ID=? AND SP_IDP_PRIMARY=?)";

        public static final String SWITCH_IDP_PRIMARY_ON_DELETE_SQL = "UPDATE SP_IDP SET SP_IDP_PRIMARY=? "
                + "WHERE (SP_TENANT_ID=? AND SP_IDP_NAME=? AND SP_IDP_PRIMARY=?)";

        public static final String DELETE_ROLE_SQL = "DELETE FROM SP_IDP_ROLES "
                + "WHERE SP_IDP_ID=?";

        public static final String RENAME_ROLE_SQL = "UPDATE SP_IDP_ROLE_MAPPINGS SET SP_LOCAL_ROLE=? "
                + "WHERE (SP_TENANT_ID=? AND SP_LOCAL_ROLE=?)";

        public static final String DELETE_CLAIM_SQL = "DELETE FROM SP_IDP_CLAIMS "
                + "WHERE SP_IDP_ID=?";

        public static final String RENAME_CLAIM_SQL = "UPDATE SP_IDP_CLAIM_MAPPINGS SET SP_LOCAL_CLAIM=? "
                + "WHERE (SP_TENANT_ID=? AND SP_LOCAL_CLAIM=?)";
    }
}
