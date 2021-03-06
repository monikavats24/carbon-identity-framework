/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.application.mgt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.SpFileContent;
import org.wso2.carbon.identity.application.common.model.ImportResponse;
import org.wso2.carbon.identity.application.common.model.LocalAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.RequestPathAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.internal.ApplicationManagementServiceComponentHolder;

import java.util.ArrayList;

/**
 * Application management admin service
 */
public class ApplicationManagementAdminService extends AbstractAdmin {

    private static Log log = LogFactory.getLog(ApplicationManagementAdminService.class);
    private ApplicationManagementService applicationMgtService;

    /**
     * Creates a service provider with basic information.First we need to create
     * a role with the
     * application name. Only the users in this role will be able to edit/update
     * the application.The
     * user will assigned to the created role.Internal roles used.
     *
     * @param serviceProvider Service provider
     * @return application id
     * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
     */
    public void createApplication(ServiceProvider serviceProvider) throws IdentityApplicationManagementException {

        try {
            applicationMgtService = ApplicationManagementService.getInstance();
            applicationMgtService.createApplication(serviceProvider, getTenantDomain(), getUsername());
        } catch (IdentityApplicationManagementException idpException) {
            log.error("Error while creating application: " + serviceProvider.getApplicationName() + " for tenant: " +
                    getTenantDomain(), idpException);
            throw idpException;
        }
    }

    /**
     * Get Service provider information for given application name
     *
     * @param applicationName Application name
     * @return service provider
     * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
     */
    public ServiceProvider getApplication(String applicationName) throws IdentityApplicationManagementException {

        try {
            if (!ApplicationConstants.LOCAL_SP.equals(applicationName) &&
                    !ApplicationMgtUtil.isUserAuthorized(applicationName, getUsername())) {
                log.warn("Illegal Access! User " + CarbonContext.getThreadLocalCarbonContext().getUsername() +
                        " does not have access to the application " + applicationName);
                throw new IdentityApplicationManagementException("User not authorized");
            }
            applicationMgtService = ApplicationManagementService.getInstance();
            return applicationMgtService.getApplicationExcludingFileBasedSPs(applicationName, getTenantDomain());
        } catch (IdentityApplicationManagementException idpException) {
            log.error("Error while retrieving application: " + applicationName + " for tenant: " + getTenantDomain(),
                    idpException);
            throw idpException;
        }
    }

    /**
     * Get all basic application information
     *
     * @return Application Basic information array
     * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
     */
    public ApplicationBasicInfo[] getAllApplicationBasicInfo() throws IdentityApplicationManagementException {

        try {
            applicationMgtService = ApplicationManagementService.getInstance();
            ApplicationBasicInfo[] applicationBasicInfos =
                    applicationMgtService.getAllApplicationBasicInfo(getTenantDomain(), getUsername());
            ArrayList<ApplicationBasicInfo> appInfo = new ArrayList<>();
            for (ApplicationBasicInfo applicationBasicInfo : applicationBasicInfos) {
                if (ApplicationMgtUtil.isUserAuthorized(applicationBasicInfo.getApplicationName(), getUsername())) {
                    appInfo.add(applicationBasicInfo);
                    if (log.isDebugEnabled()) {
                        log.debug("Retrieving basic information of application: " +
                                applicationBasicInfo.getApplicationName());
                    }
                }
            }
            return appInfo.toArray(new ApplicationBasicInfo[appInfo.size()]);
        } catch (IdentityApplicationManagementException idpException) {
            log.error("Error while retrieving all application basic info for tenant: " + getTenantDomain(),
                    idpException);
            throw idpException;
        }
    }

    /**
     * Update application
     *
     * @param serviceProvider Service provider
     * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
     */
    public void updateApplication(ServiceProvider serviceProvider) throws IdentityApplicationManagementException {

        // check whether use is authorized to update the application.
        try {
            if (!ApplicationConstants.LOCAL_SP.equals(serviceProvider.getApplicationName()) &&
                    !ApplicationMgtUtil.isUserAuthorized(serviceProvider.getApplicationName(), getUsername(),
                            serviceProvider.getApplicationID())) {
                log.warn("Illegal Access! User " + CarbonContext.getThreadLocalCarbonContext().getUsername() +
                        " does not have access to the application " + serviceProvider.getApplicationName());
                throw new IdentityApplicationManagementException("User not authorized");
            }
            applicationMgtService = ApplicationManagementService.getInstance();
            applicationMgtService.updateApplication(serviceProvider, getTenantDomain(), getUsername());
        } catch (IdentityApplicationManagementException idpException) {
            log.error("Error while updating application: " + serviceProvider.getApplicationName() + " for tenant: " +
                    getTenantDomain(), idpException);
            throw idpException;

        }
    }

    /**
     * Delete Application
     *
     * @param applicationName Application name
     * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
     */
    public void deleteApplication(String applicationName) throws IdentityApplicationManagementException {

        try {
            if (!ApplicationMgtUtil.isUserAuthorized(applicationName, getUsername())) {
                log.warn("Illegal Access! User " + CarbonContext.getThreadLocalCarbonContext().getUsername() +
                        " does not have access to the application " + applicationName);
                throw new IdentityApplicationManagementException("User not authorized");
            }
            applicationMgtService = ApplicationManagementService.getInstance();
            applicationMgtService.deleteApplication(applicationName, getTenantDomain(), getUsername());
        } catch (IdentityApplicationManagementException idpException) {
            log.error("Error while deleting application: " + applicationName + " for tenant: " + getTenantDomain(),
                    idpException);
            throw idpException;

        }
    }

    /**
     * Get identity provider by identity provider name
     *
     * @param federatedIdPName Federated identity provider name
     * @return Identity provider
     * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
     */
    public IdentityProvider getIdentityProvider(String federatedIdPName) throws IdentityApplicationManagementException {

        try {
            applicationMgtService = ApplicationManagementService.getInstance();
            return applicationMgtService.getIdentityProvider(federatedIdPName, getTenantDomain());
        } catch (IdentityApplicationManagementException idpException) {
            log.error("Error while retrieving identity provider: " + federatedIdPName + " for tenant: " +
                            getTenantDomain(), idpException);
            throw idpException;

        }
    }

    /**
     * Get all identity providers
     *
     * @return Identity providers array
     * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
     */
    public IdentityProvider[] getAllIdentityProviders() throws IdentityApplicationManagementException {

        try {
            applicationMgtService = ApplicationManagementService.getInstance();
            return applicationMgtService.getAllIdentityProviders(getTenantDomain());
        } catch (IdentityApplicationManagementException idpException) {
            log.error("Error while retrieving all identity providers for tenant: " + getTenantDomain(), idpException);
            throw idpException;

        }
    }

    /**
     * Get all local authenticators
     *
     * @return local authenticators array
     * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
     */
    public LocalAuthenticatorConfig[] getAllLocalAuthenticators() throws IdentityApplicationManagementException {

        try {
            applicationMgtService = ApplicationManagementService.getInstance();
            return applicationMgtService.getAllLocalAuthenticators(getTenantDomain());
        } catch (IdentityApplicationManagementException idpException) {
            log.error("Error while retrieving all local authenticators for tenant: " + getTenantDomain(),
                    idpException);
            throw idpException;

        }
    }

    /**
     * Get all request path authenticator config
     *
     * @return Request path authenticator config array
     * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
     */
    public RequestPathAuthenticatorConfig[] getAllRequestPathAuthenticators()
            throws IdentityApplicationManagementException {

        try {
            applicationMgtService = ApplicationManagementService.getInstance();
            return applicationMgtService.getAllRequestPathAuthenticators(getTenantDomain());
        } catch (IdentityApplicationManagementException idpException) {
            log.error("Error while retrieving all request path authenticators for tenant: " + getTenantDomain(),
                    idpException);
            throw idpException;

        }
    }

    /**
     * Get all local claim uris
     *
     * @return claim uri array
     * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
     */
    public String[] getAllLocalClaimUris() throws IdentityApplicationManagementException {

        try {
            applicationMgtService = ApplicationManagementService.getInstance();
            return applicationMgtService.getAllLocalClaimUris(getTenantDomain());
        } catch (IdentityApplicationManagementException idpException) {
            log.error("Error while retrieving all local claim URIs for tenant: " + getTenantDomain(), idpException);
            throw idpException;

        }
    }

    /**
     * Retrieve the set of authentication templates configured from file system in JSON format
     * @return Authentication templates.
     */
    public String getAuthenticationTemplatesJSON() {

        return ApplicationManagementServiceComponentHolder.getInstance().getAuthenticationTemplatesJson();
    }

    /**
     * Import application from XML file from UI.
     *
     * @param spFileContent xml string of the SP and file name
     * @return Created application name
     * @throws IdentityApplicationManagementException Identity Application Management Exception
     */
    public ImportResponse importApplication(SpFileContent spFileContent) throws IdentityApplicationManagementException {

        try {
            applicationMgtService = ApplicationManagementService.getInstance();
            return applicationMgtService.importSPApplication(spFileContent, getTenantDomain(), getUsername(), false);
        } catch (IdentityApplicationManagementException idpException) {
            log.error("Error while importing application for tenant: " + getTenantDomain(), idpException);
            throw idpException;
        }
    }

    /**
     * Export service provider as XML.
     *
     * @param applicationName Name of the application to be exported
     * @return XML content of the service provider
     * @throws IdentityApplicationManagementException Identity Application Management Exception
     */
    public String exportApplication(String applicationName, boolean exportSecrets) throws
            IdentityApplicationManagementException {

        try {
            if (ApplicationConstants.LOCAL_SP.equals(applicationName)) {
                log.warn("Illegal access! Local service provider can't be exported");
                throw new IdentityApplicationManagementException("Local service provider can't be exported");
            }
            if (!ApplicationMgtUtil.isUserAuthorized(applicationName, getUsername())) {
                log.warn("Illegal Access! User " + CarbonContext.getThreadLocalCarbonContext().getUsername() +
                        " does not have export the application " + applicationName);
                throw new IdentityApplicationManagementException("User not authorized");
            }
            applicationMgtService = ApplicationManagementService.getInstance();
            return applicationMgtService.exportSPApplication(applicationName, exportSecrets, getTenantDomain());
        } catch (IdentityApplicationManagementException idpException) {
            log.error("Error while exporting application: " + applicationName + " for tenant: " + getTenantDomain(),
                    idpException);
            throw idpException;
        }
    }
}