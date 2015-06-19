/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.provisioning.connector.spml;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openspml.v2.client.Spml2Client;
import org.openspml.v2.msg.spml.AddRequest;
import org.openspml.v2.msg.spml.AddResponse;
import org.openspml.v2.msg.spml.DeleteRequest;
import org.openspml.v2.msg.spml.DeleteResponse;
import org.openspml.v2.msg.spml.Extensible;
import org.openspml.v2.msg.spml.Modification;
import org.openspml.v2.msg.spml.ModifyRequest;
import org.openspml.v2.msg.spml.ModifyResponse;
import org.openspml.v2.msg.spml.PSO;
import org.openspml.v2.msg.spml.PSOIdentifier;
import org.openspml.v2.msg.spml.ReturnData;
import org.openspml.v2.msg.spml.StatusCode;
import org.openspml.v2.profiles.dsml.DSMLAttr;
import org.openspml.v2.util.Spml2Exception;
import org.openspml.v2.util.xml.ReflectiveXMLMarshaller;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.provisioning.AbstractOutboundProvisioningConnector;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningConstants;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;
import org.wso2.carbon.identity.provisioning.ProvisionedIdentifier;
import org.wso2.carbon.identity.provisioning.ProvisioningEntity;
import org.wso2.carbon.identity.provisioning.ProvisioningEntityType;
import org.wso2.carbon.identity.provisioning.ProvisioningOperation;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;


public class SPMLProvisioningConnector extends AbstractOutboundProvisioningConnector {

    private static final long serialVersionUID = -1046148327813739881L;

    private static final Log log = LogFactory.getLog(SPMLProvisioningConnector.class);
    private SPMLProvisioningConnectorConfig configHolder;

    @Override
    public void init(Property[] provisioningProperties) throws IdentityProvisioningException {

        Properties configs = new Properties();

        if (provisioningProperties != null && provisioningProperties.length > 0) {
            for (Property property : provisioningProperties) {
                configs.put(property.getName(), property.getValue());

                if (IdentityProvisioningConstants.JIT_PROVISIONING_ENABLED.equals(property
                        .getName()) && "1".equals(property.getValue())){
                    jitProvisioningEnabled = true;
                }
            }

        }

        configHolder = new SPMLProvisioningConnectorConfig(configs);
    }

    @Override
    public ProvisionedIdentifier provision(ProvisioningEntity provisioningEntity)
            throws IdentityProvisioningException {

        String provisionedId = null;

        if (provisioningEntity.isJitProvisioning() && !isJitProvisioningEnabled()) {
            log.debug("JIT provisioning disabled for SPML connector");
            return null;
        }

        if (provisioningEntity != null) {
            if (provisioningEntity.getEntityType() == ProvisioningEntityType.USER) {
                if (provisioningEntity.getOperation() == ProvisioningOperation.DELETE) {
                    deleteUser(provisioningEntity);
                } else if (provisioningEntity.getOperation() == ProvisioningOperation.PUT) {
                    updateUser(provisioningEntity);
                } else if (provisioningEntity.getOperation() == ProvisioningOperation.POST) {
                    provisionedId = createUser(provisioningEntity);
                } else {
                    log.warn("Unsupported provisioning opertaion.");
                }
            } else {
                log.warn("Unsupported provisioning opertaion.");
            }
        }

        // creates a provisioned identifier for the provisioned user.
        ProvisionedIdentifier identifier = new ProvisionedIdentifier();
        identifier.setIdentifier(provisionedId);
        return identifier;
    }

    /**
     * @param provisioningEntity
     */
    private void updateUser(ProvisioningEntity provisioningEntity) {
        boolean isDebugEnabled = log.isDebugEnabled();
        String provisioningIdentifier = null;

        try {
            ReflectiveXMLMarshaller marshaller = new ReflectiveXMLMarshaller();
            Spml2Client spml2Client = new Spml2Client(configHolder.getValue("spml-ep"));
            spml2Client.setTrace(log.isDebugEnabled());
            spml2Client.setSOAPAction("SPMLModifyRequest");

            if (provisioningEntity != null && provisioningEntity.getIdentifier() != null) {
                provisioningIdentifier = provisioningEntity.getIdentifier().getIdentifier();
            } else {
                if (isDebugEnabled) {
                    log.debug("User updating faild. No provisioning identifier");
                }
                return;
            }
            PSOIdentifier psoId = new PSOIdentifier(provisioningIdentifier, null, null);

            ModifyRequest modifyRequest = new ModifyRequest();
            modifyRequest.setPsoID(psoId);
            Modification modification = new Modification();

            Map<String, String> claims = getSingleValuedClaims(provisioningEntity.getAttributes());
            Iterator claimsKeySet = claims.entrySet().iterator();

            while (claimsKeySet.hasNext()) {
                Map.Entry pairs = (Map.Entry) claimsKeySet.next();
                modification.addOpenContentElement(new DSMLAttr(pairs.getKey().toString(), pairs.getValue().toString()));
            }

            modifyRequest.addModification(modification);

            if (isDebugEnabled) {
                log.debug("Sent SPML request:" + modifyRequest.toXML(marshaller));

            }

            ModifyResponse modifyResponse = (ModifyResponse) spml2Client.send(modifyRequest);

            if (modifyResponse.getStatus().equals(StatusCode.SUCCESS)) {
                if (isDebugEnabled) {
                    log.debug("User updated successfully.");
                }
            } else {
                log.warn("SPML user update failed.");
            }
        } catch (Spml2Exception e) {
            log.error("Error while SPML user updating", e);
        }

        if (log.isTraceEnabled()) {
            log.trace("SPML user updated.");
        }
    }

    /**
     * @param provisioningEntity
     * @return
     * @throws IdentityProvisioningException
     */
    private String createUser(ProvisioningEntity provisioningEntity)
            throws IdentityProvisioningException {

        boolean isDebugEnabled = log.isDebugEnabled();

        String psoIdString = null;
        List<String> userNames = getUserNames(provisioningEntity.getAttributes());
        String userName = null;

        if (CollectionUtils.isNotEmpty(userNames)) {
            // first element must be the user name.
            userName = userNames.get(0);
        }

        try {

            ReflectiveXMLMarshaller marshaller = new ReflectiveXMLMarshaller();

            Spml2Client spml2Client = new Spml2Client(configHolder.getValue("spml-ep"));
            spml2Client.setTrace(log.isDebugEnabled());

            AddRequest req = new AddRequest();
            req.setReturnData(ReturnData.IDENTIFIER);

            Extensible attrs = new Extensible();
            attrs.addOpenContentElement(new DSMLAttr("objectclass", configHolder
                    .getValue("spml-oc")));
            attrs.addOpenContentElement(new DSMLAttr("accountId", userName));
            attrs.addOpenContentElement(new DSMLAttr("credentials", UUID.randomUUID().toString()));

            List<String> extractAttributes = configHolder.extractAttributes();

            // get user attributes.
            Map<String, String> claims = getSingleValuedClaims(provisioningEntity.getAttributes());
            Iterator claimsKeySet = claims.entrySet().iterator();

            while (claimsKeySet.hasNext()) {
                Map.Entry pairs = (Map.Entry) claimsKeySet.next();
                attrs.addOpenContentElement(new DSMLAttr(pairs.getKey().toString(), pairs.getValue().toString()));
            }

            req.setData(attrs);

            if (isDebugEnabled) {
                log.debug("Sent SPML request:" + req.toXML(marshaller));
            }

            spml2Client.setSOAPAction("SPMLAddRequest");

            AddResponse res = (AddResponse) spml2Client.send(req);

            if (res != null && res.getStatus().equals(StatusCode.SUCCESS)) {

                if (isDebugEnabled) {
                    log.debug("Recived positive add response of  " + userName);
                }

                PSO pso = res.getPso();
                PSOIdentifier psoId = pso.getPsoID();
                psoIdString = psoId.getID();

            } else {
                throw new IdentityProvisioningException(
                        "SPML provisioning failed. Invalid Response.");
            }

        } catch (Spml2Exception e) {
            log.error("Error while SPML provisioning", e);

        }

        if (log.isTraceEnabled()) {
            log.trace("SPML user provisioned.");
        }

        return psoIdString;
    }

    /**
     * @param provisioningEntity
     */
    private void deleteUser(ProvisioningEntity provisioningEntity) {

        boolean isDebugEnabled = log.isDebugEnabled();
        String provisioningIdentifier = null;

        try {
            ReflectiveXMLMarshaller marshaller = new ReflectiveXMLMarshaller();
            Spml2Client spml2Client = new Spml2Client(configHolder.getValue("spml-ep"));
            spml2Client.setTrace(log.isDebugEnabled());
            spml2Client.setSOAPAction("SPMLDeleteRequest");

            if (provisioningEntity != null && provisioningEntity.getIdentifier() != null) {
                provisioningIdentifier = provisioningEntity.getIdentifier().getIdentifier();
            } else {
                if (isDebugEnabled) {
                    log.debug("User de-provisioned faild. No provisioning identifier");
                }
                return;
            }
            PSOIdentifier psoId = new PSOIdentifier(provisioningIdentifier, null, null);
            DeleteRequest deleteRequest = new DeleteRequest();
            deleteRequest.setPsoID(psoId);

            if (isDebugEnabled) {
                log.debug("Sent SPML request:" + deleteRequest.toXML(marshaller));

            }

            DeleteResponse deleteResponse = (DeleteResponse) spml2Client.send(deleteRequest);

            if (deleteResponse.getStatus().equals(StatusCode.SUCCESS)) {
                if (isDebugEnabled) {
                    log.debug("User de-provisioned successfully.");
                }
            } else {
                log.warn("SPML user provisioning failed.");
            }
        } catch (Spml2Exception e) {
            log.error("Error while SPML de-provisioning", e);
        }

        if (log.isTraceEnabled()) {
            log.trace("SPML user de-provisioned.");
        }
    }

}
