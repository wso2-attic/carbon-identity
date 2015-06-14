/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 */

package org.wso2.carbon.identity.entitlement.policy.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.entitlement.EntitlementException;
import org.wso2.carbon.identity.entitlement.PAPStatusDataHandler;
import org.wso2.carbon.identity.entitlement.PDPConstants;
import org.wso2.carbon.identity.entitlement.common.EntitlementConstants;
import org.wso2.carbon.identity.entitlement.dto.PolicyDTO;
import org.wso2.carbon.identity.entitlement.dto.PublisherDataHolder;
import org.wso2.carbon.identity.entitlement.dto.StatusHolder;
import org.wso2.carbon.identity.entitlement.internal.EntitlementServiceComponent;
import org.wso2.carbon.identity.entitlement.pap.EntitlementAdminEngine;
import org.wso2.carbon.identity.entitlement.policy.version.PolicyVersionManager;
import org.wso2.carbon.registry.api.Registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Policy publish executor
 */
public class PolicyPublishExecutor implements Runnable {

    public static final String PDPSUBSCRIBER = "PDPSubscriber";
    private static final Log log = LogFactory.getLog(PolicyPublishExecutor.class);
    public static final String POLICY_IDS = "policyIds";
    public static final String VERSION = "version";
    public static final String SUBSCRIBER_IDS = "subscriberIds";
    public static final String ACTION = "action";
    public static final String ORDER = "order";
    private String[] policyIds;
    private String[] subscriberIds;
    private PolicyPublisher publisher;
    private String version;
    private String action;
    private String verificationCode;
    private boolean toPDP;
    private String tenantDomain;
    private int tenantId;
    private String userName;
    private int order;
    private boolean enabled;

    public PolicyPublishExecutor(String[] policyIds, String version, String action, boolean enabled, int order,
                                 String[] subscriberIds, PolicyPublisher publisher,
                                 boolean toPDP, String verificationCode) {

        if (policyIds != null) {
            this.policyIds = policyIds.clone();
        }
        if (toPDP) {
            this.subscriberIds = new String[]{PDPSUBSCRIBER};
        }
        if (subscriberIds != null) {
            this.subscriberIds = subscriberIds.clone();
        }
        this.action = action;
        this.version = version;
        this.publisher = publisher;
        this.toPDP = toPDP;
        this.order = order;
        this.enabled = enabled;
        this.verificationCode = verificationCode;
    }

    @Override
    public void run() {

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext context = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        context.setTenantDomain(tenantDomain);
        context.setTenantId(tenantId);
        context.setUsername(userName);
        try {
            publish();
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

    }

    public void publish() {


        if ((policyIds == null || policyIds.length > 0) && verificationCode != null) {
            loadVerificationCode(verificationCode);
        }

        String newVerificationCode = null;
        List<String> notPublishedSubscribers = new ArrayList<>();


        PolicyPublisherModule policyPublisherModule = null;
        Set<PolicyPublisherModule> publisherModules = publisher.getPublisherModules();

        if (publisherModules == null) {
            return;
        }

        PublisherDataHolder holder = null;
        Set<PAPStatusDataHandler> papStatusDataHandler = publisher.getPapStatusDataHandlers();
        for (String subscriberId : subscriberIds) {

            // there is only one known subscriber, if policies are publishing to PDP
            List<StatusHolder> subscriberHolders = new ArrayList<>();
            List<StatusHolder> policyHolders = new ArrayList<>();
            if (toPDP) {
                policyPublisherModule = new CarbonPDPPublisher();
                holder = new PublisherDataHolder(policyPublisherModule.getModuleName());
            } else {
                try {
                    holder = publisher.retrieveSubscriber(subscriberId, true);
                } catch (EntitlementException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Exception stack trace : ", e);
                    }
                    log.error("Subscriber details can not be retrieved. So skip publishing policies " +
                              "for subscriber : " + subscriberId);
                }

                if (holder != null) {
                    for (PolicyPublisherModule publisherModule : publisherModules) {
                        if (publisherModule.getModuleName().equals(holder.getModuleName())) {
                            policyPublisherModule = publisherModule;
                            if (policyPublisherModule instanceof AbstractPolicyPublisherModule) {
                                try {
                                    ((AbstractPolicyPublisherModule) policyPublisherModule).init(holder);
                                } catch (Exception e) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Exception ignored. ", e);
                                    }
                                    subscriberHolders.add(new StatusHolder(
                                            EntitlementConstants.StatusTypes.PUBLISH_POLICY,
                                            subscriberId, version, "More than one Policy", action, false, e
                                                    .getMessage()));
                                    continue;
                                }
                            }
                            break;
                        }
                    }
                }
            }

            if (policyPublisherModule == null) {
                subscriberHolders.add(new StatusHolder(EntitlementConstants.StatusTypes.PUBLISH_POLICY,
                                                       subscriberId, version, "More than one Policy", action, false,
                                                       "No policy publish module is defined for subscriber : " + subscriberId));
                continue;
            }

            // try with post verification module.
            try {
                PublisherVerificationModule verificationModule = publisher.getVerificationModule();
                if (verificationModule != null && !verificationModule.doVerify(verificationCode)) {
                    newVerificationCode = verificationModule.getVerificationCode(holder);
                    notPublishedSubscribers.add(subscriberId);
                    break;
                }

            } catch (EntitlementException e) {
                // ignore
                log.error("Error while calling the post verification publisher module", e);
            }

            for (String policyId : policyIds) {

                PolicyDTO policyDTO = null;

                if (EntitlementConstants.PolicyPublish.ACTION_CREATE.equalsIgnoreCase(action) ||
                    EntitlementConstants.PolicyPublish.ACTION_UPDATE.equalsIgnoreCase(action)) {
                    PolicyVersionManager manager = EntitlementAdminEngine.getInstance().getVersionManager();
                    try {
                        policyDTO = manager.getPolicy(policyId, version);
                    } catch (EntitlementException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Exception ignored. ", e);
                        }
                    }
                } else {
                    policyDTO = new PolicyDTO();
                    policyDTO.setPolicyId(policyId);
                    policyDTO.setVersion(version);
                    policyDTO.setPolicyOrder(order);
                }

                if (policyDTO == null) {
                    subscriberHolders.add(new StatusHolder(EntitlementConstants.StatusTypes.PUBLISH_POLICY,
                                                           subscriberId, version, policyId, action, false,
                                                           "Can not found policy under policy id : " + policyId));
                    policyHolders.add(new StatusHolder(EntitlementConstants.StatusTypes.PUBLISH_POLICY,
                                                       policyId, version, subscriberId, action, false,
                                                       "Can not found policy under policy id : " + policyId));
                    continue;
                }

                try {
                    policyPublisherModule.publish(policyDTO, action, enabled, order);
                    subscriberHolders.add(new StatusHolder(EntitlementConstants.StatusTypes.PUBLISH_POLICY,
                                                           subscriberId, version, policyId, action));
                    policyHolders.add(new StatusHolder(EntitlementConstants.StatusTypes.PUBLISH_POLICY,
                                                       policyId, version, subscriberId, action));
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Exception ignored. ", e);
                    }
                    subscriberHolders.add(new StatusHolder(EntitlementConstants.StatusTypes.PUBLISH_POLICY,
                                                           subscriberId, version, policyId, action, false, e.getMessage()));
                    policyHolders.add(new StatusHolder(EntitlementConstants.StatusTypes.PUBLISH_POLICY,
                                                       policyId, version, subscriberId, action, false, e.getMessage()));
                }

                for (PAPStatusDataHandler module : papStatusDataHandler) {
                    try {
                        module.handle(EntitlementConstants.Status.ABOUT_POLICY, policyId, policyHolders);
                        policyHolders = new ArrayList<StatusHolder>();
                    } catch (EntitlementException e) {
                        // ignore
                        log.error("Error while calling post publishers", e);
                    }
                }
            }

            for (PAPStatusDataHandler module : papStatusDataHandler) {
                try {
                    module.handle(EntitlementConstants.Status.ABOUT_SUBSCRIBER, subscriberId, subscriberHolders);
                    subscriberHolders = new ArrayList<StatusHolder>();
                } catch (EntitlementException e) {
                    // ignore
                    log.error("Error while calling post publishers", e);
                }
            }
        }

        if (newVerificationCode != null) {
            persistVerificationCode(newVerificationCode,
                                    notPublishedSubscribers.toArray(new String[notPublishedSubscribers.size()]));
        }
    }

    /**
     * Helper method
     *
     * @param verificationCode verificationCode as String
     * @param subscriberIds    Array of subscriberIds
     */
    private void persistVerificationCode(String verificationCode, String[] subscriberIds) {

        Registry registry = EntitlementServiceComponent.
                getGovernanceRegistry(CarbonContext.getThreadLocalCarbonContext().getTenantId());
        if (registry != null) {
            try {
                org.wso2.carbon.registry.api.Resource resource = null;
                resource = registry.newResource();
                resource.setProperty(SUBSCRIBER_IDS, Arrays.asList(subscriberIds));
                resource.setProperty(POLICY_IDS, Arrays.asList(policyIds));
                resource.setProperty(ACTION, action);
                resource.setProperty(VERSION, version);
                resource.setProperty(ORDER, Integer.toString(order));
                registry.put(PDPConstants.ENTITLEMENT_POLICY_PUBLISHER_VERIFICATION + verificationCode,
                             resource);
            } catch (org.wso2.carbon.registry.api.RegistryException e) {
                log.error("Error while persisting verification code", e);
            }
        } else {
            log.error("Error while persisting verification code");
        }
    }

    /**
     * Helper method
     *
     * @param verificationCode verificationCode as String
     */
    private void loadVerificationCode(String verificationCode) {

        Registry registry = EntitlementServiceComponent.
                getGovernanceRegistry(CarbonContext.getThreadLocalCarbonContext().getTenantId());
        if(registry != null) {
            try {
                org.wso2.carbon.registry.api.Resource resource = registry.
                        get(PDPConstants.ENTITLEMENT_POLICY_PUBLISHER_VERIFICATION + verificationCode);
                List<String> list = resource.getPropertyValues(SUBSCRIBER_IDS);
                if (list != null) {
                    subscriberIds = list.toArray(new String[list.size()]);
                }
                list = resource.getPropertyValues(POLICY_IDS);
                if (list != null) {
                    policyIds = list.toArray(new String[list.size()]);
                }
                String tempVersion = resource.getProperty(VERSION);
                if (tempVersion != null) {
                    this.version = tempVersion;
                }
                String tempAction = resource.getProperty(ACTION);
                if (tempAction != null) {
                    this.action = tempAction;
                }
                String tempOrder = resource.getProperty(ORDER);
                if (tempOrder != null) {
                    this.order = Integer.parseInt(tempOrder);
                }
            } catch (org.wso2.carbon.registry.api.RegistryException e) {
                log.error("Error while loading verification code", e);
            }
        } else {
            log.error("Error while loading verification code");
        }
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}