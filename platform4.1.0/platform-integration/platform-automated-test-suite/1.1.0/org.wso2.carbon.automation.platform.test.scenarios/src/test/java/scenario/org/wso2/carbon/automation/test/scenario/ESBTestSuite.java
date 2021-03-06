/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package scenario.org.wso2.carbon.automation.test.scenario;

import org.testng.annotations.AfterSuite;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.utils.suiteutills.SuiteVariables;
import org.wso2.carbon.automation.platform.scenarios.MasterTestSuite;

import java.util.ArrayList;
import java.util.List;

public class ESBTestSuite extends MasterTestSuite {

    @AfterSuite
    public void suiteRunner() {
        List<SuiteVariables> suiteVariablesList = new ArrayList<SuiteVariables>();

        suiteVariablesList.add(new SuiteVariables("ProxyTest", AddProxyTestCase.class));
//        setServerList(ProductConstant.ESB_SERVER_NAME + "," + ProductConstant.ESB_SERVER_NAME);
        superSuite("org.wso2.carbon.automation.test.scenario.ESBTestSuite", suiteVariablesList).run();
    }

    public static void main(String[] args) {
        ESBTestSuite esbTestSuite = new ESBTestSuite();
        esbTestSuite.suiteRunner();
    }


}
