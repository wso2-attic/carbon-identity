/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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

package org.wso2.carbon.identity.certificateauthority.utils;

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public class CsrUtils {


    /**
     * convert a base 64 encoded csr request into a PKCS10CertificateRequest class (bouncy-castle class)
     *
     * @param encodedCsr Base 64 encoded csr request
     * @return PKCS10CertificationRequest constructed from the encoded string
     */
    public static PKCS10CertificationRequest getCRfromEncodedCsr(String encodedCsr) throws IOException {
        PEMParser pemParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(encodedCsr.getBytes()), "8859_1"));
        PKCS10CertificationRequest csr = (PKCS10CertificationRequest) pemParser.readObject();
        return csr;
    }

    public static HashMap<String, String> getSubjectInfo(PKCS10CertificationRequest csr) {
        String name = csr.getSubject().toString();
        HashMap<String, String> map = new HashMap<String, String>();
        if (name.split("C").length > 1) {
            String country = name.split("C=")[1].split(",")[0];
            map.put("C", country);
        }
        if (name.split("CN").length > 1) {
            String commonName = name.split("CN=")[1].split(",")[0];
            map.put("CN", commonName);
        }
        if (name.split("O").length > 1) {
            String organization = name.split("O=")[1].split(",")[0];
            map.put("O", organization);
        }
        if (name.split("L").length > 1) {
            String l = name.split("L=")[1].split(",")[0];
            map.put("L", l);
        }
        if (name.split("OU").length > 1) {
            String ou = name.split("OU=")[1].split(",")[0];
            map.put("OU", ou);
        }
        if (name.split("ST").length > 1) {
            String st = name.split("ST=")[1].split(",")[0];
            map.put("ST", st);
        }
        return map;
    }

}
