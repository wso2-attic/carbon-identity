/*
 *Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.carbon.identity.application.common.model;

import org.apache.axiom.om.OMElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

public class LocalAndOutboundAuthenticationConfig implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -932772940989929376L;

    private AuthenticationStep[] authenticationSteps = new AuthenticationStep[0];
    private String authenticationType;
    private AuthenticationStep authenticationStepForSubject;
    private AuthenticationStep authenticationStepForAttributes;
    private boolean alwaysSendBackAuthenticatedListOfIdPs;
    private String subjectClaimUri;

    /*
     * <LocalAndOutboundAuthenticationConfig> <AuthenticationSteps></AuthenticationSteps>
     * <AuthenticationType></AuthenticationType>
     * <AuthenticationStepForSubject></AuthenticationStepForSubject>
     * <AuthenticationStepForAttributes></AuthenticationStepForAttributes>
     * </LocalAndOutboundAuthenticationConfig>
     */
    public static LocalAndOutboundAuthenticationConfig build(
            OMElement localAndOutboundAuthenticationConfigOM) {

        LocalAndOutboundAuthenticationConfig localAndOutboundAuthenticationConfig = new LocalAndOutboundAuthenticationConfig();

        if (localAndOutboundAuthenticationConfigOM == null) {
            return localAndOutboundAuthenticationConfig;
        }

        Iterator<?> iter = localAndOutboundAuthenticationConfigOM.getChildElements();

        while (iter.hasNext()) {
            OMElement member = (OMElement) iter.next();

            if (member.getLocalName().equals("AuthenticationSteps")) {

                Iterator<?> authenticationStepsIter = member.getChildElements();
                ArrayList<AuthenticationStep> authenticationStepsArrList = new ArrayList<AuthenticationStep>();

                if (authenticationStepsIter != null) {
                    while (authenticationStepsIter.hasNext()) {
                        OMElement authenticationStepsElement = (OMElement) (authenticationStepsIter
                                .next());
                        AuthenticationStep authStep = AuthenticationStep
                                .build(authenticationStepsElement);
                        if (authStep != null) {
                            authenticationStepsArrList.add(authStep);
                        }
                    }
                }

                if (authenticationStepsArrList.size() > 0) {
                    AuthenticationStep[] authenticationStepsArr = authenticationStepsArrList
                            .toArray(new AuthenticationStep[0]);
                    localAndOutboundAuthenticationConfig
                            .setAuthenticationSteps(authenticationStepsArr);
                }

            } else if (member.getLocalName().equals("AuthenticationType")) {
                localAndOutboundAuthenticationConfig.setAuthenticationType(member.getText());
            } else if (member.getLocalName().equals("AuthenticationStepForSubject")) {
                AuthenticationStep authStep = AuthenticationStep.build(member);
                if (authStep != null) {
                    localAndOutboundAuthenticationConfig.setAuthenticationStepForSubject(authStep);
                }
            } else if (member.getLocalName().equals("AuthenticationStepForAttributes")) {
                AuthenticationStep authStep = AuthenticationStep.build(member);
                if (authStep != null) {
                    localAndOutboundAuthenticationConfig
                            .setAuthenticationStepForAttributes(authStep);
                }
            } else if (member.getLocalName().equals("alwaysSendBackAuthenticatedListOfIdPs")) {
                if (member.getText() != null && "true".equals(member.getText())) {
                    localAndOutboundAuthenticationConfig.setAlwaysSendBackAuthenticatedListOfIdPs(true);
                }
            } else if (member.getLocalName().equals("subjectClaimUri")) {
                localAndOutboundAuthenticationConfig.setSubjectClaimUri(member.getText());
            }
        }

        return localAndOutboundAuthenticationConfig;
    }

    /**
     * @return
     */
    public AuthenticationStep[] getAuthenticationSteps() {
        return authenticationSteps;
    }

    /**
     * @param authSteps
     */
    public void setAuthenticationSteps(AuthenticationStep[] authenticationSteps) {
        this.authenticationSteps = authenticationSteps;
    }

    /**
     * @return
     */
    public String getAuthenticationType() {
        return authenticationType;
    }

    /**
     * @param authenticationType
     */
    public void setAuthenticationType(String authenticationType) {
        this.authenticationType = authenticationType;
    }

    /**
     * @return
     */
    public AuthenticationStep getAuthenticationStepForSubject() {
        return authenticationStepForSubject;
    }

    /**
     * @param authenticationStepForSubject
     */
    public void setAuthenticationStepForSubject(AuthenticationStep authenticationStepForSubject) {
        this.authenticationStepForSubject = authenticationStepForSubject;
    }

    /**
     * @return
     */
    public AuthenticationStep getAuthenticationStepForAttributes() {
        return authenticationStepForAttributes;
    }

    /**
     * @param authenticationStepForAttributes
     */
    public void setAuthenticationStepForAttributes(
            AuthenticationStep authenticationStepForAttributes) {
        this.authenticationStepForAttributes = authenticationStepForAttributes;
    }

    /**
     * @return
     */
    public boolean isAlwaysSendBackAuthenticatedListOfIdPs() {
        return alwaysSendBackAuthenticatedListOfIdPs;
    }

    /**
     * @param alwaysSendBackAuthenticatedListOfIdPs
     */
    public void setAlwaysSendBackAuthenticatedListOfIdPs(boolean alwaysSendBackAuthenticatedListOfIdPs) {
        this.alwaysSendBackAuthenticatedListOfIdPs = alwaysSendBackAuthenticatedListOfIdPs;
    }

    /**
     * @return
     */
    public String getSubjectClaimUri() {
        return subjectClaimUri;
    }

    /**
     * @param subjectClaimUri
     */
    public void setSubjectClaimUri(String subjectClaimUri) {
        this.subjectClaimUri = subjectClaimUri;
    }

}