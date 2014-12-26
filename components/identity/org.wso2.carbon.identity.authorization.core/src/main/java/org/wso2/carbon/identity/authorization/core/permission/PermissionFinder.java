///*
//*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//*
//*  WSO2 Inc. licenses this file to you under the Apache License,
//*  Version 2.0 (the "License"); you may not use this file except
//*  in compliance with the License.
//*  You may obtain a copy of the License at
//*
//*    http://www.apache.org/licenses/LICENSE-2.0
//*
//* Unless required by applicable law or agreed to in writing,
//* software distributed under the License is distributed on an
//* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//* KIND, either express or implied.  See the License for the
//* specific language governing permissions and limitations
//* under the License.
//*/
//
//package org.wso2.carbon.identity.authorization.core.permission;
//
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.wso2.carbon.identity.authorization.core.dto.PermissionModule;
//import org.wso2.carbon.identity.authorization.core.dto.PermissionTreeNodeDTO;
//import org.wso2.carbon.identity.authorization.core.internal.AuthorizationConfigHolder;
//import org.wso2.carbon.identity.authorization.core.internal.AuthorizationServiceComponent;
//
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
///**
// * 
// */
//public class PermissionFinder {
//
//    Set<PermissionFinderModule> finderModules = new HashSet<PermissionFinderModule>();
//    
//
//    private static Log log = LogFactory.getLog(PermissionFinder.class);
//
//    public PermissionFinder() {
//
//        AuthorizationConfigHolder configHolder = AuthorizationServiceComponent.getConfigHolder();
//        if(configHolder.getPermissionFinderModules() != null){
//            finderModules = configHolder.getPermissionFinderModules().keySet();
//        }       
//    }
//
//    public PermissionTreeNodeDTO getPermissionTreeNodes(String finderModule, String root,
//                                                        String secondary, String filter){
//
//        for(PermissionFinderModule module : finderModules){
//            if(finderModule.equals(module.getModuleName())){
//                    PermissionTreeNodeDTO nodeDTO = module.getPermissionTree(root, secondary, filter);
//                    nodeDTO.setFullPathSupported(module.isFullPathSupported());
//                    nodeDTO.setHierarchicalTree(module.isHierarchicalTree());
//                    nodeDTO.setRootId(root);
//                    nodeDTO.setModuleName(module.getModuleName());
//                    nodeDTO.setRootIdentifier(module.getRootIdentifier());
//                    Set<String> actions = module.getSupportedActions();
//                    if(actions != null){
//                        nodeDTO.setSupportedActions(actions.toArray(new String[actions.size()]));
//                    }
//                    return  nodeDTO;
//            }
//        }
//        return null;
//    }
//
//    public String[] getModuleNames(){
//
//        Set<String> moduleNames = new HashSet<String>();
//
//        for(PermissionFinderModule module : finderModules){
//            moduleNames.add(module.getModuleName());
//        }
//        
//        return moduleNames.toArray(new String[moduleNames.size()]);    
//    }
//
//    public PermissionModule getModuleInfo(String moduleName){
//
//        PermissionModule moduleDTO = null;
//        if(moduleName != null){
//            for(PermissionFinderModule module : finderModules){
//                if(moduleName.equals(module.getModuleName())){
//                    moduleDTO = new PermissionModule();
//                    moduleDTO.setFullyPathSupported(module.isFullPathSupported());
//                    moduleDTO.setHierarchicalTree(module.isHierarchicalTree());
//                    moduleDTO.setModuleName(module.getModuleName());
//                    moduleDTO.setRootIdentifier(module.getRootIdentifier());
//                    moduleDTO.setSecondaryRootSupported(module.isSecondaryRootSupported());
//                    Set<String> actions = module.getSupportedActions();
//                    if(actions != null){
//                        moduleDTO.setSupportedActions(actions.toArray(new String[actions.size()]));
//                    }
//                    List<String> childSetNames = module.getNameForChildRootNodeSet();
//                    if(childSetNames != null){
//                        moduleDTO.setNameForChildRootNodeSet(childSetNames.toArray(new String[childSetNames.size()]));   
//                    }
//
//                }
//            }
//        }
//
//        return moduleDTO;
//    }
//
//    public String[] getRootNodes(String moduleName, String filter){
//
//        if(moduleName != null){
//            for(PermissionFinderModule module : finderModules){
//                if(moduleName.equals(module.getModuleName())){
//                    Set<String> nodes = module.getRootNodeNames(filter);
//                    if(nodes != null){
//                        return nodes.toArray(new String[nodes.size()]);
//                    }
//                }
//            }
//        }
//
//        return new String[0];
//    }
//
//    public String[] getRootSecondaryNodes(String moduleName, String  rootNode, String filter){
//
//        if(moduleName != null && rootNode != null){
//            for(PermissionFinderModule module : finderModules){
//                if(moduleName.equals(module.getModuleName()) && module.isSecondaryRootSupported()){
//                    Set<String> nodes = module.getSecondaryRootNodeNames(rootNode, filter);
//                    if(nodes != null){
//                        return nodes.toArray(new String[nodes.size()]);
//                    }
//                }
//            }
//        }
//
//        return new String[0];
//    }
//}
