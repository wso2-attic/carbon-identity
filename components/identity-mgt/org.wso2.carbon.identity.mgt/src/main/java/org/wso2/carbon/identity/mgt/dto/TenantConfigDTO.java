package org.wso2.carbon.identity.mgt.dto;

public class TenantConfigDTO {

    private String property;
    private String propertyValue;

    public TenantConfigDTO(){
    }

    public TenantConfigDTO(String property, String propertyValue){
        this.property = property;
        this.propertyValue = propertyValue;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }
}
