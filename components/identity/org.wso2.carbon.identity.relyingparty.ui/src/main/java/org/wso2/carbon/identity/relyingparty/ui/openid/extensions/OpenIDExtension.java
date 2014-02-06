/*
 * Copyright 2005-2008 WSO2, Inc. (http://wso2.com)
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

package org.wso2.carbon.identity.relyingparty.ui.openid.extensions;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openid4java.message.MessageExtension;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.relyingparty.stub.dto.ClaimDTO;
import org.wso2.carbon.identity.relyingparty.ui.openid.OpenIDAuthenticationRequest;

public interface OpenIDExtension {

	/**
	 * Creates an instance of MessageExtension for the OpenID authentication request
	 * @param request OpenID authentication request
	 * @return An instance of MessageExtension
	 * @throws RelyingPartyException
	 */
	public MessageExtension getMessageExtension(OpenIDAuthenticationRequest request)
			throws IdentityException;

	/**
	 * Set session attributes with the received OpenID response
	 * @param claimList List<ClaimDTO>
	 * @throws RelyingPartyException
	 */
	public void setSessionAttributes(List<ClaimDTO> claimList) throws IdentityException;

}
