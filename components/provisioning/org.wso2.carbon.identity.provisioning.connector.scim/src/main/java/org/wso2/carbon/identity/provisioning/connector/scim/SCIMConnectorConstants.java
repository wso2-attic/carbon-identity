package org.wso2.carbon.identity.provisioning.connector.scim;

public class SCIMConnectorConstants {

    public class PropertyConfig {

        public static final String IDP_NAME_KEY = "identityProviderName";

        public static final String PROVISIONING_PATTERN_KEY = "scim-prov-pattern";
        public static final String PROVISIONING_SEPERATOR_KEY = "scim-prov-separator";
        public static final String PROVISIONING_DOMAIN_KEY = "scim-prov-domainName";

        public static final String DEFAULT_PROVISIONING_PATTERN = "{UN}";
        public static final String DEFAULT_PROVISIONING_SEPERATOR = "_";

        private PropertyConfig(){}

    }
}
