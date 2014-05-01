/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.identity.provisioning;

/**
 *  Identity provision related constants
 */
public class IdentityProvisioningConstants {

    public static final String IDENTITY_PROVISIONING_REG_PATH =
            "/repository/components/org.wso2.carbon.identity.provisioning";

    public class PropertyConfig{
        public static final String CONFIG_FILE_NAME = "identity-provision.properties";
        public static final String IDENTITY_PROVISION_ENABLE = "Identity.Provisioning.Enable";
        
        public static final String IDENTITY_PROVISIONING_REGISTORED_CONNECTORS = "Identity.Provisioning.Registored.Connectors";
        public static final String IDENTITY_PROVISIONING_CONNECTOR_NAME = "Identity.Provisioning.Connector.Name";
        
        public static final String PREFIX_IDENTITY_PROVISIONING_CONNECTOR = "Identity.Provisioning.Connector.";
        public static final String PREFIX_IDENTITY_PROVISIONING_CONNECTOR_ENABLE = "Identity.Provisioning.Connector.Enable.";
        public static final String PREFIX_IDENTITY_PROVISIONING_CONNECTOR_CLASS = "Identity.Provisioning.Connector.Class.";
        
        public static final String DELIMATOR = ",";
    }
}
