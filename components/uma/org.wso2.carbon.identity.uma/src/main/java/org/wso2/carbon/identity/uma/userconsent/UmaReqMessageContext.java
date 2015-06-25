package org.wso2.carbon.identity.uma.userconsent;

import org.wso2.carbon.identity.uma.model.UmaRptRequest;
import org.wso2.carbon.identity.uma.model.UmaRptResponse;

public class UmaReqMessageContext {

    private UmaRptRequest umaRptRequest;

    private UmaRptResponse umaRptResponse;

    private String authorizedUser;

    private int tenantID;

    private long validityPeriod;


}
