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
package org.wso2.carbon.identity.user.store.configuration.deployer;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.AbstractDeployer;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.deployment.repository.util.DeploymentFileData;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.util.FileUtils;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.identity.user.store.configuration.deployer.exception.UserStoreConfigurationDeployerException;
import org.wso2.carbon.identity.user.store.configuration.deployer.internal.UserStoreConfigComponent;
import org.wso2.carbon.identity.user.store.configuration.deployer.util.UserStoreConfigurationConstants;
import org.wso2.carbon.identity.user.store.configuration.deployer.util.UserStoreUtil;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.common.UserStoreDeploymentManager;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * This is to deploy a new User Store Management Configuration file dropped or created at repository/deployment/server/userstores
 * or repository/tenant/<>tenantId</>/userstores. Whenever a new file with .xml extension is added/deleted or a modification is done to
 * an existing file, deployer will automatically update the existing realm configuration org.wso2.carbon.identity.user.store.configuration
 * according to the new file.
 */
public class UserStoreConfigurationDeployer extends AbstractDeployer {


    private static Log log = LogFactory.getLog(UserStoreConfigurationDeployer.class);


    private AxisConfiguration axisConfig;

    public void init(ConfigurationContext configurationContext) {
        log.info("User Store Configuration Deployer initiated.");
        this.axisConfig = configurationContext.getAxisConfiguration();

    }

    /**
     * Trigger deploying of new org.wso2.carbon.identity.user.store.configuration file
     *
     * @param deploymentFileData information about the user store org.wso2.carbon.identity.user.store.configuration
     * @throws org.apache.axis2.deployment.DeploymentException
     *          for any errors
     */
    public void deploy(DeploymentFileData deploymentFileData) throws DeploymentException {
        ServerConfigurationService config =
                UserStoreConfigComponent.getServerConfigurationService();

        if(config == null){
            return;
        }
        String absolutePath = deploymentFileData.getAbsolutePath();
        String ext = FilenameUtils.getExtension(absolutePath);

        if(UserStoreConfigurationConstants.ENC_EXTENSION.equalsIgnoreCase(ext)){
            try {
                UserStoreUtil.initializeKeyStore();
                OMElement secondaryStoreDocument = UserStoreUtil.initializeOMElement(absolutePath);
                UserStoreUtil.updateSecondaryUserStore(secondaryStoreDocument);

                int index = absolutePath.lastIndexOf(".");
                if(index != 1 ){
                    String encFileName = absolutePath.substring(0, index+1) + UserStoreConfigurationConstants.XML_EXTENSION;
                    OutputStream outputStream;
                    outputStream = new FileOutputStream(encFileName);
                    secondaryStoreDocument.serialize(outputStream);

                    File file = new File(absolutePath);
                    if(file.exists()){
                        FileUtils.delete(file);
                    }
                }
                return;
            } catch (UserStoreConfigurationDeployerException e) {
                String errMsg = "Secondary user store processing failed while processing "+absolutePath;
                throw new DeploymentException(errMsg,e);
            } catch (FileNotFoundException e) {
                String errMsg = "Secondary user store File path "+absolutePath + " is invalid";
                throw new DeploymentException(errMsg,e);
            } catch (XMLStreamException e) {
                String errMsg = "Unexpected xml processing errors while trying to update file "
                        +absolutePath;
                throw new DeploymentException(errMsg,e);
            } catch (UserStoreException e) {
                String errMsg = "Error while initializing key store";
                throw new DeploymentException(errMsg,e);
            }
        }

        UserStoreDeploymentManager userStoreDeploymentManager = new UserStoreDeploymentManager();
        userStoreDeploymentManager.deploy(deploymentFileData.getAbsolutePath());
  }


    /**
     * Trigger un-deploying of a deployed file. Removes the deleted user store from chain
     *
     * @param fileName: domain name --> file name
     * @throws org.apache.axis2.deployment.DeploymentException
     *          for any errors
     */
    public void undeploy(String fileName) throws DeploymentException {

        if(fileName != null){
            String ext = FilenameUtils.getExtension(fileName);
            if(!UserStoreConfigurationConstants.ENC_EXTENSION.equalsIgnoreCase(ext)){
                UserStoreDeploymentManager userStoreDeploymentManager = new UserStoreDeploymentManager();
                userStoreDeploymentManager.undeploy(fileName);
            }
        }
    }


    public void setDirectory(String s) {

    }

    public void setExtension(String s) {

    }
}


