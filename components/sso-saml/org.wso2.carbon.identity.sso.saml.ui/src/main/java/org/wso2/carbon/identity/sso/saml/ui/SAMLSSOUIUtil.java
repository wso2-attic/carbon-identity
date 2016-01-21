/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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

package org.wso2.carbon.identity.sso.saml.ui;

import org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOServiceProviderDTO;
import org.wso2.carbon.ui.util.CharacterEncoder;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

public class SAMLSSOUIUtil {

    private SAMLSSOUIUtil() {
    }

    /**
     * Return
     *
     * @param request
     * @param parameter
     * @return
     */
    public static String getSafeInput(HttpServletRequest request, String parameter) {
        return request.getParameter(parameter);
    }

    public static SAMLSSOServiceProviderDTO[] doPaging(int pageNumber,
                                                       SAMLSSOServiceProviderDTO[] serviceProviderSet) {

        int itemsPerPageInt = SAMLSSOUIConstants.DEFAULT_ITEMS_PER_PAGE;
        SAMLSSOServiceProviderDTO[] returnedServiceProviderSet;

        int startIndex = pageNumber * itemsPerPageInt;
        int endIndex = (pageNumber + 1) * itemsPerPageInt;
        if (serviceProviderSet.length > itemsPerPageInt) {

            returnedServiceProviderSet = new SAMLSSOServiceProviderDTO[itemsPerPageInt];
        } else {
            returnedServiceProviderSet = new SAMLSSOServiceProviderDTO[serviceProviderSet.length];
        }

        for (int i = startIndex, j = 0; i < endIndex && i < serviceProviderSet.length; i++, j++) {
            returnedServiceProviderSet[j] = serviceProviderSet[i];
        }

        return returnedServiceProviderSet;
    }

    public static SAMLSSOServiceProviderDTO[] doFilter(String filter,
                                                       SAMLSSOServiceProviderDTO[] serviceProviderSet) {
        String regPattern = filter.replace("*", ".*");
        List<SAMLSSOServiceProviderDTO> list = new ArrayList<>();
        for (SAMLSSOServiceProviderDTO serviceProvider : serviceProviderSet) {
            if (serviceProvider.getIssuer().toLowerCase().matches(regPattern.toLowerCase())) {
                list.add(serviceProvider);
            }
        }
        SAMLSSOServiceProviderDTO[] filteredProviders = new SAMLSSOServiceProviderDTO[list.size()];
        for (int i = 0; i < list.size(); i++) {
            filteredProviders[i] = list.get(i);

        }

        return filteredProviders;
    }

}
