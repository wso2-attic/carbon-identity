/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.carbon.identity.uma.model;

import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.identity.uma.UMAConstants;
import org.wso2.carbon.identity.uma.beans.protection.ResourceSetDescriptionBean;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class ResourceSetDO {

    private static final String PROPERTY_CREATED_TIME = "CREATED_TIME";

    // resource set keys (mentioned in the spec)
    private static final String PROPERTY_NAME = UMAConstants.OAuthResourceSetRegistration.RESOURCE_SET_NAME;
    private static final String PROPERTY_URI= UMAConstants.OAuthResourceSetRegistration.RESOURCE_SET_URI;
    private static final String PROPERTY_TYPE = UMAConstants.OAuthResourceSetRegistration.RESOURCE_SET_TYPE;
    private static final String PROPERTY_SCOPES = UMAConstants.OAuthResourceSetRegistration.RESOURCE_SET_SCOPES;
    private static final String PROPERTY_ICON_URI = UMAConstants.OAuthResourceSetRegistration.RESOURCE_SET_ICON_URI;


    private String resourceSetId;

    private Timestamp timeCreated;

    private String tokenId;

    private Map<String,Object> metadata = new HashMap<>();


    public ResourceSetDO() {

    }

    /**
     * Constructor to create a ResourceSetDO from a ResourceSetDescriptionBean object
     *
     * @param resourceSetDescriptionBean ResourceSetDescriptionBean is created at run time by JAX-RS with the JSON params
     * passed to the API
     */
    public ResourceSetDO(ResourceSetDescriptionBean resourceSetDescriptionBean) {
        this.setName(resourceSetDescriptionBean.getName());
        this.setURI(resourceSetDescriptionBean.getUri());
        this.setType(resourceSetDescriptionBean.getType());
        this.setScopes(resourceSetDescriptionBean.getScopes());
        this.setIconURI(resourceSetDescriptionBean.getIcon_uri());
    }


    public ResourceSetDO(String name, String URI, String type, String[] scopes, String iconURI) {
        this.setName(name);
        this.setURI(URI);
        this.setType(type);
        this.setScopes(scopes);
        this.setIconURI(iconURI);
    }

    public Timestamp getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(Timestamp timeCreated) {
        this.timeCreated = timeCreated;
    }

    public String getResourceSetId() {
        return resourceSetId;
    }

    public void setResourceSetId(String resourceSetId) {
        this.resourceSetId = resourceSetId;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    // getters for meta data stored in the map
    public String getName() {
        return metadata.containsKey(PROPERTY_NAME) ? (String) metadata.get(PROPERTY_NAME) : null;
    }

    public void setName(String name) {
        if (name != null){
            metadata.put(PROPERTY_NAME, name);
        }
    }

    public String getURI() {
        return metadata.containsKey(PROPERTY_URI) ? (String) metadata.get(PROPERTY_URI) : null;
    }

    public void setURI(String URI) {
        if (URI != null){
            metadata.put(PROPERTY_URI, URI);
        }
    }

    public String getType() {
        return metadata.containsKey(PROPERTY_TYPE) ? (String) metadata.get(PROPERTY_TYPE) : null;
    }

    public void setType(String type) {
        if (type != null){
            metadata.put(PROPERTY_TYPE, type);
        }
    }

    public String[] getScopes() {
        return metadata.containsKey(PROPERTY_SCOPES) ? (String[]) metadata.get(PROPERTY_SCOPES) : null;
    }

    public void setScopes(String[] scopes) {
        if (scopes != null && scopes.length != 0){
            metadata.put(PROPERTY_SCOPES, scopes);
        }
    }

    public String getIconURI() {
        return metadata.containsKey(PROPERTY_ICON_URI) ? (String) metadata.get(PROPERTY_ICON_URI) : null;
    }

    public void setIconURI(String iconURI) {
        if (iconURI != null){
            metadata.put(PROPERTY_ICON_URI, iconURI);
        }
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
