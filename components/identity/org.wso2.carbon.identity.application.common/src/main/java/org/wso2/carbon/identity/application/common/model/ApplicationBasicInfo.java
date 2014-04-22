package org.wso2.carbon.identity.application.common.model;

public class ApplicationBasicInfo {

    private String applicationName;
    private String description;

    /**
     * 
     * @return
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * 
     * @param applicationName
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * 
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * 
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

}
