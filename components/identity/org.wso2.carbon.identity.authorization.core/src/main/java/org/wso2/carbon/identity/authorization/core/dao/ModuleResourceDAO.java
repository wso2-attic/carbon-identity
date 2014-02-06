package org.wso2.carbon.identity.authorization.core.dao;

import org.wso2.carbon.identity.authorization.core.dto.Resource;
import org.wso2.carbon.identity.authorization.core.jdbc.dao.JDBCConstantsDAO;

public abstract class ModuleResourceDAO extends GenericDAO {
	private int id;
	private int moduleId;
	private String resource;

	public int getModuleId() {
		return moduleId;
	}

	public void setModuleId(int moduleId) {
		this.moduleId = moduleId;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public ModuleResourceDAO map(Resource resource) {
		id = resource.getId();
		moduleId = resource.getModuleId();
		this.resource = resource.getName();
		if (resource.getState() <= 0) {
			setStatus(JDBCConstantsDAO.INSERT);
		} else {
			setStatus(resource.getState());
		}

		return this;
	}

}
