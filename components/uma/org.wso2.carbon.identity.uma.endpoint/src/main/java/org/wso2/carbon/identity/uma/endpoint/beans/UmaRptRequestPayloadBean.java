/*
 *
 * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * /
 */

package org.wso2.carbon.identity.uma.endpoint.beans;

import java.util.ArrayList;

/**
 *  Bean class to parse the json payload of the the RPT Request sent by the authorization client
 *  on behalf of a Requesting Party to get an RPT (Request Party Token) to access the UMA Protected resource
 */


public class UmaRptRequestPayloadBean {

    // permission ticket containing details about the resource,scope of access requested
    // opaque string to the client
    private String ticket;

    // any RPTs that were used in a failed attempt
    private String rpt;

    // claim tokens pushed by a claims aware client
    private ArrayList<ClaimTokenBean> claim_tokens;

    public UmaRptRequestPayloadBean() {
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getRpt() {
        return rpt;
    }

    public void setRpt(String rpt) {
        this.rpt = rpt;
    }

    public ArrayList<ClaimTokenBean> getClaim_tokens() {
        return claim_tokens;
    }

    public void setClaim_tokens(ArrayList<ClaimTokenBean> claim_tokens) {
        this.claim_tokens = claim_tokens;
    }
}
