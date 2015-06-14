/*
 * Copyright (c) 2005-2007, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.provider.xmpp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPException;

public class XmppSubscriber {

    private static final Log log = LogFactory.getLog(MPAuthenticationProvider.class);

    /**
     * Add the user to the contact list, if he is not added yet.
     *
     * @param roster
     * @param userXmppId
     * @return
     */
    public boolean addContact(Roster roster, String userXmppId) {
        XmppSubscriptionListener listener = new XmppSubscriptionListener(userXmppId);
        roster.addRosterListener(listener);

        // Send a invitation to the user and wait until he accepts it.
        try {
            roster.createEntry(userXmppId, "test", null);
            Thread.sleep(5000);
            while (!listener.isAccepted()) {
                Thread.sleep(5000);
            }

        } catch (XMPPException e) {
            log.error("Failed to add contact", e);
            return false;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;

    }
}
