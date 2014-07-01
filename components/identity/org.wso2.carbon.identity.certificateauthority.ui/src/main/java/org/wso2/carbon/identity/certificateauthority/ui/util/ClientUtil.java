/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.certificateauthority.ui.util;


import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.certificateauthority.stub.CertificateMetaInfo;
import org.wso2.carbon.identity.certificateauthority.stub.CsrMetaInfo;
import org.wso2.carbon.identity.certificateauthority.ui.CAConstants;

import java.util.HashMap;

/**
 *
 */
public class ClientUtil {
    private static final Log log = LogFactory.getLog(ClientUtil.class);


    public static CsrMetaInfo[] doPagingForStrings(int pageNumber, int itemsPerPageInt, CsrMetaInfo[] names) {

        CsrMetaInfo[] returnedSubscriberNameSet;

        int startIndex = pageNumber * itemsPerPageInt;
        int endIndex = (pageNumber + 1) * itemsPerPageInt;
        if (itemsPerPageInt < names.length) {
            returnedSubscriberNameSet = new CsrMetaInfo[itemsPerPageInt];
        } else {
            returnedSubscriberNameSet = new CsrMetaInfo[names.length];
        }
        for (int i = startIndex, j = 0; i < endIndex && i < names.length; i++, j++) {
            returnedSubscriberNameSet[j] = names[i];
        }

        return returnedSubscriberNameSet;
    }

    public static CertificateMetaInfo[] doPagingForCertificates(int pageNumber, int itemsPerPageInt, CertificateMetaInfo[] names) {

        CertificateMetaInfo[] returnedSubscriberNameSet;

        int startIndex = pageNumber * itemsPerPageInt;
        int endIndex = (pageNumber + 1) * itemsPerPageInt;
        if (itemsPerPageInt < names.length) {
            returnedSubscriberNameSet = new CertificateMetaInfo[itemsPerPageInt];
        } else {
            returnedSubscriberNameSet = new CertificateMetaInfo[names.length];
        }
        for (int i = startIndex, j = 0; i < endIndex && i < names.length; i++, j++) {
            returnedSubscriberNameSet[j] = names[i];
        }

        return returnedSubscriberNameSet;
    }

    public static HashMap<String, Integer> getReasonMap() {
        HashMap<String, Integer> reasonList = new HashMap<String, Integer>();
        reasonList.put(CAConstants.REVOCATION_REASON_UNSPECIFIED, CAConstants.REVOCATION_REASON_UNSPECIFIED_VAL);
        reasonList.put(CAConstants.REVOCATION_REASON_KEYCOMPROMISE, CAConstants.REVOCATION_REASON_KEYCOMPROMISE_VAL);
        reasonList.put(CAConstants.REVOCATION_REASON_CACOMPROMISE, CAConstants.REVOCATION_REASON_CACOMPROMISE_VAL);
        reasonList.put(CAConstants.REVOCATION_REASON_AFFILIATIONCHANGED, CAConstants.REVOCATION_REASON_AFFILIATIONCHANGED_VAL);
        reasonList.put(CAConstants.REVOCATION_REASON_SUPERSEDED, CAConstants.REVOCATION_REASON_SUPERSEDED_VAL);
        reasonList.put(CAConstants.REVOCATION_REASON_CESSATIONOFOPERATION, CAConstants.REVOCATION_REASON_CESSATIONOFOPERATION_VAL);
        reasonList.put(CAConstants.REVOCATION_REASON_CERTIFICATEHOLD, CAConstants.REVOCATION_REASON_CERTIFICATEHOLD_VAL);
        reasonList.put(CAConstants.REVOCATION_REASON_REMOVEFROMCRL, CAConstants.REVOCATION_REASON_REMOVEFROMCRL_VAL);
        reasonList.put(CAConstants.REVOCATION_REASON_PRIVILEGESWITHDRAWN, CAConstants.REVOCATION_REASON_PRIVILEGESWITHDRAWN_VAL);
        reasonList.put(CAConstants.REVOCATION_REASON_AACOMPROMISE, CAConstants.REVOCATION_REASON_AACOMPROMISE_VAL);
        return reasonList;

    }


    /*public static void downloadServiceArchive(String certificateContent,HttpServletResponse response) throws AxisFault {
        try {
            ServletOutputStream os = response.getOutputStream();


            response.setHeader("Content-Disposition", "fileName=txt.key");
            response.setContentType("application/octet-string");
            String s = "hduehduhe";
            InputStream in = new ByteArrayInputStream(s.getBytes("UTF8"));


            int read = 0;
            byte[] bytes = new byte[1024];


            //data form resultset

            while ((read = in.read(bytes)) != -1) {
                os.write(bytes, 0, read);
            }
            os.flush();
            os.close();

        } catch (RemoteException e) {
            handleException("error.downloading.service", e);
        } catch (IOException e) {
            handleException("error.downloading.service", e);
        }
    }*/
    public static void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }


}
