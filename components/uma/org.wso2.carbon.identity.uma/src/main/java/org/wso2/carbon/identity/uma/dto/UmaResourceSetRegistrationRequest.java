/*
 *
 *  *
 *  * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *  * /
 *
 */

package org.wso2.carbon.identity.uma.dto;

import org.wso2.carbon.identity.uma.beans.protection.ResourceSetDescriptionBean;

import javax.servlet.http.HttpServletRequest;

public class UmaResourceSetRegistrationRequest extends UmaRequest{

    private ResourceSetDescriptionBean resourceSetDescription;
    private String resourceId;


    public UmaResourceSetRegistrationRequest(HttpServletRequest httpServletRequest){
        super(httpServletRequest);
    }

    public UmaResourceSetRegistrationRequest
            (HttpServletRequest httpServletRequest, ResourceSetDescriptionBean resourceSetDescriptionBean) {
        super(httpServletRequest);
        this.resourceSetDescription = resourceSetDescriptionBean;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public ResourceSetDescriptionBean getResourceSetDescription() {
        return resourceSetDescription;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResourceSetName(){
        return resourceSetDescription!= null ? resourceSetDescription.getName() : null;
    }

    public String getResourceSetType(){
        return resourceSetDescription!= null ? resourceSetDescription.getType() : null;
    }

    public String getResourceSetURI(){
        return resourceSetDescription != null ? resourceSetDescription.getUri() : null;
    }

    public String[] getResourceSetScopes(){
        return resourceSetDescription != null ? resourceSetDescription.getScopes() : null;
    }

    public String getResouceSetIconURI(){
        return resourceSetDescription != null ? resourceSetDescription.getIcon_uri() : null;
    }
}
