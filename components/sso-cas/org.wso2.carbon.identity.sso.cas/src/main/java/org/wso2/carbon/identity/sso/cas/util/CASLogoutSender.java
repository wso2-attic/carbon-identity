package org.wso2.carbon.identity.sso.cas.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.wso2.carbon.identity.sso.cas.cache.ServiceTicketCache;
import org.wso2.carbon.identity.sso.cas.cache.ServiceTicketCacheKey;
import org.wso2.carbon.identity.sso.cas.cache.TicketGrantingTicketCache;
import org.wso2.carbon.identity.sso.cas.cache.TicketGrantingTicketCacheKey;
import org.wso2.carbon.identity.sso.cas.config.CASConfiguration;
import org.wso2.carbon.identity.sso.cas.ticket.ServiceTicket;
import org.wso2.carbon.identity.sso.cas.ticket.TicketGrantingTicket;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;

/**
 * CAS asynchronous logout handling 
 *
 */
public class CASLogoutSender {
	private static Log log = LogFactory.getLog(CASLogoutSender.class);
	
	private static ExecutorService threadPool = Executors.newFixedThreadPool(2);
	
	private static CASLogoutSender instance = new CASLogoutSender();
	
    private CASLogoutSender() {

    }

    public static CASLogoutSender getInstance() {
        return instance;
    }
	
    public void logoutSession(String ticketGrantingTicketId) {
    	try {
	    	TicketGrantingTicket ticketGrantingTicket = CASSSOUtil.getTicketGrantingTicket(ticketGrantingTicketId);
	    	
	    	for( ServiceTicket serviceTicket : ticketGrantingTicket.getServiceTickets() ) {    		
	    		ServiceTicketLogoutSenderTask task = new ServiceTicketLogoutSenderTask(serviceTicket);
	    		threadPool.submit(task);
	    	}
	    	
	    	ticketGrantingTicket.clearServiceTickets();
	    	
	    	TicketGrantingTicketCache cache = TicketGrantingTicketCache.getInstance( CASConfiguration.getCacheTimeout() );
	    	TicketGrantingTicketCacheKey key = new TicketGrantingTicketCacheKey(ticketGrantingTicketId);
	    	cache.clearCacheEntry(key);
	    	log.debug("CAS ticket granting ticket removed: " + ticketGrantingTicketId);
	        
		} catch(Exception ex) {
			log.debug("CAS session logout failed for " + ticketGrantingTicketId);
			if( log.isDebugEnabled() ) {
				ex.printStackTrace();
			}
		}
    }

    private class ServiceTicketLogoutSenderTask implements Runnable {

        private ServiceTicket serviceTicket;
    	private int readTimeout = 30000;
    	private int connectionTimeout = 30000;

        public ServiceTicketLogoutSenderTask(ServiceTicket serviceTicket) {
            this.serviceTicket = serviceTicket;
        }

        public void run() {
        	ServiceTicketCache cache = ServiceTicketCache.getInstance( CASConfiguration.getCacheTimeout() );
    		String serviceTicketId = serviceTicket.getId();
//    		String serviceProviderUrl = CASSSOUtil.getServiceProviderUrl(serviceTicket.getService());
    		String serviceProviderUrl = serviceTicket.getOriginalUrl();
    		
        	log.debug("logout serviceProviderUrl: "+serviceProviderUrl);
        	
            List<NameValuePair> logoutReqParams = new ArrayList<NameValuePair>();
            // set the logout request
            logoutReqParams.add(new BasicNameValuePair("logoutRequest", buildLogoutRequest(serviceTicket.getId())));
            try {
            	HttpClient httpClient = buildHttpClient(serviceProviderUrl);
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(logoutReqParams, "UTF-8");
                HttpPost httpPost = new HttpPost(serviceProviderUrl);
                httpPost.setEntity(entity);
                httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
                
                HttpResponse response = null;
                boolean isSuccessfullyLogout = false;
                for (int currentRetryCount = 0; currentRetryCount < CASConfiguration.getLogoutRetryLimit(); currentRetryCount++) {
                    int statusCode = 0;
                    
                    //Completely consume the previous response before retrying 
                    if (response != null) {
                    	HttpEntity httpEntity = response.getEntity();
    					if (httpEntity != null && httpEntity.isStreaming()) {
    						InputStream instream = httpEntity.getContent();
    						if (instream != null)
    							instream.close();
    					}
                    }
                    
                    // send the logout request as a POST
                    try {
                        response = httpClient.execute(httpPost);
                        statusCode = response.getStatusLine().getStatusCode();
                    } catch (IOException e) {
                        // ignore this exception since retrying is enabled if response is null.
                    }
                    if (response != null && CASSSOUtil.isHttpSuccessStatusCode(statusCode)) {
                        log.debug("single logout request is sent to : " + serviceProviderUrl +
                                 " is returned with " + HttpStatus.getStatusText(response.getStatusLine().getStatusCode()));
                        isSuccessfullyLogout = true;
                        break;
                    } else {
                        if (statusCode != 0) {
                            log.debug("Failed single logout response from " +
                            		serviceProviderUrl + " with status code " +
                                     HttpStatus.getStatusText(statusCode));
                        }
                        try {
                            synchronized (Thread.currentThread()) {
                                Thread.currentThread().wait(CASConfiguration.getLogoutRetryInterval());
                            }
                            log.debug("Sending single log out request again with retry count " +
                                     (currentRetryCount + 1) + " after waiting for " +
                                     CASConfiguration.getLogoutRetryInterval() + " milli seconds to " +
                                     serviceProviderUrl);
                        } catch (InterruptedException e) {
                            // Do nothing
                        }
                    }

                }
                if (!isSuccessfullyLogout) {
                    log.info("Single logout failed for "+ serviceProviderUrl + "after retrying " + CASConfiguration.getLogoutRetryLimit()  + " times ");
                } else {
                	log.debug("CAS session ticket removed: " + serviceTicket.getId());
                }

            } catch (IOException e) {
                log.error("Error sending logout requests to : " +
                		serviceProviderUrl, e);
            } catch (GeneralSecurityException e) {
                log.error("Error registering the EasySSLProtocolSocketFactory", e);
            } catch (RuntimeException e) {
                log.error("Runtime exception occurred.", e);
            } catch (URISyntaxException e) {
                log.error("Error deriving port from the assertion consumer url", e);
            } finally {
        		ServiceTicketCacheKey key = new ServiceTicketCacheKey(serviceTicketId);
        		cache.clearCacheEntry(key);
            }
        }
        
        private HttpClient buildHttpClient(String serviceProviderUrl) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, UnsupportedEncodingException {
            boolean usesHTTPS = true;
        	int port = 443;    // use 443 as the default port
            try {
                URI uri = new URI(serviceProviderUrl);
                if (uri.getPort() != -1) {    // if the port is mentioned in the URL
                    port = uri.getPort();
                } else if ("http".equals(uri.getScheme())) {  // if it is using http
                    port = 80;
                }
                
                usesHTTPS = "https".equals(uri.getScheme());
            } catch (URISyntaxException e) {
                log.error("Error deriving port from the assertion consumer url", e);
                throw e;
            }

            HttpClient httpClient = new DefaultHttpClient();
            
            if( usesHTTPS ) {
            	httpClient.getConnectionManager().getSchemeRegistry().register(buildSecureScheme(port));
            }
            
            // this one causes a timeout if a connection is established but there is 
            // no response within 10 seconds
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, readTimeout);

            // this one causes a timeout if no connection is established within 10 seconds
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);

            
            return httpClient;
        }
        
        @SuppressWarnings("deprecation")
		private Scheme buildSecureScheme(int port) throws NoSuchAlgorithmException, KeyManagementException {
            TrustManager easyTrustManager = new X509TrustManager() {
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] x509Certificates,
                        String s)
                        throws java.security.cert.CertificateException {
                }

                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] x509Certificates,
                        String s)
                        throws java.security.cert.CertificateException {
                }

                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{easyTrustManager}, null);
            SSLSocketFactory sf = new SSLSocketFactory(sslContext);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            Scheme httpsScheme = new Scheme("https", sf, port);
            
            return httpsScheme;
        }
        
        private String buildLogoutRequest(String serviceTicketId) {
	        String logoutRequest = "<samlp:LogoutRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\""
	                + UUIDGenerator.generateUUID().replaceAll("-", "")
	                + "\" Version=\"2.0\" IssueInstant=\"" + CASSSOUtil.formatSoapDate(new Date())
	                + "\"><saml:NameID xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">@NOT_USED@</saml:NameID><samlp:SessionIndex>"
	                + serviceTicketId + "</samlp:SessionIndex></samlp:LogoutRequest>";
	        
	        return logoutRequest;
        }
    }
}
