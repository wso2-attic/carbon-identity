/*
 *
 * Copyright (c) 2005, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.apacheds.impl;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.Hashtable;

public class MyUser implements DirContext {

    Attributes myAttrs;

    public MyUser(String userId, String surName, String commonName) {
        myAttrs = new BasicAttributes(true);  // Case ignore
        Attribute oc = new BasicAttribute("objectclass");
        oc.add("inetOrgPerson");
        oc.add("organizationalPerson");
        oc.add("person");
        oc.add("top");

        Attribute sn = new BasicAttribute("sn");
        sn.add(surName);

        Attribute cn = new BasicAttribute("cn");
        cn.add(commonName);

        Attribute uid = new BasicAttribute("uid");
        uid.add(userId);

        myAttrs.put(sn);
        myAttrs.put(cn);
        myAttrs.put(uid);
        myAttrs.put(oc);

    }

    public Attributes getAttributes(Name name)
        throws NamingException {
         return (Attributes)myAttrs.clone();
    }

    public Attributes getAttributes(String name)
        throws NamingException {
        return (Attributes)myAttrs.clone();
    }

    public Attributes getAttributes(Name name, String[] attrIds)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Attributes getAttributes(String name, String[] attrIds)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void modifyAttributes(Name name, int mod_op, Attributes attrs)
        throws NamingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void modifyAttributes(String name, int mod_op, Attributes attrs)
        throws NamingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void modifyAttributes(Name name, ModificationItem[] mods)
        throws NamingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void modifyAttributes(String name, ModificationItem[] mods)
        throws NamingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void bind(Name name, Object obj, Attributes attrs)
        throws NamingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void bind(String name, Object obj, Attributes attrs)
        throws NamingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void rebind(Name name, Object obj, Attributes attrs)
        throws NamingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void rebind(String name, Object obj, Attributes attrs)
        throws NamingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public DirContext createSubcontext(Name name, Attributes attrs)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public DirContext createSubcontext(String name, Attributes attrs)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public DirContext getSchema(Name name)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public DirContext getSchema(String name)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public DirContext getSchemaClassDefinition(Name name)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public DirContext getSchemaClassDefinition(String name)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes,
                                                  String[] attributesToReturn)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs,
                                                  SearchControls cons)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs,
                                                  SearchControls cons)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object lookup(Name name)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object lookup(String name)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void bind(Name name, Object obj)
        throws NamingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void bind(String name, Object obj)
        throws NamingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void rebind(Name name, Object obj)
        throws NamingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void rebind(String name, Object obj)
        throws NamingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void unbind(Name name)
        throws NamingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void unbind(String name)
        throws NamingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void rename(Name oldName, Name newName)
        throws NamingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void rename(String oldName, String newName)
        throws NamingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public NamingEnumeration<NameClassPair> list(Name name)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NamingEnumeration<NameClassPair> list(String name)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NamingEnumeration<Binding> listBindings(Name name)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NamingEnumeration<Binding> listBindings(String name)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void destroySubcontext(Name name)
        throws NamingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void destroySubcontext(String name)
        throws NamingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Context createSubcontext(Name name)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Context createSubcontext(String name)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object lookupLink(Name name)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object lookupLink(String name)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NameParser getNameParser(Name name)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NameParser getNameParser(String name)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Name composeName(Name name, Name prefix)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String composeName(String name, String prefix)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object addToEnvironment(String propName, Object propVal)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object removeFromEnvironment(String propName)
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Hashtable<?, ?> getEnvironment()
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void close()
        throws NamingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getNameInNamespace()
        throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}