/*
 * Copyright (c) 2007, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.core.dao;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.common.impl.AbstractSAMLObjectBuilder;
import org.opensaml.common.impl.AbstractSAMLObjectMarshaller;
import org.opensaml.common.impl.AbstractSAMLObjectUnmarshaller;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.common.Extensions;
import org.opensaml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml2.metadata.NameIDFormat;
import org.opensaml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml2.metadata.provider.DOMMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.KeyName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.IdentityRegistryResources;
import org.wso2.carbon.identity.core.extensions.audience.Audience;
import org.wso2.carbon.identity.core.extensions.audience.AudienceBuilder;
import org.wso2.carbon.identity.core.extensions.audience.AudienceMarshaller;
import org.wso2.carbon.identity.core.extensions.audience.AudienceUnmarshaller;
import org.wso2.carbon.identity.core.extensions.doSignResponse.DoSignResponse;
import org.wso2.carbon.identity.core.extensions.doSignResponse.DoSignResponseBuilder;
import org.wso2.carbon.identity.core.extensions.doSignResponse.DoSignResponseMarshaller;
import org.wso2.carbon.identity.core.extensions.doSignResponse.DoSignResponseUnmarshaller;
import org.wso2.carbon.identity.core.extensions.enableAttributesByDefault.EnableAttributesByDefault;
import org.wso2.carbon.identity.core.extensions.enableAttributesByDefault.EnableAttributesByDefaultBuilder;
import org.wso2.carbon.identity.core.extensions.enableAttributesByDefault.EnableAttributesByDefaultMarshaller;
import org.wso2.carbon.identity.core.extensions.enableAttributesByDefault.EnableAttributesByDefaultUnmarshaller;
import org.wso2.carbon.identity.core.extensions.enableEncryptedAssertion.EnableEncryptedAssertion;
import org.wso2.carbon.identity.core.extensions.enableEncryptedAssertion.EnableEncryptedAssertionBuilder;
import org.wso2.carbon.identity.core.extensions.enableEncryptedAssertion.EnableEncryptedAssertionMarshaller;
import org.wso2.carbon.identity.core.extensions.enableEncryptedAssertion.EnableEncryptedAssertionUnmarshaller;
import org.wso2.carbon.identity.core.extensions.idPInitSSOEnabled.IdPInitSSOEnabled;
import org.wso2.carbon.identity.core.extensions.idPInitSSOEnabled.IdPInitSSOEnabledBuilder;
import org.wso2.carbon.identity.core.extensions.idPInitSSOEnabled.IdPInitSSOEnabledMarshaller;
import org.wso2.carbon.identity.core.extensions.idPInitSSOEnabled.IdPInitSSOEnabledUnmarshaller;
import org.wso2.carbon.identity.core.extensions.recipient.Recipient;
import org.wso2.carbon.identity.core.extensions.recipient.RecipientBuilder;
import org.wso2.carbon.identity.core.extensions.recipient.RecipientMarshaller;
import org.wso2.carbon.identity.core.extensions.recipient.RecipientUnmarshaller;
import org.wso2.carbon.identity.core.extensions.requestedAudiences.RequestedAudiences;
import org.wso2.carbon.identity.core.extensions.requestedAudiences.RequestedAudiencesBuilder;
import org.wso2.carbon.identity.core.extensions.requestedAudiences.RequestedAudiencesMarshaller;
import org.wso2.carbon.identity.core.extensions.requestedAudiences.RequestedAudiencesUnmarshaller;
import org.wso2.carbon.identity.core.extensions.requestedRecipients.RequestedRecipients;
import org.wso2.carbon.identity.core.extensions.requestedRecipients.RequestedRecipientsBuilder;
import org.wso2.carbon.identity.core.extensions.requestedRecipients.RequestedRecipientsMarshaller;
import org.wso2.carbon.identity.core.extensions.requestedRecipients.RequestedRecipientsUnmarshaller;
import org.wso2.carbon.identity.core.extensions.useFullyQualifiedUsername.UseFullyQualifiedUsername;
import org.wso2.carbon.identity.core.extensions.useFullyQualifiedUsername.UseFullyQualifiedUsernameBuilder;
import org.wso2.carbon.identity.core.extensions.useFullyQualifiedUsername.UseFullyQualifiedUsernameMarshaller;
import org.wso2.carbon.identity.core.extensions.useFullyQualifiedUsername.UseFullyQualifiedUsernameUnmarshaller;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.utils.Transaction;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.UserStoreException;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class SAMLSSOServiceProviderDAO extends AbstractDAO<SAMLSSOServiceProviderDO> {

    private static Log log = LogFactory.getLog(SAMLSSOServiceProviderDAO.class);

    public SAMLSSOServiceProviderDAO(Registry registry) {
        this.registry = registry;
    }

    /**
     * generate an opensaml metadata object from a metadata string
     *
     * @param metadataString
     * @return
     */
    private EntityDescriptor generateMetadataObjectFromString(String metadataString) {
        EntityDescriptor entityDescriptor = null;

        try {
            Configuration.registerObjectProvider(UseFullyQualifiedUsername.DEFAULT_ELEMENT_NAME, new
                    UseFullyQualifiedUsernameBuilder(), new UseFullyQualifiedUsernameMarshaller(), new
                    UseFullyQualifiedUsernameUnmarshaller());
            Configuration.registerObjectProvider(DoSignResponse.DEFAULT_ELEMENT_NAME, new DoSignResponseBuilder(),
                    new DoSignResponseMarshaller(), new DoSignResponseUnmarshaller());
            Configuration.registerObjectProvider(IdPInitSSOEnabled.DEFAULT_ELEMENT_NAME, new IdPInitSSOEnabledBuilder
                    (), new IdPInitSSOEnabledMarshaller(), new IdPInitSSOEnabledUnmarshaller());
            Configuration.registerObjectProvider(RequestedRecipients.DEFAULT_ELEMENT_NAME, new
                    RequestedRecipientsBuilder(), new RequestedRecipientsMarshaller(), new
                    RequestedRecipientsUnmarshaller());
            Configuration.registerObjectProvider(RequestedAudiences.DEFAULT_ELEMENT_NAME, new
                    RequestedAudiencesBuilder(), new RequestedAudiencesMarshaller(), new
                    RequestedAudiencesUnmarshaller());
            Configuration.registerObjectProvider(Audience.DEFAULT_ELEMENT_NAME, new AudienceBuilder(), new
                    AudienceMarshaller(), new AudienceUnmarshaller());
            Configuration.registerObjectProvider(Recipient.DEFAULT_ELEMENT_NAME, new RecipientBuilder(), new
                    RecipientMarshaller(), new RecipientUnmarshaller());
            Configuration.registerObjectProvider(EnableEncryptedAssertion.DEFAULT_ELEMENT_NAME, new
                    EnableEncryptedAssertionBuilder(), new EnableEncryptedAssertionMarshaller(), new
                    EnableEncryptedAssertionUnmarshaller());

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.parse(new ByteArrayInputStream(metadataString.getBytes()));
            Element node = document.getDocumentElement();

            DOMMetadataProvider idpMetaDataProvider = new DOMMetadataProvider(node);

            idpMetaDataProvider.setRequireValidMetadata(true);
            idpMetaDataProvider.setParserPool(new BasicParserPool());
            idpMetaDataProvider.initialize();

            XMLObject xmlObject = idpMetaDataProvider.getMetadata();
            entityDescriptor = (EntityDescriptor) xmlObject;
        } catch (MetadataProviderException | SAXException | ParserConfigurationException | IOException e) {
            log.error("Error While reading Service Provider metadata xml", e);
        }

        return entityDescriptor;
    }

    /**
     * convert a opensaml metadata object to a SAMLSSOServiceProviderDO
     *
     * @param entityDescriptor
     * @param samlssoServiceProviderDO
     * @return
     */
    private SAMLSSOServiceProviderDO convertMetadataObjectToServiceProviderDO(EntityDescriptor entityDescriptor,
                                                                              SAMLSSOServiceProviderDO
            samlssoServiceProviderDO) {

        if (entityDescriptor != null) {
            samlssoServiceProviderDO.setIssuer(entityDescriptor.getEntityID());

            List<XMLObject> extensions = entityDescriptor.getExtensions().getUnknownXMLObjects();

            for (XMLObject extension : extensions) {

                if (extension instanceof UseFullyQualifiedUsername) {
                    //Use fully qualified username in the NameID
                    samlssoServiceProviderDO.setUseFullyQualifiedUsername(((UseFullyQualifiedUsername) extension)
                            .getUseFullyQualifiedUsername());
                } else if (extension instanceof DoSignResponse) {
                    //Enable Response Signing
                    samlssoServiceProviderDO.setDoSignResponse(((DoSignResponse) extension).getDoSignResponse());
                } else if (extension instanceof IdPInitSSOEnabled) {
                    //Enable IdP Initiated SSO
                    samlssoServiceProviderDO.setIdPInitSSOEnabled(((IdPInitSSOEnabled) extension)
                            .getIdPInitSSOEnabled());
                } else if (extension instanceof RequestedAudiences) {
                    //Enable audience Restriction
                    List<Audience> audiences = ((RequestedAudiences) extension).getAudiences();

                    if (audiences != null && audiences.size() > 0) {
                        List<String> audiencesString = new ArrayList<>();
                        for (Audience audience : audiences) {
                            audiencesString.add(audience.getAudience());
                        }
                        samlssoServiceProviderDO.setRequestedAudiences(audiencesString);
                    }
                } else if (extension instanceof RequestedRecipients) {
                    //Enable recipient Validation
                    List<Recipient> recipients = ((RequestedRecipients) extension).getRecipients();
                    if (recipients != null && recipients.size() > 0) {
                        List<String> recipientString = new ArrayList<>();
                        for (Recipient recipient : recipients) {
                            recipientString.add(recipient.getRecipient());
                        }
                        samlssoServiceProviderDO.setRequestedRecipients(recipientString);
                    }
                } else if (extension instanceof EnableEncryptedAssertion) {
                    //Enable Assertion Encryption
                    samlssoServiceProviderDO.setDoEnableEncryptedAssertion(((EnableEncryptedAssertion) extension)
                            .getEnableEncryptedAssertion());
                } else if (extension instanceof EnableAttributesByDefault) {
                    //Enable attributes by default
                    samlssoServiceProviderDO.setEnableAttributesByDefault(((EnableAttributesByDefault) extension)
                            .getEnableAttributesByDefault());
                }
            }

            List<RoleDescriptor> roleDescriptors = entityDescriptor.getRoleDescriptors();

            //TODO: handle when multiple role descriptors are available
            //assuming only one SPSSO is inside the entitydescripter
            RoleDescriptor roleDescriptor = roleDescriptors.get(0);
            SPSSODescriptor spssoDescriptor = (SPSSODescriptor) roleDescriptor;

            //Assertion Consumer URL
            //search for the url with the post binding, if there is no post binding select the default url
            //TODO: if there is no default one select at least one
            List<AssertionConsumerService> assertionConsumerServices = spssoDescriptor.getAssertionConsumerServices();
            boolean foundAssertionConsumerUrlPostBinding = false;
            for (AssertionConsumerService assertionConsumerService : assertionConsumerServices) {
                if (assertionConsumerService.getBinding().equals(SAMLConstants.SAML2_POST_BINDING_URI)) {
                    samlssoServiceProviderDO.setAssertionConsumerUrl(assertionConsumerService.getLocation());
                    foundAssertionConsumerUrlPostBinding = true;
                    break;
                }
            }
            if (!foundAssertionConsumerUrlPostBinding && assertionConsumerServices.size() > 0) {
                samlssoServiceProviderDO.setAssertionConsumerUrl(spssoDescriptor.getDefaultAssertionConsumerService()
                        .getLocation());
            }

            //NameID format
            List<NameIDFormat> nameIDFormats = spssoDescriptor.getNameIDFormats();
            samlssoServiceProviderDO.setNameIDFormat(nameIDFormats.get(0).getFormat());

            //Enable Assertion Signing
            samlssoServiceProviderDO.setDoSignAssertions(spssoDescriptor.getWantAssertionsSigned());

            //Enable Signature Validation in Authentication Requests and Logout Requests
            samlssoServiceProviderDO.setDoValidateSignatureInRequests(spssoDescriptor.isAuthnRequestsSigned());

            //Enable Single Logout
            //search for the url with the post binding, if there is no post binding select the default url
            //TODO: if there is no default one select at least one
            List<SingleLogoutService> singleLogoutServices = spssoDescriptor.getSingleLogoutServices();
            boolean foundSingleLogoutServicePostBinding = false;
            for (SingleLogoutService singleLogoutService : singleLogoutServices) {
                if (singleLogoutService.getBinding().equals(SAMLConstants.SAML2_POST_BINDING_URI)) {
                    samlssoServiceProviderDO.setLogoutURL(singleLogoutService.getLocation());
                    foundSingleLogoutServicePostBinding = true;
                    break;
                }
            }
            if (!foundSingleLogoutServicePostBinding && singleLogoutServices.size() > 0) {
                samlssoServiceProviderDO.setLogoutURL(singleLogoutServices.get(0).getBinding());
            }

            if (spssoDescriptor.getSingleLogoutServices().size() > 0) {
                samlssoServiceProviderDO.setDoSingleLogout(true);
            }

            //certificate alias
            List<KeyDescriptor> keyDescriptors = spssoDescriptor.getKeyDescriptors();
            if (keyDescriptors.size() > 0) {
                List<KeyName> keyInfo = keyDescriptors.get(0).getKeyInfo().getKeyNames();
                if (keyInfo.size() > 0) {
                    KeyName keyName = keyDescriptors.get(0).getKeyInfo().getKeyNames().get(0);
                    samlssoServiceProviderDO.setCertAlias(keyName.getValue());
                }
            }

            //Enable Attribute Profile
            //TODO: currently this is stored as a property in registry. need to add it to the metadata file
        }

        return samlssoServiceProviderDO;
    }

    protected SAMLSSOServiceProviderDO resourceToObject(Resource resource) {
        SAMLSSOServiceProviderDO serviceProviderDO = new SAMLSSOServiceProviderDO();

        try {
            byte[] contentObj = (byte[]) resource.getContent();
            String content = new String(contentObj);

            serviceProviderDO = convertMetadataObjectToServiceProviderDO(generateMetadataObjectFromString(content),
                    serviceProviderDO);
        } catch (RegistryException e) {
            e.printStackTrace();
        }

        if (resource
                .getProperty(IdentityRegistryResources.PROP_SAML_SSO_ENABLE_NAMEID_CLAIMURI) != null) {
            if (new Boolean(resource.getProperty(
                    IdentityRegistryResources.PROP_SAML_SSO_ENABLE_NAMEID_CLAIMURI)
                    .trim())) {
                serviceProviderDO.setNameIdClaimUri(resource.
                        getProperty(IdentityRegistryResources.PROP_SAML_SSO_NAMEID_CLAIMURI));
            }
        }

        if (resource
                .getProperty(IdentityRegistryResources.PROP_SAML_SSO_ATTRIB_CONSUMING_SERVICE_INDEX) != null) {
            serviceProviderDO
                    .setAttributeConsumingServiceIndex(resource
                            .getProperty(IdentityRegistryResources.PROP_SAML_SSO_ATTRIB_CONSUMING_SERVICE_INDEX));
        }

        if (resource.getProperty(IdentityRegistryResources.PROP_SAML_SSO_REQUESTED_CLAIMS) != null) {
            serviceProviderDO.setRequestedClaims(resource
                    .getPropertyValues(IdentityRegistryResources.PROP_SAML_SSO_REQUESTED_CLAIMS));
        }

        if (resource
                .getProperty(IdentityRegistryResources.PROP_SAML_SSO_ENABLE_ATTRIBUTES_BY_DEFAULT) != null) {
            String enableAttrByDefault = resource
                    .getProperty(IdentityRegistryResources.PROP_SAML_SSO_ENABLE_ATTRIBUTES_BY_DEFAULT);
            if ("true".equals(enableAttrByDefault)) {
                serviceProviderDO.setEnableAttributesByDefault(true);
            } else {
                serviceProviderDO.setEnableAttributesByDefault(false);
            }
        }

        return serviceProviderDO;
    }

    /**
     * generate opensaml metadata object from a SAMLSSOServiceProviderDO
     *
     * @param samlssoServiceProviderDO
     * @return
     */
    private EntityDescriptor generateMetadataObjectFromServiceProviderDO(SAMLSSOServiceProviderDO
                                                                                 samlssoServiceProviderDO) {

        EntityDescriptor entityDescriptor = null;
        try {
            entityDescriptor = createSAMLObject(EntityDescriptor.class);
            entityDescriptor.setEntityID(samlssoServiceProviderDO.getIssuer());

            //create extensions
            XMLObjectBuilderFactory xmlObjectBuilderFactory = Configuration.getBuilderFactory();
            QName extQName = new QName(SAMLConstants.SAML20MD_NS, Extensions.LOCAL_NAME, SAMLConstants.SAML20MD_PREFIX);
            XMLObjectBuilder extBuilder = xmlObjectBuilderFactory.getBuilder(extQName);
            Extensions extensions = (Extensions) extBuilder.buildObject(extQName);

            //Use fully qualified username in the NameID
            UseFullyQualifiedUsername useFullyQualifiedUsername = createSAMLExtensionObject(UseFullyQualifiedUsername
                    .class, new UseFullyQualifiedUsernameBuilder(), new UseFullyQualifiedUsernameMarshaller(), new
                    UseFullyQualifiedUsernameUnmarshaller());
            useFullyQualifiedUsername.setUseFullyQualifiedUsername(samlssoServiceProviderDO
                    .isUseFullyQualifiedUsername());
            extensions.getUnknownXMLObjects().add(useFullyQualifiedUsername);
            entityDescriptor.setExtensions(extensions);

            //Enable Response Signing
            DoSignResponse doSignResponse = createSAMLExtensionObject(DoSignResponse.class, new DoSignResponseBuilder
                    (), new DoSignResponseMarshaller(), new DoSignResponseUnmarshaller());
            doSignResponse.setDoSignResponse(samlssoServiceProviderDO.isDoSignResponse());
            extensions.getUnknownXMLObjects().add(doSignResponse);

            //Enable IdP Initiated SSO
            IdPInitSSOEnabled idPInitSSOEnabled = createSAMLExtensionObject(IdPInitSSOEnabled.class, new
                    IdPInitSSOEnabledBuilder(), new IdPInitSSOEnabledMarshaller(), new IdPInitSSOEnabledUnmarshaller());
            idPInitSSOEnabled.setIdPInitSSOEnabled(samlssoServiceProviderDO.isIdPInitSSOEnabled());
            extensions.getUnknownXMLObjects().add(idPInitSSOEnabled);

            if (samlssoServiceProviderDO.getRequestedAudiencesList() != null
                    && samlssoServiceProviderDO.getRequestedAudiencesList().size() > 0) {
                //set audiences
                RequestedAudiences requestedAudiences = createSAMLExtensionObject(RequestedAudiences.class, new
                        RequestedAudiencesBuilder(), new RequestedAudiencesMarshaller(), new
                        RequestedAudiencesUnmarshaller());
                for (String audience : samlssoServiceProviderDO.getRequestedAudiencesList()) {
                    Audience audienceObj = createSAMLExtensionObject(Audience.class, new AudienceBuilder(), new
                            AudienceMarshaller(), new AudienceUnmarshaller());
                    audienceObj.setAudience(audience);
                    requestedAudiences.getAudiences().add(audienceObj);
                }
                extensions.getUnknownXMLObjects().add(requestedAudiences);
            }
            if (samlssoServiceProviderDO.getRequestedRecipientsList() != null
                    && samlssoServiceProviderDO.getRequestedRecipientsList().size() > 0) {
                //set recipients
                RequestedRecipients requestedRecipients = createSAMLExtensionObject(RequestedRecipients.class, new
                        RequestedRecipientsBuilder(), new RequestedRecipientsMarshaller(), new
                        RequestedRecipientsUnmarshaller());
                for (String recipient : samlssoServiceProviderDO.getRequestedRecipientsList()) {
                    Recipient recipientObj = createSAMLExtensionObject(Recipient.class, new RecipientBuilder(), new
                            RecipientMarshaller(), new RecipientUnmarshaller());
                    recipientObj.setRecipient(recipient);
                    requestedRecipients.getRecipients().add(recipientObj);
                }
                extensions.getUnknownXMLObjects().add(requestedRecipients);
            }

            //Enable encrypted assertions
            EnableEncryptedAssertion doEnableEncryptedAssertion = createSAMLExtensionObject(EnableEncryptedAssertion
                    .class, new EnableEncryptedAssertionBuilder(), new EnableEncryptedAssertionMarshaller(), new
                    EnableEncryptedAssertionUnmarshaller());
            doEnableEncryptedAssertion.setEnableEncryptedAssertion(samlssoServiceProviderDO
                    .isDoEnableEncryptedAssertion());
            extensions.getUnknownXMLObjects().add(doEnableEncryptedAssertion);

            //Enable attributed by default
            EnableAttributesByDefault enableAttributesByDefault = createSAMLExtensionObject(EnableAttributesByDefault
                    .class, new EnableAttributesByDefaultBuilder(), new EnableAttributesByDefaultMarshaller(), new
                    EnableAttributesByDefaultUnmarshaller());
            enableAttributesByDefault.setEnableAttributesByDefault(samlssoServiceProviderDO
                    .isDoEnableEncryptedAssertion());
            extensions.getUnknownXMLObjects().add(enableAttributesByDefault);


            SPSSODescriptor spSSODescriptor = createSAMLObject(SPSSODescriptor.class);

            //Enable Assertion Signing
            spSSODescriptor.setWantAssertionsSigned(samlssoServiceProviderDO.isDoSignAssertions());

            //Enable Signature Validation in Authentication Requests and Logout Requests
            spSSODescriptor.setAuthnRequestsSigned(samlssoServiceProviderDO.isDoSignResponse());

            //setting nameIdFormats
            NameIDFormat nameIDFormat = createSAMLObject(NameIDFormat.class);
            nameIDFormat.setFormat(samlssoServiceProviderDO.getNameIDFormat());
            spSSODescriptor.getNameIDFormats().add(nameIDFormat);

            // Setting AssertionConsumerService
            AssertionConsumerService assertionConsumerService = createSAMLObject(AssertionConsumerService.class);
            assertionConsumerService.setIndex(0);
            assertionConsumerService.setBinding(SAMLConstants.SAML2_POST_BINDING_URI);
            assertionConsumerService.setLocation(samlssoServiceProviderDO.getAssertionConsumerUrl());
            spSSODescriptor.getAssertionConsumerServices().add(assertionConsumerService);
            entityDescriptor.getRoleDescriptors().add(spSSODescriptor);

            //Single Logout
            if (samlssoServiceProviderDO.isDoSingleLogout()) {
                SingleLogoutService singleLogoutService = createSAMLObject(SingleLogoutService.class);
                singleLogoutService.setBinding(SAMLConstants.SAML2_POST_BINDING_URI);
                if (samlssoServiceProviderDO.getLogoutURL() != null && !samlssoServiceProviderDO.getLogoutURL()
                        .equals("")) {
                    singleLogoutService.setLocation(samlssoServiceProviderDO.getLogoutURL());
                } else {
                    singleLogoutService.setLocation(samlssoServiceProviderDO.getAssertionConsumerUrl());
                }
                spSSODescriptor.getSingleLogoutServices().add(singleLogoutService);
            }

            //Certificate Alias
            if (samlssoServiceProviderDO.getCertAlias() != null
                    && !samlssoServiceProviderDO.getCertAlias().equals("")) {

                KeyDescriptor keyDescriptor = createSAMLObject(KeyDescriptor.class);

                KeyInfo keyInfo = createSAMLObject(KeyInfo.class);
                KeyName keyName = createSAMLObject(KeyName.class);
                keyName.setValue(samlssoServiceProviderDO.getCertAlias());
                keyInfo.getKeyNames().add(keyName);
                keyDescriptor.setKeyInfo(keyInfo);
                spSSODescriptor.getKeyDescriptors().add(keyDescriptor);
            }


        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Error While reading Service Provider details", e);
        }


        return entityDescriptor;
    }

    /**
     * create a metadata object of given type
     *
     * @param clazz
     * @param <T>
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private <T> T createSAMLObject(final Class<T> clazz) throws NoSuchFieldException, IllegalAccessException {
        XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();

        QName defaultElementName = (QName) clazz.getDeclaredField("DEFAULT_ELEMENT_NAME").get(null);

        return (T) builderFactory.getBuilder(defaultElementName).buildObject(defaultElementName);
    }

    /**
     * create a metadata extension object of given type
     *
     * @param clazz
     * @param builder
     * @param useFullyQualifiedUsernameMarshaller
     * @param useFullyQualifiedUsernameUnmarshaller
     * @param <T>
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private <T> T createSAMLExtensionObject(final Class<T> clazz, AbstractSAMLObjectBuilder builder,
                                            AbstractSAMLObjectMarshaller useFullyQualifiedUsernameMarshaller,
                                            AbstractSAMLObjectUnmarshaller useFullyQualifiedUsernameUnmarshaller)
            throws NoSuchFieldException, IllegalAccessException {
        XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();

        QName defaultElementName = (QName) clazz.getDeclaredField("DEFAULT_ELEMENT_NAME").get(null);
        Configuration.registerObjectProvider(defaultElementName, builder, useFullyQualifiedUsernameMarshaller,
                useFullyQualifiedUsernameUnmarshaller);

        return (T) builderFactory.getBuilder(defaultElementName).buildObject(defaultElementName);
    }

    /**
     * create the metadata string from the opensaml metadata object
     *
     * @param entityDescriptor
     * @return
     */
    private String getMetadataString(EntityDescriptor entityDescriptor) throws IdentityException {

        String metadataXML;

        DocumentBuilder builder;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Marshaller out = Configuration.getMarshallerFactory().getMarshaller(entityDescriptor);
            out.marshall(entityDescriptor, document);

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter stringWriter = new StringWriter();
            StreamResult streamResult = new StreamResult(stringWriter);
            DOMSource source = new DOMSource(document);
            transformer.transform(source, streamResult);
            stringWriter.close();
            metadataXML = stringWriter.toString();
        } catch (ParserConfigurationException | TransformerConfigurationException | MarshallingException e) {
            throw new IdentityException("Error while generating metadata", e);
        } catch (IOException | TransformerException e) {
            throw new IdentityException("Error while building the metadata xml", e);
        }

        return metadataXML;
    }

    public boolean addServiceProvider(SAMLSSOServiceProviderDO serviceProviderDO) throws IdentityException {

        String path = null;
        Resource resource;

        if (serviceProviderDO.getIssuer() != null) {
            path = IdentityRegistryResources.SAML_SSO_SERVICE_PROVIDERS + encodePath(serviceProviderDO.getIssuer());
        }

        boolean isTransactionStarted = Transaction.isStarted();
        try {
            if (registry.resourceExists(path)) {
                if (log.isDebugEnabled()) {
                    log.debug("Service Provider already exists with the same issuer name" + serviceProviderDO
                            .getIssuer());
                }
                return false;
            }

            resource = registry.newResource();
            resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_ISSUER,
                    serviceProviderDO.getIssuer());
            resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_ASSERTION_CONS_URL,
                    serviceProviderDO.getAssertionConsumerUrl());
            resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_ISSUER_CERT_ALIAS,
                    serviceProviderDO.getCertAlias());
            resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_LOGOUT_URL,
                    serviceProviderDO.getLogoutURL());
            resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_LOGIN_PAGE_URL,
                    serviceProviderDO.getLoginPageURL());
            resource.addProperty(
                    IdentityRegistryResources.PROP_SAML_SSO_NAMEID_FORMAT,
                    serviceProviderDO.getNameIDFormat());

            if (serviceProviderDO.getNameIdClaimUri() != null
                    && serviceProviderDO.getNameIdClaimUri().trim().length() > 0) {
                resource.addProperty(
                        IdentityRegistryResources.PROP_SAML_SSO_ENABLE_NAMEID_CLAIMURI,
                        "true");
                resource.addProperty(
                        IdentityRegistryResources.PROP_SAML_SSO_NAMEID_CLAIMURI,
                        serviceProviderDO.getNameIdClaimUri());
            } else {
                resource.addProperty(
                        IdentityRegistryResources.PROP_SAML_SSO_ENABLE_NAMEID_CLAIMURI,
                        "false");
            }

            String useFullyQualifiedUsername = serviceProviderDO.isUseFullyQualifiedUsername() ? "true"
                    : "false";
            resource.addProperty(
                    IdentityRegistryResources.PROP_SAML_SSO_USE_FULLY_QUALIFIED_USERNAME_AS_SUBJECT,
                    useFullyQualifiedUsername);

            String doSingleLogout = serviceProviderDO.isDoSingleLogout() ? "true" : "false";
            resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_DO_SINGLE_LOGOUT,
                    doSingleLogout);
            String doSignResponse = serviceProviderDO.isDoSignResponse() ? "true" : "false";
            resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_DO_SIGN_RESPONSE,
                    doSignResponse);
            String doSignAssertions = serviceProviderDO.isDoSignAssertions() ? "true" : "false";
            resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_DO_SIGN_ASSERTIONS,
                    doSignAssertions);
            if (serviceProviderDO.getRequestedClaimsList() != null
                    && serviceProviderDO.getRequestedClaimsList().size() > 0) {
                resource.setProperty(IdentityRegistryResources.PROP_SAML_SSO_REQUESTED_CLAIMS,
                        serviceProviderDO.getRequestedClaimsList());
            }
            if (serviceProviderDO.getAttributeConsumingServiceIndex() != null) {
                resource.addProperty(
                        IdentityRegistryResources.PROP_SAML_SSO_ATTRIB_CONSUMING_SERVICE_INDEX,
                        serviceProviderDO.getAttributeConsumingServiceIndex());
            }
            if (serviceProviderDO.getRequestedAudiencesList() != null
                    && serviceProviderDO.getRequestedAudiencesList().size() > 0) {
                resource.setProperty(IdentityRegistryResources.PROP_SAML_SSO_REQUESTED_AUDIENCES,
                        serviceProviderDO.getRequestedAudiencesList());
            }
            if (serviceProviderDO.getRequestedRecipientsList() != null
                    && serviceProviderDO.getRequestedRecipientsList().size() > 0) {
                resource.setProperty(IdentityRegistryResources.PROP_SAML_SSO_REQUESTED_RECIPIENTS,
                        serviceProviderDO.getRequestedRecipientsList());
            }

            String enableAttributesByDefault = serviceProviderDO.isEnableAttributesByDefault() ? "true"
                    : "false";
            resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_ENABLE_ATTRIBUTES_BY_DEFAULT,
                    enableAttributesByDefault);
            String idPInitSSOEnabled = serviceProviderDO.isIdPInitSSOEnabled() ? "true" : "false";
            resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_IDP_INIT_SSO_ENABLED,
                    idPInitSSOEnabled);
            String enableEncryptedAssertion = serviceProviderDO.isDoEnableEncryptedAssertion() ? "true" : "false";
            resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_ENABLE_ENCRYPTED_ASSERTION,
                    enableEncryptedAssertion);

            String validateSignatureInRequests = serviceProviderDO.isDoValidateSignatureInRequests() ? "true" : "false";
            resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_VALIDATE_SIGNATURE_IN_REQUESTS,
                    validateSignatureInRequests);

            EntityDescriptor entityDescriptor = generateMetadataObjectFromServiceProviderDO(serviceProviderDO);
            String metadataString = getMetadataString(entityDescriptor);
            log.info(metadataString);
            resource.setContent(metadataString.getBytes());

            try {
                if (!isTransactionStarted) {
                    registry.beginTransaction();
                }

                registry.put(path + "/initial", resource);
                registry.put(path + "/editable", resource);

                if (!isTransactionStarted) {
                    registry.commitTransaction();
                }

            } catch (RegistryException e) {
                if (!isTransactionStarted) {
                    registry.rollbackTransaction();
                }
                throw e;
            }

        } catch (RegistryException e) {
            throw new IdentityException("Error while adding Service Provider", e);
        }

        if (log.isDebugEnabled()) {
            log.debug("Service Provider " + serviceProviderDO.getIssuer() + " is added successfully.");
        }
        return true;
    }

    /**
     * upload a service provider metadata string
     *
     * @param metadata
     * @return
     * @throws IdentityException
     */
    public SAMLSSOServiceProviderDO uploadServiceProvider(String metadata) throws IdentityException {

        //TODO: check the constraints in issuer id
        EntityDescriptor entityDescriptor = generateMetadataObjectFromString(metadata);
        SAMLSSOServiceProviderDO serviceProviderDO = new SAMLSSOServiceProviderDO();
        serviceProviderDO = convertMetadataObjectToServiceProviderDO(entityDescriptor, serviceProviderDO);

        String path = null;
        Resource resource;

        if (serviceProviderDO.getIssuer() != null) {
            path = IdentityRegistryResources.SAML_SSO_SERVICE_PROVIDERS + encodePath(serviceProviderDO.getIssuer());
        }

        boolean isTransactionStarted = Transaction.isStarted();
        try {
            if (registry.resourceExists(path)) {
                if (log.isDebugEnabled()) {
                    log.debug("Service Provider already exists with the same issuer name" + serviceProviderDO
                            .getIssuer());
                }
                throw new IdentityException("Service provider already exists");
            }

            resource = registry.newResource();

            String metadataString = getMetadataString(entityDescriptor);
            log.info(metadataString);
            resource.setContent(metadataString.getBytes());

            try {
                if (!isTransactionStarted) {
                    registry.beginTransaction();
                }

                registry.put(path + "/initial", resource);
                registry.put(path + "/editable", resource);

                if (!isTransactionStarted) {
                    registry.commitTransaction();
                }

            } catch (RegistryException e) {
                if (!isTransactionStarted) {
                    registry.rollbackTransaction();
                }
                throw e;
            }

        } catch (RegistryException e) {
            throw new IdentityException("Error while adding Service Provider", e);
        }

        if (log.isDebugEnabled()) {
            log.debug("Service Provider " + serviceProviderDO.getIssuer() + " is added successfully.");
        }
        return serviceProviderDO;

    }

    /**
     * update an existing service provider
     *
     * @param serviceProviderDO
     * @return
     * @throws IdentityException
     */
    public boolean updateServiceProvider(SAMLSSOServiceProviderDO serviceProviderDO) throws IdentityException {
        Resource resource;
        String issuer = serviceProviderDO.getIssuer();
        if (issuer == null || StringUtils.isEmpty(issuer.trim())) {
            throw new IllegalArgumentException("Trying to update issuer \'" + issuer + "\'");
        }

        String path = IdentityRegistryResources.SAML_SSO_SERVICE_PROVIDERS + encodePath(issuer);
        boolean isTransactionStarted = Transaction.isStarted();
        try {

            if (registry.resourceExists(path)) {
                resource = registry.newResource();
                resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_ISSUER,
                        serviceProviderDO.getIssuer());
                resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_ASSERTION_CONS_URL,
                        serviceProviderDO.getAssertionConsumerUrl());
                resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_ISSUER_CERT_ALIAS,
                        serviceProviderDO.getCertAlias());
                resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_LOGOUT_URL,
                        serviceProviderDO.getLogoutURL());
                resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_LOGIN_PAGE_URL,
                        serviceProviderDO.getLoginPageURL());
                resource.addProperty(
                        IdentityRegistryResources.PROP_SAML_SSO_NAMEID_FORMAT,
                        serviceProviderDO.getNameIDFormat());

                if (serviceProviderDO.getNameIdClaimUri() != null
                        && serviceProviderDO.getNameIdClaimUri().trim().length() > 0) {
                    resource.addProperty(
                            IdentityRegistryResources.PROP_SAML_SSO_ENABLE_NAMEID_CLAIMURI,
                            "true");
                    resource.addProperty(
                            IdentityRegistryResources.PROP_SAML_SSO_NAMEID_CLAIMURI,
                            serviceProviderDO.getNameIdClaimUri());
                } else {
                    resource.addProperty(
                            IdentityRegistryResources.PROP_SAML_SSO_ENABLE_NAMEID_CLAIMURI,
                            "false");
                }

                String useFullyQualifiedUsername = serviceProviderDO.isUseFullyQualifiedUsername() ? "true"
                        : "false";
                resource.addProperty(
                        IdentityRegistryResources.PROP_SAML_SSO_USE_FULLY_QUALIFIED_USERNAME_AS_SUBJECT,
                        useFullyQualifiedUsername);

                String doSingleLogout = serviceProviderDO.isDoSingleLogout() ? "true" : "false";
                resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_DO_SINGLE_LOGOUT,
                        doSingleLogout);
                String doSignResponse = serviceProviderDO.isDoSignResponse() ? "true" : "false";
                resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_DO_SIGN_RESPONSE,
                        doSignResponse);
                String doSignAssertions = serviceProviderDO.isDoSignAssertions() ? "true" : "false";
                resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_DO_SIGN_ASSERTIONS,
                        doSignAssertions);
                if (serviceProviderDO.getRequestedClaimsList() != null
                        && serviceProviderDO.getRequestedClaimsList().size() > 0) {
                    resource.setProperty(IdentityRegistryResources.PROP_SAML_SSO_REQUESTED_CLAIMS,
                            serviceProviderDO.getRequestedClaimsList());
                }
                if (serviceProviderDO.getAttributeConsumingServiceIndex() != null) {
                    resource.addProperty(
                            IdentityRegistryResources.PROP_SAML_SSO_ATTRIB_CONSUMING_SERVICE_INDEX,
                            serviceProviderDO.getAttributeConsumingServiceIndex());
                }
                if (serviceProviderDO.getRequestedAudiencesList() != null
                        && serviceProviderDO.getRequestedAudiencesList().size() > 0) {
                    resource.setProperty(IdentityRegistryResources.PROP_SAML_SSO_REQUESTED_AUDIENCES,
                            serviceProviderDO.getRequestedAudiencesList());
                }
                if (serviceProviderDO.getRequestedRecipientsList() != null
                        && serviceProviderDO.getRequestedRecipientsList().size() > 0) {
                    resource.setProperty(IdentityRegistryResources.PROP_SAML_SSO_REQUESTED_RECIPIENTS,
                            serviceProviderDO.getRequestedRecipientsList());
                }

                String enableAttributesByDefault = serviceProviderDO.isEnableAttributesByDefault() ? "true"
                        : "false";
                resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_ENABLE_ATTRIBUTES_BY_DEFAULT,
                        enableAttributesByDefault);
                String idPInitSSOEnabled = serviceProviderDO.isIdPInitSSOEnabled() ? "true" : "false";
                resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_IDP_INIT_SSO_ENABLED,
                        idPInitSSOEnabled);
                String enableEncryptedAssertion = serviceProviderDO.isDoEnableEncryptedAssertion() ? "true" : "false";
                resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_ENABLE_ENCRYPTED_ASSERTION,
                        enableEncryptedAssertion);

                String validateSignatureInRequests = serviceProviderDO.isDoValidateSignatureInRequests() ? "true" :
                        "false";
                resource.addProperty(IdentityRegistryResources.PROP_SAML_SSO_VALIDATE_SIGNATURE_IN_REQUESTS,
                        validateSignatureInRequests);

                EntityDescriptor entityDescriptor = generateMetadataObjectFromServiceProviderDO(serviceProviderDO);
                String metadataString = getMetadataString(entityDescriptor);
                log.info(metadataString);
                resource.setContent(metadataString.getBytes());

                try {
                    if (!isTransactionStarted) {
                        registry.beginTransaction();
                    }
                    registry.delete(path + "/editable");
                    registry.put(path + "/editable", resource);

                    if (!isTransactionStarted) {
                        registry.commitTransaction();
                    }

                } catch (RegistryException e) {
                    if (!isTransactionStarted) {
                        registry.rollbackTransaction();
                    }
                    throw e;
                }

                return true;
            }


        } catch (RegistryException e) {
            throw new IdentityException("Error while updating the service provider", e);
        }

        return false;
    }

    public SAMLSSOServiceProviderDO[] getServiceProviders() throws IdentityException {

        SAMLSSOServiceProviderDO[] serviceProvidersList = new SAMLSSOServiceProviderDO[0];
        try {
            if (registry.resourceExists(IdentityRegistryResources.SAML_SSO_SERVICE_PROVIDERS)) {
                Resource resource = registry.get(IdentityRegistryResources.SAML_SSO_SERVICE_PROVIDERS);
                if (resource instanceof Collection) {
                    Collection collection = (Collection) resource;
                    String[] providers = collection.getChildren();
                    if (providers != null) {
                        serviceProvidersList = new SAMLSSOServiceProviderDO[providers.length];
                        for (int i = 0; i < providers.length; i++) {
                            serviceProvidersList[i] = resourceToObject(registry.get(providers[i] + "/editable"));
                        }
                    }
                }

            }
        } catch (RegistryException e) {
            log.error("Error reading Service Providers from Registry", e);
            throw new IdentityException("Error reading Service Providers from Registry", e);
        }
        return serviceProvidersList;
    }

    /**
     * remove an existing service provider
     *
     * Remove the service provider with the given name
     *
     * @param issuer
     * @throws IdentityException
     */
    public boolean removeServiceProvider(String issuer) throws IdentityException {

        if (issuer == null || StringUtils.isEmpty(issuer.trim())) {
            throw new IllegalArgumentException("Trying to delete issuer \'" + issuer + "\'");
        }

        String path = IdentityRegistryResources.SAML_SSO_SERVICE_PROVIDERS + encodePath(issuer);
        boolean isTransactionStarted = Transaction.isStarted();
        try {
            if (registry.resourceExists(path)) {
                try {
                    if (!isTransactionStarted) {
                        registry.beginTransaction();
                    }

                    registry.delete(path);

                    if (!isTransactionStarted) {
                        registry.commitTransaction();
                    }

                    return true;

                } catch (RegistryException e) {
                    if (!isTransactionStarted) {
                        registry.rollbackTransaction();
                    }
                    throw e;
                }
            }
        } catch (RegistryException e) {
            log.error("Error removing the service provider from the registry", e);
            throw new IdentityException("Error removing the service provider from the registry", e);
        }

        return false;
    }

    /**
     * Get the service provider
     *
     * @param issuer
     * @return
     * @throws IdentityException
     */
    public SAMLSSOServiceProviderDO getServiceProvider(String issuer) throws IdentityException {

        String path = IdentityRegistryResources.SAML_SSO_SERVICE_PROVIDERS + encodePath(issuer) + "/editable";
        SAMLSSOServiceProviderDO serviceProviderDO = null;

        UserRegistry userRegistry = null;
        String tenantDomain = null;
        try {
            userRegistry = (UserRegistry) registry;
            tenantDomain = IdentityTenantUtil.getRealmService().getTenantManager().getDomain(userRegistry.getTenantId
                    ());
            if (registry.resourceExists(path)) {
                serviceProviderDO = resourceToObject(registry.get(path));
                serviceProviderDO.setTenantDomain(tenantDomain);
            }
        } catch (RegistryException e) {
            throw new IdentityException("Error occurred while checking if resource path \'" + path + "\' exists in " +
                    "registry for tenant domain : " + tenantDomain, e);
        } catch (UserStoreException e) {
            throw new IdentityException("Error occurred while getting tenant domain from tenant ID : " +
                    userRegistry.getTenantId(), e);
        }

        return serviceProviderDO;
    }

    private String encodePath(String path) {
        String encodedStr = new String(Base64.encodeBase64(path.getBytes()));
        return encodedStr.replace("=", "");
    }

}
