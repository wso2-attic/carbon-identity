package org.wso2.carbon.identity.application.common.util;

import org.wso2.carbon.identity.application.common.model.FederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnector;
import org.wso2.carbon.identity.application.common.model.ProvisioningProperty;

public class IdentityCommonUtil {

    /**
     * 
     * @param o1
     * @param o2
     * @return
     */
    public static ProvisioningConnector[] concatArrays(ProvisioningConnector[] o1,
            ProvisioningConnector[] o2) {
        ProvisioningConnector[] ret = new ProvisioningConnector[o1.length + o2.length];

        System.arraycopy(o1, 0, ret, 0, o1.length);
        System.arraycopy(o2, 0, ret, o1.length, o2.length);

        return ret;
    }

    /**
     * 
     * @param o1
     * @param o2
     * @return
     */
    public static ProvisioningProperty[] concatArrays(ProvisioningProperty[] o1,
            ProvisioningProperty[] o2) {
        ProvisioningProperty[] ret = new ProvisioningProperty[o1.length + o2.length];

        System.arraycopy(o1, 0, ret, 0, o1.length);
        System.arraycopy(o2, 0, ret, o1.length, o2.length);

        return ret;
    }

    /**
     * 
     * @param o1
     * @param o2
     * @return
     */
    public static FederatedAuthenticator[] concatArrays(FederatedAuthenticator[] o1,
            FederatedAuthenticator[] o2) {
        FederatedAuthenticator[] ret = new FederatedAuthenticator[o1.length + o2.length];

        System.arraycopy(o1, 0, ret, 0, o1.length);
        System.arraycopy(o2, 0, ret, o1.length, o2.length);

        return ret;
    }

}
