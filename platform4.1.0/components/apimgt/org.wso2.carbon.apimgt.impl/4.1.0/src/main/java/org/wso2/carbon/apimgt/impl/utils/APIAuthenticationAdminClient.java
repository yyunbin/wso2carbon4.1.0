/*
 *  Copyright WSO2 Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.apimgt.impl.utils;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.Stub;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.wso2.carbon.apimgt.handlers.security.stub.APIAuthenticationServiceStub;
import org.wso2.carbon.apimgt.handlers.security.stub.types.APIKeyMapping;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;

/**
 * A service client implementation for the APIAuthenticationService (an admin service offered
 * by the API gateway).
 */
//TODO need to refactor code after design review
public class APIAuthenticationAdminClient { // extends AbstractAPIGatewayAdminClient {

    private APIAuthenticationServiceStub stub;

    public APIAuthenticationAdminClient() throws AxisFault {
        stub = new APIAuthenticationServiceStub(null, getServiceEndpointToClearCache("APIAuthenticationService"));
        setup(stub);
    }

    public void invalidateKeys(List<APIKeyMapping> mappings) throws AxisFault {
        try {
            stub.invalidateKeys(mappings.toArray(new APIKeyMapping[mappings.size()]));
        } catch (Exception e) {
            throw new AxisFault("Error while invalidating API keys", e);
        }
    }
    public void invalidateOAuthKeys(String consumerKey,String authUser) throws AxisFault {
        try {
            stub.invalidateOAuthKeys(consumerKey,authUser);
        } catch (Exception e) {
            throw new AxisFault("Error while invalidating API keys", e);
        }
    }

    /**
     * Log into the API gateway or keyMgt as an admin, and initialize the specified client stub using
     * the established authentication session. This method will also set some timeout
     * values and enable session management on the stub so that it can be successfully used
     * for any subsequent admin service invocations.
     *
     * @param stub A client stub to be setup
     * @throws AxisFault if an error occurs when logging into the API gateway
     */
    protected void setup(Stub stub) throws AxisFault {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        //By default login to keyMgt server
        String cookie = loginKeyMgt();
        String gatewayKeyCacheEnabledString = config.getFirstProperty(APIConstants.API_GATEWAY_KEY_CACHE_ENABLED);
        //If gateway key cache enabled we need to login to gateway
        if (gatewayKeyCacheEnabledString != null) {
            Boolean gatewayKeyCacheEnabled = Boolean.parseBoolean(gatewayKeyCacheEnabledString);
            if (gatewayKeyCacheEnabled) {
                cookie = loginGateway();
            }
        }
        ServiceClient client = stub._getServiceClient();
        Options options = client.getOptions();
        options.setTimeOutInMilliSeconds(15 * 60 * 1000);
        options.setProperty(HTTPConstants.SO_TIMEOUT, 15 * 60 * 1000);
        options.setProperty(HTTPConstants.CONNECTION_TIMEOUT, 15 * 60 * 1000);
        options.setManageSession(true);
        options.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
    }

    /**
     * Login to the API gateway as an admin
     *
     * @return A session cookie string
     * @throws AxisFault if an error occurs while logging in
     */
    private String loginGateway() throws AxisFault {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        String user = config.getFirstProperty(APIConstants.API_GATEWAY_USERNAME);
        String password = config.getFirstProperty(APIConstants.API_GATEWAY_PASSWORD);
        String url = config.getFirstProperty(APIConstants.API_GATEWAY_SERVER_URL);

        if (url == null || user == null || password == null) {
            throw new AxisFault("Required API gateway admin configuration unspecified");
        }

        String host;
        try {
            host = new URL(url).getHost();
        } catch (MalformedURLException e) {
            throw new AxisFault("API gateway URL is malformed", e);
        }

        AuthenticationAdminStub authAdminStub = new AuthenticationAdminStub(null, url + "AuthenticationAdmin");
        ServiceClient client = authAdminStub._getServiceClient();
        Options options = client.getOptions();
        options.setManageSession(true);
        try {
            authAdminStub.login(user, password, host);
            ServiceContext serviceContext = authAdminStub.
                    _getServiceClient().getLastOperationContext().getServiceContext();
            String sessionCookie = (String) serviceContext.getProperty(HTTPConstants.COOKIE_STRING);
            return sessionCookie;
        } catch (RemoteException e) {
            throw new AxisFault("Error while contacting the authentication admin services", e);
        } catch (LoginAuthenticationExceptionException e) {
            throw new AxisFault("Error while authenticating against the API gateway admin", e);
        }
    }

    /**
     * Login to the API keyMgt as an admin
     *
     * @return A session cookie string
     * @throws AxisFault if an error occurs while logging in
     */
    private String loginKeyMgt() throws AxisFault {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        String user = config.getFirstProperty(APIConstants.API_KEY_MANAGER_USERNAME);
        String password = config.getFirstProperty(APIConstants.API_KEY_MANAGER_PASSWORD);
        String url = config.getFirstProperty(APIConstants.API_KEY_MANAGER_URL);

        if (url == null || user == null || password == null) {
            throw new AxisFault("Required API keyMgt admin configuration unspecified");
        }

        String host;
        try {
            host = new URL(url).getHost();
        } catch (MalformedURLException e) {
            throw new AxisFault("API KeyMgt URL is malformed", e);
        }

        AuthenticationAdminStub authAdminStub = new AuthenticationAdminStub(null, url + "AuthenticationAdmin");
        ServiceClient client = authAdminStub._getServiceClient();
        Options options = client.getOptions();
        options.setManageSession(true);
        try {
            authAdminStub.login(user, password, host);
            ServiceContext serviceContext = authAdminStub.
                    _getServiceClient().getLastOperationContext().getServiceContext();
            String sessionCookie = (String) serviceContext.getProperty(HTTPConstants.COOKIE_STRING);
            return sessionCookie;
        } catch (RemoteException e) {
            throw new AxisFault("Error while contacting the authentication admin services", e);
        } catch (LoginAuthenticationExceptionException e) {
            throw new AxisFault("Error while authenticating against the API keyMgt admin", e);
        }
    }

    /**
     * Computes endpoint to clear Key validation information cache
     *
     * @param serviceName Name of the admin service
     * @return A String representation of the service endpoint
     */
    private String getServiceEndpointToClearCache(String serviceName) {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        String gatewayKeyCacheEnabledString = config.getFirstProperty(APIConstants.API_GATEWAY_KEY_CACHE_ENABLED);
        //If gateway key cache enabled we return gateway URL
        if (gatewayKeyCacheEnabledString != null) {
            Boolean gatewayKeyCacheEnabled = Boolean.parseBoolean(gatewayKeyCacheEnabledString);
            if (gatewayKeyCacheEnabled) {
                String url = config.getFirstProperty(APIConstants.API_GATEWAY_SERVER_URL);
                return url + serviceName;
            }
        }
        String keyMgtKeyCacheEnabledString = config.getFirstProperty(APIConstants.API_KEY_MANAGER_ENABLE_VALIDATION_INFO_CACHE);
        //If keyMgt server key cache enabled we return gateway URL
        if (keyMgtKeyCacheEnabledString != null) {
            Boolean keyMgtKeyCacheEnabled = Boolean.parseBoolean(keyMgtKeyCacheEnabledString);
            if (keyMgtKeyCacheEnabled) {
                String url = config.getFirstProperty(APIConstants.API_KEY_MANAGER_URL);
                return url + serviceName;
            }
        }
        //By default caching at keyMgt server
        String url = config.getFirstProperty(APIConstants.API_KEY_MANAGER_URL);
        return url + serviceName;
    }
}
