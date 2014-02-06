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
package org.wso2.carbon.identity.samples.oauth;

public class AuthenticationToken {

	private String userId;
	private String tenantId;
	private String signature;

	// TenantId:=0&UserId:=1&Signature:=QHGYNr0phJclgJnNUhc+RodHqCk=
	public AuthenticationToken(String token) {

		if (token != null) {
			String[] tokens = token.split("&");
			if (tokens != null && tokens.length > 2) {
				for (int i = 0; i < tokens.length; i++) {
					if (tokens[i] != null) {
						String[] intTokens = tokens[i].split(":=");
						if (intTokens.length>1){
							if ("TenantId".equals(intTokens[0])){
								this.tenantId=intTokens[1];
							}
							if ("UserId".equals(intTokens[0])){
								this.userId=intTokens[1];
							}
							if ("Signature".equals(intTokens[0])){
								this.signature=intTokens[1];
							}
						}
					}
				}
			}
		}
	}

	public String getUserId() {
		return userId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public String getSignature() {
		return signature;
	}

}
