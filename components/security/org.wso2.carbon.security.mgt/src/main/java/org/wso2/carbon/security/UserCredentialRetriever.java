package org.wso2.carbon.security;


/*
This interface is defined to provide plain text password for username token scenarios where user sends the digested
password. In default callback handler class uses this class. Usually, this interface is implemented by custom userstore managers.
 */
public interface UserCredentialRetriever {
    /**
     * Provide the password based on user store implementation.
     * @param username - Domain less username; eg; fooUser but not Domain/fooUser
     * @return - plain text password of username
     * @throws Exception - throws if failed to provide plain text password.
     */
    String getPassword(String username) throws Exception;
}
