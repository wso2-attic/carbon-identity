/*
*  Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.samples.entitlement.pip;

import org.wso2.carbon.identity.entitlement.pip.AbstractPIPAttributeFinder;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Sample PIP attribute finder module that is written for kmarket sample policies. More detail on
 * kmarket sample can be found under Balana sample.
 *
 * We have implement the <code>AbstractPIPAttributeFinder</code> of entitlement package. We need to
 * register this module with entitlement.properties file and drop the jar file in to
 * <IS_HOME>/repository/components/lib directory
 */
public class KmarketPIPAttributeFinder extends AbstractPIPAttributeFinder{


    private Set<String> supportedAttributeIds = new HashSet<String>();

    @Override
    public void init(Properties properties) throws Exception {
        // init this module and init supported attribute ids

        supportedAttributeIds.add("http://kmarket.com/id/role");
    }

    @Override
    public String getModuleName() {
        return "KmarketPIPAttributeFinder";
    }

    @Override
    public Set<String> getSupportedAttributes() {
        return supportedAttributeIds;
    }

    @Override
    public Set<String> getAttributeValues(String subject, String resource, String action,
                                  String environment, String attributeId, String issuer) throws Exception {

        Set<String> roles = new HashSet<String>();
        String role = findRole(subject);
        if(role != null){
            roles.add(role);
        }

        return roles;
    }

    /**
     * Helper method to find role or user
     *
     * @param userName user name as String
     * @return  role name as String
     */
    private String findRole(String userName){

        if(userName.equals("bob")){
            return "kmarket-blue";
        } else if(userName.equals("alice")){
            return "kmarket-silver";
        } else if(userName.equals("peter")){
            return "kmarket-gold";
        }

        return null;
    }
}
