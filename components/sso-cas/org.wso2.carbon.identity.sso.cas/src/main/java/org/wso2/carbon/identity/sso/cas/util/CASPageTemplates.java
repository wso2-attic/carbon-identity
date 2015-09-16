	package org.wso2.carbon.identity.sso.cas.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.sso.cas.handler.HandlerConstants;


/**
 * Page templates singleton
 * 
 */
public class CASPageTemplates {
	private static String loginErrorPage;
	private static String logoutLinkPage;
	private static String logoutPage;
	private static String logoutErrorPage;
    private static Log log = LogFactory.getLog(CASPageTemplates.class);
    private static CASPageTemplates instance = new CASPageTemplates();
    private static final String LANGUAGE_REFERENCE = "\\{\\{locale.language\\}\\}";

	static {
		try {
			loginErrorPage = CASResourceReader.getInstance().readSecurityResource("cas_login_error.html");
		} catch (Exception ex) {
			log.error("login page cannot be loaded", ex);
		}
		
		try {
			logoutPage = CASResourceReader.getInstance().readSecurityResource("cas_logout.html");
			logoutLinkPage = CASResourceReader.getInstance().readSecurityResource("cas_logout_link.html");
			logoutErrorPage = CASResourceReader.getInstance().readSecurityResource("cas_logout_error.html");
		} catch (Exception ex) {
			log.error("logout pages cannot be loaded", ex);
		}
	}
	
    private CASPageTemplates() {

    }

    public static CASPageTemplates getInstance() {
        return instance;
    }

	public String showLoginError(String errorMessage, Locale locale) {
		return String.format(
				updateLocalizedVariables(loginErrorPage, locale), 
				errorMessage);
	}
	
	public String showLogoutError(String errorMessage, Locale locale) {
		return String.format(
				updateLocalizedVariables(logoutErrorPage, locale), 
				errorMessage);
	}
	
	public String showLogoutSuccess(String returnUrl, Locale locale) {
		return (returnUrl != null ) ? 
				String.format(
						updateLocalizedVariables(logoutLinkPage, locale), 
						returnUrl) : updateLocalizedVariables(logoutPage, locale);
	}
	
	private String updateLocalizedVariables(String rawString, Locale locale) {     	
        String localizedString = rawString;
		
        ResourceBundle bundle = getResourceBundle(locale);
        
      	// Replace each language bundle key.
      	for (String key : bundle.keySet()) {
      		if (key != null && key.length() > 0) {
      			String keyReference = "\\{\\{" + key + "\\}\\}";
                String i18String = bundle.getString(key);
                localizedString = localizedString.replaceAll(keyReference, i18String);
      		}
      	}
      	
      	// Replace language variable
      	localizedString = localizedString.replaceAll(LANGUAGE_REFERENCE, locale.getLanguage());
      	
      	return localizedString;
	}
	
	private ResourceBundle getResourceBundle(Locale locale) {
        // Look for i18n overrides in a custom bundle and failover to the classpath version
        ResourceBundle bundle;
        
        try {
        	bundle = ResourceBundle.getBundle(HandlerConstants.CUSTOM_RESOURCE_BUNDLE, locale);
        	log.debug("updateLocalizedVariables: Using custom CAS bundle");
        } catch(MissingResourceException ex) {
        	bundle = ResourceBundle.getBundle(HandlerConstants.RESOURCE_BUNDLE, locale);
        	log.debug("updateLocalizedVariables: Using default CAS bundle");
        }
        
        return bundle;
	}
}