/*                                                                             
 * Copyright 2005,2006 WSO2, Inc. http://www.wso2.org
 *                                                                             
 * Licensed under the Apache License, Version 2.0 (the "License");             
 * you may not use this file except in compliance with the License.            
 * You may obtain a copy of the License at                                     
 *                                                                             
 *      http://www.apache.org/licenses/LICENSE-2.0                             
 *                                                                             
 * Unless required by applicable law or agreed to in writing, software         
 * distributed under the License is distributed on an "AS IS" BASIS,           
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    
 * See the License for the specific language governing permissions and         
 * limitations under the License.                                              
 */
package org.wso2.carbon.identity.sts.mex;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.util.Base64;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.EndpointReferenceHelper;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.mex.om.Metadata;
import org.apache.axis2.mex.om.MetadataSection;
import org.apache.axis2.namespace.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.RahasConstants;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.KeyUtil;
import org.wso2.carbon.service.mgt.ServiceAdmin;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.namespace.QName;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MexGetService {

	private static Log log = LogFactory.getLog(MexGetService.class);
	private static final String IDENTITY_LN = "Identity";
	private static final String KEY_INFO_LN = "KeyInfo";
	private static final String X509DATA_LN = "X509Data";
	private static final String X509CERT_LN = "X509Certificate";
	private static final String WSA_PREFIX = "a"; // workaround for oM bug

	public OMElement get(OMElement element) throws AxisFault {
		OMElement elem = null;

		if (log.isDebugEnabled()) {
			log.debug("begin Mex get");
		}

		MessageContext msgCtx = MessageContext.getCurrentMessageContext();
		String service = msgCtx.getAxisService().getName();

		ServiceAdmin admin = new ServiceAdmin();
		String stsName = null;
		if (service.equals(IdentityConstants.SERVICE_NAME_MEX_UT)) {
			stsName = IdentityConstants.SERVICE_NAME_STS_UT;
		} else if (service.equals(IdentityConstants.SERVICE_NAME_MEX_IC)) {
			stsName = IdentityConstants.SERVICE_NAME_STS_IC;
		} else if (service.equals(IdentityConstants.SERVICE_NAME_MEX_UT_SYMM)) {
			stsName = IdentityConstants.SERVICE_NAME_STS_UT_SYMM;
		} else if (service.equals(IdentityConstants.SERVICE_NAME_MEX_IC_SYMM)) {
			stsName = IdentityConstants.SERVICE_NAME_STS_IC_SYMM;
		} else if (service.equals(IdentityConstants.OpenId.SERVICE_NAME_MEX_OPENID)) {
			stsName = IdentityConstants.OpenId.SERVICE_NAME_STS_OPENID;
		} else if (service.equals(IdentityConstants.OpenId.SERVICE_NAME_MEX_IC_OPENID)) {
			stsName = IdentityConstants.OpenId.SERVICE_NAME_STS_IC_OPENID;
		} else {
			throw new AxisFault("Invalid Mex Service");
		}

		OMElement retElement = admin.getWSDL(stsName).getFirstElement();
		OMElement defElement = retElement.getFirstChildWithName(new QName(Constants.NS_URI_WSDL11,
				"definitions"));
		setIdentityAddressing(defElement);
		MetadataSection section = new MetadataSection();
		section.setDialect("http://schemas.xmlsoap.org/wsdl/");
		section.setinlineData(defElement);
		section.setIdentifier(RahasConstants.WST_NS_05_02);

		List lst = new ArrayList();
		lst.add(section);

		Metadata mdata = new Metadata();
		mdata.setMetadatSections(lst);

		elem = mdata.toOM();

		if (log.isDebugEnabled()) {
			log.debug("Mex processing DONE -> RESPONSE : " + elem);
		}

		return elem;
	}

	private void setIdentityAddressing(OMElement definitionElement) throws AxisFault {
		if (log.isDebugEnabled()) {
			log.debug("setIdentityAddressing");
		}

		Iterator ite = definitionElement.getChildrenWithName(new QName(Constants.NS_URI_WSDL11,
				"service"));
		OMElement serviceElem = null;
		if (ite.hasNext()) {
			serviceElem = (OMElement) ite.next();
		} else {
			throw new AxisFault("Cannot find element Nampsace :" + Constants.NS_URI_WSDL11
					+ " || Local Name : service");
		}

		OMFactory factory = definitionElement.getOMFactory();
		OMNamespace wsaNs = factory.createOMNamespace(AddressingConstants.Final.WSA_NAMESPACE,
				WSA_PREFIX);
		definitionElement.declareNamespace(wsaNs);

		String value = IdentityConstants.SERVICE_NAME_STS_UT;
		X509Certificate cert;
		try {
			cert = KeyUtil.getCertificateToIncludeInMex(value);
		} catch (IdentityException e) {
			throw new AxisFault(e.getMessage(), e);
		}

		if (cert == null) {
			throw new AxisFault("STS's certificate is null");
		}

		Iterator portIte = serviceElem.getChildElements();
		while (portIte.hasNext()) {
			OMElement portElem = (OMElement) portIte.next();
			if ("port".equals(portElem.getLocalName())) {
				addIIdentityAddressing(portElem, cert);
			}
		}
	}

	/**
	 * This method adds EndPointReference element into Port element of the WSDL
	 */
	private void addIIdentityAddressing(OMElement portElem, X509Certificate cert) throws AxisFault {

		if (log.isDebugEnabled()) {
			log.debug("addIIdentityAddressing - port Element found");
		}

		try {

			Iterator ite = portElem.getChildElements();
			String address = null;
			while (ite.hasNext()) {
				OMElement elem = (OMElement) ite.next();
				if ("address".equals(elem.getLocalName())) {
					address = elem.getAttributeValue(new QName("", "location"));
					break; // only one address element
				}
			}

			if (address == null) {
				throw new AxisFault("Address inside Port Element is null");
			}

			EndpointReference ref = new EndpointReference(address);

			OMFactory factory = portElem.getOMFactory(); // this is an OM bug
			// OMFactory factory = OMAbstractFactory.getOMFactory();
			OMElement identityElem = factory.createOMElement(new QName(
					IdentityConstants.IDENTITY_ADDRESSING_NS, IDENTITY_LN));

			OMNamespace ns = factory.createOMNamespace(XMLSignature.XMLNS, "dsig");
			OMElement keyInfoElem = factory.createOMElement(KEY_INFO_LN, ns);
			OMElement X509DataElem = factory.createOMElement(X509DATA_LN, ns);
			OMElement X509CertElem = factory.createOMElement(X509CERT_LN, ns);

			byte[] byteArray = cert.getEncoded();
			X509CertElem.setText(Base64.encode(byteArray));
			X509DataElem.addChild(X509CertElem);
			keyInfoElem.addChild(X509DataElem);
			identityElem.addChild(keyInfoElem);
			ArrayList lst = new ArrayList();
			lst.add(identityElem);
			ref.setExtensibleElements(lst);

			QName qname = new QName(AddressingConstants.Final.WSA_NAMESPACE, "EndpointReference",
					WSA_PREFIX);
			OMElement refElem = EndpointReferenceHelper.toOM(factory, ref, qname,
					AddressingConstants.Final.WSA_NAMESPACE);
			portElem.addChild(refElem);

		} catch (Exception e) {
			throw new AxisFault(e.getMessage(), e);
		}
	}
}
