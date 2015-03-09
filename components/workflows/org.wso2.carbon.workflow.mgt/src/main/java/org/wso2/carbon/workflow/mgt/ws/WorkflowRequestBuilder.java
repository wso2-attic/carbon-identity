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

package org.wso2.carbon.workflow.mgt.ws;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.workflow.mgt.WorkflowException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WorkflowRequestBuilder {

    private static final String WF_NS = "http://workflow.is.wso2.org/";
    private static final String WF_NS_PREFIX = "wor";
    private static final String WF_REQ_ROOT_ELEM = "GenericWorkflowRequest";
    private static final String WF_REQ_UUID_ELEM = "uuid";
    private static final String WF_REQ_ACTION_ELEM = "action";
    private static final String WF_REQ_TENANT_DOMAIN_ELEM = "tenantDomain";
    private static final String WF_REQ_PARAMS_ELEM = "params";
    private static final String WF_REQ_PARAM_ELEM = "param";
    private static final String WF_REQ_LIST_ITEM_ELEM = "item";
    private static final String WF_REQ_KEY_ATTRIB = "key";

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
     * @throws org.wso2.carbon.workflow.mgt.WorkflowException
     */
    public String buildRequest() throws WorkflowException {
        StringWriter writer = new StringWriter();
        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
        try {
            XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(writer);
            xmlStreamWriter.writeStartElement(WF_NS_PREFIX, WF_REQ_ROOT_ELEM, WF_NS);
            xmlStreamWriter.writeNamespace(WF_NS_PREFIX, WF_NS);

            xmlStreamWriter.writeStartElement(WF_NS, WF_REQ_UUID_ELEM);
            xmlStreamWriter.writeCharacters(uuid);
            xmlStreamWriter.writeEndElement();

            xmlStreamWriter.writeStartElement(WF_NS, WF_REQ_ACTION_ELEM);
            xmlStreamWriter.writeCharacters(action);
            xmlStreamWriter.writeEndElement();

            xmlStreamWriter.writeStartElement(WF_NS, WF_REQ_TENANT_DOMAIN_ELEM);
            xmlStreamWriter.writeCharacters(tenantDomain);
            xmlStreamWriter.writeEndElement();

            xmlStreamWriter.writeStartElement(WF_NS, WF_REQ_PARAMS_ELEM);

            for (Map.Entry<String, Object> entry : singleValuedParams.entrySet()) {
                xmlStreamWriter.writeStartElement(WF_NS, WF_REQ_PARAM_ELEM);
                xmlStreamWriter.writeAttribute(WF_REQ_KEY_ATTRIB, entry.getKey());
                xmlStreamWriter.writeCharacters(entry.getValue().toString());
                xmlStreamWriter.writeEndElement();  //for param elem
            }

            for (Map.Entry<String, List<Object>> entry : listTypeParams.entrySet()) {
                xmlStreamWriter.writeStartElement(WF_NS, WF_REQ_PARAM_ELEM);
                xmlStreamWriter.writeAttribute(WF_REQ_KEY_ATTRIB, entry.getKey());
                for (Object listItem : entry.getValue()) {
                    if (listItem != null) {
                        xmlStreamWriter.writeStartElement(WF_NS, WF_REQ_LIST_ITEM_ELEM);
                        xmlStreamWriter.writeCharacters(listItem.toString());
                        xmlStreamWriter.writeEndElement();
                    }
                }
                xmlStreamWriter.writeEndElement();  //for param elem
            }

            for (Map.Entry<String, Map<String, Object>> entry : mapTypeParams.entrySet()) {
                xmlStreamWriter.writeStartElement(WF_NS, WF_REQ_PARAM_ELEM);
                xmlStreamWriter.writeAttribute(WF_REQ_KEY_ATTRIB, entry.getKey());
                for (Map.Entry<String, Object> mapItem : entry.getValue().entrySet()) {
                    if (mapItem.getKey() != null && mapItem.getValue() != null) {
                        xmlStreamWriter.writeStartElement(WF_NS, WF_REQ_LIST_ITEM_ELEM);
                        xmlStreamWriter.writeAttribute(WF_REQ_KEY_ATTRIB, mapItem.getKey());
                        xmlStreamWriter.writeCharacters(mapItem.getValue().toString());
                        xmlStreamWriter.writeEndElement();
                    }
                }
                xmlStreamWriter.writeEndElement();  //for param elem
            }
            xmlStreamWriter.writeEndElement();  //for params elem
            xmlStreamWriter.writeEndElement();  //for root elem
            xmlStreamWriter.close();

        } catch (XMLStreamException e) {
            throw new WorkflowException("Error when building workflow request for action: " + action, e);
        }
        return null;
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
}
