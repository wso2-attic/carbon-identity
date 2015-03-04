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

public class ClaimConfig implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 3927992808243347563L;

    private String roleClaimURI;
    private String userClaimURI;
    private boolean localClaimDialect;
    private Claim[] idpClaims = new Claim[0];
    private ClaimMapping[] claimMappings = new ClaimMapping[0];
    private boolean alwaysSendMappedLocalSubjectId;

    /*
     * <ClaimConfig> <RoleClaimURI></RoleClaimURI> <UserClaimURI></UserClaimURI>
     * <LocalClaimDialect></LocalClaimDialect> <IdpClaims></IdpClaims>
     * <ClaimMappings></ClaimMappings> </ClaimConfig>
     */
    public static ClaimConfig build(OMElement claimConfigOM) {
        ClaimConfig claimConfig = new ClaimConfig();

        Iterator<?> iter = claimConfigOM.getChildElements();

        while (iter.hasNext()) {

            OMElement element = (OMElement) (iter.next());
            String elementName = element.getLocalName();

            if (elementName.equals("RoleClaimURI")) {
                claimConfig.setRoleClaimURI(element.getText());
            } else if (elementName.equals("LocalClaimDialect")) {
                if (element.getText() != null) {
                    claimConfig.setLocalClaimDialect(Boolean.parseBoolean(element.getText()));
                }
            } else if (elementName.equals("UserClaimURI")) {
                claimConfig.setUserClaimURI(element.getText());
            } else if (elementName.equals("AlwaysSendMappedLocalSubjectId")) {
                if (element.getText() != null && "true".equals(element.getText())) {
                    claimConfig.setAlwaysSendMappedLocalSubjectId(true);
                }
            } else if (elementName.equals("IdpClaims")) {
                Iterator<?> idpClaimsIter = element.getChildElements();
                ArrayList<Claim> idpClaimsArrList = new ArrayList<Claim>();

                if (idpClaimsIter != null) {
                    while (idpClaimsIter.hasNext()) {
                        OMElement idpClaimsElement = (OMElement) (idpClaimsIter.next());
                        Claim claim = Claim.build(idpClaimsElement);
                        if (claim != null) {
                            idpClaimsArrList.add(claim);
                        }
                    }
                }

                if (idpClaimsArrList.size() > 0) {
                    Claim[] idpClaimsArr = idpClaimsArrList.toArray(new Claim[0]);
                    claimConfig.setIdpClaims(idpClaimsArr);
                }
            } else if (elementName.equals("ClaimMappings")) {

                Iterator<?> claimMappingsIter = element.getChildElements();
                ArrayList<ClaimMapping> claimMappingsArrList = new ArrayList<ClaimMapping>();

                if (claimMappingsIter != null) {
                    while (claimMappingsIter.hasNext()) {
                        OMElement claimMappingsElement = (OMElement) (claimMappingsIter.next());
                        ClaimMapping claimMapping = ClaimMapping.build(claimMappingsElement);
                        if (claimMapping != null) {
                            claimMappingsArrList.add(claimMapping);
                        }
                    }
                }

                if (claimMappingsArrList.size() > 0) {
                    ClaimMapping[] claimMappingsArr = claimMappingsArrList
                            .toArray(new ClaimMapping[0]);
                    claimConfig.setClaimMappings(claimMappingsArr);
                }
            }
        }

        return claimConfig;
    }

    /**
     * @return
     */
    public String getRoleClaimURI() {
        return roleClaimURI;
    }

    /**
     * @param roleClaimURI
     */
    public void setRoleClaimURI(String roleClaimURI) {
        this.roleClaimURI = roleClaimURI;
    }

    /**
     * @return
     */
    public ClaimMapping[] getClaimMappings() {
        return claimMappings;
    }

    /**
     * @param claimMappins
     */
    public void setClaimMappings(ClaimMapping[] claimMappins) {
        this.claimMappings = claimMappins;
    }

    public String getUserClaimURI() {
        return userClaimURI;
    }

    public void setUserClaimURI(String userClaimURI) {
        this.userClaimURI = userClaimURI;
    }

    public Claim[] getIdpClaims() {
        return idpClaims;
    }

    public void setIdpClaims(Claim[] idpClaims) {
        this.idpClaims = idpClaims;
    }

    public boolean isLocalClaimDialect() {
        return localClaimDialect;
    }

    public void setLocalClaimDialect(boolean localClaimDialect) {
        this.localClaimDialect = localClaimDialect;
    }

    public boolean isAlwaysSendMappedLocalSubjectId() {
        return alwaysSendMappedLocalSubjectId;
    }

    public void setAlwaysSendMappedLocalSubjectId(boolean alwaysSendMappedLocalSubjectId) {
        this.alwaysSendMappedLocalSubjectId = alwaysSendMappedLocalSubjectId;
    }
}
