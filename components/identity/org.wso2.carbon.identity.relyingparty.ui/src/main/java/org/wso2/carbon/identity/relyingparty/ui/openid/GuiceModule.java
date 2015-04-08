/**
 * Copyright 2010 Google Inc.
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
 *
 */
package org.wso2.carbon.identity.relyingparty.ui.openid;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.step2.discovery.DefaultHostMetaFetcher;
import com.google.step2.discovery.HostMetaFetcher;
import com.google.step2.hybrid.HybridOauthMessage;
import com.google.step2.openid.ax2.AxMessage2;
import com.google.step2.xmlsimplesign.CertValidator;
import com.google.step2.xmlsimplesign.CnConstraintCertValidator;
import com.google.step2.xmlsimplesign.DefaultCertValidator;
import com.google.step2.xmlsimplesign.DisjunctiveCertValidator;
import org.openid4java.consumer.ConsumerAssociationStore;
import org.openid4java.message.Message;
import org.openid4java.message.MessageException;

/**
 * Guice module for configuring the Step2 library.  Modified from the original example consumer
 * in the Step2 library to be slightly simpler.
 */
public class GuiceModule extends AbstractModule {

    ConsumerAssociationStore associationStore;

    public GuiceModule(ConsumerAssociationStore associationStore) {
        this.associationStore = associationStore;
    }

    @Override
    protected void configure() {
        try {
            Message.addExtensionFactory(AxMessage2.class);
        } catch (MessageException e) {
            throw new CreationException(null);
        }

        try {
            Message.addExtensionFactory(HybridOauthMessage.class);
        } catch (MessageException e) {
            throw new CreationException(null);
        }

        bind(ConsumerAssociationStore.class)
                .toInstance(associationStore);


    }

    /**
     * Simple detection of whether or not we're running under GAE.
     *
     * @return True if running on app engine.
     */
    private boolean isRunningOnAppengine() {
        if (System.getSecurityManager() == null) {
            return false;
        }
        return System.getSecurityManager().getClass().getCanonicalName()
                .startsWith("com.google");
    }

    @Provides
    @Singleton
    public CertValidator provideCertValidator(DefaultCertValidator defaultValidator) {
        CertValidator hardCodedValidator = new CnConstraintCertValidator() {
            @Override
            protected String getRequiredCn(String authority) {
                // Trust Google for signing discovery documents
                return "hosted-id.google.com";
            }
        };

        return new DisjunctiveCertValidator(defaultValidator, hardCodedValidator);
    }

    @Provides
    @Singleton
    public HostMetaFetcher provideHostMetaFetcher(
            DefaultHostMetaFetcher fetcher1,
            GoogleHostedHostMetaFetcher fetcher2) {
        // Domains may opt to host their own host-meta documents instead of outsourcing
        // to Google.  To try the domain's own host-meta, uncomment the SerialHostMetaFetcher
        // line to adopt a strategy that tries the domain's own version first then falls back
        // on the Google hosted version if that fails.  A parallel fetching strategy can also
        // be used to speed up fetching.
        //return new SerialHostMetaFetcher(fetcher1, fetcher2);
        return fetcher2;
    }


//    @Provides
//    @Singleton
//    public URLFetchService provideUrlFetchService() {
//        return URLFetchServiceFactory.getURLFetchService();
//    }

}
