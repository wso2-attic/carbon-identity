package org.wso2.carbon.identity.application.authentication.framework.handler.sequence.impl;

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
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.config.model.ApplicationConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.model.AuthenticatorConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.model.SequenceConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.exception.InvalidCredentialsException;
import org.wso2.carbon.identity.application.authentication.framework.exception.LogoutFailedException;
import org.wso2.carbon.identity.application.authentication.framework.handler.sequence.RequestPathBasedSequenceHandler;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedIdPData;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;

public class DefaultRequestPathBasedSequenceHandler implements RequestPathBasedSequenceHandler {

    private static Log log = LogFactory.getLog(DefaultRequestPathBasedSequenceHandler.class);
    private static volatile DefaultRequestPathBasedSequenceHandler instance;

    public static DefaultRequestPathBasedSequenceHandler getInstance() {

        if (instance == null) {
            synchronized (DefaultRequestPathBasedSequenceHandler.class) {

                if (instance == null) {
                    instance = new DefaultRequestPathBasedSequenceHandler();
                }
            }
        }

        return instance;
    }

    public void handle(HttpServletRequest request, HttpServletResponse response,
            AuthenticationContext context) throws FrameworkException {

        if (log.isDebugEnabled()) {
            log.debug("Executing the Request Path Authentication...");
        }

        SequenceConfig seqConfig = context.getSequenceConfig();
        List<AuthenticatorConfig> reqPathAuthenticators = seqConfig.getReqPathAuthenticators();

        for (AuthenticatorConfig reqPathAuthenticator : reqPathAuthenticators) {

            ApplicationAuthenticator authenticator = reqPathAuthenticator
                    .getApplicationAuthenticator();

            if (log.isDebugEnabled()) {
                log.debug("Executing " + authenticator.getName());
            }

            if (authenticator.canHandle(request)) {

                if (log.isDebugEnabled()) {
                    log.debug(authenticator.getName() + " can handle the request");
                }

                try {
                    AuthenticatorFlowStatus status = authenticator.process(request, response,
                            context);

                    if (log.isDebugEnabled()) {
                        log.debug(authenticator.getName() + ".authenticate() returned: "
                                + status.toString());
                    }

                    String authenticatedUser = context.getSubject();
                    seqConfig.setAuthenticatedUser(authenticatedUser);

                    if (log.isDebugEnabled()) {
                        log.debug("Authenticated User: " + authenticatedUser);
                    }

                    AuthenticatedIdPData authenticatedIdPData = new AuthenticatedIdPData();

                    // store authenticated user
                    authenticatedIdPData.setUsername(authenticatedUser);

                    // store authenticated user's attributes
                    Map<ClaimMapping, String> userAttributes = context.getSubjectAttributes();
                    authenticatedIdPData.setUserAttributes(userAttributes);

                    // store authenticated idp
                    authenticatedIdPData.setIdpName(FrameworkConstants.LOCAL_IDP_NAME);
                    reqPathAuthenticator.setAuthenticatorStateInfo(context.getStateInfo());
                    authenticatedIdPData.setAuthenticator(reqPathAuthenticator);
                    
                    seqConfig.setAuthenticatedReqPathAuthenticator(reqPathAuthenticator);

                    
                    context.getCurrentAuthenticatedIdPs().put(FrameworkConstants.LOCAL_IDP_NAME,
                            authenticatedIdPData);

                    handlePostAuthentication(request, response, context, authenticatedIdPData);

                } catch (AuthenticationFailedException e) {
                    if (e instanceof InvalidCredentialsException) {
                        log.warn("A login attempt was failed due to invalid credentials");
                    } else {
                        log.error(e.getMessage(), e);
                    }
                    context.setRequestAuthenticated(false);
                } catch (LogoutFailedException e) {
                    throw new FrameworkException(e.getMessage(), e);
                }

                context.getSequenceConfig().setCompleted(true);
                return;
            }
        }
    }

    protected void handlePostAuthentication(HttpServletRequest request,
            HttpServletResponse response, AuthenticationContext context,
            AuthenticatedIdPData authenticatedIdPData) throws FrameworkException {

        if (log.isDebugEnabled()) {
            log.debug("Handling Post Authentication tasks");
        }

        SequenceConfig sequenceConfig = context.getSequenceConfig();
        Map<String, String> mappedAttrs = new HashMap<String, String>();
        StringBuilder jsonBuilder = new StringBuilder();

        // build the authenticated idps JWT to send to the calling servlet.
        jsonBuilder.append("\"idps\":");
        jsonBuilder.append("[");

        // build the JSON object for this step
        jsonBuilder.append("{");
        jsonBuilder.append("\"idp\":\"").append(authenticatedIdPData.getIdpName()).append("\",");
        jsonBuilder
                .append("\"authenticator\":\"")
                .append(authenticatedIdPData.getAuthenticator().getApplicationAuthenticator()
                        .getName()).append("\"");
        // wrap up the JSON object
        jsonBuilder.append("}");
        jsonBuilder.append("]");

        sequenceConfig
                .setAuthenticatedIdPs(IdentityApplicationManagementUtil.getSignedJWT(jsonBuilder
                        .toString(), sequenceConfig.getApplicationConfig().getServiceProvider()));
        
        mappedAttrs = handleClaimMappings(context);
        String spRoleUri = getSpRoleClaimUri(sequenceConfig.getApplicationConfig());
        String roleAttr = mappedAttrs.get(spRoleUri);

        if (roleAttr != null && roleAttr.trim().length() > 0) {

            String roles[] = roleAttr.split(",");
            mappedAttrs.put(spRoleUri,
                    getServiceProviderMappedUserRoles(sequenceConfig, Arrays.asList(roles)));
        }

        sequenceConfig.setUserAttributes(FrameworkUtils.buildClaimMappings(mappedAttrs));

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
                        spMappedUserRoles.append(role + ",");
                    }
                } else {
                    spMappedUserRoles.append(role + ",");
                }
            }
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

        if (spRoleClaimUri == null) {
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

        if (spRoleClaimUri == null) {
            return FrameworkConstants.LOCAL_ROLE_CLAIM_URI;
        }

        return null;
    }

    /**
     * @param context
     * @return
     * @throws FrameworkException
     */
    protected Map<String, String> handleClaimMappings(AuthenticationContext context)
            throws FrameworkException {

        Map<String, String> mappedAttrs = null;

        try {
            mappedAttrs = FrameworkUtils.getClaimHandler().handleClaimMappings(null, context, null,
                    false);
            return mappedAttrs;
        } catch (FrameworkException e) {
            log.error("Claim handling failed!", e);
        }

        return null;
    }
}
