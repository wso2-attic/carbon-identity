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

package org.wso2.carbon.identity.application.common;

import org.apache.axiom.om.util.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.CertData;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Format;
import java.text.SimpleDateFormat;

public class IdentityApplicationManagementUtil {

	private static final Log log = LogFactory
			.getLog(IdentityApplicationManagementUtil.class);

	/**
	 * Validates an URI.
	 * 
	 * @param uriString
	 *            URI String
	 * @return <code>true</code> if valid URI, <code>false</code> otherwise
	 */
	public static boolean validateURI(String uriString) {

		if (uriString != null) {
			try {
				new URL(uriString);
			} catch (MalformedURLException e) {
				log.debug(e.getMessage(), e);
				return false;
			}
		} else {
			String errorMsg = "Invalid URL: \'NULL\'";
			log.debug(errorMsg);
			return false;
		}
		return true;
	}

	/**
	 * Utility method to close a database connection
	 * 
	 * @param dbConnection
	 *            Database <code>Connection</code> object
	 * @throws java.sql.SQLException
	 *             Error when trying to close the connection
	 */
	public static void closeConnection(Connection dbConnection)
			throws SQLException {

		if (dbConnection != null) {
			dbConnection.close();
		} else {
			String errorMsg = "Invalid Connection: \'NULL\'";
			log.debug(errorMsg);
			throw new IllegalArgumentException(errorMsg);
		}
	}

	/**
	 * Utility method to rollback a database connection
	 * 
	 * @param dbConnection
	 *            Database <code>Connection</code> object
	 * @throws java.sql.SQLException
	 *             Error when trying to rollback the connection
	 */
	public static void rollBack(Connection dbConnection) throws SQLException {

		if (dbConnection != null) {
			dbConnection.rollback();
		} else {
			String errorMsg = "Invalid Connection: \'NULL\'";
			log.debug(errorMsg);
			throw new IllegalArgumentException(errorMsg);
		}
	}

	/**
	 * Extract key store filename
	 * 
	 * @param filePath
	 *            File path of a key store
	 * @return Key store file name
	 */
	public static String extractKeyStoreFileName(String filePath) {

		if (filePath != null) {
			String name = null;
			int index = filePath.lastIndexOf('/');
			if (index != -1) {
				name = filePath.substring(index + 1);
			} else {
				index = filePath.lastIndexOf(File.separatorChar);
				if (index != -1) {
					name = filePath.substring(filePath
							.lastIndexOf(File.separatorChar));
				} else {
					name = filePath;
				}
			}
			return name;
		} else {
			String errorMsg = "Invalid file path: \'NULL\'";
			log.debug(errorMsg);
			throw new IllegalArgumentException(errorMsg);
		}
	}

	/**
	 * Generate thumbprint of certificate
	 * 
	 * @param encodedCert
	 *            Base64 encoded certificate
	 * @return Certificate thumbprint
	 * @throws java.security.NoSuchAlgorithmException
	 *             Unsupported hash algorithm
	 */
	public static String generateThumbPrint(String encodedCert)
			throws NoSuchAlgorithmException {

		if (encodedCert != null) {
			MessageDigest digestValue = null;
			digestValue = MessageDigest.getInstance("SHA-1");
			byte[] der = Base64.decode(encodedCert);
			digestValue.update(der);
			byte[] digestInBytes = digestValue.digest();
			String publicCertThumbprint = hexify(digestInBytes);
			return publicCertThumbprint;
		} else {
			String errorMsg = "Invalid encoded certificate: \'NULL\'";
			log.debug(errorMsg);
			throw new IllegalArgumentException(errorMsg);
		}
	}

	/**
	 * Generate thumbprint of certificate
	 * 
	 * @param encodedCert
	 *            Base64 encoded certificate
	 * @return Decoded <code>Certificate</code>
	 * @throws java.security.cert.CertificateException
	 *             Error when decoding certificate
	 */
	public static Certificate decodeCertificate(String encodedCert)
			throws CertificateException {

		if (encodedCert != null) {
			byte[] bytes = Base64.decode(encodedCert);
			CertificateFactory factory = CertificateFactory
					.getInstance("X.509");
			X509Certificate cert = (X509Certificate) factory
					.generateCertificate(new ByteArrayInputStream(bytes));
			return cert;
		} else {
			String errorMsg = "Invalid encoded certificate: \'NULL\'";
			log.debug(errorMsg);
			throw new IllegalArgumentException(errorMsg);
		}
	}

	/**
	 * Helper method to hexify a byte array. TODO:need to verify the logic
	 * 
	 * @param bytes
	 * @return hexadecimal representation
	 */
	public static String hexify(byte bytes[]) {

		if (bytes != null) {
			char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
					'9', 'a', 'b', 'c', 'd', 'e', 'f' };
			StringBuffer buf = new StringBuffer(bytes.length * 2);
			for (int i = 0; i < bytes.length; ++i) {
				buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
				buf.append(hexDigits[bytes[i] & 0x0f]);
			}
			return buf.toString();
		} else {
			String errorMsg = "Invalid byte array: \'NULL\'";
			log.debug(errorMsg);
			throw new IllegalArgumentException(errorMsg);
		}
	}

	public static CertData getCertData(String encodedCert)
			throws CertificateException {

		if (encodedCert != null) {
			byte[] bytes = Base64.decode(encodedCert);
			CertificateFactory factory = CertificateFactory
					.getInstance("X.509");
			X509Certificate cert = (X509Certificate) factory
					.generateCertificate(new ByteArrayInputStream(bytes));
			Format formatter = new SimpleDateFormat("dd/MM/yyyy");
			return fillCertData(cert, formatter);
		} else {
			String errorMsg = "Invalid encoded certificate: \'NULL\'";
			log.debug(errorMsg);
			throw new IllegalArgumentException(errorMsg);
		}
	}

	public static boolean exclusiveOR(boolean[] array) {
		boolean foundTrue = false;
		for (boolean temp : array) {
			if (temp) {
				if (foundTrue) {
					return false;
				} else {
					foundTrue = true;
				}
			}
		}
		return foundTrue;
	}

	private static CertData fillCertData(X509Certificate cert, Format formatter)
			throws CertificateEncodingException {

		CertData certData = new CertData();
		certData.setSubjectDN(cert.getSubjectDN().getName());
		certData.setIssuerDN(cert.getIssuerDN().getName());
		certData.setSerialNumber(cert.getSerialNumber());
		certData.setVersion(cert.getVersion());
		certData.setNotAfter(formatter.format(cert.getNotAfter()));
		certData.setNotBefore(formatter.format(cert.getNotBefore()));
		certData.setPublicKey(Base64.encode(cert.getPublicKey().getEncoded()));
		return certData;
	}

	public static void closeResultSet(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				log.error(
						"Database error. Could not close result set  - "
								+ e.getMessage(), e);
			}
		}

	}

	public static void closeStatement(PreparedStatement preparedStatement) {
		if (preparedStatement != null) {
			try {
				preparedStatement.close();
			} catch (SQLException e) {
				log.error(
						"Database error. Could not close statement. Continuing with others. - "
								+ e.getMessage(), e);
			}
		}

	}
}
