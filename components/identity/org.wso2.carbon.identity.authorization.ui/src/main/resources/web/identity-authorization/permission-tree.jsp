<!--
 ~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 ~
 ~ WSO2 Inc. licenses this file to you under the Apache License,
 ~ Version 2.0 (the "License"); you may not use this file except
 ~ in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~    http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 -->
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
	prefix="carbon"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="java.util.ResourceBundle" %>
<%@page import="java.lang.Exception"%>
<%@ page import="java.io.IOException" %>
<%@ page import="org.wso2.carbon.identity.authorization.ui.IdentityAuthorizationConstants" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.identity.authorization.ui.IdentityAuthorizationClient" %>
<%@ page import="org.wso2.carbon.identity.authorization.core.dto.xsd.PermissionTreeNodeDTO" %>
<jsp:useBean id="authorizationBean" type="org.wso2.carbon.identity.authorization.ui.ErrorStatusBean"
             class="org.wso2.carbon.identity.authorization.ui.ErrorStatusBean" scope="session"/>
<jsp:setProperty name="authorizationBean" property="*" />
<%!
    public void printChildrenTree(PermissionTreeNodeDTO node, JspWriter out) throws IOException {
        if(node != null){
            PermissionTreeNodeDTO[] children = node.getChildNodes();
            if(children != null  && children.length > 0){
                out.write("<li><a class='plus' onclick='treeColapse(this)'>&nbsp;</a> " +
                          "<a class='treeNode' onclick='selectMe(this)'>" + node.getName() + "</a>");
                out.write("<ul style='display:none'>");
                for(PermissionTreeNodeDTO child : children){
                    printChildrenTree(child, out);
                }
                out.write("</ul>");
            } else {
                out.write("<li><a class='minus' onclick='treeColapse(this)'>&nbsp;</a> " +
                          "<a class='treeNode' onclick='selectMe(this)'>" + node.getName() + "</a>");                
                out.write("</li>");
            }
        }
    }

    public void printChildren(PermissionTreeNodeDTO node, String parentNodeName, JspWriter out) throws IOException {
        if(node != null){
            String nodeName;
            if(parentNodeName != null && parentNodeName.trim().length() > 0){
                nodeName = parentNodeName + "/" + node.getName();
            } else {
               nodeName = node.getName();                
            }

            out.write("<li><a class='treeNode' onclick='selectMe(this)'>" + nodeName + "</a></li>") ;
            PermissionTreeNodeDTO[] children = node.getChildNodes();
            if(children != null  && children.length > 0){
                for(PermissionTreeNodeDTO child : children){
                    printChildren(child, nodeName, out);
                }
            }
        }
    }

%>

<%
    String forwardTo;
    PermissionTreeNodeDTO selectedTree = null;
    String selectedFinderModule = authorizationBean.getPermissionModuleName();
    String selectedRootNode;
    String selectedRootSecondaryNode;
    String[] rootNodes = authorizationBean.getRootNodes();
    String[] secondaryRootNodes = null;
    String[] actions = authorizationBean.getPermissionModuleDTO().getSupportedActions();

    String firstName = null;
    String secondName = null;
    String[] names = authorizationBean.getPermissionModuleDTO().getNameForChildRootNodeSet();

    if(authorizationBean.getPermissionModuleDTO().getSecondaryRootSupported()){
        if(names != null && names.length > 0){
            firstName = names[0];
        }
        if(names != null && names.length > 1){
            secondName = names[1];
        }
    } else {
        if(names != null && names.length > 0){
            firstName = names[0];
        }
    }

    selectedRootNode = (String) request.getParameter("rootNode");
    selectedRootSecondaryNode = (String) request.getParameter("secondaryRootNode");

    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext = (ConfigurationContext) config
            .getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

	String BUNDLE = "org.wso2.carbon.identity.authorization.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());

    try {
        IdentityAuthorizationClient client =
                    new IdentityAuthorizationClient(cookie, serverURL, configContext);

        if(rootNodes == null){
            rootNodes = client.getRootNodeNames(selectedFinderModule, authorizationBean.getFilter());
            if(rootNodes != null){
                authorizationBean.setRootNodes(rootNodes);
            }
        }
        if(selectedRootNode != null && selectedRootNode.trim().length() > 0){
            secondaryRootNodes = client.getSecondaryRootNodeNames(selectedFinderModule, selectedRootNode,
                                                                authorizationBean.getFilter());
            authorizationBean.setSecondaryRootNodes(secondaryRootNodes);
        } else {
            secondaryRootNodes = authorizationBean.getSecondaryRootNodes();
        }

        if(authorizationBean.getPermissionModuleDTO().getSecondaryRootSupported() &&
                selectedRootSecondaryNode != null && selectedRootSecondaryNode.trim().length() > 0){
            selectedTree = client.getPermissionTreeNodes(selectedFinderModule, selectedRootNode,
                    selectedRootSecondaryNode, authorizationBean.getFilter());
        } else if(!authorizationBean.getPermissionModuleDTO().getSecondaryRootSupported() &&
                       selectedRootNode != null && selectedRootNode.trim().length() > 0 ) {
            selectedTree = client.getPermissionTreeNodes(selectedFinderModule, selectedRootNode,
                    selectedRootSecondaryNode, authorizationBean.getFilter());     
        }
    } catch (Exception e) {
    	String message = resourceBundle.getString("error.while.retrieving.attribute.values");
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
        forwardTo = "../admin/error.jsp";
%>
<script type="text/javascript">
    function forward() {
        location.href = "<%=forwardTo%>";
    }
</script>

<script type="text/javascript">
    forward();
</script>
<%
    }
%>
<fmt:bundle basename="org.wso2.carbon.identity.authorization.ui.i18n.Resources">
<carbon:breadcrumb
		label="advance.search"
		resourceBundle="org.wso2.carbon.identity.authorization.ui.i18n.Resources"
		topPage="true"
		request="<%=request%>" />

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="resources/js/main.js"></script>
    <!--Yahoo includes for dom event handling-->
    <script src="../yui/build/yahoo-dom-event/yahoo-dom-event.js" type="text/javascript"></script>
    <script src="../identity-authorization/js/create-basic-policy.js" type="text/javascript"></script>
    <link href="../identity-authorization/css/entitlement.css" rel="stylesheet" type="text/css" media="all"/>

    
     <!--Yahoo includes for dom event handling-->
    <script src="http://yui.yahooapis.com/2.8.1/build/yahoo-dom-event/yahoo-dom-event.js" type="text/javascript"></script>

    <!--Yahoo includes for animations-->
    <script src="http://yui.yahooapis.com/2.8.1/build/animation/animation-min.js" type="text/javascript"></script>

    <!--Local js includes-->
    <script type="text/javascript" src="js/treecontrol.js"></script>
    <script type="text/javascript" src="js/popup.js"></script>

    <link href="css/tree-styles.css" media="all" rel="stylesheet" />
    <link href="css/dsxmleditor.css" media="all" rel="stylesheet" />
<script type="text/javascript">

    function getSecondRoots() {
        var comboBox = document.getElementById("rootNode");
        var rootNode = comboBox[comboBox.selectedIndex].value;
        location.href = 'permission-tree.jsp?rootNode=' + rootNode ;
    }

    function getTree() {

        var comboBox1 = document.getElementById("rootNode");
        var rootNode = comboBox1[comboBox1.selectedIndex].value;

        var comboBox2 = document.getElementById("secondaryRootNode");
        var secondaryRootNode = comboBox2[comboBox2.selectedIndex].value;
        location.href = 'permission-tree.jsp?secondaryRootNode=' + secondaryRootNode + "&rootNode=" + rootNode;
    }


    function createInputs(value){
        var mainTable = document.getElementById('mainTable');
        var newTr = mainTable.insertRow(mainTable.rows.length);
        var cell1 = newTr.insertCell(0);
        cell1.innerHTML = '<input type="hidden" name="resourceName'+ mainTable.rows.length
                +'" id="resourceName'+ mainTable.rows.length +'" value="' + value + '"/>';
    }

    function submitForm(fullPathSupported){
        for(var i in paths){
            if(fullPathSupported){
                createInputs(paths[i].path);
            } else {
                createInputs(paths[i].name);
            }
        }
        document.attributeValueForm.action = "permission-update.jsp";
        document.attributeValueForm.submit();
    }


    function doCancel(){
        document.attributeValueForm.action = "permission-update.jsp"; 
        document.attributeValueForm.submit();
    }


</script>

<div id="middle">
    <h2>Permission Tree</h2>
    <div id="workArea">
        <form id="attributeValueForm" name="attributeValueForm" method="post" action="">


        <table width="60%" id="userAdd" class="styledLeft">

        <tbody>
        <tr>
            <td class="formRaw">
            <table class="normal" cellpadding="0" cellspacing="0" class="treeTable" style="width:100%">
            <tr>
                <td>
                <table>
                <%
                    if (rootNodes != null) {
                %>
                <tr>
                    <td class="leftCel-med">
                        <%
                            if(firstName != null ){
                        %>
                            Select <%=firstName%>
                        <%
                            } else {
                        %>
                            Select Primary Root
                        <%
                            }
                        %>
                    </td>
                    <td>
                        <select onchange="getSecondRoots()" id="rootNode" name="rootNode" class="text-box-big">
                            <option value="<%=IdentityAuthorizationConstants.COMBO_BOX_DEFAULT_VALUE%>" selected="selected">
                                <%=IdentityAuthorizationConstants.COMBO_BOX_DEFAULT_VALUE%></option>
                        <%
                            for (String node : rootNodes) {
                                if(selectedRootNode != null && selectedRootNode.equals(node)){
                        %>
                              <option value="<%=selectedRootNode%>" selected="selected"><%=selectedRootNode%></option>
                        <%
                                } else {
                        %>
                              <option value="<%=node%>"><%=node%></option>
                        <%
                                }
                            }
                        %>
                        </select>
                    </td>
                </tr>
                <%
                    } else {
                %>
                    No permission structure has been not defined for this module and filter
                <%
                    }
                %>
                <%
                    if (secondaryRootNodes != null) {
                %>
                <tr>
                    <td class="leftCel-med">
                        <%
                            if(secondName != null ){
                        %>
                            Select <%=secondName%>
                        <%
                            } else {
                        %>
                            Select Secondary Root
                        <%
                            }
                        %>
                    </td>
                    <td>
                        <select onchange="getTree()" id="secondaryRootNode" name="secondaryRootNode" class="text-box-big">
                            <option value="<%=IdentityAuthorizationConstants.COMBO_BOX_DEFAULT_VALUE%>" selected="selected">
                                <%=IdentityAuthorizationConstants.COMBO_BOX_DEFAULT_VALUE%></option>
                        <%
                            for (String node : secondaryRootNodes) {
                                if(selectedRootSecondaryNode != null && selectedRootSecondaryNode.equals(node)){
                        %>
                              <option value="<%=selectedRootSecondaryNode%>" selected="selected"><%=selectedRootSecondaryNode%></option>
                        <%
                                } else {
                        %>
                              <option value="<%=node%>"><%=node%></option>
                        <%
                                }
                            }
                        %>
                        </select>
                    </td>
                </tr>
                <%
                    }
                %>

                <%
                    if (actions != null) {
                %>
                <tr>
                    <td>Select Action </td>
                    <td>
                        <select id="selectedAction" name="selectedAction">
                        <%
                            for (String action : actions) {
                                if(action.equals(authorizationBean.getSelectedAction())){
                        %>
                            <option value="<%=action%>" selected="selected"><%=authorizationBean.getSelectedAction()%></option>
                        <%
                                } else {
                        %>
                            <option value="<%=action%>"><%=action%></option>
                        <%
                                }
                            }
                        %>
                        </select>
                    </td>
                </tr>
                <%
                    }
                %>


                </table>
                </td>
                </tr>

                <tr>
                    <td>
                        <table id="mainTable" class="styledLeft noBorders" style="display:none">
                        </table>
                    </td>
                </tr>
                <tr>
                <td colspan="2">
                    <table cellpadding="0" cellspacing="0" class="treeTable" style="width:100%">
                    <thead>
                        <tr>
                            <th> Permission Resource values </th>
                            <th  style="background-image:none;border:none"></th>
                            <th>Selected Permission Resource values</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                             <%
                                if(selectedTree != null){
                             %>
                            <td style="width: 500px;border:solid 1px #ccc">
                                <div class="treeControl">
                                <ul>
                            <%
                                if(selectedTree.getHierarchicalTree()){
                                    PermissionTreeNodeDTO[] childNodes = selectedTree.getChildNodes();
                                    if(childNodes != null && childNodes.length > 0){
                                        for(PermissionTreeNodeDTO childNode : childNodes){
                                            printChildrenTree(childNode , out);
                                        }
                                    }
                                } else {
                                    PermissionTreeNodeDTO[] childNodes = selectedTree.getChildNodes();
                                    if(childNodes != null && childNodes.length > 0){
                                        for(PermissionTreeNodeDTO childNode : childNodes){
                                            printChildren(childNode, selectedTree.getName(), out);
                                        }
                                    }
                                }
                            %>
                                </ul>
                                </div>
                            </td>
                            <td style="width:50px;vertical-align: middle;border-bottom:solid 1px #ccc">
                                <input class="button" value=">>" onclick="pickNames(<%=selectedTree.getFullPathSupported()%>)" style="width:30px;margin:10px;" />
                            </td>
                            <td style="border:solid 1px #ccc"><div style="overflow: auto;height:300px" id="listView"></div>
                            </td>
                            <%
                                }
                            %>
                        </tr>
                    </tbody>
                    </table>
                </td>
                </tr>

                <tr>
                    <td class="buttonRow" >
                         <%
                        if(selectedTree != null){
                        %>
                            <input type="button" onclick="submitForm('<%=selectedTree.getFullPathSupported()%>')" value="<fmt:message key="add"/>"  class="button"/>
                        <%
                            }
                        %>
                        <input type="button" onclick="doCancel();" value="Cancel" class="button"/>
                    </td>
                </tr>
                </table>
                </td>                
            </tr>
            </tbody>
        </table>
        </form>
    </div>
</div>
</fmt:bundle>

