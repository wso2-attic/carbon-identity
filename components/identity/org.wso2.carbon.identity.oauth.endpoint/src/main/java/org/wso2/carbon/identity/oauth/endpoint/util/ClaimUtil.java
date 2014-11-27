package org.wso2.carbon.identity.oauth.endpoint.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.claim.mgt.ClaimManagerHandler;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.oauth.endpoint.user.UserInfoEndpointException;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO;
import org.wso2.carbon.user.api.Claim;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.*;

public class ClaimUtil {
    private static Log log = LogFactory.getLog(ClaimUtil.class);
    final static String spDialect = "http://wso2.org/oidc/claim";

    public static Map<String, Object> getClaimsFromUserStore(OAuth2TokenValidationResponseDTO tokenResponse) throws Exception {
        String username = tokenResponse.getAuthorizedUser();
        String tenantUser = MultitenantUtils.getTenantAwareUsername(username);
        String tenantDomain = MultitenantUtils.getTenantDomain(tokenResponse.getAuthorizedUser());
        UserRealm realm;
        Claim claims[];
        List<String> claimURIList = new ArrayList<String>();
        Map<String, Object> mappedAppClaims = new HashMap<String, Object>();

        try {
            realm = IdentityTenantUtil.getRealm(tenantDomain, username);

            if (realm == null) {
                log.warn("No valid tenant domain provider. Empty claim returned back");
                return new HashMap<String, Object>();
            }

            claims = realm.getUserStoreManager().getUserClaimValues(tenantUser, null);
            Map<String, String> spToLocalClaimMappings;

            UserStoreManager userstore = realm.getUserStoreManager();

            // need to get all the requested claims
            Map<String, String> requestedLocalClaimMap = ClaimManagerHandler.getInstance()
                    .getMappingsMapFromOtherDialectToCarbon(spDialect, null, tenantDomain, true);
            if (requestedLocalClaimMap != null && requestedLocalClaimMap.size() > 0) {
                for (Iterator<String> iterator = requestedLocalClaimMap.keySet().iterator(); iterator
                        .hasNext(); ) {
                    claimURIList.add(iterator.next());

                }
                if (log.isDebugEnabled()) {
                    log.debug("Requested number of local claims: " + claimURIList.size());
                }

                spToLocalClaimMappings = ClaimManagerHandler.getInstance()
                        .getMappingsMapFromOtherDialectToCarbon(spDialect, null,
                                tenantDomain, false);

                Map<String, String> userClaims = userstore.getUserClaimValues(
                        MultitenantUtils.getTenantAwareUsername(username),
                        claimURIList.toArray(new String[claimURIList.size()]), null);
                if (log.isDebugEnabled()) {
                    log.debug("User claims retrieved from user store: " + userClaims.size());
                }

                if (userClaims == null || userClaims.size() == 0) {
                    return new HashMap<String, Object>();
                }

                for (Iterator<Map.Entry<String, String>> iterator = spToLocalClaimMappings.entrySet().iterator(); iterator.hasNext(); ) {
                    Map.Entry<String, String> entry = iterator.next();
                    String value = userClaims.get(entry.getValue());
                    if (value != null) {
                        mappedAppClaims.put(entry.getKey(), value);
                        if (log.isDebugEnabled()) {
                            log.debug("Mapped claim: key -  " + entry.getKey() + " value -" + value);
                        }
                    }
                }
            }

        } catch (Exception e) {
            throw new UserInfoEndpointException(e.getMessage());
        }
        return mappedAppClaims;
    }
}
