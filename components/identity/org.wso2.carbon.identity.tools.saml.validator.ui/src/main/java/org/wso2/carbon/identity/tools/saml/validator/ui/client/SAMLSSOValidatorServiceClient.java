package org.wso2.carbon.identity.tools.saml.validator.ui.client;

import java.rmi.RemoteException;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.tools.saml.validator.stub.IdentitySAMLValidatorServiceStub;
import org.wso2.carbon.identity.tools.saml.validator.stub.types.GeneratedResponseDTO;
import org.wso2.carbon.identity.tools.saml.validator.stub.types.ValidatedItemDTO;

public class SAMLSSOValidatorServiceClient {

	private static Log log = LogFactory.getLog(SAMLSSOValidatorServiceClient.class);

	private IdentitySAMLValidatorServiceStub stub;

	public SAMLSSOValidatorServiceClient(String cookie, String backendServerURL,
	                                     ConfigurationContext configCtx) throws AxisFault {
		try {
			String serviceURL = backendServerURL + "IdentitySAMLValidatorService";
			stub = new IdentitySAMLValidatorServiceStub(configCtx, serviceURL);
			ServiceClient client = stub._getServiceClient();
			Options option = client.getOptions();
			option.setManageSession(true);
			option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
		} catch (AxisFault ex) {
			throw new AxisFault("Error generating stub for IdentitySAMLValidatorService", ex);
		}
	}

	/**
	 * Validate SAML request
	 * @param samlRequest
	 * @param isPost
	 * @return ValidatedItemDTO array
	 * @throws AxisFault
	 */
	public ValidatedItemDTO[] validate(String samlRequest, boolean isPost) throws AxisFault {
		try {
			return stub.validateAuthnRequest(samlRequest, isPost);
		} catch (RemoteException e) {
			log.error("Error validating SAML request", e);
			throw new AxisFault(e.getMessage(), e);
		}
	}

	/**
	 * Build response for configuration
	 * @param issuer
	 * @param userName
	 * @return encoded & XML response
	 * @throws AxisFault
	 */
	public GeneratedResponseDTO buildResponse(String issuer, String userName) throws AxisFault {
		try {
			return stub.buildResponse(issuer, userName);
		} catch (RemoteException e) {
			log.error("Error building response", e);
			throw new AxisFault(e.getMessage(), e);
		}
	}

	/**
	 * Get Issuer List
	 * @return
	 * @throws AxisFault
	 */
	public String[] getIssuersOfSAMLServiceProviders() throws AxisFault {
		try {
			return stub.getIssuersOfSAMLServiceProviders();
		} catch (RemoteException e) {
			log.error("Error loading Issuers", e);
			throw new AxisFault(e.getMessage(), e);
		}
	}

}
