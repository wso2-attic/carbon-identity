/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.sso.saml.ui.session.mgt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used to maintain a session map at the SSO FE. This class is introduced
 *  to get rid of the session usage to hold the meta information of a incoming authentication
 *  request.  This class implements Singleton since there can be only one session manager.
 */
public class FESessionManager {

    private static FESessionManager sessionManager = new FESessionManager();
    private static SecureRandom secureRandomInstance;
    private static MessageDigest messageDigest;
    private static Log log = LogFactory.getLog(FESessionManager.class);

    public ConcurrentHashMap<String, FESessionBean> sessionMap;

    static{
        initialize();    
    }

    /**
     * Initialize the FESessionManager class. Create the instances of SecureRandom and
     * MessageDigest and keep them as static references.
     */
    private static void initialize(){
        try {
            secureRandomInstance = SecureRandom.getInstance("SHA1PRNG");
            messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            log.error("Error when initializing the SAML2 SSO FESessionManager.", e);
            throw new RuntimeException(e);
        }
    }

    private FESessionManager(){
        this.sessionMap = new ConcurrentHashMap<String, FESessionBean>();
    }

    public static FESessionManager getInstance(){
        if(sessionManager == null){
            sessionManager = new FESessionManager();
        }
        return sessionManager;
    }

    /**
     * Get the corresponding FESessionBean for a particular session id
     * @param sessionID session id
     * @return  FESessionBean
     */
    public FESessionBean getFESessionBean(String sessionID){
        if(sessionMap.containsKey(sessionID)){
            return sessionMap.get(sessionID);
        }
        return null;
    }

    /**
     * Add a new session bean object to the session map
     * @param sessionBean
     * @return created session id
     */
    public String addNewSession(FESessionBean sessionBean){
        String sessionId = generateSessionId();
        sessionMap.put(sessionId, sessionBean);
        return sessionId;
    }

    /**
     * Remove an existing session.
     * @param sessionId session id                                     
     */
    public void removeSession(String sessionId){
        if(sessionMap.containsKey(sessionId)){
            sessionMap.remove(sessionId);
        }
        else{
            log.warn("The session bean with the ID : " + sessionId + "is not available in the session map");
        }
    }

    /**
     * Generate a session id to store the FE session beans
     * @return  generated session id
     */
    private String generateSessionId(){
        //generate the random number
        String randomNum = new Integer(secureRandomInstance.nextInt()).toString();
        //get its digest
        byte[] result = messageDigest.digest(randomNum.getBytes());
        return hexEncode(result);    
    }

    /**
     * The byte[] returned by MessageDigest does not have a nice
     * textual representation, so some form of encoding is usually performed.
     * @param digestValue digested bite array
     * @return Encoded string
     */
    private String hexEncode(byte[] digestValue) {
        StringBuilder result = new StringBuilder();
        char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        for (int idx = 0; idx < digestValue.length; ++idx) {
            byte b = digestValue[idx];
            result.append(digits[(b & 0xf0) >> 4]);
            result.append(digits[b & 0x0f]);
        }
        return result.toString();
    }

}
