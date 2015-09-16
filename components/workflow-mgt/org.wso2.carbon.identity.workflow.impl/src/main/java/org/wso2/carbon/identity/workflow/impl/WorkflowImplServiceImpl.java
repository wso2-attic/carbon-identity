package org.wso2.carbon.identity.workflow.impl;


import org.wso2.carbon.identity.workflow.impl.bean.BPSProfile;
import org.wso2.carbon.identity.workflow.impl.dao.BPSProfileDAO;

import java.util.List;
import java.util.Map;

public class WorkflowImplServiceImpl implements WorkflowImplService {

    BPSProfileDAO bpsProfileDAO = new BPSProfileDAO();


    @Override
    public void addBPSProfile(BPSProfile bpsProfileDTO, int tenantId)
            throws WorkflowImplException {

        bpsProfileDAO.addProfile(bpsProfileDTO, tenantId);
    }

    @Override
    public List<BPSProfile> listBPSProfiles(int tenantId) throws WorkflowImplException {

        return bpsProfileDAO.listBPSProfiles(tenantId);
    }

    @Override
    public void removeBPSProfile(String profileName) throws WorkflowImplException {

        bpsProfileDAO.removeBPSProfile(profileName);
    }


    /*@Override
    public Map<String, Object> getBPSProfileParams(String profileName) throws WorkflowImplException {

        return bpsProfileDAO.getBPELProfileParams(profileName);
    }*/

    @Override
    public BPSProfile getBPSProfile(String profileName, int tenantId) throws WorkflowImplException {

        return bpsProfileDAO.getBPSProfile(profileName, tenantId, false);
    }

    @Override
    public void updateBPSProfile(BPSProfile bpsProfileDTO, int tenantId) throws WorkflowImplException {
        BPSProfile currentBpsProfile = bpsProfileDAO.getBPSProfile(bpsProfileDTO.getProfileName(), tenantId, true);
        if (bpsProfileDTO.getPassword() == null || bpsProfileDTO.getPassword().isEmpty()) {
            bpsProfileDTO.setPassword(currentBpsProfile.getPassword());
        }
        if (bpsProfileDTO.getCallbackPassword() == null || bpsProfileDTO.getCallbackPassword().isEmpty()) {
            bpsProfileDTO.setCallbackPassword(currentBpsProfile.getCallbackPassword());
        }
        bpsProfileDAO.updateProfile(bpsProfileDTO, tenantId);
    }
}
