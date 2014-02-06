///*
// * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
// * 
// * WSO2 Inc. licenses this file to you under the Apache License,
// * Version 2.0 (the "License"); you may not use this file except
// * in compliance with the License.
// * You may obtain a copy of the License at
// * 
// * http://www.apache.org/licenses/LICENSE-2.0
// * 
// * Unless required by applicable law or agreed to in writing,
// * software distributed under the License is distributed on an
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// * KIND, either express or implied. See the License for the
// * specific language governing permissions and limitations
// * under the License.
// */
//
//package org.wso2.carbon.identity.authorization.core;
//
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.wso2.carbon.context.CarbonContext;
//import org.wso2.carbon.identity.authorization.core.dto.PermissionGroup;
//import org.wso2.carbon.identity.authorization.core.extension.PostAuthorizationExtension;
//import org.wso2.carbon.identity.authorization.core.internal.AuthorizationConfigHolder;
//import org.wso2.carbon.identity.authorization.core.internal.AuthorizationServiceComponent;
//import org.wso2.carbon.identity.authorization.core.jdbc.SQLExecutor;
//import org.wso2.carbon.user.api.AuthorizationManager;
//import org.wso2.carbon.user.api.UserStoreException;
//import org.wso2.carbon.user.core.authorization.JDBCAuthorizationManager;
//
//import java.util.*;
//
///**
// * 
// */
//public class IdentityAuthorizationManager {
//
//	private static IdentityAuthorizationManager identityAuthorizationManager;
//
//	private JDBCAuthorizationManager carbonAuthorizationManager;
//
//	private SQLExecutor sqlExecutor;
//
//	private final static Object lock = new Object();
//
//	Set<PostAuthorizationExtension> postExtensions = new HashSet<PostAuthorizationExtension>();
//
//	private static Log log = LogFactory.getLog(IdentityAuthorizationManager.class);
//
//	private IdentityAuthorizationManager() {
//
//		AuthorizationConfigHolder configHolder = AuthorizationServiceComponent.getConfigHolder();
//		if (configHolder.getPostExtensions() != null) {
//			postExtensions = configHolder.getPostExtensions().keySet();
//		}
//
//		int tenantId = CarbonContext.getCurrentContext().getTenantId();
//		try {
//			AuthorizationManager carbonAuthorizationManager =
//			                                                  AuthorizationServiceComponent.getRealmService()
//			                                                                               .getTenantUserRealm(tenantId)
//			                                                                               .getAuthorizationManager();
//			if (carbonAuthorizationManager instanceof JDBCAuthorizationManager) {
//				this.carbonAuthorizationManager =
//				                                  (JDBCAuthorizationManager) carbonAuthorizationManager;
//			} else {
//				log.error("Unsupported Authorization Manager implementation. "
//				          + "Identity Authorization manager can only work with JDBCAuthorizationManager");
//			}
//			sqlExecutor = SQLExecutor.getInstance();
//		} catch (UserStoreException e) {
//			log.error("Error while initializing authorization manager", e);
//		}
//	}
//
//	public static IdentityAuthorizationManager getInstance() {
//
//		if (identityAuthorizationManager == null) {
//			synchronized (lock) {
//				if (identityAuthorizationManager == null) {
//					identityAuthorizationManager = new IdentityAuthorizationManager();
//				}
//			}
//		}
//
//		return identityAuthorizationManager;
//	}
//
//	public void setAuthorization(String subject, String resource, String action,
//	                             boolean authorized, boolean isRBAC)
//	                                                                throws IdentityAuthorizationException {
//
//		if (log.isDebugEnabled()) {
//			log.debug("Persisting authorization for subject : " + subject + " resource : " +
//			          resource + " and action : " + action);
//		}
//
//		try {
//			carbonAuthorizationManager.addAuthorization(subject, resource, action, authorized,
//			                                            isRBAC);
//		} catch (UserStoreException e) {
//			log.error("Error persisting authorization data for subject : " + subject +
//			          " and resource : " + resource);
//			throw new IdentityAuthorizationException("Error persisting authorization data");
//		}
//
//		if (log.isDebugEnabled()) {
//			log.debug("Authorization data is persisted successfully");
//		}
//
//		if (postExtensions == null || postExtensions.size() == 0) {
//			if (log.isDebugEnabled()) {
//				log.debug("No Post extensions are configured");
//			}
//			return;
//		}
//
//		for (PostAuthorizationExtension postExtension : postExtensions) {
//			if (log.isDebugEnabled()) {
//				log.debug("Post extension is calling : " + postExtension.getClass().getName());
//			}
//			postExtension.doPostAuthorization(subject, resource, action, authorized,
//			                                  PostAuthorizationExtension.ADD);
//		}
//	}
//
//	public PermissionGroup[] getUserPermission(String userName, String moduleRoot) {
//
//		List<PermissionGroup> permissionDTOs = new ArrayList<PermissionGroup>();
//
//		if (log.isDebugEnabled()) {
//			log.debug("Populating explicitly defined user permission for user : " + userName);
//		}
//
//		Map<Integer, Integer> integerMap = sqlExecutor.populateUserPermissionId(userName, true);
//
//		if (integerMap != null && integerMap.size() > 0) {
//
//			for (Map.Entry<Integer, Integer> entry : integerMap.entrySet()) {
//
//				int permissionId = entry.getKey();
//				int value = entry.getValue();
//
//				if (permissionId == 0) {
//					continue;
//				}
//
//				if (log.isDebugEnabled()) {
//					log.debug("Populating permission data for user permission entry");
//				}
//
//				Map<String, String> stringMap = sqlExecutor.populateResourceId(permissionId);
//				for (Map.Entry<String, String> stringEntry : stringMap.entrySet()) {
//					String resource = stringEntry.getKey();
//					if (resource != null && resource.startsWith(moduleRoot)) {
//						PermissionGroup dto = new PermissionGroup();
//						dto.setPermissionId(resource);
//						dto.setAction(stringEntry.getValue());
//						dto.setSubject(userName);
//						dto.setUserPermission(true);
//						if (value == 1) {
//							dto.setAuthorized(true);
//						}
//						permissionDTOs.add(dto);
//					}
//				}
//			}
//		} else {
//			if (log.isDebugEnabled()) {
//				log.debug("No permissions are explicitly defined for user : " + userName);
//			}
//		}
//
//		return permissionDTOs.toArray(new PermissionGroup[permissionDTOs.size()]);
//	}
//
//	public PermissionGroup[] getRolePermission(String roleName, String moduleRoot) {
//
//		List<PermissionGroup> permissionDTOs = new ArrayList<PermissionGroup>();
//
//		if (log.isDebugEnabled()) {
//			log.debug("Populating explicitly defined role permission for role : " + roleName);
//		}
//
//		Map<Integer, Integer> integerMap = sqlExecutor.populateUserPermissionId(roleName, false);
//
//		if (integerMap != null && integerMap.size() > 0) {
//
//			for (Map.Entry<Integer, Integer> entry : integerMap.entrySet()) {
//
//				int permissionId = entry.getKey();
//				int value = entry.getValue();
//
//				if (permissionId == 0) {
//					continue;
//				}
//
//				if (log.isDebugEnabled()) {
//					log.debug("Populating permission data for user permission entry");
//				}
//
//				Map<String, String> stringMap = sqlExecutor.populateResourceId(permissionId);
//				for (Map.Entry<String, String> stringEntry : stringMap.entrySet()) {
//					String resource = stringEntry.getKey();
//					if (resource != null && resource.startsWith(moduleRoot)) {
//						PermissionGroup dto = new PermissionGroup();
//						dto.setPermissionId(resource);
//						dto.setAction(stringEntry.getValue());
//						dto.setSubject(roleName);
//						dto.setUserPermission(true);
//						if (value == 1) {
//							dto.setAuthorized(true);
//						}
//						permissionDTOs.add(dto);
//					}
//				}
//			}
//		} else {
//			if (log.isDebugEnabled()) {
//				log.debug("No permissions are explicitly defined for role : " + roleName);
//			}
//		}
//
//		return permissionDTOs.toArray(new PermissionGroup[permissionDTOs.size()]);
//	}
//
//	public void clearUserPermissions(String subject, String resource, String action, boolean isRBAC)
//	                                                                                                throws IdentityAuthorizationException {
//
//		if (log.isDebugEnabled()) {
//			log.debug("Clearing authorization for subject : " + subject + " resource : " +
//			          resource + " and action : " + action);
//		}
//
//		try {
//			if (isRBAC) {
//				if (resource == null || resource.trim().length() == 0) {
//					carbonAuthorizationManager.clearRoleAuthorization(subject);
//				} else {
//					if (action == null || action.trim().length() == 0) {
//						action = AuthorizationConstants.DEFAULT_ACTION;
//					}
//					carbonAuthorizationManager.clearRoleAuthorization(subject, resource, action);
//				}
//			} else {
//				if (resource == null || resource.trim().length() == 0) {
//					carbonAuthorizationManager.clearUserAuthorization(subject);
//				} else {
//					if (action == null || action.trim().length() == 0) {
//						action = AuthorizationConstants.DEFAULT_ACTION;
//					}
//					carbonAuthorizationManager.clearUserAuthorization(subject, resource, action);
//				}
//			}
//		} catch (UserStoreException e) {
//			log.error("Error while clearing user permission for user : " + subject, e);
//			throw new IdentityAuthorizationException("Error while clearing user permission", e);
//		}
//
//		if (log.isDebugEnabled()) {
//			log.debug("Authorization data is cleared successfully");
//		}
//
//		if (postExtensions == null || postExtensions.size() == 0) {
//			if (log.isDebugEnabled()) {
//				log.debug("No Post extensions are configured");
//			}
//			return;
//		}
//
//		for (PostAuthorizationExtension postExtension : postExtensions) {
//			if (log.isDebugEnabled()) {
//				log.debug("Post extension is calling : " + postExtension.getClass().getName());
//			}
//			postExtension.doPostAuthorization(subject, resource, action, false,
//			                                  PostAuthorizationExtension.REMOVE);
//		}
//	}
//}
