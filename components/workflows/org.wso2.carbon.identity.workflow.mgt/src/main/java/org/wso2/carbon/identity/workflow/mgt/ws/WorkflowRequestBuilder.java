/*
 * Copyright (c) 2015 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
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

package org.wso2.carbon.identity.workflow.mgt.ws;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.workflow.mgt.WorkflowException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WorkflowRequestBuilder {

    private static final String WF_NS = "http://bpel.mgt.workflow.carbon.wso2.org";
    private static final String WF_NS_PREFIX = "cwf";
    private static final String WF_REQ_ROOT_ELEM = "ProcessRequest";
    private static final String WF_REQ_UUID_ELEM = "uuid";
    private static final String WF_REQ_ACTION_ELEM = "eventType";
    private static final String WF_REQ_TENANT_DOMAIN_ELEM = "tenantDomain";
    //    private static final String WF_REQ_PARAMS_ELEM = "params";
    private static final String WF_REQ_PARAM_ELEM = "parameter";
    private static final String WF_REQ_LIST_ITEM_ELEM = "item";
    private static final String WF_REQ_KEY_ATTRIB = "name";
    private static final String WF_REQ_VALUE_ELEM = "value";

    private static final Set<Class> SUPPORTED_CLASS_TYPES;

    static {
        //only following types of objects will be allowed as values to the parameters.
        SUPPORTED_CLASS_TYPES = new HashSet<Class>();
        SUPPORTED_CLASS_TYPES.add(String.class);
        SUPPORTED_CLASS_TYPES.add(Integer.class);
        SUPPORTED_CLASS_TYPES.add(Double.class);
        SUPPORTED_CLASS_TYPES.add(Float.class);
        SUPPORTED_CLASS_TYPES.add(Long.class);
        SUPPORTED_CLASS_TYPES.add(Character.class);
        SUPPORTED_CLASS_TYPES.add(Byte.class);
        SUPPORTED_CLASS_TYPES.add(Short.class);
        SUPPORTED_CLASS_TYPES.add(Boolean.class);
    }

    private String uuid;
    private String action;
    private String tenantDomain;
    private Map<String, Object> singleValuedParams;
    private Map<String, List<Object>> listTypeParams;
    private Map<String, Map<String, Object>> mapTypeParams;

    /**
     * Initialize the Request builder with uuid and action
     *
     * @param uuid   Uniquely identifies the workflow
     * @param action The identifier for the action for which the workflow was triggered
     */
    public WorkflowRequestBuilder(String uuid, String action, String tenantDomain) {
        this.uuid = uuid;
        this.action = action;
        this.tenantDomain = tenantDomain;
        singleValuedParams = new HashMap<String, Object>();
        listTypeParams = new HashMap<String, List<Object>>();
        mapTypeParams = new HashMap<String, Map<String, Object>>();
    }

    /**
     * Adds a parameter with a single value. eg. tenantDomain="carbon.super"
     *
     * @param key   The param name
     * @param value The param value, must be either a wrapper for a primitive or String
     * @return This builder instance
     */
    public WorkflowRequestBuilder addSingleValuedParam(String key, Object value) {
        if (StringUtils.isNotBlank(key)) {
            if (isValidValue(value)) {
                singleValuedParams.put(key, value);
                return this;
            } else {
                throw new IllegalArgumentException(
                        "Value provided for " + key + " is not acceptable. Use either string, " +
                                "boolean, or numeric value");
            }
        } else {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
    }

    /**
     * Check whether the given object is of valid type
     *
     * @param obj The object to be tested
     * @return
     */
    private boolean isValidValue(Object obj) {
        return obj != null && SUPPORTED_CLASS_TYPES.contains(obj.getClass());
    }

    /**
     * Adds a parameter with a list value. eg. roleList={"admin","manager"}
     *
     * @param key   The param name
     * @param value The param value list, each item must be either a wrapper for a primitive or String
     * @return This builder instance
     */
    public WorkflowRequestBuilder addListTypeParam(String key, List<Object> value) {
        if (StringUtils.isNotBlank(key)) {
            if (value != null) {
                for (Object o : value) {
                    if (!isValidValue(o)) {
                        throw new IllegalArgumentException(
                                "At least one value provided for " + key + " is not acceptable" +
                                        ". Use either string, boolean, or numeric value");
                    }
                }
                listTypeParams.put(key, value);
                return this;
            } else {
                throw new IllegalArgumentException("Value for " + key + " cannot be null");
            }
        } else {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
    }

    /**
     * Adds a parameter with a map value. eg. claimList={"First Name" = "Joan","Last Name" = "Doe"}
     *
     * @param key   The param name
     * @param value The param value map, each key of the map should be String, and each value must be either a wrapper
     *              to a primitive or String
     * @return This builder instance
     */
    public WorkflowRequestBuilder addMapTypeParam(String key, Map<String, Object> value) {
        if (StringUtils.isNotBlank(key)) {
            if (value != null) {
                for (Map.Entry<String, Object> entry : value.entrySet()) {
                    if (StringUtils.isBlank(entry.getKey())) {
                        throw new IllegalArgumentException("Map item's key value cannot be null");
                    }
                    if (!isValidValue(entry.getValue())) {
                        throw new IllegalArgumentException(
                                "Value provided for " + entry.getKey() + " is not acceptable" +
                                        ". Use either string, boolean, or numeric value");
                    }
                }
                mapTypeParams.put(key, value);
                return this;
            } else {
                throw new IllegalArgumentException("Value for " + key + " cannot be null");
            }
        } else {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
    }

    /**
     * Builds the SOAP request body for the Service endpoint
     *
     * @return
     * @throws WorkflowException
     */
    public OMElement buildRequest() throws WorkflowException {
        OMFactory omFactory = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = omFactory.createOMNamespace(WF_NS, WF_NS_PREFIX);
        OMElement rootElement = omFactory.createOMElement(WF_REQ_ROOT_ELEM, omNs);
        OMElement uuidElement = omFactory.createOMElement(WF_REQ_UUID_ELEM, omNs);
        uuidElement.setText(uuid);
        rootElement.addChild(uuidElement);
        OMElement reqIdElement = omFactory.createOMElement(WF_REQ_ACTION_ELEM, omNs);
        reqIdElement.setText(action);
        rootElement.addChild(reqIdElement);
        OMElement tenantDomainElement = omFactory.createOMElement(WF_REQ_TENANT_DOMAIN_ELEM, omNs);
        tenantDomainElement.setText(tenantDomain);
        rootElement.addChild(tenantDomainElement);
        for (Map.Entry<String, Object> entry : singleValuedParams.entrySet()) {
            OMElement paramElement = omFactory.createOMElement(WF_REQ_PARAM_ELEM, omNs);
            OMAttribute paramNameAttribute = omFactory.createOMAttribute(WF_REQ_KEY_ATTRIB, null, entry.getKey());
            paramElement.addAttribute(paramNameAttribute);
            paramElement.setText(entry.getValue().toString());
            rootElement.addChild(paramElement);
        }
        for (Map.Entry<String, List<Object>> entry : listTypeParams.entrySet()) {
            OMElement paramElement = omFactory.createOMElement(WF_REQ_PARAM_ELEM, omNs);
            OMAttribute paramNameAttribute = omFactory.createOMAttribute(WF_REQ_KEY_ATTRIB, null, entry.getKey());
            paramElement.addAttribute(paramNameAttribute);
            for (Object listItem : entry.getValue()) {
                if (listItem != null) {
                    OMElement listItemElement = omFactory.createOMElement(WF_REQ_LIST_ITEM_ELEM, null);
                    listItemElement.setText(listItem.toString());
                    paramElement.addChild(listItemElement);
                }
            }
            rootElement.addChild(paramElement);
        }

        for (Map.Entry<String, Map<String, Object>> entry : mapTypeParams.entrySet()) {
            OMElement paramElement = omFactory.createOMElement(WF_REQ_PARAM_ELEM, omNs);
            OMAttribute paramNameAttribute = omFactory.createOMAttribute(WF_REQ_KEY_ATTRIB, null, entry.getKey());
            paramElement.addAttribute(paramNameAttribute);
            for (Map.Entry<String, Object> mapItem : entry.getValue().entrySet()) {
                if (mapItem.getKey() != null && mapItem.getValue() != null) {
                    OMElement listItemElement = omFactory.createOMElement(WF_REQ_LIST_ITEM_ELEM, null);
                    OMAttribute itemNameAttribute = omFactory.createOMAttribute(WF_REQ_KEY_ATTRIB, null, mapItem.getKey
                            ());
                    listItemElement.addAttribute(itemNameAttribute);
                    listItemElement.setText(mapItem.getValue().toString());
                    paramElement.addChild(listItemElement);
                }
            }
            rootElement.addChild(paramElement);
        }
        return rootElement;
    }
}
