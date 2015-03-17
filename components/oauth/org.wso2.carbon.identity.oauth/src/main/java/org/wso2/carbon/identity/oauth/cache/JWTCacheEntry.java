package org.wso2.carbon.identity.oauth.cache;

import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;

public class JWTCacheEntry extends CacheEntry{
    private String encodedJWt;

    public JWTCacheEntry(SignedJWT jwt ) {
        this.encodedJWt = jwt.serialize();
    }

    public SignedJWT getJwt() throws ParseException {
        return SignedJWT.parse(this.encodedJWt);
    }

    public String getEncodedJWt(){
        return this.encodedJWt;
    }


}
