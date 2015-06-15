/*
 *
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.apacheds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Super class which encapsulates domain entry information.
 * Domain entry information includes attributes that can be in a store entry.
 */
@SuppressWarnings({"UnusedDeclaration"})
public class DomainNameEntry {

    private static final Logger LOG = LoggerFactory.getLogger(DomainNameEntry.class);

    // The default object classes to use for admin
    protected List<String> objectClassList = new ArrayList<String>();

    /**
     * Adds a new object class.
     *
     * @param objectClass Object class name to add
     */
    public void addObjectClass(String objectClass) {
        for (String anObjectClassList : this.objectClassList) {
            if (objectClass.equals(anObjectClassList)) {
                LOG.info("Object class " + objectClass + " already in the list. Not adding.");
                return;
            }
        }

        this.objectClassList.add(objectClass);
    }

    /**
     * Returns all object classes as a read only collection.
     *
     * @return A readonly list of object classes.
     */
    public List<String> getObjectClasses() {
        return Collections.unmodifiableList(this.objectClassList);
    }

}
