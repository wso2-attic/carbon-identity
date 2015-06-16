<%--
  ~ Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
  ~
  ~ WSO2 Inc. licenses this file to you under the Apache License,
  ~ Version 2.0 (the "License"); you may not use this file except
  ~ in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  --%>

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
           prefix="carbon" %>
<%@ page import="org.wso2.carbon.claim.mgt.stub.dto.ClaimDialectDTO" %>
<%@ page import="org.wso2.carbon.user.mgt.stub.types.carbon.UserStoreInfo" %>
<%@ page import="org.wso2.carbon.user.mgt.stub.types.carbon.UserRealmInfo" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@ page import="org.apache.axis2.context.ConfigurationContext"%>
<%@ page import="org.wso2.carbon.CarbonConstants"%>
<%@ page import="org.wso2.carbon.utils.ServerConstants"%>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="org.wso2.carbon.user.mgt.ui.UserAdminUIConstants" %>
<%@page import="org.wso2.carbon.user.mgt.ui.UserAdminClient"%>
<%@page import="org.wso2.carbon.claim.mgt.stub.dto.ClaimMappingDTO"%>
<%@page import="org.wso2.carbon.claim.mgt.ui.client.ClaimAdminClient"%>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<script type="text/javascript" src="../admin/js/main.js"></script>
<jsp:include page="../dialog/display_messages.jsp"/>
<jsp:useBean id="userBean"
             type="org.wso2.carbon.user.mgt.ui.UserBean"
             class="org.wso2.carbon.user.mgt.ui.UserBean" scope="session"/>
<jsp:setProperty name="userBean" property="*"/>
<%
    UserStoreInfo userStoreInfo = null;
    UserRealmInfo userRealmInfo = null;
    UserStoreInfo[] allUserStoreInfo = null;
    StringBuilder domainNamesBuilder = new StringBuilder();
    String hiddenDomainNames=null;
    List<String> domainNames = null;
    String selectedDomain  = null;

    try{
        userRealmInfo = (UserRealmInfo)session.getAttribute(UserAdminUIConstants.USER_STORE_INFO);
        if(userRealmInfo == null){
            String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
            String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
            ConfigurationContext configContext =
                    (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
            UserAdminClient client = new UserAdminClient(cookie, backendServerURL, configContext);
            userRealmInfo = client.getUserRealmInfo();
            session.setAttribute(UserAdminUIConstants.USER_STORE_INFO, userRealmInfo);
        }
        userStoreInfo = userRealmInfo.getPrimaryUserStoreInfo(); // TODO

        // domain name preparations
        String primaryDomainName = userRealmInfo.getPrimaryUserStoreInfo().getDomainName();

        domainNames = new ArrayList<String>();
        allUserStoreInfo = userRealmInfo.getUserStoresInfo();
        if(allUserStoreInfo!=null && allUserStoreInfo.length>0){
            for (int i =0; i<allUserStoreInfo.length;i++) {
                if (allUserStoreInfo[i]!=null){
                    if (allUserStoreInfo[i].getDomainName() != null && !allUserStoreInfo[i].getReadOnly()){
                        domainNames.add(allUserStoreInfo[i].getDomainName());
                        domainNamesBuilder.append(allUserStoreInfo[i].getDomainName());
                        domainNamesBuilder.append(",");
                    }
                }
            }
        }
        if(domainNames.size()>0){
            if(primaryDomainName == null){
                primaryDomainName = UserAdminUIConstants.PRIMARY_DOMAIN_NAME_NOT_DEFINED;
                domainNames.add(primaryDomainName);
                domainNamesBuilder.append(primaryDomainName);
                domainNamesBuilder.append(",");
            }
        }

        selectedDomain = CharacterEncoder.getSafeText(userBean.getDomain());
        if(selectedDomain == null || selectedDomain.trim().length() == 0){
            selectedDomain = primaryDomainName;
        }
        if(domainNamesBuilder != null && !domainNamesBuilder.toString().equals(""))
        {
            hiddenDomainNames= domainNamesBuilder.toString();
        }
    }
    catch (Exception e){

    }

    String dialectUri = request.getParameter("dialect");
    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    ClaimDialectDTO[] claimMappping = null;
    String wso2DialectUri = "http://wso2.org/claims";
    String BUNDLE = "org.wso2.carbon.claim.mgt.ui.i18n.Resources";
    ClaimAdminClient client = new ClaimAdminClient(cookie, serverURL, configContext);
    claimMappping = client.getAllClaimMappings();
    session.setAttribute("claimMappping",claimMappping);
%>
<style>
    .sectionHelp {
        padding-left: 17px;
    }
</style>
<fmt:bundle basename="org.wso2.carbon.claim.mgt.ui.i18n.Resources">
    <carbon:breadcrumb label="claim.add"
                       resourceBundle="org.wso2.carbon.claim.mgt.ui.i18n.Resources"
                       topPage="false" request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>

    <div id="middle">
        <h2><fmt:message key='claim.management'/></h2>

        <div id="workArea">

            <%
                String claimUri = request.getParameter("claimUri");
            %>

            <script type="text/javascript">
                String.prototype.format = function (args) {
                    var str = this;
                    return str.replace(String.prototype.format.regex, function (item) {
                        var intVal = parseInt(item.substring(1, item.length - 1));
                        var replace;
                        if (intVal >= 0) {
                            replace = args[intVal];
                        } else if (intVal === -1) {
                            replace = "{";
                        } else if (intVal === -2) {
                            replace = "}";
                        } else {
                            replace = "";
                        }
                        return replace;
                    });
                };
                String.prototype.format.regex = new RegExp("{-?[0-9]+}", "g");

                function setType(chk, hidden) {
                    var val = document.getElementById(chk).checked;
                    var hiddenElement = document.getElementById(hidden);

                    if (val) {
                        hiddenElement.value = "true";
                    } else {
                        hiddenElement.value = "false";
                    }
                }

                function remove(dialect, claim) {
                    CARBON.showConfirmationDialog('<fmt:message key="remove.message1"/>' + claim + '<fmt:message key="remove.message2"/>',
                            function () {
                                location.href = "remove-claim.jsp?dialect=" + dialect + "&claimUri=" + claim;
                            }, null);
                }

                function validate() {
                    var mappedAttributes='';
                    var mappedAttributeTableRowCount = $('#mappedAttributeTable tr').length;
                    for(var i=0; i<mappedAttributeTableRowCount-1; i++){
                        var mappedAttributeName= $('#mappedAttributeName_' + i).val();
                        var domain=$('#domain_' + i).val();
                        if(domain == "PRIMARY"){
                            mappedAttributes = mappedAttributes + mappedAttributeName + ";" ;
                        }
                        else{
                            mappedAttributes = mappedAttributes + domain + '/' + mappedAttributeName + ";" ;
                        }

                    }
                    $('#localClaim').val(mappedAttributes);$

                    var value = document.getElementsByName("displayName")[0].value;
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="displayname.is.required"/>');
                        return false;
                    } else if (value.length > 30) {
                        CARBON.showWarningDialog('<fmt:message key="displayname.is.too.long"/>');
                        return false;
                    }

                    var value = document.getElementsByName("description")[0].value;
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="description.is.required"/>');
                        return false;
                    } else if (value.length > 150) {
                        CARBON.showWarningDialog('<fmt:message key="description.is.too.long"/>');
                        return false;
                    }

                    var value = document.getElementsByName("claimUri")[0].value;
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="claim.uri.is.required"/>');
                        return false;
                    } else if (value.length > 100) {
                        CARBON.showWarningDialog('<fmt:message key="claim.uri.is.too.long"/>');
                        return false;
                    }

                    var value = document.getElementsByName("attribute")[0].value;
                    if (value == '') {
                        CARBON.showWarningDialog('<fmt:message key="attribute.is.required"/>');
                        return false;
                    } else if (value.length > 300) {
                        CARBON.showWarningDialog('<fmt:message key="attr.id.is.too.long"/>');
                        return false;
                    }

                    $.urlParam = function(name){
                        var results = new RegExp('[\?&]' + name + '=([^&#]*)').exec(window.location.href);
                        if (results==null){
                            return null;
                        }else{
                            return results[1] || 0;
                        }
                    }

                    var value = document.getElementsByName("displayOrder")[0].value;
                    if (value != '') {
                        var IsFound = /^-?\d+$/.test(value);
                        if (!IsFound) {
                            CARBON.showWarningDialog('<fmt:message key="display.order.has.to.be.integer"/>');
                            return false;
                        }
                    }

                    var value = document.getElementsByName("regex")[0].value;
                    if (value != '' && value.length > 100) {
                        CARBON.showWarningDialog('<fmt:message key="regex.is.too.long"/>');
                        return false;
                    }

                    var unsafeCharPattern = /[<>`\"]/;
                    var elements = document.getElementsByTagName("input");
                    for (i = 0; i < elements.length; i++) {
                        if ((elements[i].type === 'text' || elements[i].type === 'password') &&
                                elements[i].value != null && elements[i].value.match(unsafeCharPattern) != null) {
                            CARBON.showWarningDialog("<fmt:message key="unsafe.char.validation.msg"/>");
                            return false;
                        }
                    }

                    //Mapped Attributes Validation
                    var value = document.getElementsByName("attribute")[0].value;
                    var mappedAttributes = value.split(";");
                    var domainSeparator = "/";
                    for (var i = 0; i < mappedAttributes.length; i++) {
                        var index = mappedAttributes[i].indexOf(domainSeparator);
                        if (index >= 0) { //has domain
                            var lastIndex = mappedAttributes[i].lastIndexOf(domainSeparator);
                            if (index == 0) {
                                //domain separator cannot be the first letter of the mapped attribute
                                var message = '<fmt:message key="attribute.domain.required"/>';
                                message = message.format([mappedAttributes[i]]);
                                CARBON.showWarningDialog(message);
                                return false;
                            }
                            else if (index != lastIndex) {
                                //mapped attribute cannot have duplicated domainSeparator
                                var message = '<fmt:message key="attribute.domain.separator.duplicate"/>';
                                message = message.format([mappedAttributes[i]]);
                                CARBON.showWarningDialog(message);
                                return false;
                            } else if (index == (mappedAttributes[i].length - 1)) {
                                //domain separator cannot be the last character of the mapped attribute
                                var message = '<fmt:message key="attribute.domain.mapped.attribute.required"/>';
                                message = message.format([mappedAttributes[i]]);
                                CARBON.showWarningDialog(message);
                                return false;
                            }
                        }
                    }

                    document.addclaim.submit();
                }
                jQuery(document).ready(function() {
                    var mappedAttributeRowCount = 0;
                    jQuery('#mappedAttributeAddLink').click(function () {
                        mappedAttributeRowCount ++;
                        var array = $('#hiddenDomainNames').val().split(",").slice(0,-1);
                        var tr = jQuery('<tr></tr>');
                        var tdDomain = jQuery('<td></td>');
                        var tdMappedAttributeName = jQuery('<td><input id="mappedAttributeName_'+mappedAttributeRowCount+'" name="mappedAttributeName_'+mappedAttributeRowCount+'" type="text" value=""/></td>');
                        var tdAction = jQuery('<td><a onclick="deleteClaimRow(this);return false;" href="#" class="icon-link" style="background-image: url(images/delete.gif)"> Delete</a></td>')
                        tdDomain.append(getDropDownList(mappedAttributeRowCount,mappedAttributeRowCount,array));
                        tr.append(tdDomain).append(tdMappedAttributeName).append(tdAction);
                        jQuery('#mappedAttributeTable').append(tr);

                    });
                });

                function getDropDownList(name, id, optionList) {
                    var combo = $("<select></select>").attr("id", "domain_" + id).attr("name", "domain_" + name);

                    $.each(optionList, function (i, el) {
                        combo.append("<option>" + el + "</option>");
                    });

                    return combo;
                }
                function deleteClaimRow(obj) {
                    $(obj).closest('tr').remove()
                }

            </script>

            <form name="addclaim" action="add-claim-submit.jsp?dialect=<%=dialectUri%>" method="post">
                <input type="hidden" id="hiddenDomainNames" value="<%=hiddenDomainNames%>">
                <table style="width: 100%" class="styledLeft">
                    <thead>
                    <tr>
                        <th colspan="2"><fmt:message key='new.claim.details'/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td class="formRow">
                            <table class="normal" cellspacing="0">
                                <tr>
                                    <td class="leftCol-small"><fmt:message key='display.name'/><font
                                            color="red">*</font></td>
                                    <td class="leftCol-big"><input type="text" name="displayName" id="displayName"
                                                                   class="text-box-big"/></td>
                                </tr>

                                <tr>
                                    <td class="leftCol-small"><fmt:message key='description'/><font color="red">*</font>
                                    </td>
                                    <td class="leftCol-big"><input type="text" name="description" id="description"
                                                                   class="text-box-big"/></td>
                                </tr>

                                <tr>
                                    <td class="leftCol-small"><fmt:message key='claim.uri'/><font color="red">*</font>
                                    </td>
                                    <%
                                        if (claimUri != null && claimUri.trim().length() > 0) {
                                    %>
                                    <td class="leftCol-big"><input type="text" name="claimUri" id="claimUri"
                                                                   class="text-box-big" value="<%=claimUri%>"/></td>
                                    <%
                                    } else {
                                    %>
                                    <td class="leftCol-big"><input type="text" name="claimUri" id="claimUri"
                                                                   class="text-box-big"/></td>
                                    <%
                                        }
                                    %>
                                </tr>

                                <tr>
                                    <td class="leftCol-small"><fmt:message key='mapped.attribute'/><font
                                            color="red">*</font></td>
                                    <td>
                                        <%if(dialectUri.equals(wso2DialectUri)){%>

                                        <input type="hidden" name="localClaim" id="localClaim"/>
                                        <a id="mappedAttributeAddLink" class="icon-link" style="background-image: url(images/add.gif); margin-top: 0px !important; margin-bottom: 5px !important; margin-left: 5px;"><fmt:message key='button.add.claim.mapped.attribute' /></a>
                                        <table class="styledLeft" id="mappedAttributeTable" style="">
                                            <thead>
                                            <tr>
                                                <th>
                                                    User Store
                                                </th>
                                                <th>
                                                    Attribute Name
                                                </th>
                                                <th>
                                                    Action
                                                </th>

                                            </tr>
                                            <tr>
                                                <td><select id="domain_0" name="domain_0">
                                                    <%
                                                        for(String domainName : domainNames) {
                                                            if(selectedDomain.equals(domainName)) {
                                                    %>
                                                    <option selected="selected" value="<%=domainName%>"><%=domainName%></option>
                                                    <%
                                                    } else {
                                                    %>
                                                    <option value="<%=domainName%>"><%=domainName%></option>
                                                    <%
                                                            }
                                                        }
                                                    %>
                                                </select>
                                                </td>
                                                <td><input id="mappedAttributeName_0" name="mappedAttributeName_0" type="text" value=""/></td>
                                                <td>
                                                    <a onclick="deleteClaimRow(this);return false;"
                                                       href="#"
                                                       class="icon-link"
                                                       style="background-image: url(images/delete.gif)">Delete
                                                    </a>
                                                </td>
                                            </tr>
                                            </thead>
                                        </table>
                                        <%}else{
                                        %>
                                        <select name="localClaim" id="localClaim">
                                            <%for (int i=0; i<claimMappping.length;i++ ){
                                                if (claimMappping[i].getDialectURI().equals(wso2DialectUri)) {
                                                    ClaimMappingDTO[] claims =  claimMappping[i].getClaimMappings();
                                                    for (int j=0; j<claims.length;j++ ) {%>
                                            <option value="<%=claims[j].getClaim().getClaimUri()%>"> <%=claims[j].getClaim().getClaimUri()%></option>
                                            <% }
                                            } }%>
                                        </select>
                                        <%
                                            }%>
                                    </td>
                                </tr>

                                <tr>
                                    <td class="leftCol-small"><fmt:message key='regular.expression'/></td>
                                    <td class="leftCol-big"><input type="text" name="regex" id="regex"
                                                                   class="text-box-big"/></td>
                                </tr>
                                <tr>
                                    <td class="leftCol-small"><fmt:message key='display.order'/></td>
                                    <td class="leftCol-big"><input type="text" name="displayOrder" id="displayOrder"
                                                                   class="text-box-big"/></td>
                                </tr>
                                <tr>
                                    <td class="leftCol-small"><fmt:message key='supported.by.default'/></td>
                                    <td>
                                        <input type='checkbox' name='supported' id='supported'
                                               onclick="setType('supported','supportedhidden')"/>
                                        <input type='hidden' name='supportedhidden' id='supportedhidden'/>
                                    </td>
                                </tr>

                                <tr>
                                    <td class="leftCol-small"><fmt:message key='required'/></td>
                                    <td>
                                        <input type='checkbox' name='required' id='required'
                                               onclick="setType('required','requiredhidden')"/>
                                        <input type='hidden' name='requiredhidden' id='requiredhidden'/>
                                    </td>
                                </tr>

                                <tr>
                                    <td class="leftCol-small"><fmt:message key='readonly'/></td>
                                    <td>
                                        <input type='checkbox' name='readonly' id='readonly'
                                               onclick="setType('readonly','readonlyhidden')"/>
                                        <input type='hidden' name='readonlyhidden' id='readonlyhidden'/>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2" class="buttonRow">
                            <input type="button" value="<fmt:message key='add'/>" class="button" onclick="validate();"/>
                            <input class="button" type="button" value="<fmt:message key='cancel'/>"
                                   onclick="javascript:document.location.href='claim-view.jsp?dialect=<%=dialectUri%>&ordinal=1'"/
                                                                                                                                 >
                        </td>
                    </tr>

                    </tbody>
                </table>
            </form>
        </div>
    </div>
</fmt:bundle>
