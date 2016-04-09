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

package org.wso2.carbon.identity.uma.dto;

import com.google.gson.Gson;
import org.wso2.carbon.identity.uma.UMAConstants;

import java.util.List;

public class UmaResourceSetRegResponse extends UmaResponse{

    protected UmaResourceSetRegResponse(int responseStatus) {
        super(responseStatus);
    }

    public static UmaResourceSetRegRespBuilder status(int code) {
        return new UmaResourceSetRegRespBuilder(code);
    }

    public static class UmaResourceSetRegRespBuilder extends UmaResponse.UmaResponseBuilder {

        List<String> resourceSetIds = null;

        public UmaResourceSetRegRespBuilder(int responseCode) {
            super(responseCode);
        }

        public UmaResourceSetRegRespBuilder setResourceSetIds(List<String> resourceSetIds) {
            this.resourceSetIds = resourceSetIds;
            return this;
        }

        public UmaResourceSetRegRespBuilder setResourceSetId(String resourceSetId){
            this.setParam(UMAConstants.OAuthResourceSetRegistration.RESOURCE_SET_ID, resourceSetId);
            return this;
        }

        public UmaResourceSetRegRespBuilder setResourceSetName(String resourceSetName){
            this.setParam(UMAConstants.OAuthResourceSetRegistration.RESOURCE_SET_NAME, resourceSetName);
            return this;
        }

        public UmaResourceSetRegRespBuilder setResourceType(String resourceType){
            this.setParam(UMAConstants.OAuthResourceSetRegistration.RESOURCE_SET_TYPE, resourceType);
            return this;
        }



        @Override
        public UmaResponse buildJSONResponse() {

            // if the resourceSetId list is not null we are building a response to return a json array of ids
            if (resourceSetIds != null) {

                UmaResponse umaResponse = new UmaResponse(this.responseCode);

                // get the parameters from the map and build the json string for parameters
                String body = new Gson().toJson(this.resourceSetIds);
                umaResponse.setBody(body);

                // create the response and set the body
                return umaResponse;

            } else {
                return super.buildJSONResponse();
            }
        }
    }
}
