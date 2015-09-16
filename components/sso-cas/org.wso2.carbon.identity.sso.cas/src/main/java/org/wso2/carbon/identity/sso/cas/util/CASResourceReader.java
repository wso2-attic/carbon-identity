package org.wso2.carbon.identity.sso.cas.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.sso.cas.handler.HandlerConstants;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * Resource abstraction that attempts to read from disk for an override and failover to classpath
 *
 */
public class CASResourceReader {
	private static CASResourceReader instance = new CASResourceReader();
	private static Log log = LogFactory.getLog(CASResourceReader.class);
	private String basePath = CarbonUtils.getCarbonHome() + File.separator + "repository"
            + File.separator + "resources" + File.separator + "security" + File.separator;
	
	private CASResourceReader() {
		
	}
	
	public static CASResourceReader getInstance() {
		return instance;
	}
	
	public String getLocalizedString(String key, Locale locale) {
		ResourceBundle bundle = ResourceBundle.getBundle(HandlerConstants.RESOURCE_BUNDLE, locale);
		
		if( bundle != null ) {
			return bundle.getString(key);
		} else {
			return null;
		}
		
	}
	
    public String readSecurityResource(String filename) throws IOException {
    	String fileContents = null;
    	
    	try {
			String filePath = basePath + filename;
			FileInputStream fileInputStream = new FileInputStream(new File(filePath));
        	fileContents = new Scanner(fileInputStream,HandlerConstants.DEFAULT_ENCODING).useDelimiter("\\A").next();
        	log.debug("CAS resource read from filesystem: " + filePath);
    	} catch(Exception ex) {
    		fileContents = null;
    	}
    	
    	if( fileContents == null ) {
    		fileContents = IOUtils.toString(
				this.getClass().getClassLoader()
                .getResourceAsStream(filename)
    		);
    		
    		if( fileContents != null ) {
    			log.debug("CAS resource read from classpath: "+filename);
    		}
    	}
    	
    	return fileContents;
    }
}
