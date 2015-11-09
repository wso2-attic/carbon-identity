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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.application.authentication.framework.inbound;

import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;

public abstract class InboundAuthenticationRequestProcessor {

    /**
     *
     * @param authenticationRequest
     * @return
     * @throws FrameworkException
     */
    public abstract InboundAuthenticationResponse process(InboundAuthenticationRequest authenticationRequest)
    throws FrameworkException;

    /**
     *
     * @return
     */
    public abstract String getName();

    /**
     *
     * @return
     */
    public abstract String getCallbackPath(InboundAuthenticationContext context) throws FrameworkException;

    /**
     *
     * @return
     */
    public abstract String getRelyingPartyId();

    /**
     *
     * @return
     */
    public abstract int getPriority();

    /**
     *
     * @return
     */
    public abstract boolean canHandle(InboundAuthenticationRequest authenticationRequest) throws FrameworkException;

    /**
     *
     * @return
     */
    public abstract boolean isDirectResponseRequired();

}
