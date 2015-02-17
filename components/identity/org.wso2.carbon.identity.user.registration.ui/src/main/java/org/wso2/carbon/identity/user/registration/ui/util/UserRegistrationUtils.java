/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.identity.user.registration.ui.util;


import org.wso2.carbon.identity.user.registration.stub.dto.UserDTO;
import org.wso2.carbon.identity.user.registration.stub.dto.UserFieldDTO;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class UserRegistrationUtils {

    public static UserDTO getUserDTO(HttpServletRequest request) {

        Map map = request.getParameterMap();
        UserDTO userDTO = new UserDTO();
        UserFieldDTO[] fieldDTOs = new UserFieldDTO[map.size()];

        int index = 0;
        UserFieldDTO fieldDTO;
        for (Object mapEntry : map.entrySet()) {
            String[] value = (String[]) mapEntry;
            fieldDTO = new UserFieldDTO();
            fieldDTO.setFieldName((String) mapEntry);
            fieldDTO.setFieldValue(value[0]);
            fieldDTOs[index++] = fieldDTO;
        }

        userDTO.setUserFields(fieldDTOs);

        return userDTO;
    }
}