package org.wso2.carbon.identity.application.authentication.framework.context;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.wso2.carbon.identity.application.authentication.framework.config.model.SequenceConfig;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedIdPData;

public class SessionContext implements Serializable {
	
	private static final long serialVersionUID = -5797408810833645408L;
	
	private Map<String, SequenceConfig> authenticatedSequences = new HashMap<String, SequenceConfig>();
	private Map<String, AuthenticatedIdPData> authenticatedIdPs = new HashMap<String, AuthenticatedIdPData>();
    private boolean isRememberMe = false;
	
	public Map<String, SequenceConfig> getAuthenticatedSequences() {
		return authenticatedSequences;
	}

	public void setAuthenticatedSequences(
			Map<String, SequenceConfig> authenticatedSequences) {
		this.authenticatedSequences = authenticatedSequences;
	}

    public Map<String, AuthenticatedIdPData> getAuthenticatedIdPs() {

        return authenticatedIdPs;
    }

    public void setAuthenticatedIdPs(Map<String, AuthenticatedIdPData> authenticatedIdPs) {
        this.authenticatedIdPs = authenticatedIdPs;
    }

    public boolean isRememberMe() {
        return isRememberMe;
    }

    public void setRememberMe(boolean isRememberMe) {
        this.isRememberMe = isRememberMe;
    }
}
