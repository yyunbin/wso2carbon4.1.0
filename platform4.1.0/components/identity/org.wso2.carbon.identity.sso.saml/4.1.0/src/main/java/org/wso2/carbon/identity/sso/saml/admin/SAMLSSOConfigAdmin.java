/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.sso.saml.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOServiceProviderDTO;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOServiceProviderInfoDTO;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.session.UserRegistry;

/**
 * This class is used for managing SAML SSO providers. Adding, retrieving and removing service
 * providers are supported here.
 * In addition to that logic for generating key pairs for tenants except for tenant 0, is included
 * here.
 */
public class SAMLSSOConfigAdmin {

    private static Log log = LogFactory.getLog(SAMLSSOConfigAdmin.class);
    private UserRegistry registry;

    public SAMLSSOConfigAdmin(Registry userRegistry) {
        registry = (UserRegistry) userRegistry;
    }

    /**
     * Add a new service provider
     *
     * @param serviceProviderDTO    service Provider DTO
     * @return true if successful, false otherwise
     * @throws IdentityException    if fails to load the identity persistence manager    
     */
    public boolean addRelyingPartyServiceProvider(SAMLSSOServiceProviderDTO serviceProviderDTO) throws IdentityException {

        SAMLSSOServiceProviderDO serviceProviderDO = new SAMLSSOServiceProviderDO();
        serviceProviderDO.setIssuer(serviceProviderDTO.getIssuer());
        serviceProviderDO.setAssertionConsumerUrl(serviceProviderDTO.getAssertionConsumerUrl());
        serviceProviderDO.setCertAlias(serviceProviderDTO.getCertAlias());
        serviceProviderDO.setUseFullyQualifiedUsername(serviceProviderDTO.isUseFullyQualifiedUsername());
        serviceProviderDO.setDoSingleLogout(serviceProviderDTO.isDoSingleLogout());
        serviceProviderDO.setLogoutURL(serviceProviderDTO.getLogoutURL());
        serviceProviderDO.setDoSignAssertions(serviceProviderDTO.isDoSignAssertions());
        if(serviceProviderDTO.getRequestedClaims() != null && serviceProviderDTO.getRequestedClaims().length != 0){
        	serviceProviderDO.setAttributeConsumingServiceIndex(Integer.toString(IdentityUtil.getRandomInteger()));
        	serviceProviderDO.setRequestedClaims(serviceProviderDTO.getRequestedClaims());
        }
        IdentityPersistenceManager persistenceManager = IdentityPersistenceManager
                .getPersistanceManager();
        try {
            return persistenceManager.addServiceProvider(registry, serviceProviderDO);
        } catch (IdentityException e) {
            log.error("Error obtaining a registry for adding a new service provider", e);
            throw new IdentityException("Error obtaining a registry for adding a new service provider", e);
        }
    }

    /**
     * Retrieve all the relying party service providers
     * 
     * @return set of RP Service Providers + file path of pub. key of generated key pair
     */
    public SAMLSSOServiceProviderInfoDTO getServiceProviders() throws IdentityException {
        SAMLSSOServiceProviderDTO[] serviceProviders = null;
        try {
            IdentityPersistenceManager persistenceManager = IdentityPersistenceManager
                    .getPersistanceManager();
            SAMLSSOServiceProviderDO[] providersSet = persistenceManager.
                    getServiceProviders(registry);
            serviceProviders = new SAMLSSOServiceProviderDTO[providersSet.length];

            for (int i = 0; i < providersSet.length; i++) {

                SAMLSSOServiceProviderDO providerDO = providersSet[i];

                SAMLSSOServiceProviderDTO providerDTO = new SAMLSSOServiceProviderDTO();

                providerDTO.setIssuer(providerDO.getIssuer());
                providerDTO.setAssertionConsumerUrl(providerDO.getAssertionConsumerUrl());
                providerDTO.setCertAlias(providerDO.getCertAlias());
                providerDTO.setAttributeConsumingServiceIndex(providerDO.getAttributeConsumingServiceIndex());

                providerDTO.setUseFullyQualifiedUsername(providerDO.isUseFullyQualifiedUsername());
                providerDTO.setDoSignAssertions(providerDO.isDoSignAssertions());

                providerDTO.setDoSingleLogout(providerDO.isDoSingleLogout());
                providerDTO.setLogoutURL(providerDO.getLogoutURL());

                providerDTO.setRequestedClaims(providerDO.getRequestedClaims());
                serviceProviders[i] = providerDTO;
            }
        } catch (IdentityException e) {
            log.error("Error obtaining a registry intance for reading service provider list", e);
            throw new IdentityException("Error obtaining a registry intance for reading service provider list", e);
        }

        SAMLSSOServiceProviderInfoDTO serviceProviderInfoDTO = new SAMLSSOServiceProviderInfoDTO();
        serviceProviderInfoDTO.setServiceProviders(serviceProviders);

        //if it is tenant zero
        if(registry.getTenantId() == 0){
            serviceProviderInfoDTO.setTenantZero(true);
        }
        return serviceProviderInfoDTO;
    }

    /**
     * Remove an existing service provider.
     * 
     * @param issuer issuer name
     * @return true is successful
     * @throws IdentityException
     */
    public boolean removeServiceProvider(String issuer) throws IdentityException {
        try {
            IdentityPersistenceManager persistenceManager = IdentityPersistenceManager.getPersistanceManager();
            return persistenceManager.removeServiceProvider(registry, issuer);
        } catch (IdentityException e) {
            log.error("Error removing a Service Provider");
            throw new IdentityException("Error removing a Service Provider", e);
        }
    }

}
