/*
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.totp;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import com.warrenstrange.googleauth.KeyRepresentation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.totp.internal.TOTPManagerComponent;
import org.wso2.carbon.identity.totp.exception.TOTPException;
import org.wso2.carbon.identity.totp.util.TOTPUtil;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

public class TOTPKeyGenerator {

    private static Log log = LogFactory.getLog(TOTPKeyGenerator.class);
    private static volatile TOTPKeyGenerator instance;
    
    private TOTPKeyGenerator(){};
    
    public static TOTPKeyGenerator getInstance(){
        
        if(instance==null){
            synchronized (TOTPKeyGenerator.class){
                if(instance==null){
                    instance = new TOTPKeyGenerator();
                }
            }
        }
        return instance;
    }
    
    public TOTPDTO generateTOTPKeyLocal(String username) throws TOTPException {
        //check for user store domain
        String secretkey = null;
        String qrCodeURL = null;
        GoogleAuthenticatorKey key = generateKey();
        secretkey = key.getKey();
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        qrCodeURL = GoogleAuthenticatorQRGenerator.getOtpAuthURL(tenantDomain, username, key);

        try {
            int tenantId = IdentityUtil.getTenantIdOFUser(username);
            UserRealm userRealm = TOTPManagerComponent.getRealmService().getTenantUserRealm(tenantId);

            if (userRealm != null) {
                UserStoreManager userStoreManager = userRealm.getUserStoreManager();
                userStoreManager.setUserClaimValue(MultitenantUtils.getTenantAwareUsername(username), Constants.SECRET_KEY,secretkey,null);
                userStoreManager.setUserClaimValue(MultitenantUtils.getTenantAwareUsername(username),Constants.QR_CODE_URL,qrCodeURL,null);
                String encoding = TOTPUtil.getEncodingMethod();
                userStoreManager.setUserClaimValue(MultitenantUtils.getTenantAwareUsername(username),Constants.Encoding,encoding,null);
            }else{
                log.error("Cannot find the user realm for the given tenant: " + tenantId);
                throw new TOTPException("Cannot find the user realm for the given tenant: " + tenantId);
            }
        } catch (IdentityException e) {
            log.error("TOTPKeyGenerator failed while trying to get the tenant ID of the use", e);
            throw new TOTPException(e.getMessage(),e);
        } catch (UserStoreException e) {
            log.error("TOTPKeyGenerator failed while trying to access userRealm",e);
            throw new TOTPException(e.getMessage(),e);
        } catch (IdentityApplicationManagementException e) {
            log.error("Error when getting the encoding method",e);
            throw new TOTPException(e.getMessage(),e);
        }
        return new TOTPDTO(secretkey,qrCodeURL);
    }
    
    public boolean resetLocal(String username) throws TOTPException {

        try {
            
            int tenantId = IdentityUtil.getTenantIdOFUser(username);
            UserRealm userRealm = TOTPManagerComponent.getRealmService().getTenantUserRealm(tenantId);
            if (userRealm != null) {
                
                UserStoreManager userStoreManager = userRealm.getUserStoreManager();
                userStoreManager.setUserClaimValue(MultitenantUtils.getTenantAwareUsername(username), Constants.SECRET_KEY, "", null);
                userStoreManager.setUserClaimValue(MultitenantUtils.getTenantAwareUsername(username),Constants.QR_CODE_URL,"",null);
                userStoreManager.setUserClaimValue(MultitenantUtils.getTenantAwareUsername(username),Constants.Encoding,"",null);
                return true;
            }else{
                log.error("Cannot find the user realm for the given tenant: " + tenantId);
                throw new TOTPException("Cannot find the user realm for the given tenant: " + tenantId);
            }
            
        } catch (UserStoreException e) {
            log.error("totp resetlocal failed while trying to access userRealm", e);
            throw new TOTPException(e.getMessage(),e);
        } catch (IdentityException e) {
            log.error("totp resetlocal failed while trying to get the tenant ID of the use", e);
            throw new TOTPException(e.getMessage(),e);
        }
    }

    public TOTPDTO generateTOTPKey(String username){

        String secretkey = null;
        String qrCodeURL = null;
        GoogleAuthenticatorKey key = generateKey();
        secretkey = key.getKey();
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        qrCodeURL = GoogleAuthenticatorQRGenerator.getOtpAuthURL(tenantDomain, username, key);
        return new TOTPDTO(secretkey,qrCodeURL);
    }
    
    private GoogleAuthenticatorKey generateKey(){
        KeyRepresentation encoding = KeyRepresentation.BASE32;
        try {
            if("Base64".equals(TOTPUtil.getEncodingMethod())){
                encoding = KeyRepresentation.BASE64;
            }
        } catch (IdentityApplicationManagementException e) {
            log.error("Error when reading the tenant encoding method");
        }

        GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder gacb = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setKeyRepresentation(encoding);
        GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator(gacb.build());
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key;
    }
}
