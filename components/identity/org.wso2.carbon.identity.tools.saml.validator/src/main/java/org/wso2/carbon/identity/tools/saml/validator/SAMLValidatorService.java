/*
 * Copyright 2005-2014 WSO2, Inc. (http://wso2.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.tools.saml.validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Response;
import org.opensaml.xml.XMLObject;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;
import org.wso2.carbon.identity.tools.saml.validator.dto.GeneratedResponseDTO;
import org.wso2.carbon.identity.tools.saml.validator.dto.ValidatedItemDTO;
import org.wso2.carbon.identity.tools.saml.validator.processors.SAMLAuthnRequestValidator;
import org.wso2.carbon.identity.tools.saml.validator.processors.SAMLResponseBuilder;
import org.wso2.carbon.identity.tools.saml.validator.util.SAMLValidatorConstants;
import org.wso2.carbon.identity.tools.saml.validator.util.SAMLValidatorUtil;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class SAMLValidatorService {

    private static Log log = LogFactory.getLog(SAMLValidatorService.class);

	public ValidatedItemDTO[] validateAuthnRequest(String samlRequest, boolean isPost) {
		List<ValidatedItemDTO> validatedItems = new ArrayList<ValidatedItemDTO>();
		XMLObject request;
		String decodedRequest = null;
		String queryString = null;

		if (isPost) {
			try {
				decodedRequest = SAMLSSOUtil.decodeForPost(samlRequest);
				validatedItems.add(new ValidatedItemDTO(
				                                        SAMLValidatorConstants.ValidationType.VAL_DECODE,
				                                        true,
				                                        SAMLValidatorConstants.ValidationMessage.VAL_DECODE_SUCCESS));
			} catch (IdentityException e) {
				if (log.isDebugEnabled()) {
					log.debug(e.getMessage());
				}
				validatedItems.add(new ValidatedItemDTO(
				                                        SAMLValidatorConstants.ValidationType.VAL_DECODE,
				                                        false,
				                                        SAMLValidatorConstants.ValidationMessage.VAL_DECODE_FAIL));
			}
		} else {
			boolean isExtractSuccess = false;
			queryString = SAMLValidatorUtil.getQueryString(samlRequest);
			if (queryString != null && !queryString.isEmpty()) {
				try {
					samlRequest = SAMLValidatorUtil.getSAMLRequestFromURL(samlRequest);
					isExtractSuccess = samlRequest != null ? true : false;
				} catch (UnsupportedEncodingException e) {
					if (log.isDebugEnabled()) {
						log.debug(e.getMessage());
					}
					validatedItems.add(new ValidatedItemDTO(
					                                        SAMLValidatorConstants.ValidationType.VAL_DECODE,
					                                        false,
					                                        SAMLValidatorConstants.ValidationMessage.VAL_DECODE_QUERY_STRING_FAIL));
				}
			} else {
				validatedItems.add(new ValidatedItemDTO(
				                                        SAMLValidatorConstants.ValidationType.VAL_DECODE,
				                                        false,
				                                        SAMLValidatorConstants.ValidationMessage.VAL_EXTRACT_SAML_REQ_FAIL));
			}
			if (isExtractSuccess && samlRequest != null) {
				try {

					decodedRequest = SAMLSSOUtil.decode(samlRequest);
					validatedItems.add(new ValidatedItemDTO(
					                                        SAMLValidatorConstants.ValidationType.VAL_DECODE,
					                                        true,
					                                        SAMLValidatorConstants.ValidationMessage.VAL_DECODE_SUCCESS));
				} catch (IdentityException e) {
					if (log.isDebugEnabled()) {
						log.debug(e.getMessage());
					}
					validatedItems.add(new ValidatedItemDTO(
					                                        SAMLValidatorConstants.ValidationType.VAL_DECODE,
					                                        false,
					                                        SAMLValidatorConstants.ValidationMessage.VAL_DECODE_FAIL));
				}
			} else {
				validatedItems.add(new ValidatedItemDTO(
				                                        SAMLValidatorConstants.ValidationType.VAL_DECODE,
				                                        false,
				                                        SAMLValidatorConstants.ValidationMessage.VAL_DECODE_FAIL));
			}
		}

		if (decodedRequest == null) {
			return validatedItems.toArray(new ValidatedItemDTO[validatedItems.size()]);
		} else {
			try {
				request = SAMLSSOUtil.unmarshall(decodedRequest);
				validatedItems.add(new ValidatedItemDTO(
				                                        SAMLValidatorConstants.ValidationType.VAL_UNMARSHAL,
				                                        true,
				                                        SAMLValidatorConstants.ValidationMessage.VAL_UNMARSHAL_SUCCESS));
			} catch (IdentityException e) {
				if (log.isDebugEnabled()) {
					log.debug(e.getMessage());
				}
				validatedItems.add(new ValidatedItemDTO(
				                                        SAMLValidatorConstants.ValidationType.VAL_UNMARSHAL,
				                                        false,
				                                        SAMLValidatorConstants.ValidationMessage.VAL_UNMARSHAL_FAIL));
				if (isPost) {
					if(isDecodeableAsRedirect(samlRequest))
					{
						validatedItems.add(new ValidatedItemDTO(
						                                        SAMLValidatorConstants.ValidationType.VAL_WRONG_BINDING,
						                                        false,
						                                        SAMLValidatorConstants.ValidationMessage.VAL_WRONG_BINDING_MSG));
					}
				}
				return validatedItems.toArray(new ValidatedItemDTO[validatedItems.size()]);
			}

		}

		if (request instanceof AuthnRequest) {
			SAMLAuthnRequestValidator authnRequestValidator =
			                                                  new SAMLAuthnRequestValidator(
			                                                                                (AuthnRequest) request);
			authnRequestValidator.setPost(isPost);
			authnRequestValidator.setQueryString(queryString);
			try {
				authnRequestValidator.validate(validatedItems);
			} catch (IdentityException e) {
				if (log.isDebugEnabled()) {
					log.debug(e.getMessage());
				}
				return validatedItems.toArray(new ValidatedItemDTO[validatedItems.size()]);
			}
		} else {
			validatedItems.add(new ValidatedItemDTO(
			                                        SAMLValidatorConstants.ValidationType.VAL_UNMARSHAL,
			                                        false,
			                                        SAMLValidatorConstants.ValidationMessage.VAL_AUTHN_REQUEST_FAIL));
			return validatedItems.toArray(new ValidatedItemDTO[validatedItems.size()]);
		}

		return validatedItems.toArray(new ValidatedItemDTO[validatedItems.size()]);
	}

	public GeneratedResponseDTO buildResponse(String issuer, String userName) {
		if (issuer == null || issuer.isEmpty() || userName == null || userName.isEmpty()) {
			return new GeneratedResponseDTO(
			                                false,
			                                SAMLValidatorConstants.ErrorMessage.ERROR_INCOMPLETE_DATA);
		}

		SAMLSSOServiceProviderDO ssoIdPConfigs = null;
		String xmlResponse = null;
		String encodeResponse = null;
		try {
			ssoIdPConfigs = SAMLValidatorUtil.getServiceProviderConfig(issuer);
		} catch (IdentityException e) {
			if (log.isDebugEnabled()) {
				log.debug(e.getMessage());
			}
			return new GeneratedResponseDTO(
			                                false,
			                                String.format(SAMLValidatorConstants.ErrorMessage.ERROR_CONFIG_NOT_AVAIL,
			                                              issuer));
		}
		SAMLResponseBuilder responseBuilder = new SAMLResponseBuilder();
		try {
			Response reponse = responseBuilder.buildSAMLResponse(ssoIdPConfigs, userName);
			if (reponse != null) {
				xmlResponse = SAMLSSOUtil.marshall(reponse);
				encodeResponse = SAMLSSOUtil.encode(xmlResponse);
			}
		} catch (IdentityException e) {
			if (log.isDebugEnabled()) {
				log.debug(e.getMessage());
			}
			return new GeneratedResponseDTO(
			                                false,
			                                String.format(SAMLValidatorConstants.ErrorMessage.ERROR_BUILD_FAIL,
			                                              e.getMessage()));
		}
		return new GeneratedResponseDTO(true, null, xmlResponse, encodeResponse);
	}

	public String[] getIssuersOfSAMLServiceProviders() {
		try {
			return SAMLValidatorUtil.getIssuersOfSAMLServiceProviders();
		} catch (IdentityException e) {
			if (log.isDebugEnabled()) {
				log.debug(e.getMessage());
			}
		}
		return null;
	}

	private boolean isDecodeableAsRedirect(String samlRequest) {
		String decodedRequest = null;
		try {
			decodedRequest = SAMLSSOUtil.decode(samlRequest);
		} catch (IdentityException e) {
			return false;
		}
		if (decodedRequest != null && !decodedRequest.isEmpty()) {
			try {
				SAMLSSOUtil.unmarshall(decodedRequest);
				return true;
			} catch (IdentityException e) {
				return false;
			}
		}
		return false;
	}
}
