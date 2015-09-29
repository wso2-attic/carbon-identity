package org.wso2.carbon.identity.workflow.mgt.ui.util;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.stub.metadata.Template;
import org.wso2.carbon.identity.workflow.mgt.stub.metadata.WorkflowImpl;
import org.wso2.carbon.identity.workflow.mgt.stub.metadata.WorkflowWizard;
import org.wso2.carbon.identity.workflow.mgt.stub.metadata.bean.ParameterMetaData;
import org.wso2.carbon.identity.workflow.mgt.stub.metadata.bean.ParametersMetaData;
import org.wso2.carbon.identity.workflow.mgt.ui.WorkflowAdminServiceClient;
import org.wso2.carbon.identity.workflow.mgt.ui.WorkflowUIConstants;

import javax.servlet.http.HttpServletRequest;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WorkflowUIUtil {
    public static void test(String s,Object client){
        try {
            System.out.println("ffff");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static Map<String,Map<String,String>> test2(Map<String,Parameter> stringParameterMap){
        Map<String,Map<String,String>> stepMap = new HashMap<String,Map<String,String>>();
        if(stringParameterMap !=null ) {
            Set<String> keys = stringParameterMap.keySet();
            for (String key : keys) {
                String[] split = key.split("-");
                Map<String, String> stringStringMap = stepMap.get(split[2]);
                if (stringStringMap == null) {
                    stringStringMap = new HashMap<String, String>();
                    stepMap.put(split[2], stringStringMap);
                }
                stringStringMap
                        .put(split[3], stringParameterMap.get(key).getParamValue());
            }
        }
        return stepMap ;
    }

    public static  void loadTemplateParameters(Map<String, String[]> requestParameterMap, WorkflowWizard workflowWizard){
        Set<String> keys = requestParameterMap.keySet();
        Parameter[] templateParameters = workflowWizard.getTemplateParameters();

        Map<String,Parameter> templateParameterMap = new HashMap<>();
        /*if(templateParameters!=null) {
            for (Parameter param : templateParameters) {
                templateParameterMap.put(param.getQName(), param);
            }
        }*/
        Template template = workflowWizard.getTemplate();
        if(template!=null) {
            ParametersMetaData parametersMetaData = template.getParametersMetaData();
            if(parametersMetaData !=null && parametersMetaData.getParameterMetaData()!=null) {
                ParameterMetaData[] parameterMetaData = parametersMetaData.getParameterMetaData();
                for (ParameterMetaData metaData : parameterMetaData) {

                    if (requestParameterMap.get(metaData.getName()) != null) {

                        //Parameter parameter = templateParameterMap.get(metaData.getName());
                        String value = requestParameterMap.get(metaData.getName())[0];
                        //if (parameter == null) {
                            Parameter parameter = new Parameter();
                            parameter.setParamName(metaData.getName());
                            parameter.setHolder(WorkflowUIConstants.ParameterHolder.TEMPLATE);
                            templateParameterMap.put(parameter.getParamName(), parameter);
                        //}
                        parameter.setParamValue(value);
                        parameter.setQName(metaData.getName());


                    }else{
                        for (String key : keys) {
                            if (key.startsWith(metaData.getName())) {
                                //Parameter parameter = templateParameterMap.get(key);
                                //if (parameter == null) {
                                    Parameter parameter = new Parameter();
                                    parameter.setParamName(metaData.getName());
                                    parameter.setHolder(WorkflowUIConstants.ParameterHolder.TEMPLATE);
                                    parameter.setQName(key);
                                    templateParameterMap.put(key, parameter);
                                //}
                                String[] values = requestParameterMap.get(key);
                                if (values != null && values.length > 0) {
                                    String aValue = values[0];
                                    parameter.setParamValue(aValue);
                                }
                            }

                        }

                    }
                }
            }
        }

        Collection<Parameter> values = templateParameterMap.values();
        Parameter[] parameters = values.toArray(new Parameter[values.size()]);
        workflowWizard.setTemplateParameters(parameters);
    }




    public static  void loadWorkflowImplParameters(Map<String, String[]> requestParameterMap, WorkflowWizard workflowWizard){
        Set<String> keys = requestParameterMap.keySet();
        Parameter[] workflowImplParameters = workflowWizard.getWorkflowImplParameters();

        Map<String,Parameter> workflowImplParameterMap = new HashMap<>();
        if(workflowImplParameters!=null) {
            for (Parameter param : workflowImplParameters) {
                workflowImplParameterMap.put(param.getQName(), param);
            }
        }
        WorkflowImpl workflowImpl = workflowWizard.getWorkflowImpl();
        if(workflowImpl!=null) {
            ParametersMetaData parametersMetaData = workflowImpl.getParametersMetaData();
            if(parametersMetaData !=null && parametersMetaData.getParameterMetaData()!=null) {
                ParameterMetaData[] parameterMetaData = parametersMetaData.getParameterMetaData();
                for (ParameterMetaData metaData : parameterMetaData) {

                    if (requestParameterMap.get(metaData.getName()) != null) {

                        Parameter parameter = workflowImplParameterMap.get(metaData.getName());
                        String value = requestParameterMap.get(metaData.getName())[0];
                        if (parameter == null) {
                            parameter = new Parameter();
                            parameter.setParamName(metaData.getName());
                            parameter.setHolder(WorkflowUIConstants.ParameterHolder.WORKFLOW_IMPL);
                            workflowImplParameterMap.put(parameter.getParamName(), parameter);
                        }
                        parameter.setParamValue(value);
                        parameter.setQName(metaData.getName());


                    }else{
                        for (String key : keys) {
                            if (key.startsWith(metaData.getName())) {
                                Parameter parameter = workflowImplParameterMap.get(key);
                                if (parameter == null) {
                                    parameter = new Parameter();
                                    parameter.setParamName(metaData.getName());
                                    parameter.setHolder(WorkflowUIConstants.ParameterHolder.WORKFLOW_IMPL);
                                    parameter.setQName(key);
                                    workflowImplParameterMap.put(key, parameter);
                                }
                                String[] values = requestParameterMap.get(key);
                                if (values != null && values.length > 0) {
                                    String aValue = values[0];
                                    parameter.setParamValue(aValue);
                                }
                            }

                        }

                    }
                }
            }
        }

        Collection<Parameter> values = workflowImplParameterMap.values();
        Parameter[] parameters = values.toArray(new Parameter[values.size()]);
        workflowWizard.setWorkflowImplParameters(parameters);
    }
}
