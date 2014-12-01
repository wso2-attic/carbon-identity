package org.wso2.carbon.identity.application.authentication.framework.handler.sequence.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.config.model.ApplicationConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.model.AuthenticatorConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.model.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.model.SequenceConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.model.StepConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.handler.sequence.StepBasedSequenceHandler;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.ThreadLocalProvisioningServiceProvider;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.application.mgt.ApplicationConstants;
import org.wso2.carbon.identity.user.profile.mgt.UserProfileAdmin;
import org.wso2.carbon.identity.user.profile.mgt.UserProfileException;

public class DefaultStepBasedSequenceHandler implements StepBasedSequenceHandler {

    private static Log log = LogFactory.getLog(DefaultStepBasedSequenceHandler.class);
    private static volatile DefaultStepBasedSequenceHandler instance;

    public static DefaultStepBasedSequenceHandler getInstance() {

        if (instance == null) {
            synchronized (DefaultStepBasedSequenceHandler.class) {
                if (instance == null) {
                    instance = new DefaultStepBasedSequenceHandler();
                }
            }
        }

        return instance;
    }

    /**
     * Executes the steps
     * 
     * @param request
     * @param response
     * @throws FrameworkException
     * @throws Exception
     */
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AuthenticationContext context) throws FrameworkException {

        if (log.isDebugEnabled()) {
            log.debug("Executing the Step Based Authentication...");
        }

        while (!context.getSequenceConfig().isCompleted()) {

            int currentStep = context.getCurrentStep();

            // let's initialize the step count to 1 if this the beginning of the sequence
            if (currentStep == 0) {
                currentStep++;
                context.setCurrentStep(currentStep);
            }

            StepConfig stepConfig = context.getSequenceConfig().getStepMap().get(currentStep);

            // if the current step is completed
            if (stepConfig.isCompleted()) {
                stepConfig.setCompleted(false);
                stepConfig.setRetrying(false);

                // if the request didn't fail during the step execution
                if (context.isRequestAuthenticated()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Step " + stepConfig.getOrder()
                                + " is completed. Going to get the next one.");
                    }

                    currentStep = context.getCurrentStep() + 1;
                    context.setCurrentStep(currentStep);
                    stepConfig = context.getSequenceConfig().getStepMap().get(currentStep);

                } else {

                    if (log.isDebugEnabled()) {
                        log.debug("Authentication has failed in the Step "
                                + (context.getCurrentStep()));
                    }

                    // if the step contains multiple login options, we should give the user to retry
                    // authentication
                    if (stepConfig.isMultiOption() && !context.isPassiveAuthenticate()) {
                        stepConfig.setRetrying(true);
                        context.setRequestAuthenticated(true);
                    } else {
                        context.getSequenceConfig().setCompleted(true);
                    }
                }

                resetAuthenticationContext(context);
            }

            // if no further steps exists
            if (stepConfig == null) {

                if (log.isDebugEnabled()) {
                    log.debug("There are no more steps to execute");
                }

                // if no step failed at authentication we should do post authentication work (e.g.
                // claim handling, provision etc)
                if (context.isRequestAuthenticated()) {

                    if (log.isDebugEnabled()) {
                        log.debug("Request is successfully authenticated");
                    }

                    context.getSequenceConfig().setCompleted(true);
                    handlePostAuthentication(request, response, context);
                }

                // we should get out of steps now.
                if (log.isDebugEnabled()) {
                    log.debug("Step processing is completed");
                }
                continue;
            }

            // if the sequence is not completed, we have work to do.
            if (log.isDebugEnabled()) {
                log.debug("Starting Step: " + String.valueOf(stepConfig.getOrder()));
            }

            FrameworkUtils.getStepHandler().handle(request, response, context);

            // if step is not completed, that means step wants to redirect to outside
            if (!stepConfig.isCompleted()) {
                if (log.isDebugEnabled()) {
                    log.debug("Step is not complete yet. Redirecting to outside.");
                }
                return;
            }

            context.setReturning(false);
        }
    }

    @SuppressWarnings("unchecked")
    protected void handlePostAuthentication(HttpServletRequest request,
            HttpServletResponse response, AuthenticationContext context) throws FrameworkException {

        if (log.isDebugEnabled()) {
            log.debug("Handling Post Authentication tasks");
        }

        SequenceConfig sequenceConfig = context.getSequenceConfig();
        StringBuilder jsonBuilder = new StringBuilder();

        boolean subjectFoundInStep = false;
        boolean subjectAttributesFoundInStep = false;
        int stepCount = 1;
        Map<String, String> mappedAttrs = new HashMap<String, String>();

        for (Map.Entry<Integer, StepConfig> entry : sequenceConfig.getStepMap().entrySet()) {
            StepConfig stepConfig = entry.getValue();
            AuthenticatorConfig authenticatorConfig = stepConfig.getAuthenticatedAutenticator();
            ApplicationAuthenticator authenticator = authenticatorConfig
                    .getApplicationAuthenticator();

            // build the authenticated idps JWT to send to the calling servlet.
            if (stepCount == 1) {
                jsonBuilder.append("\"idps\":");
                jsonBuilder.append("[");
            }

            // build the JSON object for this step
            jsonBuilder.append("{");
            jsonBuilder.append("\"idp\":\"").append(stepConfig.getAuthenticatedIdP()).append("\",");
            jsonBuilder.append("\"authenticator\":\"").append(authenticator.getName()).append("\"");

            if (stepCount != sequenceConfig.getStepMap().size()) {
                jsonBuilder.append("},");
            } else {
                // wrap up the JSON object
                jsonBuilder.append("}");
                jsonBuilder.append("]");

                sequenceConfig.setAuthenticatedIdPs(IdentityApplicationManagementUtil.getSignedJWT(
                        jsonBuilder.toString(), sequenceConfig.getApplicationConfig()
                                .getServiceProvider()));

                if (!subjectFoundInStep) {
                    stepConfig.setSubjectIdentifierStep(true);
                }

                if (!subjectAttributesFoundInStep) {
                    stepConfig.setSubjectAttributeStep(true);
                }
            }

            stepCount++;

            if (authenticator instanceof FederatedApplicationAuthenticator) {

                ExternalIdPConfig externalIdPConfig = ConfigurationFacade.getInstance()
                        .getIdPConfigByName(stepConfig.getAuthenticatedIdP(),
                                context.getTenantDomain());

                context.setExternalIdP(externalIdPConfig);

                String originalExternalIdpSubjectValueForThisStep = stepConfig
                        .getAuthenticatedUser();

                if (externalIdPConfig == null) {
                    String errorMsg = "An External IdP cannot be null for a FederatedApplicationAuthenticator";
                    log.error(errorMsg);
                    throw new FrameworkException(errorMsg);
                }

                Map<ClaimMapping, String> extAttrs = null;
                Map<String, String> extAttibutesValueMap = null;
                Map<String, String> localClaimValues = null;

                extAttrs = stepConfig.getAuthenticatedUserAttributes();
                extAttibutesValueMap = FrameworkUtils.getClaimMappings(extAttrs, true);

                if (stepConfig.isSubjectIdentifierStep()) {
                    // there can be only step for subject attributes.

                    subjectFoundInStep = true;
                    String associatedID = null;
                    String userIdClaimUri = externalIdPConfig.getUserIdClaimUri();

                    if (userIdClaimUri != null) {
                        String subjectIdFromClaim = null;
                        // get the subject id from the attributes.

                        if (extAttrs != null && !extAttrs.isEmpty()) {
                            // do claim handling
                            mappedAttrs = handleClaimMappings(stepConfig, context,
                                    extAttibutesValueMap, true);

                            subjectIdFromClaim = mappedAttrs.get(userIdClaimUri);

                        }

                        if (subjectIdFromClaim != null) {
                            // if we found any value - then we can update the step configuration.
                            sequenceConfig.setAuthenticatedUser(subjectIdFromClaim);
                            // TODO : this may not be needed.
                            stepConfig.setAuthenticatedUser(subjectIdFromClaim);
                            originalExternalIdpSubjectValueForThisStep = subjectIdFromClaim;
                        }
                    }

                    // now we know the value of the subject - from the external identity provider.

                    if (sequenceConfig.getApplicationConfig().isAlwaysSendMappedLocalSubjectId()) {

                        // okay - now we need to find out the corresponding mapped local subject
                        // identifier.

                        UserProfileAdmin userProfileAdmin = UserProfileAdmin.getInstance();
                        try {
                            associatedID = userProfileAdmin.getNameAssociatedWith(
                                    stepConfig.getAuthenticatedIdP(),
                                    originalExternalIdpSubjectValueForThisStep);
                        } catch (UserProfileException e) {
                            throw new FrameworkException("Error while getting associated ID");
                        }
                    }

                    // external claim values mapped to local claim uris.
                    localClaimValues = (Map<String, String>) context
                            .getProperty(FrameworkConstants.UNFILTERED_LOCAL_CLAIM_VALUES);

                    if (associatedID != null && associatedID.trim().length() > 0) {
                        // we found an associated user identifier
                        sequenceConfig.setAuthenticatedUser(associatedID);
                        // TODO : this may not be needed.
                        stepConfig.setAuthenticatedUser(associatedID);

                        sequenceConfig.getApplicationConfig().setMappedSubjectIDSelected(true);

                        // if we found a local mapped user - then we will also take attributes from
                        // that user - this will load local claim values for the user.
                        mappedAttrs = handleClaimMappings(stepConfig, context, null, false);

                        sequenceConfig.setUserAttributes(FrameworkUtils
                                .buildClaimMappings(mappedAttrs));

                    } else {

                        // there is no mapped local user found. set the value we got as the subject
                        // identifier.
                        sequenceConfig.setAuthenticatedUser(stepConfig.getAuthenticatedUser());

                    }

                }

                if (stepConfig.isSubjectAttributeStep()) {

                    subjectAttributesFoundInStep = true;

                    String idpRoleClaimUri = getIdpRoleClaimUri(externalIdPConfig);

                    List<String> locallyMappedUserRoles = getLocallyMappedUserRoles(sequenceConfig,
                            externalIdPConfig, extAttibutesValueMap, idpRoleClaimUri);

                    if (idpRoleClaimUri != null && locallyMappedUserRoles != null && locallyMappedUserRoles.size() > 0) {
                        extAttibutesValueMap.put(
                                idpRoleClaimUri,
                                getServiceProviderMappedUserRoles(sequenceConfig,
                                        locallyMappedUserRoles));
                    }

                    if (extAttrs != null && !extAttrs.isEmpty()
                            && (mappedAttrs == null || mappedAttrs.isEmpty())) {
                        // do claim handling
                        mappedAttrs = handleClaimMappings(stepConfig, context,
                                extAttibutesValueMap, true);
                    }

                    if (!sequenceConfig.getApplicationConfig().isMappedSubjectIDSelected()) {
                        // if we found the mapped subject - then we do not need to worry about
                        // finding attributes.
                        sequenceConfig.setUserAttributes(FrameworkUtils
                                .buildClaimMappings(mappedAttrs));
                    }

                    // do user provisioning. we should provision the user with the original external
                    // subject identifier.
                    if (externalIdPConfig.isProvisioningEnabled()) {

                        if (localClaimValues == null) {
                            localClaimValues = extAttibutesValueMap;
                        }

                        handleJitProvisioning(originalExternalIdpSubjectValueForThisStep, context,
                                locallyMappedUserRoles, localClaimValues);
                    }

                }

            } else {

                if (stepConfig.isSubjectIdentifierStep()) {
                    subjectFoundInStep = true;
                    sequenceConfig.setAuthenticatedUser(stepConfig.getAuthenticatedUser());
                }

                if (stepConfig.isSubjectAttributeStep()) {
                    subjectAttributesFoundInStep = true;
                    // local authentications
                    mappedAttrs = handleClaimMappings(stepConfig, context, null, false);

                    String spRoleUri = getSpRoleClaimUri(sequenceConfig.getApplicationConfig());

                    String roleAttr = mappedAttrs.get(spRoleUri);

                    if (roleAttr != null && roleAttr.trim().length() > 0) {

                        String roles[] = roleAttr.split(",");
                        mappedAttrs.put(
                                spRoleUri,
                                getServiceProviderMappedUserRoles(sequenceConfig,
                                        Arrays.asList(roles)));
                    }

                    sequenceConfig
                            .setUserAttributes(FrameworkUtils.buildClaimMappings(mappedAttrs));

                }
            }
        }

        if (context.getSequenceConfig().getApplicationConfig().getSubjectClaimUri() != null
                && context.getSequenceConfig().getApplicationConfig().getSubjectClaimUri().trim()
                        .length() > 0) {
            Map<String, String> unfilteredClaimValues = (Map<String, String>) context
                    .getProperty(FrameworkConstants.UNFILTERED_LOCAL_CLAIM_VALUES);

            String subjectValue = null;

            if (unfilteredClaimValues != null) {
                subjectValue = unfilteredClaimValues.get(context.getSequenceConfig()
                        .getApplicationConfig().getSubjectClaimUri().trim());
            } else {
                subjectValue = mappedAttrs.get(context.getSequenceConfig().getApplicationConfig()
                        .getSubjectClaimUri().trim());
            }
            if (subjectValue != null) {
                sequenceConfig.setAuthenticatedUser(subjectValue);
            }
        }

    }

    /**
     * 
     * @param sequenceConfig
     * @param locallyMappedUserRoles
     * @return
     */
    protected String getServiceProviderMappedUserRoles(SequenceConfig sequenceConfig,
            List<String> locallyMappedUserRoles) throws FrameworkException {

        if (locallyMappedUserRoles != null && locallyMappedUserRoles.size() > 0) {

            Map<String, String> localToSpRoleMapping = sequenceConfig.getApplicationConfig()
                    .getRoleMappings();

            boolean roleMappingDefined = false;

            if (localToSpRoleMapping != null && localToSpRoleMapping.size() > 0) {
                roleMappingDefined = true;
            }

            StringBuffer spMappedUserRoles = new StringBuffer();

            for (String role : locallyMappedUserRoles) {
                if (roleMappingDefined) {
                    if (localToSpRoleMapping.containsKey(role)) {
                        spMappedUserRoles.append(localToSpRoleMapping.get(role) + ",");
                    } else {
                        spMappedUserRoles.append(role + ",");
                    }
                } else {
                    spMappedUserRoles.append(role + ",");
                }
            }

            return spMappedUserRoles.length() > 0 ? spMappedUserRoles.toString().substring(0,
                    spMappedUserRoles.length() - 1) : null;
        }

        return null;
    }

    /**
     * 
     * @param appConfig
     * @return
     */
    protected String getSpRoleClaimUri(ApplicationConfig appConfig) throws FrameworkException {
        // get external identity provider role claim uri.
        String spRoleClaimUri = appConfig.getRoleClaim();

        if (spRoleClaimUri == null || spRoleClaimUri.isEmpty()) {
            // no role claim uri defined
            // we can still try to find it out - lets have a look at the claim
            // mapping.
            Map<String, String> spToLocalClaimMapping = appConfig.getClaimMappings();

            if (spToLocalClaimMapping != null && spToLocalClaimMapping.size() > 0) {

                for (Iterator<Entry<String, String>> iterator = spToLocalClaimMapping.entrySet()
                        .iterator(); iterator.hasNext();) {
                    Entry<String, String> entry = iterator.next();
                    if (FrameworkConstants.LOCAL_ROLE_CLAIM_URI.equals(entry.getValue())) {
                        return entry.getKey();
                    }
                }
            }
        }

        return spRoleClaimUri;
    }

    /**
     * 
     * @param externalIdPConfig
     * @return
     */
    protected String getIdpRoleClaimUri(ExternalIdPConfig externalIdPConfig)
            throws FrameworkException {
        // get external identity provider role claim uri.
        String idpRoleClaimUri = externalIdPConfig.getRoleClaimUri();

        if (idpRoleClaimUri == null || idpRoleClaimUri.isEmpty()) {
            // no role claim uri defined
            // we can still try to find it out - lets have a look at the claim
            // mapping.
            ClaimMapping[] idpToLocalClaimMapping = externalIdPConfig.getClaimMappings();

            if (idpToLocalClaimMapping != null && idpToLocalClaimMapping.length > 0) {

                for (ClaimMapping mapping : idpToLocalClaimMapping) {
                    if (FrameworkConstants.LOCAL_ROLE_CLAIM_URI.equals(mapping.getLocalClaim()
                            .getClaimUri())) {
                        if (mapping.getRemoteClaim() != null) {
                            return mapping.getRemoteClaim().getClaimUri();
                        }
                    }
                }
            }
        }

        return idpRoleClaimUri;
    }

    /**
     * 
     * @param sequenceConfig
     * @param externalIdPConfig
     * @param extAttibutesValueMap
     * @return
     */
    protected List<String> getLocallyMappedUserRoles(SequenceConfig sequenceConfig,
            ExternalIdPConfig externalIdPConfig, Map<String, String> extAttibutesValueMap,
            String idpRoleClaimUri) throws FrameworkException {

        String idpRoleAttrValue = null;

        if (idpRoleClaimUri == null) {
            // we cannot do role mapping.
            log.debug("Role claim uri not found for the identity provider");
            return new ArrayList<String>();
        }

        idpRoleAttrValue = extAttibutesValueMap.get(idpRoleClaimUri);

        String[] idpRoles = null;

        if (idpRoleAttrValue != null) {
            idpRoles = idpRoleAttrValue.split(",");
        } else {
            // no identity provider role values found.
            return new ArrayList<String>();
        }

        // identity provider role to local role.
        Map<String, String> idpToLocalRoleMapping = externalIdPConfig.getRoleMappings();

        boolean roleMappingDefined = false;

        if (idpToLocalRoleMapping != null && idpToLocalRoleMapping.size() > 0) {
            // if no role mapping defined in the identity provider configuration
            // - we will just
            // pass-through the roles.
            roleMappingDefined = true;
        }

        List<String> locallyMappedUserRoles = new ArrayList<String>();

        if (idpRoles != null && idpRoles.length > 0) {
            for (String idpRole : idpRoles) {
                if (roleMappingDefined) {
                    if (idpToLocalRoleMapping.containsKey(idpRole)) {
                        locallyMappedUserRoles.add(idpToLocalRoleMapping.get(idpRole));
                    } else {
                        locallyMappedUserRoles.add(idpRole);
                    }
                } else {
                    locallyMappedUserRoles.add(idpRole);
                }
            }
        }

        return locallyMappedUserRoles;
    }

    /**
     * 
     * @param stepConfig
     * @param context
     * @param extAttrs
     * @param isFederatedClaims
     * @return
     */
    protected Map<String, String> handleClaimMappings(StepConfig stepConfig,
            AuthenticationContext context, Map<String, String> extAttrs, boolean isFederatedClaims)
            throws FrameworkException {

        Map<String, String> mappedAttrs = new HashMap<String, String>();

        try {
            mappedAttrs = FrameworkUtils.getClaimHandler().handleClaimMappings(stepConfig, context,
                    extAttrs, isFederatedClaims);
        } catch (FrameworkException e) {
            log.error("Claim handling failed!", e);
        }

        return mappedAttrs;
    }

    /**
     * 
     * @param context
     * @param externalIdPConfig
     * @param mappedAttrs
     */
    protected void handleJitProvisioning(String subjectIdentifier, AuthenticationContext context,
            List<String> mappedRoles, Map<String, String> extAttibutesValueMap)
            throws FrameworkException {

        try {
            @SuppressWarnings("unchecked")
            String userStoreDomain = null;
            String provisioningClaimUri = context.getExternalIdP()
                    .getProvisioningUserStoreClaimURI();
            String provisioningUserStoreId = context.getExternalIdP().getProvisioningUserStoreId();

            if (provisioningUserStoreId != null) {
                userStoreDomain = provisioningUserStoreId;
            } else if (provisioningClaimUri != null) {
                userStoreDomain = extAttibutesValueMap.get(provisioningClaimUri);
            }

            // setup thread local variable to be consumed by the provisioning
            // framework.
            ThreadLocalProvisioningServiceProvider serviceProvider = new ThreadLocalProvisioningServiceProvider();
            serviceProvider.setServiceProviderName(context.getSequenceConfig()
                    .getApplicationConfig().getApplicationName());
            serviceProvider.setJustInTimeProvisioning(true);
            serviceProvider.setClaimDialect(ApplicationConstants.LOCAL_IDP_DEFAULT_CLAIM_DIALECT);
            serviceProvider.setTenantDomain(context.getTenantDomain());
            IdentityApplicationManagementUtil
                    .setThreadLocalProvisioningServiceProvider(serviceProvider);

            FrameworkUtils.getProvisioningHandler().handle(mappedRoles, subjectIdentifier,
                    extAttibutesValueMap, userStoreDomain, context.getTenantDomain());

        } catch (FrameworkException e) {
            log.error("User provisioning failed!", e);
        } finally {
            IdentityApplicationManagementUtil.resetThreadLocalProvisioningServiceProvider();
        }
    }

    protected void resetAuthenticationContext(AuthenticationContext context)
            throws FrameworkException {

        context.setSubject(null);
        context.setSubjectAttributes(new HashMap<ClaimMapping, String>());
        context.setStateInfo(null);
        context.setExternalIdP(null);
        context.setAuthenticatorProperties(new HashMap<String, String>());
        context.setRetryCount(0);
        context.setRetrying(false);
        context.setCurrentAuthenticator(null);
    }
}
