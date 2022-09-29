/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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

package org.keycloak.testsuite.adapter.page;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.keycloak.testsuite.page.PageContext;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.net.URL;

/**
 * Created by fkiss.
 */
public class AngularCorsProductTestApp extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "angular-cors-product";
    public static final String CLIENT_ID = "integration-arquillian-test-apps-cors-angular-product";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    public AngularCorsProductTestApp(PageContext pageContext) {
        super(pageContext);
    }

    @Override
    public URL getInjectedUrl() {
        return url;
    }

    @FindBy(xpath = "//button[@data-ng-click='reloadData()']")
    private WebElement reloadDataButton;

    @FindBy(xpath = "//button[@data-ng-click='loadRoles()']")
    private WebElement loadRolesButton;

    @FindBy(xpath = "//button[@data-ng-click='addRole()']")
    private WebElement addRoleButton;

    @FindBy(xpath = "//button[@data-ng-click='deleteRole()']")
    private WebElement deleteRoleButton;

    @FindBy(xpath = "//button[@data-ng-click='loadServerInfo()']")
    private WebElement loadAvailableSocialProvidersButton;

    @FindBy(xpath = "//button[@data-ng-click='loadPublicRealmInfo()']")
    private WebElement loadPublicRealmInfoButton;

    @FindBy(xpath = "//button[@data-ng-click='loadVersion()']")
    private WebElement loadVersionButton;

    @FindBy(id = "output")
    private WebElement outputArea;
    @FindBy(id = "headers")
    private WebElement headers;

    public void reloadData() {
        WaitUtils.waitUntilElement(ExpectedConditions.elementToBeClickable(reloadDataButton));
        reloadDataButton.click();
    }

    public void loadRoles() {
        WaitUtils.waitUntilElement(ExpectedConditions.elementToBeClickable(loadRolesButton));
        loadRolesButton.click();
    }

    public void addRole() {
        WaitUtils.waitUntilElement(ExpectedConditions.elementToBeClickable(addRoleButton));
        addRoleButton.click();
    }

    public void deleteRole() {
        WaitUtils.waitUntilElement(ExpectedConditions.elementToBeClickable(deleteRoleButton));
        deleteRoleButton.click();
    }

    public void loadAvailableSocialProviders() {
        WaitUtils.waitUntilElement(ExpectedConditions.elementToBeClickable(loadAvailableSocialProvidersButton));
        loadAvailableSocialProvidersButton.click();
    }

    public void loadPublicRealmInfo() {
        WaitUtils.waitUntilElement(ExpectedConditions.elementToBeClickable(loadPublicRealmInfoButton));
        loadPublicRealmInfoButton.click();
    }

    public void loadVersion() {
        WaitUtils.waitUntilElement(ExpectedConditions.elementToBeClickable(loadVersionButton));

        loadVersionButton.click();
    }

    public WebElement getOutput() {
        WaitUtils.waitUntilElement(ExpectedConditions.visibilityOf(outputArea));

        return outputArea;
    }

    public WebElement getHeaders() {
        WaitUtils.waitUntilElement(ExpectedConditions.visibilityOf(headers));
        return headers;
    }


}
