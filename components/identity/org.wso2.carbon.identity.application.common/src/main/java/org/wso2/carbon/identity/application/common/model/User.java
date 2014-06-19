/*
 *Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.carbon.identity.application.common.model;

import java.io.Serializable;
import java.util.Iterator;

import org.apache.axiom.om.OMElement;

public class User implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -3605277664796682611L;
    private int tenantId;
    private String userStoreDomain;
    private String userName;

    /**
     * 
     * @return
     */
    public int getTenantId() {
        return tenantId;
    }

    /**
     * 
     * @param tenantId
     */
    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * 
     * @return
     */
    public String getUserStoreDomain() {
        return userStoreDomain;
    }

    /**
     * 
     * @param userStoreDomain
     */
    public void setUserStoreDomain(String userStoreDomain) {
        this.userStoreDomain = userStoreDomain;
    }

    /**
     * 
     * @return
     */
    public String getUserName() {
        return userName;
    }

    /**
     * 
     * @param userName
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /*
     * <User> <TenantId></TenantId> <UserStoreDomain></UserStoreDomain> <UserName></UserName>
     * </User>
     */

    public static User build(OMElement userOM) {
        User user = new User();

        if (userOM == null) {
            return user;
        }

        Iterator<?> iter = userOM.getChildElements();
        while (iter.hasNext()) {
            OMElement member = (OMElement) iter.next();
            if (member.getLocalName().equals("TenantId")) {
                if (member.getText() != null) {
                    user.setTenantId(Integer.parseInt(member.getText()));
                }
            } else if (member.getLocalName().equalsIgnoreCase("UserStoreDomain")) {
                user.setUserStoreDomain(member.getText());
            } else if (member.getLocalName().equalsIgnoreCase("UserName")) {
                user.setUserName(member.getText());
            }
        }
        return user;

    }

}
