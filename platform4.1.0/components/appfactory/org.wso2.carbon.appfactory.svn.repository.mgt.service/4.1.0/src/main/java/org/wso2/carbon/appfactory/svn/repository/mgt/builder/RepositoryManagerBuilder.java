/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.wso2.carbon.appfactory.svn.repository.mgt.builder;

import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.svn.repository.mgt.RepositoryManager;
import org.wso2.carbon.appfactory.svn.repository.mgt.impl.SCMManagerBasedRepositoryManager;

/**
 *
 *
 */
public class RepositoryManagerBuilder {
    private AppFactoryConfiguration configuration;

    public RepositoryManagerBuilder(AppFactoryConfiguration configuration) {
        this.configuration = configuration;
    }
    public RepositoryManager buildRepositoryManager(){
        //TODO:get the implementation class from the appfactory.xml and initialize
        RepositoryManager manager=new SCMManagerBasedRepositoryManager();
        manager.setConfig(configuration);
        return manager;
    }
}