package org.wso2.carbon.identity.application.authentication.framework;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.identity.application.authentication.framework.cache.SessionContextCache;
import org.wso2.carbon.identity.application.authentication.framework.context.SessionContext;
import org.wso2.carbon.identity.application.authentication.framework.model.SessionInfo;

import java.util.ArrayList;

/**
 * Created by lakshani on 3/23/15.
 */
public class SessionManagementService extends AbstractAdmin {
    private static Log log = LogFactory.getLog(SessionManagementService.class);

    public ArrayList<SessionInfo> getAllSessionInfo() {
        return SessionContextCache.getInstance(0).getSessionDetailsFromDbAndCache();
    }

    public void removeSession(String userName) {
        SessionContextCache.getInstance(0).removeSessionDetailsFromDbAndCache(userName);
    }
}
