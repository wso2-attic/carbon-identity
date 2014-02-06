/*
*  Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authentication.framework.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.impl.xb.xsdschema.FractionDigitsDocument;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.internal.ApplicationAuthenticationFrameworkServiceComponent;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * Application Authenticators Framework configuration reader.
 *
 */
public class FileBasedConfigurationBuilder {
	
	private static Log log = LogFactory.getLog(FileBasedConfigurationBuilder.class);
	private static FileBasedConfigurationBuilder instance;
	
	private String authenticationEndpointURL;
	private List<ExternalIdPConfig> idpList = new ArrayList<ExternalIdPConfig>();
	private List<SequenceConfig> sequenceList = new ArrayList<SequenceConfig>();
	private Map<String, AuthenticatorConfig> authenticatorConfigMap = new Hashtable<String, AuthenticatorConfig>();
	
	public static FileBasedConfigurationBuilder getInstance() {
        if(instance == null){
            instance = new FileBasedConfigurationBuilder();
        }
        
        return instance;
    }
	
    /**
     * Read the authenticator info from the file and populate the in-memory model
     */
    public void build() {
    	
        String authenticatorsFilePath = CarbonUtils.getCarbonConfigDirPath() + File.separator +
                                        "security" + File.separator + FrameworkConstants.Config.AUTHENTICATORS_FILE_NAME;
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(new File(authenticatorsFilePath));
            OMElement documentElement = new StAXOMBuilder(fileInputStream).getDocumentElement();
            
            //########### Read Authentication Endpoint URL ###########
            OMElement authEndpointURLElem = documentElement.getFirstChildWithName(new QName(FrameworkConstants.Config.QNAME_AUTHENTICATION_ENDPOINT_URL));
            
            if (authEndpointURLElem != null) {
            	 authenticationEndpointURL = authEndpointURLElem.getText();
            }
            
            //########### Read Authenticator Configs ###########
            OMElement authenticatorConfigsElem = documentElement.getFirstChildWithName(new QName(FrameworkConstants.Config.QNAME_AUTHENTICATOR_CONFIGS));
            
            if (authenticatorConfigsElem != null) {
            	// for each and every authenticator defined, create an AuthenticatorConfig instance
                for (Iterator authenticatorConfigElements = authenticatorConfigsElem.getChildrenWithLocalName(FrameworkConstants.Config.ELEM_AUTHENTICATOR_CONFIG); 
        				authenticatorConfigElements.hasNext();) {
        			AuthenticatorConfig authenticatorConfig = processAuthenticatorConfigElement((OMElement) authenticatorConfigElements.next());

        			if (authenticatorConfig != null) {
        				this.authenticatorConfigMap.put(authenticatorConfig.getName(), authenticatorConfig);
        			}
        		}
            }
            
            //########### Read IdP Configs ###########
            OMElement idpConfigsElem = documentElement.getFirstChildWithName(new QName(FrameworkConstants.Config.QNAME_IDP_CONFIGS));
            
            if (idpConfigsElem != null) {
            	// for each and every external idp defined, create an ExternalIdPConfig instance
                for (Iterator idpConfigElements = idpConfigsElem.getChildrenWithLocalName(FrameworkConstants.Config.ELEM_IDP_CONFIG);
        				idpConfigElements.hasNext();) {
        			
                	ExternalIdPConfig idpConfig = processIdPConfigElement((OMElement)idpConfigElements.next());
                	
                	if (idpConfig != null) {
                		idpList.add(idpConfig);
        			}
                }
            }
            
            //########### Read Sequence Configs ###########
            OMElement sequencesElem = documentElement.getFirstChildWithName(new QName(FrameworkConstants.Config.QNAME_SEQUENCES));
            
            if (sequencesElem != null) {
            	// for each every application defined, create a ApplicationBean instance
                for (Iterator sequenceElements = sequencesElem.getChildrenWithLocalName(FrameworkConstants.Config.ELEM_SEQUENCE);
                     sequenceElements.hasNext();) {
                    SequenceConfig sequenceConfig = processSequenceElement((OMElement) sequenceElements.next());
                    
                    if (sequenceConfig != null) {
                        //this.applicationConfigMap.put(sequenceDO.getName(), sequenceDO);
                        this.sequenceList.add(sequenceConfig);
                    }	
                }
            }
        } catch (FileNotFoundException e) {
            log.error("application-authenticators.xml file is not available");
        } catch (XMLStreamException e) {
            log.error("Error reading the application-authenticators.xml");
        }
        finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                log.warn("Unable to close the file input stream created for application-authenticators.xml");
            }	
        }
    }
    
    /**
     * Create SequenceDOs for each sequence entry
     * @param sequenceElem
     * @return
     */
    private SequenceConfig processSequenceElement(OMElement sequenceElem){
    	SequenceConfig sequenceConfig = new SequenceConfig();
    	/*OMAttribute nameAttr = sequenceElem.getAttribute(new QName(ApplicationAuthenticatorConstants.Config.ATTR_APPLICATION_NAME));
    	
    	String seqName = "default";
    	
		if (nameAttr != null) {
			log.warn("Each Sequence should have an unique name attribute. +"
			         + "This authenticator sequence will not be registered.");
			return null;
			seqName = nameAttr.getAttributeValue();
		}
		
    	sequenceDO.setName(seqName);*/
		
    	String relyingParty = "default";
    	
		OMAttribute rpAttr = sequenceElem.getAttribute(new QName(FrameworkConstants.Config.ATTR_RELYING_PARTY));
		
		if (rpAttr != null) {
			relyingParty = rpAttr.getAttributeValue();
		}
		
		sequenceConfig.setRelyingParty(relyingParty);
		
    	OMAttribute forceAuthnAttr = sequenceElem.getAttribute(new QName(FrameworkConstants.Config.ATTR_FORCE_AUTHENTICATE));
    	
    	if (forceAuthnAttr != null) {
    	    sequenceConfig.setForceAuthn(Boolean.valueOf(forceAuthnAttr.getAttributeValue()));
    	}
    	
    	OMAttribute checkAuthnAttr = sequenceElem.getAttribute(new QName(FrameworkConstants.Config.ATTR_CHECK_AUTHENTICATE));
        
        if (checkAuthnAttr != null) {
            sequenceConfig.setCheckAuthn(Boolean.valueOf(checkAuthnAttr.getAttributeValue()));
        }
    	
    	// for each step defined, create a StepDO instance
        for (Iterator stepElements = sequenceElem.getChildrenWithLocalName(FrameworkConstants.Config.ELEM_STEP);
             stepElements.hasNext();) {
        	StepConfig stepConfig = processStepElement((OMElement) stepElements.next());
            
            if (stepConfig != null) {
            	sequenceConfig.getStepMap().put(stepConfig.getOrder(), stepConfig);
            }
        }
        
    	return sequenceConfig;
    }
    
    /**
     * Create StepDOs for each step entry
     * @param stepElem
     * @return
     */
    private StepConfig processStepElement(OMElement stepElem) {
    	
    	StepConfig stepConfig = new StepConfig();
    	OMAttribute loginPageAttr = stepElem.getAttribute(new QName(FrameworkConstants.Config.ATTR_STEP_LOGIN_PAGE));
    	
    	if (loginPageAttr != null) {
    		stepConfig.setLoginPage(loginPageAttr.getAttributeValue());
    	}
    	
    	OMAttribute orderAttr = stepElem.getAttribute(new QName(FrameworkConstants.Config.ATTR_STEP_ORDER));
    	
    	if (orderAttr == null) {
    		log.warn("Each Step Configuration should have an order. +"
			         + "Authenticators under this Step will not be registered.");
			return null;
    	}
    	
    	stepConfig.setOrder(Integer.valueOf(orderAttr.getAttributeValue()));
    	
		for (Iterator authenticatorElements = stepElem.getChildrenWithLocalName(FrameworkConstants.Config.ELEM_AUTHENTICATOR); 
				authenticatorElements.hasNext();) {
			OMElement authenticatorElem = (OMElement) authenticatorElements.next();
			
			String authenticatorName = authenticatorElem.getAttributeValue(new QName(FrameworkConstants.Config.ATTR_AUTHENTICATOR_NAME));
			AuthenticatorConfig authenticatorConfig = authenticatorConfigMap.get(authenticatorName);
			String idps = authenticatorElem.getAttributeValue(new QName(FrameworkConstants.Config.ATTR_AUTHENTICATOR_IDPS));
			
			if (idps != null && !idps.isEmpty()) {
				String[] idpArr = idps.split(",");
				
				for (String idp : idpArr) {
					authenticatorConfig.getIdpList().add(idp);
				}
			} else {
				authenticatorConfig.getIdpList().add("internal");
			}
			
			stepConfig.getAuthenticatorList().add(authenticatorConfig);
			//stepDO.getAuthenticatorMappings().add(authenticatorName);processIdPConfigElement
		}
    	
    	return stepConfig;
    }

    /**
     * Create AuthenticatorBean elements for each authenticator entry
     * @param authenticatorConfigElem OMElement for Authenticator
     * @return  AuthenticatorBean object
     */
    private AuthenticatorConfig processAuthenticatorConfigElement(OMElement authenticatorConfigElem) {
    	
        // read the name of the authenticator. this is a mandatory attribute.
        OMAttribute nameAttr = authenticatorConfigElem.getAttribute(new QName(FrameworkConstants.Config.ATTR_AUTHENTICATOR_CONFIG_NAME));
        // if the name is not given, do not register this authenticator
        if (nameAttr == null) {
            log.warn("Each Authenticator Configuration should have a unique name attribute. +" +
                     "This Authenticator will not be registered.");
            return null;
        }
        
        String authenticatorName = nameAttr.getAttributeValue();

        // check whether the disabled attribute is set
        boolean enabled = false;
        
        if (authenticatorConfigElem.getAttribute(new QName(FrameworkConstants.Config.ATTR_AUTHENTICATOR_ENABLED)) != null) {
            enabled = Boolean.parseBoolean(authenticatorConfigElem.getAttribute(
                    new QName(FrameworkConstants.Config.ATTR_AUTHENTICATOR_ENABLED)).getAttributeValue());
        }
        
        // read the config parameters
        Map<String, String> parameterMap = new Hashtable<String, String>();
            
        for (Iterator paramIterator = authenticatorConfigElem.getChildrenWithLocalName(FrameworkConstants.Config.ELEM_PARAMETER);
            paramIterator.hasNext();) {
            OMElement paramElem = (OMElement)paramIterator.next();
            OMAttribute paramNameAttr = paramElem.getAttribute(new QName(FrameworkConstants.Config.ATTR_PARAMETER_NAME));
            
            if (paramNameAttr == null) {
                log.warn("An Authenticator Parameter should have a name attribute. Skipping the parameter.");
                continue;
            }   
            
            parameterMap.put(paramNameAttr.getAttributeValue(), paramElem.getText());
        }   

        AuthenticatorConfig authenticatorConfig = new AuthenticatorConfig(authenticatorName, enabled, parameterMap);
        authenticatorConfig.setApplicationAuthenticator( FrameworkUtils.getAppAuthenticatorByName(authenticatorName));

        return authenticatorConfig;
    }
    
    private ExternalIdPConfig processIdPConfigElement(OMElement idpConfigElem) {

    	OMAttribute nameAttr = idpConfigElem.getAttribute(new QName("name"));
        
    	// if the name is not given, do not register this config
        if (nameAttr == null) {
            log.warn("Each IDP configuration should have a unique name attribute");
        }
        
		// read the config parameters
        Map<String, String> parameterMap = new Hashtable<String, String>();
        
        for (Iterator paramIterator = idpConfigElem.getChildrenWithLocalName("Parameter");
            paramIterator.hasNext();) {
            OMElement paramElem = (OMElement)paramIterator.next();
            OMAttribute paramNameAttr = paramElem.getAttribute(new QName("name"));
            
            if (paramNameAttr == null) {
                log.warn("A Parameter should have a name attribute. Skipping the parameter.");
                continue;
            }   
            
            parameterMap.put(paramNameAttr.getAttributeValue(), paramElem.getText());
        }   

        ExternalIdPConfig externalIdPConfig = new ExternalIdPConfig();
        externalIdPConfig.setName(nameAttr.getAttributeValue());
        externalIdPConfig.setParameterMap(parameterMap);
        
        return externalIdPConfig;
        
    }
    
    public AuthenticatorConfig getAuthenticatorBean(String authenticatorName) {
		return authenticatorConfigMap.get(authenticatorName);
	}
    
	public Map<String, AuthenticatorConfig> getAuthenticatorConfigMap() {
		return authenticatorConfigMap;
	}
	
	public SequenceConfig findSequenceByRelyingParty(String relyingParty) {
		
		for (SequenceConfig seq : sequenceList) {
			String rp = seq.getRelyingParty();
			
			if (rp != null && rp.equalsIgnoreCase(relyingParty)) {
				return seq;
			}
		}
		
		return null;
	}

	public List<SequenceConfig> getSequenceList() {
		return sequenceList;
	}
	
	/*private void mapConfigsWithAuthenticators() {
        
        if (log.isTraceEnabled()) {
            log.trace("Inside mapConfigsWithAuthenticators()");
        }

        for (SequenceDO sequenceDO : sequenceList) {  
        
            for(Map.Entry<Integer, StepDO> e1 : sequenceDO.getStepMap().entrySet()) {
                StepDO stepDO = e1.getValue();
            
                for (String mapping : stepDO.getAuthenticatorMappings()) {
                    
                    for (ApplicationAuthenticator authenticator : ApplicationAuthenticationFrameworkServiceComponent.authenticators) {
                        
                        if (mapping.equals(authenticator.getAuthenticatorName())) {
                            stepDO.getAppAuthenticatorList().add(authenticator);
                        }
                    }
                }
            }
        }
    }*/
	
	public List<ExternalIdPConfig> getIdpList() {
		return idpList;
	}
	
	public ExternalIdPConfig getIdPConfigs(String name) {
		for (ExternalIdPConfig externalIdPConfig : idpList) {
			
			if (externalIdPConfig.getName().equals(name)) {
				return externalIdPConfig;
			}
		}
		
		return null;
	}

	public String getAuthenticationEndpointURL() {
		return authenticationEndpointURL;
	}

	public void setAuthenticationEndpointURL(String authenticationEndpointURL) {
		this.authenticationEndpointURL = authenticationEndpointURL;
	}
} 
