/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.identity.mgt.policy.password;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wso2.carbon.identity.mgt.policy.AbstractPasswordPolicyEnforcer;

public class DefaultPasswordPatternPolicy extends AbstractPasswordPolicyEnforcer {

	private String PASSWORD_PATTERN = ".";
	private Pattern pattern;
	private Matcher matcher;

	@Override
	public boolean enforce(Object... args) {

		if (args != null) {
			String password = args[0].toString();
			
			matcher = pattern.matcher(password);
			if(matcher.matches()) {
				return true;
			}else {
				errorMessage = "Password pattern policy violated";
				return false;
			}
			
		} else {
			return true;
		}

	}

	@Override
	public void init(Map<String, String> params) {

		if (params != null && params.size() > 0) {
			PASSWORD_PATTERN = params.get("pattern");
		}

		pattern = Pattern.compile(PASSWORD_PATTERN);
	}

}
