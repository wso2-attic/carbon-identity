/*
 * Copyright 2004,2005 The Apache Software Foundation.
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

package org.wso2.carbon.identity.sts.store;

import java.io.Serializable;
import java.util.Date;
import java.util.Properties;

/**
 * Token is not fully serializable Object, therefore we are having problems while adding it to cache
 * Therefore we have build SerializableToken from Token.
 */
public class SerializableToken implements Serializable {

    private String id;
    private int state;
    private String token;
    private String previousToken;
    private String attachedReference;
    private String unattachedReference;
    private java.util.Properties properties;
    private boolean changed;
    private byte[] secret;
    private Date created;
    private Date expires;
    private String issuerAddress;
    private boolean persistenceEnabled;

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPreviousToken() {
        return previousToken;
    }

    public void setPreviousToken(String previousToken) {
        this.previousToken = previousToken;
    }

    public String getAttachedReference() {
        return attachedReference;
    }

    public void setAttachedReference(String attachedReference) {
        this.attachedReference = attachedReference;
    }

    public String getUnattachedReference() {
        return unattachedReference;
    }

    public void setUnattachedReference(String unattachedReference) {
        this.unattachedReference = unattachedReference;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public byte[] getSecret() {
        return secret;
    }

    public void setSecret(byte[] secret) {
        this.secret = secret;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getExpires() {
        return expires;
    }

    public void setExpires(Date expires) {
        this.expires = expires;
    }

    public String getIssuerAddress() {
        return issuerAddress;
    }

    public void setIssuerAddress(String issuerAddress) {
        this.issuerAddress = issuerAddress;
    }

    public boolean isPersistenceEnabled() {
        return persistenceEnabled;
    }

    public void setPersistenceEnabled(boolean persistenceEnabled) {
        this.persistenceEnabled = persistenceEnabled;
    }
}
