/*
 * Copyright 2004,2005 The Apache Software Foundation.
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

package org.wso2.carbon.identity.relyingparty;

public class HTMLEncoder {

    public static String encode(String val) {
        StringBuffer buffer;
        int length;

        buffer = new StringBuffer();
        length = (val == null ? -1 : val.length());

        for (int i = 0; i < length; i++) {
            char c = val.charAt(i);
            if (c > 127 || c == '"' || c == '<' || c == '>') {
                buffer.append("&#" + (int) c + ";");
            } else {
                buffer.append(c);
            }
        }
        return buffer.toString();
    }

}