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
package org.wso2.carbon.identity.thrift.authentication.client;

import java.io.File;

public class KeyStoreUtil {

    File filePath;
    static String version="4.2.0";
    static String packageName="org.wso2.carbon.identity.thrift.authentication.client";

    public static void setTrustStoreParams() {
//        File filePath = new File("src/test/resources");
        File filePath = new File("/Users/suho/wso2/dev/thrift-login/wso2cep-3.0.0/repository/resources/security");
        if (!filePath.exists()) {
            filePath = new File(""+packageName+"/src/test/resources");
        }
        if (!filePath.exists()) {
            filePath = new File("resources");
        }
        if (!filePath.exists()) {
            filePath = new File(""+packageName+"/"+version+"/src/test/resources");
        }
        String trustStore = filePath.getAbsolutePath();
        System.setProperty("javax.net.ssl.trustStore", trustStore + "/client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");

    }

    public static void setKeyStoreParams() {
//        File filePath = new File("src/test/resources");
        File filePath = new File("/Users/suho/wso2/dev/thrift-login/wso2cep-3.0.0/repository/resources/security");
        if (!filePath.exists()) {
            filePath = new File(""+packageName+"/src/test/resources");
        }
        if (!filePath.exists()) {
            filePath = new File("resources");
        }
        if (!filePath.exists()) {

            filePath = new File(""+packageName+"/"+version+"/src/test/resources");
        }
        String keyStore = filePath.getAbsolutePath();
        System.setProperty("Security.KeyStore.Location", keyStore + "/wso2carbon.jks");
        System.setProperty("Security.KeyStore.Password", "wso2carbon");

    }
}
