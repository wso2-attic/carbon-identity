/*
*  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.application.common.util;

/*
 * This class validates special characters to avoid any XSS vulnerabilities.
 */
public class CharacterEncoder {

    private CharacterEncoder(){
    }

    public static String getSafeText(String text) {

        String tmptext1=null;
        String tmptext2=null;
        if (text== null) {
            return text;
        }
        tmptext1 = text.trim();
        if (tmptext1.indexOf('<') > -1) {
            tmptext2 = tmptext1.replace("<", "&lt;");
        }
        if (tmptext1.indexOf('>') > -1) {
            tmptext2 = tmptext1.replace(">", "&gt;");
        }
        return tmptext2;
    }

}
