package org.wso2.carbon.identity.sts.passive.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.TokenStorage;
import org.wso2.carbon.identity.core.util.IdentityUtil;

public class PassiveSTSUtil {

    private static Log log = LogFactory.getLog(PassiveSTSUtil.class);
    private static TokenStorage tokenStorage;

    public static TokenStorage getTokenStorage() {
        return tokenStorage;
    }

    static {

        String tokenStoreClassName = IdentityUtil.getProperty("PassiveSTS.TokenStoreClassName");

        if (StringUtils.isNotBlank(tokenStoreClassName)) {
            try {
                Class clazz = Thread.currentThread().getContextClassLoader().loadClass(tokenStoreClassName);
                tokenStorage = (TokenStorage) clazz.newInstance();

                if (log.isDebugEnabled()) {
                    log.debug("Passive STS token storage set to: " + tokenStoreClassName);
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {

                log.error("Error while initiating Passive STS token storage " + tokenStoreClassName + ". Using the " +
                          "default token store: NoStorageTokenStore", e);

                tokenStorage = new NoPersistenceTokenStore();
            }
        } else {
            tokenStorage = new NoPersistenceTokenStore();
        }
    }
}

