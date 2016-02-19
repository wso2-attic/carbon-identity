package org.wso2.carbon.identity.analytic.authn;

import org.wso2.carbon.identity.analytic.authn.model.AuthData;
import org.wso2.carbon.identity.analytic.authn.model.AuthnStep;
import org.wso2.carbon.identity.application.authentication.framework.config.model.SequenceConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.model.StepConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.handler.request.impl.DefaultAuthenticationRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

public class AnalyticAuthnRequestHandler extends DefaultAuthenticationRequestHandler {

    private AuthnDataPublisher publisher;


    public AnalyticAuthnRequestHandler() {

        this.publisher = new AuthnDataPublisher();
    }

    @Override
    protected void doPostAuthentication(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationContext context) {

        SequenceConfig sequenceConfig = context.getSequenceConfig();
        String authenticatedUserTenantDomain = (String) context.getProperties().get("user-tenant-domain");


        AuthData authData = new AuthData();
        authData.setId(UUID.randomUUID().toString());
        authData.setServiceProvider(context.getServiceProviderName());
        authData.setRemoteIp(request.getRemoteHost());
        authData.setAuthType(context.getRequestType());
        authData.setUsername(sequenceConfig.getAuthenticatedUser().getUserName());
        authData.setUserstoreDomain(sequenceConfig.getAuthenticatedUser().getUserStoreDomain());
        authData.setTenantDomain(sequenceConfig.getAuthenticatedUser().getTenantDomain());
        authData.setAuthnSuccess(context.isRequestAuthenticated());

        Map<Integer, StepConfig> stepMap = sequenceConfig.getStepMap();

        if (stepMap != null) {
            AuthnStep[] authnSteps = new AuthnStep[stepMap.size()];
            int i = 0;
            for (Map.Entry<Integer, StepConfig> stepConfigEntry : stepMap.entrySet()) {
                AuthnStep authnStep = new AuthnStep();
                authnStep.setAuthenticator(stepConfigEntry.getValue().getAuthenticatedAutenticator().getName());
                authnStep.setIdp(stepConfigEntry.getValue().getAuthenticatedIdP());
                authnStep.setAuthnStepSuccess(stepConfigEntry.getValue().isCompleted());
                authnStep.setStepNumber(stepConfigEntry.getKey());
                authnSteps[i++] = authnStep;
            }
            authData.setAuthnSteps(authnSteps);
        } else {
            authData.setAuthnSteps(new AuthnStep[0]);
        }

        publisher.publishAuthNSuccess(authData);

        super.doPostAuthentication(request, response, context);
    }
}
