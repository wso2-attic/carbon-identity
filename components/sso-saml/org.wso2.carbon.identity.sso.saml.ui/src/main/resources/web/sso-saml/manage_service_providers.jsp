<!--
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
~ KIND, either express or implied. See the License for the
~ specific language governing permissions and limitations
~ under the License.
-->
<%@ page import="org.apache.axis2.AxisFault" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page
        import="org.wso2.carbon.CarbonError" %>
<%@ page import="org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOServiceProviderDTO" %>
<%@ page import="org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOServiceProviderInfoDTO" %>
<%@ page import="org.wso2.carbon.identity.sso.saml.ui.SAMLSSOUIConstants" %>
<%@ page import="org.wso2.carbon.identity.sso.saml.ui.SAMLSSOUIUtil" %>
<%@ page import="org.wso2.carbon.identity.sso.saml.ui.client.SAMLSSOConfigServiceClient" %>
<%@ page
        import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page
        import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Collections" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
           prefix="carbon" %>
<jsp:useBean id="samlSsoServuceProviderConfigBean"
             type="org.wso2.carbon.identity.sso.saml.ui.SAMLSSOProviderConfigBean"
             class="org.wso2.carbon.identity.sso.saml.ui.SAMLSSOProviderConfigBean"
             scope="session"/>
<jsp:setProperty name="samlSsoServuceProviderConfigBean" property="*"/>
<jsp:include page="../dialog/display_messages.jsp"/>

<fmt:bundle
        basename="org.wso2.carbon.identity.sso.saml.ui.i18n.Resources">
<carbon:breadcrumb label="sso.configuration"
                   resourceBundle="org.wso2.carbon.identity.sso.saml.ui.i18n.Resources"
                   topPage="true" request="<%=request%>"/>

<script type="text/javascript" src="global-params.js"></script>
<script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
<script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
<script type="text/javascript" src="../carbon/admin/js/main.js"></script>

<script type="text/javascript">

    function edit(issuer) {
        location.href = "add_service_provider.jsp?region=region1&item=manage_saml_sso&SPAction=editServiceProvider&issuer=" + issuer;
    }

    function removeItem(issuer) {
        CARBON.showConfirmationDialog(
                "<fmt:message key='remove.message1'/>" + " " + issuer
                        + "<fmt:message key='remove.message2'/>",
                function () {
                    location.href = "remove_service_providers.jsp?issuer="
                            + issuer;
                }, null);
    }


</script>

<%
    String addAction = "add_service_provider.jsp";
    String filter = request.getParameter(SAMLSSOUIConstants.ISSUER_LIST_FILTER);
    String cookie;
    String serverURL;
    ConfigurationContext configContext;
    SAMLSSOConfigServiceClient spConfigClient = null;
    int numberOfPages = 0;
    int itemsPerPageInt = SAMLSSOUIConstants.DEFAULT_ITEMS_PER_PAGE;
    String isPaginatedString = request.getParameter("isPaginated");
    if (isPaginatedString != null && isPaginatedString.equals("true")) {
        spConfigClient = (SAMLSSOConfigServiceClient) session.getAttribute(SAMLSSOUIConstants.CONFIG_CLIENT);
    }
    String paginationValue = "isPaginated=true";



    if (filter == null || filter.trim().length() == 0) {
            filter = (String) session.getAttribute(SAMLSSOUIConstants.ISSUER_LIST_FILTER);
            if (filter == null || filter.trim().length() == 0) {
                filter = "*";
            }
    }
    filter = filter.trim();
    session.setAttribute(SAMLSSOUIConstants.ISSUER_LIST_FILTER, filter);


    String pageNumber = request.getParameter("pageNumber");
    if (pageNumber == null) {
        pageNumber = "0";
    }
    int pageNumberInt = 0;
    try {
        pageNumberInt = Integer.parseInt(pageNumber);
    } catch (NumberFormatException ignored) {
    }


    SAMLSSOServiceProviderInfoDTO serviceProviderInfoDTO = null;
    ArrayList<SAMLSSOServiceProviderDTO> providers =
            new ArrayList<SAMLSSOServiceProviderDTO>();
    String reload = null;

    try {
        reload = request.getParameter("reload");
        serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        configContext =
                (ConfigurationContext) config.getServletContext()
                        .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

        if (spConfigClient == null) {
            spConfigClient =
                    new SAMLSSOConfigServiceClient(cookie, serverURL,
                                                   configContext);
                session.setAttribute(SAMLSSOUIConstants.CONFIG_CLIENT, spConfigClient);
        }

        serviceProviderInfoDTO = spConfigClient.getRegisteredServiceProviders();
        if (serviceProviderInfoDTO.getServiceProviders() != null) {
            SAMLSSOServiceProviderDTO[] filteredProviders = SAMLSSOUIUtil.doFilter(filter, serviceProviderInfoDTO.getServiceProviders()) ;
            numberOfPages = (int) Math.ceil((double) filteredProviders.length / itemsPerPageInt);
            SAMLSSOServiceProviderDTO[] paginatedServiceProviders = SAMLSSOUIUtil.doPaging(pageNumberInt, filteredProviders);
            Collections.addAll(providers, paginatedServiceProviders);
        }

    } catch (AxisFault e) {
        CarbonError error = new CarbonError();
        error.addError(e.getMessage());
        request.getSession().setAttribute(CarbonError.ID, error);
%>
<script type="text/javascript">
    location.href = '../admin/error.jsp';
</script>
<%
    }
%>

<div id="middle">
    <h2>
        <fmt:message key="saml.sso"/>
    </h2>

    <div style="height:30px;">
        <a href="javascript:document.location.href='<%=addAction%>'" class="icon-link"
           style="background-image:url(../admin/images/add.gif);"><fmt:message
                key='saml.sso.register.service.provider'/></a>
    </div>

    <div id="workArea">

        <form name="filterForm" method="post" action="manage_service_providers.jsp">
            <table class="styledLeft noBorders">
                <thead>
                <tr>
                    <th colspan="2"><fmt:message key="issuer.search"/></th>
                </tr>
                </thead>
                <tbody>


                <tr>
                    <td class="leftCol-big" style="padding-right: 0 !important;"><fmt:message
                            key="list.issuer"/></td>
                    <td>
                        <input type="text" name="<%=SAMLSSOUIConstants.ISSUER_LIST_FILTER%>"
                               value="<%=Encode.forHtmlAttribute(filter)%>"/>

                        <input class="button" type="submit"
                               value="<fmt:message key="issuer.search"/>"/>
                    </td>

                </tr>
                </tbody>
            </table>
        </form>
        <p>&nbsp;</p>

        <form>
            <table class="styledLeft" width="100%" id="ServiceProviders">
                <thead>
                <tr style="white-space:nowrap">
                    <th><fmt:message key="sp.issuer"/></th>
                    <th><fmt:message key="sp.assertionConsumerURL"/></th>
                    <th><fmt:message key="sp.certAlias"/></th>
                    <th><fmt:message key="sp.consumerIndex"/></th>
                    <th><fmt:message key="sp.action"/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    if (providers != null && providers.size() > 0) {
                        for (SAMLSSOServiceProviderDTO sp : providers) {
                            if (sp != null) {
                %>
                <tr>
                    <td style="width:20%"><%=Encode.forHtml(sp.getIssuer())%>
                    </td>
                    <td><%=Encode.forHtml(sp.getDefaultAssertionConsumerUrl())%>
                    </td>
                    <td style="width:10%"><%=sp.getCertAlias() == null ? "" : Encode.forHtml(sp.getCertAlias())%>
                    </td>
                    <td style="width:15%"><%=sp.getAttributeConsumingServiceIndex() == null
                                             ? ""
                                             : Encode.forHtml(sp.getAttributeConsumingServiceIndex())%>
                    </td>
                    <td style="width:200px;white-space:nowrap;">
                        <a title="Edit Service Providers"
                           onclick="edit('<%=Encode.forJavaScriptAttribute(Encode.forUriComponent(sp.getIssuer()))%>');return false;"
                           href="#"
                           class="icon-link"
                           style="background-image: url(../admin/images/edit.gif)">Edit</a>

                        <a title="Remove Service Providers"
                           onclick="removeItem('<%=Encode.forJavaScriptAttribute(sp.getIssuer())%>');return false;" href="#"
                           class="icon-link"
                           style="background-image: url(../admin/images/delete.gif)">Delete
                        </a></td>
                </tr>
                <%
                        }
                    }
                } else {
                %>
                <tr>
                    <td colspan="6"><i><fmt:message
                            key="saml.sso.service.providers.not.found"/></i></td>
                </tr>
                <%
                    }
                %>
                </tbody>
            </table>

            <carbon:paginator pageNumber="<%=pageNumberInt%>"
                              numberOfPages="<%=numberOfPages%>"
                              page="manage_service_providers.jsp"
                              pageNumberParameterName="pageNumber"
                              parameters="<%=paginationValue%>"
                              resourceBundle="org.wso2.carbon.identity.sso.saml.ui.i18n.Resources"
                              prevKey="prev" nextKey="next"/>

            <script type="text/javascript">

                alternateTableRows('ServiceProviders', 'tableEvenRow',
                                   'tableOddRow');
            </script>
        </form>

        <br/>

        <br/>
    </div>
</div>
</fmt:bundle>