package org.wso2.carbon.identity.sso.cas.ticket;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.opensaml.saml1.binding.artifact.SAML1ArtifactType0001;

public class SAMLTicketIdGenerator {
    /** Assertion handles are randomly-generated 20-byte identifiers. */
    private static final int ASSERTION_HANDLE_SIZE = 20;
	
	public static String generate(String ticketSeed) {
		String uniqueId = "UNKNOWN";
		
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA");
            messageDigest.update(ticketSeed.getBytes("8859_1"));
            byte[] ticketSeedDigest = messageDigest.digest();
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            
    		// Mimic existing JASIG CAS handling for SAMLart hash
            uniqueId = new SAML1ArtifactType0001(ticketSeedDigest, newAssertionHandle(random)).base64Encode();
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot get SHA1PRNG secure random instance.");
        } catch (final Exception e) {
            throw new IllegalStateException("Exception generating digest of source ID.", e);
        }
        
        return uniqueId;
	}
	
    private static byte[] newAssertionHandle(SecureRandom random) {
        final byte[] handle = new byte[ASSERTION_HANDLE_SIZE];
        random.nextBytes(handle);
        return handle;
    }
}
