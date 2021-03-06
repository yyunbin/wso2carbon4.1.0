/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
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

package org.wso2.carbon.appfactory.jenkins.build.service;

import java.util.List;

import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.jenkins.build.internal.ServiceContainer;
import org.wso2.carbon.core.AbstractAdmin;

public class JenkinsCISystemDriverService extends AbstractAdmin {
     ////////// TODO check whether this class is used anywhere ??????
     public void createJob(String applicationId, String version,
                          String revision) throws AppFactoryException {

        ServiceContainer.getJenkinsCISystemDriver().createJob(applicationId, version, revision);
    }

    public void deleteJob(String jobName) throws AppFactoryException {
        ServiceContainer.getJenkinsCISystemDriver().deleteJob(jobName);
    }

    public List<String> getAllJobNames() throws AppFactoryException {
        return ServiceContainer.getJenkinsCISystemDriver().getAllJobNames();
    }

    public void startBuild(String jobName, boolean doDeploy, String deployStage)
            throws AppFactoryException {
        ///////// TODO get the tagname as parameter, temp passing a random value as tagName
        String tagName = "tag"+(Math.random()*100);
        ServiceContainer.getJenkinsCISystemDriver().startBuild(jobName, doDeploy, deployStage,
                                                               tagName);
    }

    public boolean isJobExists(String jobName) throws AppFactoryException {
        return ServiceContainer.getJenkinsCISystemDriver().isJobExists(jobName);
    }

    public String getJobName(String applicationId, String version, String revision) {
        return ServiceContainer.getJenkinsCISystemDriver().getJobName(applicationId, version,
                                                                      revision);
    }
}
