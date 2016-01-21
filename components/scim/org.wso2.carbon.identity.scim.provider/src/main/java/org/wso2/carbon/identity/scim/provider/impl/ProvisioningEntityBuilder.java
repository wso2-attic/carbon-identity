/*
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
 */

package org.wso2.carbon.identity.scim.provider.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningConstants;
import org.wso2.carbon.identity.provisioning.ProvisioningEntity;
import org.wso2.carbon.identity.provisioning.ProvisioningEntityType;
import org.wso2.carbon.identity.provisioning.ProvisioningOperation;
import org.wso2.carbon.identity.provisioning.dao.ProvisioningManagementDAO;
import org.wso2.carbon.identity.scim.common.utils.AttributeMapper;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.charon.core.attributes.SimpleAttribute;
import org.wso2.charon.core.exceptions.CharonException;
import org.wso2.charon.core.exceptions.NotFoundException;
import org.wso2.charon.core.objects.AbstractSCIMObject;
import org.wso2.charon.core.objects.Group;
import org.wso2.charon.core.objects.SCIMObject;
import org.wso2.charon.core.objects.User;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class ProvisioningEntityBuilder {

    private static volatile ProvisioningEntityBuilder provisioningEntityBuilder = null;
    private static Log log = LogFactory.getLog(ProvisioningEntityBuilder.class);

    public static ProvisioningEntityBuilder getInstance() {
        if (provisioningEntityBuilder == null) {
            synchronized (ProvisioningEntityBuilder.class) {
                if (provisioningEntityBuilder == null) {
                    provisioningEntityBuilder = new ProvisioningEntityBuilder();
                }
            }
        }
        return provisioningEntityBuilder;
    }

    ProvisioningEntity buildProvisioningEntityForUserAdd(SCIMObject provisioningObject,
        Map<ClaimMapping, List<String>> outboundAttributes, String domainName) throws CharonException {
        User user = (User) provisioningObject;
        if (user.getPassword() != null) {
            outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                           IdentityProvisioningConstants.PASSWORD_CLAIM_URI, null, null, false),
                                   Arrays.asList(new String[] { user.getPassword() }));
        }

        if (user.getUserName() != null) {
            outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping
                                           .build(IdentityProvisioningConstants.USERNAME_CLAIM_URI, null, null, false),
                                   Arrays.asList(new String[] { user.getUserName() }));
        }

        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                       IdentityProvisioningConstants.ID_CLAIM_URI, null, null, false),
                               Arrays.asList(new String[] { user.getId() }));

        String domainAwareName =
                UserCoreUtil.addDomainToName(user.getUserName(), domainName);
        ProvisioningEntity provisioningEntity =
                new ProvisioningEntity(ProvisioningEntityType.USER, domainAwareName, ProvisioningOperation.POST,
                                       outboundAttributes);
        Map<String, String> inboundAttributes =
                AttributeMapper.getClaimsMap((AbstractSCIMObject) provisioningObject);
        provisioningEntity.setInboundAttributes(inboundAttributes);

        return provisioningEntity;
    }

    ProvisioningEntity buildProvisioningEntityForUserDelete(SCIMObject provisioningObject,
        Map<org.wso2.carbon.identity.application.common.model.ClaimMapping, List<String>> outboundAttributes,
        String domainName) throws CharonException, IdentityApplicationManagementException {

        User user = (User) provisioningObject;
        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                       IdentityProvisioningConstants.ID_CLAIM_URI, null, null, false),
                               Arrays.asList(new String[] { user.getId() }));

        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                       IdentityProvisioningConstants.USER_STORE_DOMAIN_CLAIM_URI, null, null, false),
                               Arrays.asList(new String[] { domainName }));

        ProvisioningEntity provisioningEntity = new ProvisioningEntity(ProvisioningEntityType.USER,
                                                                       ProvisioningOperation.DELETE,
                                                                       outboundAttributes);
        return provisioningEntity;
    }

    ProvisioningEntity buildProvisioningEntityForUserUpdate(SCIMObject provisioningObject,
        Map<org.wso2.carbon.identity.application.common.model.ClaimMapping, List<String>> outboundAttributes,
        String domainName) throws CharonException, IdentityApplicationManagementException {

        User user = (User) provisioningObject;
        //username should be included in user update SCIM request
        if (user.getUserName() != null) {
            outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                           IdentityProvisioningConstants.USERNAME_CLAIM_URI, null, null, false),
                                   Arrays.asList(new String[] { user.getUserName() }));
        }
        String domainAwareName = UserCoreUtil.addDomainToName(user.getUserName(), domainName);
        ProvisioningEntity provisioningEntity =
                new ProvisioningEntity(ProvisioningEntityType.USER, domainAwareName, ProvisioningOperation.PUT,
                                       outboundAttributes);
        Map<String, String> inboundAttributes =
                AttributeMapper.getClaimsMap((AbstractSCIMObject) provisioningObject);
        provisioningEntity.setInboundAttributes(inboundAttributes);
        return provisioningEntity;
    }

    ProvisioningEntity buildProvisioningEntityForUserPatch(SCIMObject provisioningObject,
        Map<org.wso2.carbon.identity.application.common.model.ClaimMapping, List<String>> outboundAttributes,
        String domainName) throws CharonException, IdentityApplicationManagementException {

        User user = (User) provisioningObject;

        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                       IdentityProvisioningConstants.ID_CLAIM_URI, null, null, false),
                               Arrays.asList(new String[] { user.getId() }));

        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                       IdentityProvisioningConstants.USER_STORE_DOMAIN_CLAIM_URI, null, null, false),
                               Arrays.asList(new String[] { domainName }));

        ProvisioningEntity provisioningEntity =
                new ProvisioningEntity(ProvisioningEntityType.USER, ProvisioningOperation.PATCH, outboundAttributes);
        Map<String, String> inboundAttributes = AttributeMapper.getClaimsMap((AbstractSCIMObject) provisioningObject);
        provisioningEntity.setInboundAttributes(inboundAttributes);

        return provisioningEntity;
    }

    ProvisioningEntity buildProvisioningEntityForGroupAdd(SCIMObject provisioningObject,
        Map<org.wso2.carbon.identity.application.common.model.ClaimMapping, List<String>> outboundAttributes,
        String domainName) throws CharonException, IdentityApplicationManagementException, NotFoundException {

        Group group = (Group) provisioningObject;
        if (provisioningObject.getAttribute("displayName") != null) {
            outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                    IdentityProvisioningConstants.GROUP_CLAIM_URI, null, null, false), Arrays.asList(
                    new String[] { ((SimpleAttribute) provisioningObject.getAttribute("displayName"))
                                           .getStringValue() }));
        }
        List<String> userList = group.getMembersWithDisplayName();

        if (!userList.isEmpty()) {
            outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                    IdentityProvisioningConstants.USERNAME_CLAIM_URI, null, null, false), userList);
        }

        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                       IdentityProvisioningConstants.ID_CLAIM_URI, null, null, false),
                               Arrays.asList(new String[] { group.getId() }));

        if (log.isDebugEnabled()) {
            log.debug("Adding domain name : " + domainName + " to role : " + group.getDisplayName());
        }
        String domainAwareName = UserCoreUtil.addDomainToName(group.getDisplayName(), domainName);

        ProvisioningEntity provisioningEntity = new ProvisioningEntity(
                ProvisioningEntityType.GROUP, domainAwareName, ProvisioningOperation.POST,
                outboundAttributes);
        return provisioningEntity;
    }

    ProvisioningEntity buildProvisioningEntityForGroupDelete(SCIMObject provisioningObject,
        Map<org.wso2.carbon.identity.application.common.model.ClaimMapping, List<String>> outboundAttributes,
        String domainName) throws CharonException, IdentityApplicationManagementException, NotFoundException {

        Group group = (Group) provisioningObject;
        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                       IdentityProvisioningConstants.ID_CLAIM_URI, null, null, false),
                               Arrays.asList(new String[] { group.getId() }));

        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                       IdentityProvisioningConstants.USER_STORE_DOMAIN_CLAIM_URI, null, null, false),
                               Arrays.asList(new String[] { domainName }));
        ProvisioningEntity provisioningEntity = new ProvisioningEntity(
                ProvisioningEntityType.GROUP, ProvisioningOperation.DELETE, outboundAttributes);
        return provisioningEntity;
    }

    ProvisioningEntity buildProvisioningEntityForGroupUpdate(SCIMObject provisioningObject,
        Map<org.wso2.carbon.identity.application.common.model.ClaimMapping, List<String>> outboundAttributes,
        String domainName) throws CharonException, IdentityApplicationManagementException, NotFoundException {

        Group group = (Group) provisioningObject;
        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                IdentityProvisioningConstants.GROUP_CLAIM_URI, null, null, false), Arrays
                                       .asList(new String[] { group.getDisplayName() }));

        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping
                                       .build(IdentityProvisioningConstants.USERNAME_CLAIM_URI,
                                              null, null, false), group.getMembersWithDisplayName());

        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                       IdentityProvisioningConstants.ID_CLAIM_URI, null, null, false),
                               Arrays.asList(new String[] { group.getId() }));

        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                       IdentityProvisioningConstants.USER_STORE_DOMAIN_CLAIM_URI, null, null, false),
                               Arrays.asList(new String[] { domainName }));

        ProvisioningEntity provisioningEntity =
                new ProvisioningEntity(ProvisioningEntityType.GROUP, ProvisioningOperation.PUT, outboundAttributes);

        return provisioningEntity;
    }

    ProvisioningEntity buildProvisioningEntityForGroupPatch(SCIMObject provisioningObject,
        Map<org.wso2.carbon.identity.application.common.model.ClaimMapping, List<String>> outboundAttributes,
        String domainName) throws CharonException, IdentityApplicationManagementException, NotFoundException {

        Group group = (Group) provisioningObject;

        if (group.getDisplayName() != null) {
            outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                    IdentityProvisioningConstants.GROUP_CLAIM_URI, null, null, false), Arrays
                                           .asList(new String[] { group.getDisplayName() }));
        }

        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping
                                       .build(IdentityProvisioningConstants.USERNAME_CLAIM_URI,
                                              null, null, false), group.getMembersWithDisplayName());

        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                       IdentityProvisioningConstants.ID_CLAIM_URI, null, null, false),
                               Arrays.asList(new String[] { group.getId() }));

        outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                       IdentityProvisioningConstants.USER_STORE_DOMAIN_CLAIM_URI, null, null, false),
                               Arrays.asList(new String[] { domainName }));

        ProvisioningEntity provisioningEntity =
                new ProvisioningEntity(ProvisioningEntityType.GROUP, ProvisioningOperation.PATCH, outboundAttributes);

        return provisioningEntity;
    }
}
