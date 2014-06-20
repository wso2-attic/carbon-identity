/*
*  Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.identity.application.authentication.framework.config.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.wso2.carbon.identity.application.common.model.ClaimMapping;

/**
 * Configuration holder for an application
 *
 */
public class SequenceConfig implements Serializable {
    	
	private static final long serialVersionUID = 3316149796008510127L;
	
	private String name;
	private boolean isForceAuthn;
	private boolean isCheckAuthn;
	private String applicationId;
	private Map <Integer, StepConfig> stepMap = new Hashtable<Integer, StepConfig>();
	private List<AuthenticatorConfig> reqPathAuthenticators = new ArrayList<AuthenticatorConfig>();
	private ApplicationConfig applicationConfig = null;
	private boolean completed;
	
	private String authenticatedUser;
    private Map<ClaimMapping, String> userAttributes = new HashMap<ClaimMapping, String>();
	private String authenticatedIdPs;
	
	private AuthenticatorConfig authenticatedReqPathAuthenticator;
	
	public SequenceConfig() {}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public Map<Integer, StepConfig> getStepMap() {
		return stepMap;
	}

	public void setStepMap(Map<Integer, StepConfig> stepMap) {
		this.stepMap = stepMap;
	}

    public boolean isForceAuthn() {
        return isForceAuthn;
    }

    public void setForceAuthn(boolean isForceAuthn) {
        this.isForceAuthn = isForceAuthn;
    }

    public boolean isCheckAuthn() {
        return isCheckAuthn;
    }

    public void setCheckAuthn(boolean isCheckAuthn) {
        this.isCheckAuthn = isCheckAuthn;
    }

	public List<AuthenticatorConfig> getReqPathAuthenticators() {
		return reqPathAuthenticators;
	}

	public void setReqPathAuthenticators(
			List<AuthenticatorConfig> reqPathAuthenticators) {
		this.reqPathAuthenticators = reqPathAuthenticators;
	}

	public ApplicationConfig getApplicationConfig() {
		return applicationConfig;
	}

	public void setApplicationConfig(ApplicationConfig applicationConfig) {
		this.applicationConfig = applicationConfig;
	}

	public String getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getAuthenticatedUser() {
        return authenticatedUser;
    }

    public Map<ClaimMapping, String> getUserAttributes() {
        return userAttributes;
    }

    public void setAuthenticatedUser(String authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    public void setUserAttributes(Map<ClaimMapping, String> userAttributes) {
        this.userAttributes = userAttributes;
    }

    public void setAuthenticatedIdPs(String authenticatedIdPs) {
        this.authenticatedIdPs = authenticatedIdPs;
    }

    public String getAuthenticatedIdPs() {
        return authenticatedIdPs;
    }

    public AuthenticatorConfig getAuthenticatedReqPathAuthenticator() {
        return authenticatedReqPathAuthenticator;
    }

    public void setAuthenticatedReqPathAuthenticator(
            AuthenticatorConfig authenticatedReqPathAuthenticator) {
        this.authenticatedReqPathAuthenticator = authenticatedReqPathAuthenticator;
    }
}