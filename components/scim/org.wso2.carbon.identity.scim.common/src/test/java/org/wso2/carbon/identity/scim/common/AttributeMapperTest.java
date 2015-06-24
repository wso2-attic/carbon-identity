/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.scim.common;

import org.junit.Assert;
import org.junit.Test;
import org.wso2.carbon.identity.scim.common.utils.AttributeMapper;
import org.wso2.charon.core.exceptions.CharonException;
import org.wso2.charon.core.exceptions.NotFoundException;
import org.wso2.charon.core.objects.User;
import org.wso2.charon.core.schema.SCIMConstants;
import org.wso2.charon.core.util.AttributeUtil;

import java.util.HashMap;
import java.util.Map;

public class AttributeMapperTest {
    private static Map<String, String> claimsMapDefined = new HashMap<String, String>();
    String id = "2417e51c-438b-45b1-a38b-0bb4c64d9832";
    String createdDate = "2012-08-24T06:59:02";
    String lastModifiedDate = "2012-08-24T06:59:02";
    String location = "https://localhost:9443/wso2/scim/Users/2417e51c-438b-45b1-a38b-0bb4c64d9832";
    String givenName = "hasini";
    String uid = "hasini";
    String sn = "gunasinghe";
    String workEmail = "abc_work.com";
    String homeEmail = "abc_home.com";

    @Test
    public void testSCIMObjectToClaimConversion() throws CharonException {

        //create scim object
        User user = new User();
        user.setId(id);

        user.setCreatedDate(AttributeUtil.parseDateTime(createdDate));
        user.setLastModified(AttributeUtil.parseDateTime(lastModifiedDate));
        user.setFamilyName(sn);
        user.setGivenName(givenName);
        user.setLocation(location);
        user.setUserName(uid);
        user.setWorkEmail(workEmail, false);
        user.setHomeEmail(homeEmail, false);
        //pass to method
        Map<String, String> claimsMap = AttributeMapper.getClaimsMap(user);

        claimsMapDefined.put(SCIMConstants.ID_URI, id);
        claimsMapDefined.put(SCIMConstants.USER_NAME_URI, uid);
        claimsMapDefined.put(SCIMConstants.NAME_GIVEN_NAME_URI, givenName);
        claimsMapDefined.put(SCIMConstants.NAME_FAMILY_NAME_URI, sn);
        claimsMapDefined.put(SCIMConstants.META_CREATED_URI, createdDate);
        claimsMapDefined.put(SCIMConstants.META_LAST_MODIFIED_URI, lastModifiedDate);
        claimsMapDefined.put(SCIMConstants.META_LOCATION_URI, location);
        claimsMapDefined.put(SCIMConstants.WORK_EMAIL_URI, workEmail);
        claimsMapDefined.put(SCIMConstants.HOME_EMAIL_URI, homeEmail);

        Assert.assertEquals(true, claimsMap.equals(claimsMapDefined));
    }

    @Test
    public void testClaimsToSCIMObjectConversion() throws CharonException, NotFoundException {
        //create a set of claims & pass to method
        User user = (User) AttributeMapper.constructSCIMObjectFromAttributes(claimsMapDefined,
                                                                            SCIMConstants.USER_INT);
        //go though scim object and see if they exist
        Assert.assertEquals(id, user.getId());
        Assert.assertEquals(uid, user.getUserName());
        Assert.assertEquals(createdDate,AttributeUtil.formatDateTime(user.getCreatedDate()));
        Assert.assertEquals(givenName,user.getGivenName());
        Assert.assertEquals(sn,user.getFamilyName());
        Assert.assertEquals(lastModifiedDate, AttributeUtil.formatDateTime(user.getLastModified()));
        Assert.assertEquals(location,user.getLocation());
        Assert.assertEquals(workEmail,user.getWorkEmail());
        Assert.assertEquals(homeEmail,user.getHomeEmail());
    }

}
