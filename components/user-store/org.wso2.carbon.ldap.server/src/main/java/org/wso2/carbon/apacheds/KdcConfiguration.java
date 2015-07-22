/*
 *
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apacheds;

import org.wso2.carbon.apacheds.impl.ConfigurationConstants;
import org.wso2.carbon.ldap.server.exception.DirectoryServerException;

import static org.wso2.carbon.apacheds.KdcConfiguration.ProtocolType.UDP_PROTOCOL;

/**
 * Class representing the KDC configurations.
 * <KDCServer>
 * <Property name="enabled">true</Property>
 * <Property name="protocol">UDP</Property>
 * <Property name="host">localhost</Property>
 * <Property name="port">8000</Property>
 * </KDCServer>
 */

@SuppressWarnings({"UnusedDeclaration"})
public class KdcConfiguration {

    public static final String TCP = "TCP";
    public static final String UDP = "UDP";
    /**
     * A name given to a KDC server.
     */
    private String kdcName;
    /**
     * Host address which KDC is running.
     */
    private String kdcHostAddress;
    /**
     * Protocol used by KDC to communicate with clients.
     */
    private ProtocolType kdcCommunicationProtocol;
    /**
     * KDC running port.
     */
    private int kdcCommunicationPort = -1;
    /**
     * Connection password.
     */
    private String systemAdminPassword;
    /**
     * Number of maximum possible threads allowed.
     */
    private int numberOfThreads;
    /**
     * Backlog count. Refer apacheds configuration for more information about this.
     */
    private int backLogCount;
    /**
     * Life time of a ticket in milliseconds.
     */
    private long maxTicketLifeTime;
    /**
     * Renewable ticket life time.
     */
    private long maxRenewableLifeTime;
    /**
     * Specified whether, timestamp is required during pre-authentication.
     */
    private boolean preAuthenticateTimeStampRequired = true;
    private PartitionInfo partitionInfo;

    public KdcConfiguration(PartitionInfo partitionInfo) {

        this.partitionInfo = partitionInfo;

        this.kdcCommunicationProtocol = UDP_PROTOCOL;

        this.kdcName = ConfigurationConstants.DEFAULT_KDC_NAME;
        this.kdcHostAddress = ConfigurationConstants.DEFAULT_KDC_HOST_ADDRESS;
        this.systemAdminPassword = ConfigurationConstants.DEFAULT_SYS_ADMIN_PASSWORD;
        this.numberOfThreads = ConfigurationConstants.DEFAULT_NUMBER_OF_THREADS;
        this.backLogCount = ConfigurationConstants.DEFAULT_BACK_LOG_COUNT;
        this.maxTicketLifeTime = ConfigurationConstants.DEFAULT_TICKET_LIFETIME;
        this.maxRenewableLifeTime = ConfigurationConstants.DEFAULT_RENEWABLE_LIFE_TIME;

    }

    public KdcConfiguration() {
        this(null);
    }

    public ProtocolType getKdcCommunicationProtocol() {
        return kdcCommunicationProtocol;
    }

    public void setKdcCommunicationProtocol(String protocolName) throws DirectoryServerException {
        if (protocolName == null) return;

        this.kdcCommunicationProtocol = ProtocolType.getProtocolType(protocolName);
    }

    public void setPartitionInfo(PartitionInfo partitionInfo) {
        this.partitionInfo = partitionInfo;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public void setNumberOfThreads(int numberOfThreads) {
        if (numberOfThreads == -1) return;

        this.numberOfThreads = numberOfThreads;
    }

    public int getBackLogCount() {
        return backLogCount;
    }

    public void setBackLogCount(int backLogCount) {
        if (backLogCount == -1) return;

        this.backLogCount = backLogCount;
    }

    public String getKdcHostAddress() {
        return kdcHostAddress;
    }

    public void setKdcHostAddress(String kdcHostAddress) {
        if (kdcHostAddress == null) return;

        this.kdcHostAddress = kdcHostAddress;
    }

    public String getKdcName() {
        return kdcName;
    }

    public void setKdcName(String kdcName) {
        if (kdcName == null) return;

        this.kdcName = kdcName;
    }

    /**
     * Returns kerberos principle. Should take following form,
     * krbtgt/REALM@REALM
     * E.g :- krbtgt/WSO2.COM@WSO2.COM
     *
     * @return KDC principle name.
     */
    public String getKdcPrinciple() {

        return "krbtgt/" + this.partitionInfo.getRealm().toUpperCase() + "@" +
                this.partitionInfo.getRealm().toUpperCase();
    }

    public String getPrimaryRealm() {
        return this.partitionInfo.getRealm();
    }

    public long getMaxTicketLifeTime() {
        return maxTicketLifeTime;
    }

    public void setMaxTicketLifeTime(long maxTicketLifeTime) {
        if (maxTicketLifeTime == -1) return;

        this.maxTicketLifeTime = maxTicketLifeTime;
    }

    public long getMaxRenewableLifeTime() {
        return maxRenewableLifeTime;
    }

    public void setMaxRenewableLifeTime(long maxRenewableLifeTime) {
        if (maxRenewableLifeTime == -1) return;

        this.maxRenewableLifeTime = maxRenewableLifeTime;
    }

    public int getKdcCommunicationPort() {
        return kdcCommunicationPort;
    }

    public void setKdcCommunicationPort(int kdcCommunicationPort) {
        if (kdcCommunicationPort == -1) return;

        this.kdcCommunicationPort = kdcCommunicationPort;
    }

    public String getSystemAdminPassword() {
        return systemAdminPassword;
    }

    public void setSystemAdminPassword(String systemAdminPassword) {
        if (systemAdminPassword == null) return;

        this.systemAdminPassword = systemAdminPassword;
    }

    /**
     * Gets the base domain name which KDC starts searching for principles.
     * We will always have "Users" sub context appended to this.
     *
     * @return Users subcontext domain name. E.g :- ou=Users,dc=example,dc=com.
     */
    public String getSearchBaseDomainName() {
        return this.partitionInfo.getRootDN();
    }

    public boolean isPreAuthenticateTimeStampRequired() {
        return preAuthenticateTimeStampRequired;
    }

    public void setPreAuthenticateTimeStampRequired(boolean preAuthenticateTimeStampRequired) {
        this.preAuthenticateTimeStampRequired = preAuthenticateTimeStampRequired;
    }

    /**
     * An enumeration to select the protocol type which KDC is going to communicate.
     * At the moment there are only 2 protocols. They are TCP and UDP.
     */
    public enum ProtocolType {
        TCP_PROTOCOL,
        UDP_PROTOCOL;

        public static ProtocolType getProtocolType(String protocolName)
                throws DirectoryServerException {
            if (TCP.equals(protocolName)) {
                return ProtocolType.TCP_PROTOCOL;
            } else if (UDP.equals(protocolName)) {
                return UDP_PROTOCOL;
            } else {
                throw new DirectoryServerException(
                        "Invalid protocol name. Only supported protocols for KDC are TCP and UDP.");
            }
        }
    }

}
