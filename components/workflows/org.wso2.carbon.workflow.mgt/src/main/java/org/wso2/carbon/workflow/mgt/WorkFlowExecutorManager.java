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

package org.wso2.carbon.workflow.mgt;

import org.wso2.carbon.workflow.mgt.bean.WorkflowExecutionData;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

public class WorkFlowExecutorManager {

    private static WorkFlowExecutorManager instance = new WorkFlowExecutorManager();

    private WorkFlowExecutorManager(){
        Comparator<WorkFlowExecutor> priorityBasedComparator = new Comparator<WorkFlowExecutor>() {
            @Override
            public int compare(WorkFlowExecutor o1, WorkFlowExecutor o2) {
                return o1.getPriority()-o2.getPriority();
            }
        };
        workFlowExecutors = new TreeSet<WorkFlowExecutor>(priorityBasedComparator);
    }

    public static WorkFlowExecutorManager getInstance() {
        return instance;
    }

    private SortedSet<WorkFlowExecutor> workFlowExecutors;

    public void executeWorkflow(WorkflowExecutionData requestData){
        //executors are sorted by priority by the time they are added.
        for (WorkFlowExecutor workFlowExecutor : workFlowExecutors) {
            if(workFlowExecutor.canHandle(requestData)){
                try {
                    workFlowExecutor.execute(requestData);
                } catch (WorkflowException e) {
                    //todo
                }
                return;
            }
        }

    }

    public void registerExecutor(WorkFlowExecutor workFlowExecutor){
        workFlowExecutors.add(workFlowExecutor);
    }
}
