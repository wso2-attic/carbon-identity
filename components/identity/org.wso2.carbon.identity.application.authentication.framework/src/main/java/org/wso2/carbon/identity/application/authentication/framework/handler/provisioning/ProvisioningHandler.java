package org.wso2.carbon.identity.application.authentication.framework.handler.provisioning;

import org.wso2.carbon.identity.application.authentication.framework.config.dto.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;

import java.util.List;
import java.util.Map;

public interface ProvisioningHandler {
	
	/**
	 * @param context
	 * @param externalIdPConfig
	 * @param roles
	 * @param subject
	 * @param attributes
	 * @throws FrameworkException
	 */
	public void handle(AuthenticationContext context, ExternalIdPConfig externalIdPConfig, 
			List<String> roles, String subject, Map<String, String> attributes)
			throws FrameworkException;
}
