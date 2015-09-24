/*
 * Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.provider.openid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.OpenIDException;
import org.openid4java.association.Association;
import org.openid4java.association.AssociationException;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.DirectError;
import org.openid4java.message.Message;
import org.openid4java.message.ParameterList;
import org.openid4java.message.VerifyRequest;
import org.openid4java.message.VerifyResponse;
import org.openid4java.server.ServerAssociationStore;
import org.openid4java.server.ServerException;
import org.openid4java.server.ServerManager;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.util.IdentityUtil;

/**
 * This class passes OpenID messages such as OpenID Association, OpenID Request
 * and Response messages to the ServerManager class of the openid4java library.
 * However the purpose of this class is to force the super class to use the
 * custom AssociationStore instead the default InMemoryAssociationStores.
 *
 * @author WSO2 Inc.
 */
public class OpenIDServerManager extends ServerManager {

    private static final Log log = LogFactory.getLog(OpenIDServerManager.class);

    //When an association is generated, it would be set as a thread local variable.
    private static ThreadLocal<Association> threadLocalAssociation = new ThreadLocal<Association> ();

    /**
     * Here we set our AssociationStore implementation to the parent.
     *
     */
    public OpenIDServerManager() {

        ServerAssociationStore sharedAssociations = new OpenIDServerAssociationStore(OpenIDServerConstants
                .ASSOCIATION_STORE_TYPE_SHARED);
        super.setSharedAssociations(sharedAssociations);

        ServerAssociationStore privateAssociations = null;
        synchronized (Runtime.getRuntime().getClass()){
            String privateAssociationStoreClassName = IdentityUtil.getProperty(IdentityConstants.ServerConfig.OPENID_PRIVATE_ASSOCIATION_STORE_CLASS);
            if(privateAssociationStoreClassName != null && !privateAssociationStoreClassName.trim().isEmpty()) {
                privateAssociationStoreClassName = privateAssociationStoreClassName.trim();
                if(log.isDebugEnabled()) {
                    log.debug("Initialising privateAssociation Store : " + privateAssociationStoreClassName);
                }
                try {
                    privateAssociations = (ServerAssociationStore)Class.forName(privateAssociationStoreClassName)
                            .newInstance();
                    if(log.isDebugEnabled()) {
                        log.debug("Successfully initialized privateAssociation Store : "
                                +  privateAssociationStoreClassName);
                    }
                } catch (ClassNotFoundException e) {
                    log.error("Private association store class : " + privateAssociationStoreClassName + " not found", e);
                } catch (InstantiationException e) {
                    log.error("Error while initializing association store class : " + privateAssociationStoreClassName, e);
                } catch (IllegalAccessException e) {
                    log.error("Error while initializing association store class : " + privateAssociationStoreClassName, e);
                } catch (Exception e) {
                    log.error("Error while initializing private association store", e);
                }
            }

            if(privateAssociations == null) {
                privateAssociations = new OpenIDServerAssociationStore(OpenIDServerConstants.ASSOCIATION_STORE_TYPE_PRIVATE);
                if(log.isDebugEnabled()){
                    log.debug("Setting default OpenID Server Association Store: " +
                            OpenIDServerAssociationStore.class.getName());
                }
            }
        }

        super.setPrivateAssociations(privateAssociations);
    }

    @Override
    public Message authResponse(AuthRequest authReq, String userSelId, String userSelClaimed,
                                boolean authenticatedAndApproved, String opEndpoint, boolean signNow) {

        if(log.isDebugEnabled()) {
            log.debug("Association handle in AuthRequest : " + authReq.getHandle());
        }
        return super.authResponse(authReq, userSelId, userSelClaimed, authenticatedAndApproved, opEndpoint, signNow);
    }

    public void sign(AuthSuccess authSuccess)
            throws ServerException, AssociationException {
        String handle = authSuccess.getHandle();

        Association assoc = null;
        try {
            // First try in thread local
            assoc = getThreadLocalAssociation();
        } finally {
            // Clear thread local
            clearThreadLocalAssociation();
        }

        // try shared associations, then private
        if (assoc == null) {
            assoc = getSharedAssociations().load(handle);
        }

        if (assoc == null) {
            assoc = getPrivateAssociations().load(handle);
        }

        if (assoc == null) {
            throw new ServerException("No association found for handle: " + handle);
        }

        authSuccess.setSignature(assoc.sign(authSuccess.getSignedText()));
    }

    public Message verify(ParameterList requestParams) {

        if(log.isDebugEnabled()) {
            log.debug("Processing verification request...");
        }

        boolean isVersion2 = true;

        try {
            // build request message from response params (+ ntegrity check)
            VerifyRequest vrfyReq = VerifyRequest.createVerifyRequest(requestParams);
            isVersion2 = vrfyReq.isVersion2();
            String handle = vrfyReq.getHandle();

            boolean verified = false;

            Association assoc = getPrivateAssociations().load(handle);
            String sigMod = null;
            if (assoc != null) { // verify the signature
                if (log.isDebugEnabled()) {
                    log.debug("Loaded private association; handle: " + handle);
                }
                sigMod = vrfyReq.getSignature().replaceAll("\\s", "+");
                verified = assoc.verifySignature(vrfyReq.getSignedText(), sigMod);

                // remove the association so that the request
                // cannot be verified more than once
                getPrivateAssociations().remove(handle);
            } else {
                log.error("No association loaded from the database; handle: " + handle);
            }

            VerifyResponse vrfyResp =
                    VerifyResponse.createVerifyResponse(!vrfyReq.isVersion2());

            vrfyResp.setSignatureVerified(verified);

            if (verified) {
                String invalidateHandle = vrfyReq.getInvalidateHandle();
                if (invalidateHandle != null &&
                        getSharedAssociations().load(invalidateHandle) == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Shared association invalidated; handle: " + invalidateHandle);
                    }

                    vrfyResp.setInvalidateHandle(invalidateHandle);
                }
            } else {
                log.error("Signature verification failed. handle : " + handle +
                        " , signed text : " + vrfyReq.getSignedText() +
                        " , signature : " + sigMod);
            }

            if (log.isDebugEnabled()) {
                log.debug("Responding with " + (verified ? "positive" : "negative") + " verification response");
            }

            return vrfyResp;
        } catch (OpenIDException e) {
            log.error("Error processing verification request; responding with verification error", e);
            return DirectError.createDirectError(e, !isVersion2);
        }
    }

    static Association getThreadLocalAssociation() {
        Association association = threadLocalAssociation.get();
        threadLocalAssociation.remove();
        return association;
    }

    static void setThreadLocalAssociation(Association association) {
        threadLocalAssociation.set(association);
    }

    static void clearThreadLocalAssociation(){
        threadLocalAssociation.remove();
    }
}

