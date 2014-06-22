package org.wso2.carbon.identity.provisioning;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.wso2.carbon.identity.application.common.model.ClaimMapping;

public class ProvisioningUtil {

    /**
     * 
     * @return
     */
    public static List<String> getClaimValues(Map<ClaimMapping, List<String>> attributeMap,
            String claimUri, String userStoreDomainName) {

        List<String> claimValues = new ArrayList<String>();
        for (Map.Entry<ClaimMapping, List<String>> entry : attributeMap.entrySet()) {
            ClaimMapping mapping = entry.getKey();
            if (mapping.getLocalClaim() != null
                    && claimUri.equals(mapping.getLocalClaim().getClaimUri())) {
                claimValues = entry.getValue();
                break;
            }
        }

        if (userStoreDomainName != null) {

            List<String> modifiedClaimValues = new ArrayList<String>();

            for (Iterator<String> iterator = claimValues.iterator(); iterator.hasNext();) {
                String claimValue = iterator.next();
                if (claimValue != null && claimValue.indexOf("/") > 0) {
                    claimValue = claimValue.substring(claimValue.indexOf("/") + 1);
                }

                claimValue = userStoreDomainName + "/" + claimValue;
                modifiedClaimValues.add(claimValue);

            }

            claimValues = modifiedClaimValues;
        }

        return claimValues;
    }

    /**
     * 
     * @param claimUri
     * @param attributeList
     */
    public static void setClaimValue(String claimUri, Map<ClaimMapping, List<String>> attributeMap,
            List<String> attributeList) {

        ClaimMapping clmMapping = null;

        for (Map.Entry<ClaimMapping, List<String>> entry : attributeMap.entrySet()) {
            ClaimMapping mapping = entry.getKey();
            if (mapping.getLocalClaim() != null
                    && claimUri.equals(mapping.getLocalClaim().getClaimUri())) {
                clmMapping = mapping;
                break;
            }
        }

        if (clmMapping != null) {
            attributeMap.put(clmMapping, attributeList);
        }
    }
}
