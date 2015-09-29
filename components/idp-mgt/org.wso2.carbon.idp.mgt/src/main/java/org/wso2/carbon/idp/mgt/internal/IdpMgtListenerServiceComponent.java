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

package org.wso2.carbon.idp.mgt.internal;

import org.wso2.carbon.idp.mgt.listener.IdentityProviderMgtListener;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * @scr.component name="org.wso2.carbon.idp.mgt.listener" immediate="true"
 * @scr.reference name="idp.mgt.event.listener.service"
 * interface="org.wso2.carbon.idp.mgt.listener.IdentityProviderMgtListener"
 * cardinality="0..n" policy="dynamic"
 * bind="setIdentityProviderMgtListenerService"
 * unbind="unsetIdentityProviderMgtListenerService"
 */
public class IdpMgtListenerServiceComponent {

    private static Map<Integer, IdentityProviderMgtListener> idpMgtListeners;
    private static Comparator<Integer> idpMgtListenerComparator = new Comparator<Integer>(){

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(Integer orderId1, Integer orderId2) {
            if (orderId1 > orderId2) {
                return 1;
            } else if (orderId1 < orderId2) {
                return -1;
            } else {
                return 0;
            }
        }
    };

    protected static synchronized void setIdentityProviderMgtListenerService(
            IdentityProviderMgtListener applicationMgtListenerService) {
        if (idpMgtListeners == null) {
            idpMgtListeners = new TreeMap<>(idpMgtListenerComparator);
        }
        idpMgtListeners.put(applicationMgtListenerService.getExecutionOrderId(),
                applicationMgtListenerService);
    }

    protected static synchronized void unsetIdentityProviderMgtListenerService(
            IdentityProviderMgtListener applicationMgtListenerService) {
        if (applicationMgtListenerService != null &&
                idpMgtListeners != null) {
            idpMgtListeners = null;
        }
    }

    public static synchronized Collection<IdentityProviderMgtListener> getIdpMgtListeners() {
        if (idpMgtListeners == null) {
            idpMgtListeners = new TreeMap<>(idpMgtListenerComparator);
        }
        return idpMgtListeners.values();
    }
}
