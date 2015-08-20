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
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromContainsFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.model.XMPPSettingsDO;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import java.util.Collection;

public class MPAuthenticationProvider {

    private static final Log log = LogFactory.getLog(MPAuthenticationProvider.class);

    private String userXmppId;
    private String userPIN;
    private boolean isPINEnabled;
    private boolean pinDisabledResponse = true;

    /**
     * @param xmppSettingsDO
     */
    public MPAuthenticationProvider(XMPPSettingsDO xmppSettingsDO) {
        this.userXmppId = xmppSettingsDO.getXmppUserName().trim();
        this.userPIN = xmppSettingsDO.getUserCode().trim();
        this.isPINEnabled = xmppSettingsDO.isPINEnabled();
    }

    /**
     * Authenticating a particular user
     *
     * @return true if authentication is successful, and false otherwise.
     */
    public boolean authenticate() {
        //creating the connection
        XMPPConnection connection = createConnection();

        //connecting to the server
        boolean connectionStatus = connect(connection);
        if (!connectionStatus) {
            log.info("Failed to connect to the Server");
            return false;
        }

        //signing in
        boolean loginStatus = login(connection);
        if (!loginStatus) {
            log.info("login failed");
            return false;
        }

        Roster roster = connection.getRoster();
        Presence presence = roster.getPresence(userXmppId);

        boolean isAvailable = false;

        if (!presence.isAvailable()) {
            isAvailable = checkforUserAvailability(roster, userXmppId);
        }

        if (!isAvailable) {
            log.info("user is not online");
            return false;
        }

        boolean validation = getUserResponse(connection, userXmppId);

        // Giving a second chance to provide the PIN
        if (!validation && !pinDisabledResponse) {
            validation = getUserResponse(connection, userXmppId);
        }
        return validation;
    }

    /**
     * Create a connection to the XMPP server with the available configuration details given in
     * the identity.xml
     *
     * @return XMPPConnection
     */
    private XMPPConnection createConnection() {
        String xmppServer = IdentityUtil.getProperty(IdentityConstants.ServerConfig.XMPP_SETTINGS_SERVER);
        int xmppPort = Integer.parseInt(IdentityUtil.getProperty(IdentityConstants.ServerConfig.XMPP_SETTINGS_PORT));
        String xmppExt = IdentityUtil.getProperty(IdentityConstants.ServerConfig.XMPP_SETTINGS_EXT);

        ConnectionConfiguration config = new ConnectionConfiguration(xmppServer, xmppPort, xmppExt);
        config.setSASLAuthenticationEnabled(true);
        return new XMPPConnection(config);
    }

    /**
     * Checking whether the user is avaiable online.
     *
     * @param roster
     * @param userName
     * @return
     */
    private boolean isAvailable(Roster roster, String userName) {
        Collection<RosterEntry> entrySet = roster.getEntries();
        for (RosterEntry entry : entrySet) {
            if (entry.getUser().equals(userName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Connecting to the server
     *
     * @param connection
     * @return
     */
    private boolean connect(XMPPConnection connection) {
        for (int i = 0; i < 3; i++) {
            try {
                connection.connect();
                return true;
            } catch (XMPPException ex) {
                log.error("Failed to create connection with the server", ex);
                if (log.isInfoEnabled()) {
                    log.info("Attempting to connect to the server again..");
                }
            }
        }
        return false;
    }

    /**
     * Login to the XMPP server with the credentials specifid in the Identity.xml
     *
     * @param connection
     * @return
     */
    public boolean login(XMPPConnection connection) {
        String userName = IdentityUtil.getProperty(IdentityConstants.ServerConfig.XMPP_SETTINGS_USERNAME);
        String password = IdentityUtil.getProperty(IdentityConstants.ServerConfig.XMPP_SETTINGS_PASSWORD);
        for (int i = 0; i < 3; i++) {
            try {
                connection.login(userName, password, null);
                return true;
            } catch (XMPPException ex) {
                log.error("login failed. Trying again..", ex);
            }
        }
        return false;
    }

    /**
     * Checking whether the user is available in the contact list.
     *
     * @param roster
     * @param userXmppId
     * @return
     */
    public boolean checkforUserAvailability(Roster roster, String userXmppId) {
        boolean isAvailable = false;
        XmppSubscriptionListener listener = new XmppSubscriptionListener(userXmppId);
        roster.addRosterListener(listener);
        Presence presence = roster.getPresence(userXmppId);
        boolean status = isAvailable(roster, userXmppId);

        try {
            if (!status) {
                if (log.isInfoEnabled()) {
                    log.info("The User is not subscribed.");
                }
                boolean contactAddStatus = new XmppSubscriber().addContact(roster, userXmppId);
                if (contactAddStatus) {
                    if (log.isInfoEnabled()) {
                        log.info("User's Contact is added successfully..");
                    }
                } else {
                    if (log.isInfoEnabled()) {
                        log.info("Failed to add contact.");
                    }
                }

                if (!presence.isAvailable()) {
                    while (!listener.isOnline()) {
                        Thread.sleep(5000);
                    }
                }
                isAvailable = true;

            } else {

                if (log.isInfoEnabled()) {
                    log.info("User is not online..");
                }

                while (!listener.isOnline()) {
                    Thread.sleep(5000);
                }
                isAvailable = true;
            }
        } catch (InterruptedException e) {
            log.error("Error while checking user availability", e);
        } finally {
            roster.removeRosterListener(listener);
        }

        return isAvailable;
    }

    /**
     * Read the PIN number sent by the user as the reply.
     *
     * @param connection
     * @param userName
     * @return
     */
    public boolean getUserResponse(XMPPConnection connection, String userName) {

        String response = null;
        Presence presence = connection.getRoster().getPresence(userName);

        if (presence.isAvailable()) {
            try {

                ChatManager chatManager = connection.getChatManager();
                Chat chat = chatManager.createChat(userName, null);
                PacketFilter filter =
                        new AndFilter(new PacketTypeFilter(Message.class), new FromContainsFilter(userName));
                XmppResponseListener chatListener = new XmppResponseListener();
                connection.addPacketListener(chatListener, filter);

                if (isPINEnabled) {

                    chat.sendMessage("Please reply with your PIN Number here.");

                    if (log.isInfoEnabled()) {
                        log.info("User PIN is sent to the user and awaiting for the response.");
                    }

                    while (!chatListener.isResponseReceived()) {
                        Thread.sleep(100);
                    }

                    response = chatListener.getResponse();

                    if (response != null) {
                        return userPIN.contentEquals(response.trim());
                    }
                } else {
                    chat.sendMessage(
                            "You are about to get authenticated for your OpenID. Do you want to continue: [Yes] or [No]");

                    if (log.isInfoEnabled()) {
                        log.info("User PIN is sent to the user and awaiting for the response.");
                    }

                    while (!chatListener.isResponseReceived()) {
                        Thread.sleep(100);
                    }

                    response = chatListener.getResponse();

                    if (response != null) {
                        if ("YES".equalsIgnoreCase(response.trim())) {
                            return true;
                        } else if ("NO".equalsIgnoreCase(response.trim())) {
                            return false;
                        } else {
                            pinDisabledResponse = false;
                            return false;
                        }
                    }
                }

            } catch (Exception e) {
                log.error("Error while getting user response", e);
            }
        } else {
            return false;
        }
        return false;

    }
}
