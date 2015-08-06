package org.wso2.carbon.identity.oidcdiscovery;

/**
 * Created by chamara on 7/27/15.
 */
public interface OIDProviderResponseBuilder {
    public String getOIDProviderConfigString(OIDProviderConfigDTO oidProviderConfig) throws
            OIDCDiscoveryEndPointException;

}
