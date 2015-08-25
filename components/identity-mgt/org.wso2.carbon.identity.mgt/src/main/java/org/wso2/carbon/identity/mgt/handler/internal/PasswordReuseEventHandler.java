package org.wso2.carbon.identity.mgt.handler.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.model.IdentityErrorMsgContext;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.mgt.IdentityMgtConfig;
import org.wso2.carbon.identity.mgt.constants.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.dto.UserIdentityClaimsDO;
import org.wso2.carbon.identity.mgt.event.IdentityMgtEvent;
import org.wso2.carbon.identity.mgt.store.UserIdentityDataStore;
import org.wso2.carbon.idp.mgt.IdentityProviderManager;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PasswordReuseEventHandler extends AbstractEventHandler {

    private static final Log log = LogFactory.getLog(PasswordReuseEventHandler.class);

    // save what are the event types registered in this handler
    protected ArrayList<String> registeredEventList;

    @Override
    public void init() {

        registeredEventList = new ArrayList<String>() {{
            add("PRE_UPDATE_CREDENTIAL");
        }};
    }


    @Override
    public boolean isRegistered(IdentityMgtEvent event) {
        HashMap<String, Object> properties = event.getEventProperties();
        IdentityMgtConfig config = (IdentityMgtConfig) properties.get(IdentityMgtConstants.EventProperty.IDENTITY_MGT_CONFIG);

        if (config.isAccountPasswordReuseEnable()) {

            //check whether the event name is exists in the registeredEventList
            for (String eventName : registeredEventList) {
                if (eventName.equals(event.getEventName())) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean handleEvent(IdentityMgtEvent identityMgtEvent) throws UserStoreException {

        String eventName = identityMgtEvent.getEventName();
        HashMap<String, Object> properties = identityMgtEvent.getEventProperties();

        UserIdentityClaimsDO userIdentityDTO = (UserIdentityClaimsDO) properties.get("userIdentityDTO");
        IdentityMgtConfig config = (IdentityMgtConfig) properties.get("identityMgtConfig");
        String credential = (String) properties.get("credential");
        String userName = (String) properties.get("userName");
        UserStoreManager userStoreManager = (UserStoreManager) properties.get("userStoreManager");
        UserIdentityDataStore module = (UserIdentityDataStore) properties.get("module");



        long currentTime = Calendar.getInstance().getTimeInMillis();

        boolean isPasswordReused = false;

        Map<Long, Object> usedPasswordMap = userIdentityDTO.getUsedPasswordMap();


        // password reuse based on time
        List<Long> keyList = new ArrayList<Long>(usedPasswordMap.keySet());
        List<Object> valueList = new ArrayList<Object>(usedPasswordMap.values());
        int iterateCount = keyList.size() - 1;

        if (iterateCount != -1) {
            for (int i = iterateCount; i >= 0; i--) {

                long time = keyList.get(iterateCount);
                String password = (String) valueList.get(iterateCount);

                if ((currentTime - time) <= (config.getPasswordReuseTime()) * 24 * 60 * 1000) {
                    if (password.equals(credential)) {
                        isPasswordReused = true;
                        break;
                    }
                } else {
                    break;
                }

                --iterateCount;

            }
        }


        // password reuse based on frequency
        int count = 1;
        int iterateCount2 = valueList.size() - 1;

        while (count <= config.getPasswordReuseFrequency()) {
            if (iterateCount2 != -1) {
                if (valueList.get(iterateCount2).equals(credential)) {
                    isPasswordReused = true;
                    break;
                } else {
                    --iterateCount2;
                    ++count;
                }
            } else {
                break;
            }

        }

        // check password is reused or not
        // if reused throw an error message
        if (isPasswordReused) {

            IdentityErrorMsgContext customErrorMessageContext = new IdentityErrorMsgContext(UserCoreConstants.ErrorCode.USER_IS_LOCKED,
                    userIdentityDTO.getFailAttempts(), config.getAuthPolicyMaxLoginAttempts());
            IdentityUtil.setIdentityErrorMsg(customErrorMessageContext);
            String errorMsg = "Error in password change: Used same password used before. Please use another password. ";

            throw new UserStoreException(
                    errorMsg);

        }


        return true;
    }
}