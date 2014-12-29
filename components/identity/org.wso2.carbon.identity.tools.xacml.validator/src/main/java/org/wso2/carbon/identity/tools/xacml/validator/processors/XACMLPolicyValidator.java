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
import org.wso2.carbon.identity.tools.xacml.validator.bean.ValidationResult;
import org.wso2.carbon.identity.tools.xacml.validator.exceptions.SystemException;
import org.wso2.carbon.identity.tools.xacml.validator.util.ResponseType;
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
import java.util.ArrayList;
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
    public static ValidationResult[] validatePolicy(PolicyDTO policyDTO) {
        final List<ValidationResult> validationResultList = new ArrayList<ValidationResult>();
        try {
            if(policyDTO.getPolicy()==null){
                throw new SystemException();
            }
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
                        processException(exception, ResponseType.WARNING.name());
                    }

                    @Override
                    public void fatalError(SAXParseException exception) throws SAXException {
                        processException(exception, ResponseType.FATAL_ERROR.name());
                    }

                    @Override
                    public void error(SAXParseException exception) throws SAXException {
                        processException(exception, ResponseType.ERROR.name());
                    }

                    private void processException(SAXParseException exception, String type) {
                        ValidationResult validationResult =
                                new ValidationResult(exception.getMessage(), exception.getLineNumber(), type);
                        validationResultList.add(validationResult);
                    }
                });
                validator.validate(saxSource, new SAXResult());

            } else {
                String message = "Invalid Namespace in policy";
                ValidationResult validationResult =
                        new ValidationResult(message, FIRST_LINE_NUMBER, ResponseType.ERROR.name());
                validationResultList.add(validationResult);
            }
        } catch (SAXException e) {
            // since custom error handler sets, validate method does not throw
            // SAXException but
            // still need to add SAXException to catch ladder to complete
            // compilation
        } catch (IOException e) {
            String message = "XML content is not readable";
            ValidationResult validationResult =
                    new ValidationResult(message, FIRST_LINE_NUMBER, ResponseType.ERROR.name());
            validationResultList.add(validationResult);
        } catch (SystemException e) {
            String message = "System Error";
            ValidationResult validationResult = new ValidationResult(message,0 , ResponseType.SYSTEM_ERROR.name());
            validationResultList.add(validationResult);
        }
        //
        if (validationResultList.isEmpty()) {
            String message = "Valid Policy";
            ValidationResult validationResult = new ValidationResult(message, 0 , ResponseType.SUCCESS.name());
            validationResultList.add(validationResult);
        }
        return validationResultList.toArray(new ValidationResult[validationResultList.size()]);
    }
}
