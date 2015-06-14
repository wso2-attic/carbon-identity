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

import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.packet.Presence;

import java.util.Collection;

public class XmppSubscriptionListener implements RosterListener {

    String xmppUserId;
    boolean isOnline = false;
    boolean isAccepted = false;

    /**
     * @param xmppUserId
     */
    public XmppSubscriptionListener(String xmppUserId) {
        this.xmppUserId = xmppUserId;
    }

    /**
     * @return
     */
    public boolean isAccepted() {
        return isAccepted;
    }

    /**
     * @param strings
     */
    @Override
    public void entriesAdded(Collection<String> strings) {
        if (strings.contains(xmppUserId)) {
            isAccepted = true;
        }

    }

    /**
     * @return
     */
    public boolean isOnline() {
        return isOnline;
    }

    /**
     * @param strings
     */
    @Override
    public void entriesUpdated(Collection<String> strings) {
        // Nothing to implement
    }

    /**
     * @param strings
     */
    @Override
    public void entriesDeleted(Collection<String> strings) {
        // Nothing to implement
    }

    /**
     * Listennig for a change in presence of the user, eg: unavailable => available
     *
     * @param presence
     */
    @Override
    public void presenceChanged(Presence presence) {
        String user = presence.getFrom();
        if (user.contains(xmppUserId) && presence.isAvailable()) {
            isOnline = true;
        }
    }
}
