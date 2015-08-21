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

package org.wso2.carbon.identity.uma.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.uma.model.PermissionTicketDO;

import java.util.Set;

/**
 * Data Access functionality for UMA Permission Ticket Registration.
 * This includes storing, retrieving and updating permission tickets registered
 * by Resource Servers
 */
public class PermissionTicketMgtDAO {


    private static final Log log = LogFactory.getLog(PermissionTicketMgtDAO.class);

    // table to store the resource set descriptions
    private static final String IDN_UMA_PROTECTION_TICKET = "IDN_UMA_PERMISSION_TICKET";


    // Method to persist the Permission Tickets Created by the AS
    public void savePermissionTicket(PermissionTicketDO permissionTicketDO, String userStoreDomain){


    }

    // helper method for the savePermissionTicket
    // does the query execution
    private void persistPermissionTicket(PermissionTicketDO permissionTicketDO, String userStoreDomain){

    }

    // method to retrieve permission tickets issued for a consumerKey
    public Set<PermissionTicketDO> retrievePermissionTickets(String consumerKey, String userStoreDomain){

        return null;
    }

    // retrieve a particular permission ticket issued for a consumerKey
    public PermissionTicketDO retrievePermissionTicket(String ticket,String consumerKey, String userStoreDomain){


        return null;
    }


    // update the state of the permission ticket
    public void updatePermissionTicketStatus(String newState, String consumerKey, String userStoreDomain){}



}
