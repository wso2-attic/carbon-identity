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
package org.wso2.carbon.identity.user.store.configuration.deployer.util;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.Base64;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.identity.user.store.configuration.deployer.exception.UserStoreConfigurationDeployerException;
import org.wso2.carbon.identity.user.store.configuration.deployer.internal.UserStoreConfigComponent;
import org.wso2.carbon.user.api.Property;
import org.wso2.carbon.user.core.UserStoreConfigConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.tracker.UserStoreManagerRegistry;

import javax.crypto.Cipher;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Utility class toperform utility functions when deployer get triggered
 */

public class UserStoreUtil {

	//private static Log log = LogFactory.getLog(UserStoreUtil.class);

	//these changes are introduced in chunkccesible from chunk 1

	private static final String SERVER_REGISTRY_KEYSTORE_FILE = "Security.RegistryKeyStore.Location";
	private static final String SERVER_REGISTRY_KEYSTORE_TYPE = "Security.RegistryKeyStore.Type";
	private static final String SERVER_REGISTRY_KEYSTORE_PASSWORD = "Security.RegistryKeyStore.Password";
	private static final String SERVER_REGISTRY_KEYSTORE_KEY_ALIAS = "Security.RegistryKeyStore.KeyAlias";

    private static final String SECURE_VAULT_NS = "http://org.wso2.securevault/configuration";
    private static final String SECRET_ALIAS = "";
    private static Cipher cipher = null;

    public static void initializeKeyStore() throws UserStoreException {

        if(cipher == null){
            ServerConfigurationService config =
                    UserStoreConfigComponent.getServerConfigurationService();

            if (config == null) {
                String errMsg = "ServerConfigurationService is null - this situation can't occur";
                throw new UserStoreException(errMsg);
            }

            String filePath = config.getFirstProperty(SERVER_REGISTRY_KEYSTORE_FILE);
            String keyStoreType = config.getFirstProperty(SERVER_REGISTRY_KEYSTORE_TYPE);
            String password = config.getFirstProperty(SERVER_REGISTRY_KEYSTORE_PASSWORD);
            String keyAlias = config.getFirstProperty(SERVER_REGISTRY_KEYSTORE_KEY_ALIAS);

            KeyStore store;
            InputStream inputStream = null;

            try {
                inputStream = new FileInputStream(new File(filePath).getAbsolutePath());
                store = KeyStore.getInstance(keyStoreType);
                store.load(inputStream, password.toCharArray());
                Certificate[] certs = store.getCertificateChain(keyAlias);
                cipher = Cipher.getInstance("RSA", "BC");
                cipher.init(Cipher.ENCRYPT_MODE, certs[0].getPublicKey());
            } catch (FileNotFoundException e) {
                String errorMsg = "Keystore File Not Found in configured location";
                throw new UserStoreException(errorMsg, e);
            } catch (IOException e) {
                String errorMsg = "Keystore File IO operation failed";
                throw new UserStoreException(errorMsg, e);
            } catch (InvalidKeyException e) {
                String errorMsg = "Invalid key is used to access keystore";
                throw new UserStoreException(errorMsg, e);
            } catch (KeyStoreException e) {
                String errorMsg = "Faulty keystore";
                throw new UserStoreException(errorMsg, e);
            } catch (GeneralSecurityException e) {
                String errorMsg = "Some parameters assigned to access the " +
                        "keystore is invalid";
                throw new UserStoreException(errorMsg, e);
            }finally {
                if(inputStream != null){
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        //TODO
                    }
                }
            }
        }
    }

	/**
	 * Initializes the XML Document object
	 *
	 * @param absoluteFilePath  xml path
	 * @return                  OMElement object will be returned
	 */

    public static OMElement initializeOMElement(String absoluteFilePath) throws
            UserStoreConfigurationDeployerException {
        StAXOMBuilder builder;
        InputStream inStream;
        try {
            inStream = new FileInputStream(absoluteFilePath);
            builder = new StAXOMBuilder(inStream);
            return builder.getDocumentElement();
        } catch (FileNotFoundException e) {
            String errMsg = " Secondary storage file Not found in given repo" + absoluteFilePath;
            throw new UserStoreConfigurationDeployerException(errMsg, e);
        } catch (XMLStreamException e) {
            String errMsg = " Secondary storage file reading for repo= " + absoluteFilePath + "failed";
            throw new UserStoreConfigurationDeployerException(errMsg, e);
        }
    }

    /**
     * Encrypts the secondary user store configuration
     * @param secondaryStoreDocument  OMElement of respective file path
     * @throws UserStoreConfigurationDeployerException
     * @return  File should be updated or not
     */
   public static void updateSecondaryUserStore(OMElement secondaryStoreDocument) throws
           UserStoreConfigurationDeployerException {
       String className =  secondaryStoreDocument.getAttributeValue(new QName(UserStoreConfigurationConstants.PROPERTY_CLASS));
       ArrayList<String> encryptList = getEncryptPropertyList(className);
       Iterator<?> ite = secondaryStoreDocument.getChildrenWithName(new QName(UserStoreConfigurationConstants.PROPERTY));
       while (ite.hasNext()) {
           OMElement propElem = (OMElement) ite.next();

           if(propElem != null && (propElem.getText() != null) ){
               String propertyName = propElem.getAttributeValue(new QName(UserStoreConfigurationConstants.PROPERTY_NAME));

               boolean encrypt;
               if(encryptList.contains(propertyName)){
                   encrypt=true;
               }else{
                   encrypt= isEligibleTobeEncrypted(propElem);
               }

               if(encrypt){
                   OMAttribute encryptAttr = propElem.getAttribute(new QName(UserStoreConfigurationConstants.PROPERTY_ENCRYPT));
                   if(encryptAttr != null){
                       propElem.removeAttribute(encryptAttr);
                   }
                   try{
                       String  cipherText = Base64.encode(cipher.doFinal((propElem.getText().getBytes())));
                       propElem.setText(cipherText);
                       propElem.addAttribute(UserStoreConfigurationConstants.PROPERTY_ENCRYPTED, "true", null);
                   }catch (GeneralSecurityException e){
                       String errMsg = "Encryption in secondary user store failed";
                       throw new UserStoreConfigurationDeployerException(errMsg, e);
                   }
               }
           }
       }
  //     return fileTobeUpdated;
   }

    /**
     *
     * @param propElem                     Property Element
     * @return                            If the element text can be encrypted
     */
    private static boolean isEligibleTobeEncrypted(OMElement propElem){
        if(propElem != null){

            String secAlias = propElem.getAttributeValue(new QName(SECURE_VAULT_NS, SECRET_ALIAS));
            if(secAlias == null){
                String secretPropName = propElem.getAttributeValue(new QName("encrypt"));
                if( secretPropName != null && secretPropName.equalsIgnoreCase("true")){
                    String plainText = propElem.getText();
                    if(plainText != null){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static ArrayList<String> getEncryptPropertyList(String userStoreClass) {
        //First check for mandatory field with #encrypt
        Property[] mandatoryProperties = UserStoreManagerRegistry.getUserStoreProperties(userStoreClass).
                getMandatoryProperties();

        ArrayList<String> propertyList = new ArrayList<String>();
        for (Property property : mandatoryProperties) {
            if (property != null) {
                String propertyName = property.getName();
                if (propertyName != null && property.getDescription().contains
                        (UserStoreConfigurationConstants.ENCRYPT_TEXT)) {
                    propertyList.add(propertyName);
                }
            }
        }
        return propertyList;
    }

    private Property[] getMandatoryProperties(String userStoreClass){
        return UserStoreManagerRegistry.getUserStoreProperties(userStoreClass).
                getMandatoryProperties();
    }
}
