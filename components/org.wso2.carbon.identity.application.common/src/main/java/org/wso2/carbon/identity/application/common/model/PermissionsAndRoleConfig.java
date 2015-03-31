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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.axiom.om.OMElement;

public class PermissionsAndRoleConfig implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -7994146492198021069L;

    private ApplicationPermission[] permissions = new ApplicationPermission[0];
    private RoleMapping[] roleMappings = new RoleMapping[0];
    private String[] idpRoles = new String[0];

    /**
     * 
     * @return
     */
    public ApplicationPermission[] getPermissions() {
        return permissions;
    }

    /**
     * 
     * @param permissions
     */
    public void setPermissions(ApplicationPermission[] permissions) {
        this.permissions = permissions;
    }

    /**
     * 
     * @return
     */
    public RoleMapping[] getRoleMappings() {
        return roleMappings;
    }

    /**
     * 
     * @param roleMappings
     */
    public void setRoleMappings(RoleMapping[] roleMappings) {
        this.roleMappings = roleMappings;
    }

    public String[] getIdpRoles() {
        return idpRoles;
    }

    public void setIdpRoles(String[] idpRoles) {
        this.idpRoles = idpRoles;
    }

    /*
     * <PermissionsAndRoleConfig> <Permissions></Permissions> <RoleMappings></RoleMappings>
     * <IdpRoles></IdpRoles> </PermissionsAndRoleConfig>
     */
    public static PermissionsAndRoleConfig build(OMElement permissionsAndRoleConfigOM) {
        PermissionsAndRoleConfig permissionsAndRoleConfig = new PermissionsAndRoleConfig();

        if (permissionsAndRoleConfigOM == null) {
            return permissionsAndRoleConfig;
        }

        Iterator<?> iter = permissionsAndRoleConfigOM.getChildElements();

        while (iter.hasNext()) {

            OMElement element = (OMElement) (iter.next());
            String elementName = element.getLocalName();

            if (elementName.equals("Permissions")) {
                Iterator<?> permissionsIter = element.getChildElements();
                ArrayList<ApplicationPermission> permissionsArrList = new ArrayList<ApplicationPermission>();

                if (permissionsIter != null) {
                    while (permissionsIter.hasNext()) {
                        OMElement permissionsElement = (OMElement) (permissionsIter.next());
                        ApplicationPermission appPermission = ApplicationPermission
                                .build(permissionsElement);
                        if (appPermission != null) {
                            permissionsArrList.add(appPermission);
                        }
                    }
                }

                if (permissionsArrList.size() > 0) {
                    ApplicationPermission[] permissionsArr = permissionsArrList
                            .toArray(new ApplicationPermission[0]);
                    permissionsAndRoleConfig.setPermissions(permissionsArr);
                }
            }

            if (elementName.equals("RoleMappings")) {
                Iterator<?> roleMappingsIter = element.getChildElements();
                ArrayList<RoleMapping> roleMappingsArrList = new ArrayList<RoleMapping>();

                if (roleMappingsIter != null) {
                    while (roleMappingsIter.hasNext()) {
                        OMElement roleMappingsElement = (OMElement) (roleMappingsIter.next());
                        RoleMapping roleMapping = RoleMapping.build(roleMappingsElement);
                        if (roleMapping != null) {
                            roleMappingsArrList.add(roleMapping);
                        }
                    }
                }

                if (roleMappingsArrList.size() > 0) {
                    RoleMapping[] roleMappingsArr = roleMappingsArrList.toArray(new RoleMapping[0]);
                    permissionsAndRoleConfig.setRoleMappings(roleMappingsArr);
                }

            }
            
            if (elementName.equals("IdpRoles")) {
                Iterator<?> idpRolesIter = element.getChildElements();
                ArrayList<String> roleMappingsArrList = new ArrayList<String>();

                while (idpRolesIter.hasNext()) {
                    OMElement idpRolesElement = (OMElement) (idpRolesIter.next());
                    if (idpRolesElement.getText() != null) {
                        roleMappingsArrList.add(idpRolesElement.getText());
                    }
                }

                if (roleMappingsArrList != null) {
                    String[] idpRolesArr = roleMappingsArrList.toArray(new String[0]);
                    permissionsAndRoleConfig.setIdpRoles(idpRolesArr);
                }
            }
        }

        return permissionsAndRoleConfig;
    }
}
