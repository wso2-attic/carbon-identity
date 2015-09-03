/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
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

package org.wso2.carbon.identity.workflow.mgt.bean;

public class WorkflowEventDTO {

    private String eventId;
    private String eventFriendlyName;
    private String eventDescription;
    private String eventCategory;
    private ParameterDTO[] parameterDTOs;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventFriendlyName() {
        return eventFriendlyName;
    }

    public void setEventFriendlyName(String eventFriendlyName) {
        this.eventFriendlyName = eventFriendlyName;
    }

    public String getEventCategory() {
        return eventCategory;
    }

    public void setEventCategory(String eventCategory) {
        this.eventCategory = eventCategory;
    }

    public ParameterDTO[] getParameterDTOs() {
        return parameterDTOs;
    }

    public void setParameterDTOs(ParameterDTO[] parameterDTOs) {
        this.parameterDTOs = parameterDTOs;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }
}
