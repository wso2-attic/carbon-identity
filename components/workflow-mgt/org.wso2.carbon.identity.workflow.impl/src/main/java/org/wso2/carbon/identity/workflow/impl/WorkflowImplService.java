package org.wso2.carbon.identity.workflow.impl;

import org.wso2.carbon.identity.workflow.impl.bean.BPSProfile;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequest;

import java.util.List;
import java.util.Map;

public interface WorkflowImplService {

    void addBPSProfile(BPSProfile bpsProfileDTO, int tenantId)
            throws WorkflowImplException;

    List<BPSProfile> listBPSProfiles(int tenantId) throws WorkflowImplException;

    void removeBPSProfile(String profileName) throws WorkflowImplException;

    BPSProfile getBPSProfile(String profileName, int tenantId) throws WorkflowImplException;

    void updateBPSProfile(BPSProfile bpsProfileDTO, int tenantId) throws WorkflowImplException;

    void deleteHumanTask(WorkflowRequest workflowRequest) throws WorkflowImplException;
}
