/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.tools.xacml.validator.processors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.entitlement.EntitlementUtil;
import org.wso2.carbon.identity.entitlement.dto.PolicyDTO;
import org.wso2.carbon.identity.tools.xacml.validator.dto.ErrorItem;
import org.wso2.carbon.identity.tools.xacml.validator.util.XACMLValidatorConstants;
import org.wso2.carbon.identity.tools.xacml.validator.util.XACMLValidatorUtils;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * This is the actual class doing xml processing and generate list of Exceptions
 * as a list
 */

public class XACMLPolicyValidator {

    private static Log log = LogFactory.getLog(XACMLPolicyValidator.class);
    private final static int FIRST_LINE_NUMBER = 1;

    /**
     * This method return validated Items as an Array, If there is no errors
     * validatePolicy
     * function include single record with success element,a validated exception
     * item include xml
     * parser error or fatal errors or warnings
     * 
     * @param policyDTO
     * @return
     */
    public static ErrorItem[] validatePolicy(PolicyDTO policyDTO) {
        final List<ErrorItem> errorItemList = new LinkedList<ErrorItem>();
        try {
            // get schema according to xacml version
            Schema schema = EntitlementUtil.getSchema(policyDTO);

            if (schema != null) {
                // use SAXSource instead of DOMSource to catch exceptions with
                // line numbers
                SAXSource saxSource =
                                      new SAXSource(new InputSource(new ByteArrayInputStream(policyDTO.getPolicy()
                                                                                                      .getBytes())));
                Validator validator = schema.newValidator();
                validator.setErrorHandler(new ErrorHandler() {
                    @Override
                    public void warning(SAXParseException exception) throws SAXException {
                        processException(exception, XACMLValidatorConstants.WARNING.name());
                    }

                    @Override
                    public void fatalError(SAXParseException exception) throws SAXException {
                        processException(exception, XACMLValidatorConstants.FATAL_ERROR.name());
                    }

                    @Override
                    public void error(SAXParseException exception) throws SAXException {
                        processException(exception, XACMLValidatorConstants.ERROR.name());
                    }

                    private void processException(SAXParseException exception, String type) {
                        ErrorItem errorItem =
                                              XACMLValidatorUtils.getErrorItem(exception.getMessage(), type,
                                                                               exception.getLineNumber());
                        errorItemList.add(errorItem);
                    }
                });
                validator.validate(saxSource, new SAXResult());

            } else {
                String message = "Invalid Namespace in policy";
                ErrorItem errorItem =
                                      XACMLValidatorUtils.getErrorItem(message, XACMLValidatorConstants.ERROR.name(),
                                                                       FIRST_LINE_NUMBER);
                errorItemList.add(errorItem);
            }
            // since custom error handler sets, validate method does not throw
            // SAXException but
            // still need to add SAXException to catch ladder to complete
            // compilation
        } catch (SAXException e) {
            // reason to ignore has been mentioned in the above comment
        } catch (IOException e) {
            String message = "XML content is not readable";
            ErrorItem errorItem =
                                  XACMLValidatorUtils.getErrorItem(message, XACMLValidatorConstants.ERROR.name(),
                                                                   FIRST_LINE_NUMBER);
            errorItemList.add(errorItem);
        }
        //
        if (errorItemList.isEmpty()) {
            String message = "Valid Policy";
            ErrorItem errorItem = XACMLValidatorUtils.getErrorItem(message, XACMLValidatorConstants.SUCCESS.name(), 0);
            errorItemList.add(errorItem);
        }
        return errorItemList.toArray(new ErrorItem[errorItemList.size()]);
    }
}
