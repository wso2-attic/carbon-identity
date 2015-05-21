package org.wso2.carbon.identity.mgt.event;

import java.util.HashMap;

public class IdentityMgtEvent {


    private String eventName;

    private HashMap<String, Object> eventProperties;

    public IdentityMgtEvent(String eventName) {

        this.eventName = eventName;
        eventProperties = new HashMap<String, Object>();

    }

    public IdentityMgtEvent(String eventName, HashMap<String, Object> eventProperties) {

        this.eventName = eventName;
        this.eventProperties = eventProperties;

    }

    public HashMap<String, Object> getEventProperties() {
        return eventProperties;
    }

    public String getEventName() {
        return eventName;
    }

    public void addEventProperty(String key, String value) {
        eventProperties.put(key, value);
    }
}

