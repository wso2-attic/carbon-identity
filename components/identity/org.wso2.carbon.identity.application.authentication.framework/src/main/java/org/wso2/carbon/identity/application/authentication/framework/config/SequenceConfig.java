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

package org.wso2.carbon.identity.application.authentication.framework.config;

import java.util.Hashtable;
import java.util.Map;

/**
 * Configuration holder for an application
 *
 */
public class SequenceConfig {
    	
	private String name;
	private boolean isForceAuthn;
	private boolean isCheckAuthn;
	private String relyingParty;
	private Map <Integer, StepConfig> stepMap = new Hashtable<Integer, StepConfig>();
	
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

	public String getRelyingParty() {
		return relyingParty;
	}

	public void setRelyingParty(String relyingParty) {
		this.relyingParty = relyingParty;
	}
}