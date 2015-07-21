/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.provider.openid.replication;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Association cleanup task which is running for pre-defined period to clear the expired associations
 */
public class AssociationCleanupService {

    private final ScheduledExecutorService scheduler;
    private final long initialDelay;
    private final long delayBetweenRuns;

    private static final int NUM_THREADS = 1;

    private static Log log = LogFactory.getLog(AssociationCleanupService.class);

    public AssociationCleanupService(long initialDelay, long delayBetweenRuns) {
        this.initialDelay = initialDelay;
        this.delayBetweenRuns = delayBetweenRuns;
        this.scheduler = Executors.newScheduledThreadPool(NUM_THREADS);
    }

    public void activateCleanUp() {
        Runnable associationCleanupTask = new AssociationCleanupTask();
        scheduler.scheduleWithFixedDelay(associationCleanupTask, initialDelay, delayBetweenRuns, TimeUnit.MINUTES);
    }

    private static final class AssociationCleanupTask implements Runnable{

        private static Log log = LogFactory.getLog(AssociationCleanupTask.class);

        @Override
        public void run() {
            log.debug("Start running the Association cleanup task.");
            OpenIDAssociationReplicationManager.getPersistenceManager().removeExpiredAssociations();
            log.info("Association cleanup task is completed successfully for removing expired Associations");
        }

    }

}