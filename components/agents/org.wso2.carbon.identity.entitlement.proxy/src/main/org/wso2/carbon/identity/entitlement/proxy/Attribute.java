/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.identity.entitlement.proxy;

public class Attribute {

    private String type;
    private String id;
    private String value;
    private String category;

    public Attribute(String category, String id, String type, String value) {
        this.category = category;
        this.id = id;
        this.type = type;
        this.value = value;
    }


    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 7 * result + ((type == null) ? 0 : type.hashCode());
        result = 17 * result + ((id == null) ? 0 : id.hashCode());
        result = 37 * result + ((value == null) ? 0 : value.hashCode());
        result = 57 * result + ((category == null) ? 0 : category.hashCode());
        return result;
    }

    @Override
    public  boolean equals(Object o){

        if(o instanceof Attribute){
            Attribute comp = (Attribute)o;
            boolean t = ((type == null)? false : (type.equals(comp.getType())));
            boolean i = ((id == null))? false : (type.equals(comp.getId()));
            boolean v = ((value == null)? false: (type.equals(comp.getValue())));
            boolean c = ((category == null)? false: (category.equals(comp.getCategory())));

            return (t && i && v && c);
        }

        return false;

    }

}
