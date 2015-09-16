package org.wso2.carbon.identity.workflow.mgt.util;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserCoreConstants;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class WorkflowManagementUtil {
    private static Log log = LogFactory.getLog(WorkflowManagementUtil.class);

    public static void createAppRole(String workflowName) throws WorkflowException {
        String roleName = getWorkflowRoleName(workflowName);
        String qualifiedUsername = CarbonContext.getThreadLocalCarbonContext().getUsername();
        String[] user = {qualifiedUsername};

        try {
            if (log.isDebugEnabled()) {
                log.debug("Creating workflow role : " + roleName + " and assign the user : "
                          + Arrays.toString(user) + " to that role");
            }
            CarbonContext.getThreadLocalCarbonContext().getUserRealm().getUserStoreManager()
                    .addRole(roleName, user, null);
        } catch (UserStoreException e) {
            throw new WorkflowException("Error while creating role", e);
        }

    }

    public static void deleteWorkflowRole(String workflowName) throws WorkflowException {
        String roleName = getWorkflowRoleName(workflowName);

        try {
            if (log.isDebugEnabled()) {
                log.debug("Deleting workflow role : " + roleName);
            }
            CarbonContext.getThreadLocalCarbonContext().getUserRealm().getUserStoreManager()
                    .deleteRole(roleName);
        } catch (UserStoreException e) {
            throw new WorkflowException("Error while creating workflow", e);
        }
    }

    public static String getWorkflowRoleName(String workflowName) {
        return UserCoreConstants.INTERNAL_DOMAIN + UserCoreConstants.DOMAIN_SEPARATOR + workflowName;
        //return WorkFlowConstants.WORKFLOW_DOMAIN + UserCoreConstants.DOMAIN_SEPARATOR + workflowName;
    }


    /**
     * Un-marshall given string to given class type
     *
     * @param xmlString XML String that is validated against its XSD
     * @param classType Root Class Name to convert XML String to Object
     * @param <T> Root Class that should return
     * @return Instance of T
     * @throws JAXBException
     */
    public static <T> T unmarshalXML(String xmlString, Class<T> classType) throws JAXBException {

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(xmlString.getBytes());
        JAXBContext jaxbContext = JAXBContext.newInstance(classType);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        T t = (T) jaxbUnmarshaller.unmarshal(byteArrayInputStream);
        return t ;
    }


    /**
     * Reading File Content from the resource path
     *
     * @param relativeFileName File Name to read the content
     * @return File Content
     * @throws URISyntaxException
     * @throws IOException
     */
    public static String readFileFromResource(String relativeFileName) throws URISyntaxException, IOException {
        URL url = WorkflowManagementUtil.class.getClassLoader().getResource(relativeFileName);
        File file = new File(url.toURI());
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        StringBuilder fileContent = new StringBuilder();
        String line = null ;
        while((line=bufferedReader.readLine())!=null){
            fileContent.append(line);
            line = null ;
        }
        return fileContent.toString();
    }



}
