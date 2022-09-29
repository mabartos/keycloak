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

package org.keycloak.testsuite.pages;

import org.junit.Assert;
import org.keycloak.testsuite.page.PageContext;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Provides some generic utils available on most of login pages (Language combobox, Link "Try another way" etc)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class LanguageComboboxAwarePage extends AbstractPage {

    @FindBy(id = "kc-current-locale-link")
    private WebElement languageText;

    @FindBy(id = "kc-locale-dropdown")
    private WebElement localeDropdown;

    @FindBy(id = "try-another-way")
    private WebElement tryAnotherWayLink;

    @FindBy(id = "kc-attempted-username")
    private WebElement attemptedUsernameLabel;

    @FindBy(id = "reset-login")
    private WebElement resetLoginLink;

    @FindBy(id = "account")
    private WebElement accountLink;

    public LanguageComboboxAwarePage(PageContext pageContext) {
        super(pageContext);
    }

    public String getLanguageDropdownText() {
        return languageText.getText();
    }

    public void openLanguage(String language){
        WebElement langLink = localeDropdown.findElement(By.xpath("//a[text()='" + language + "']"));
        String url = langLink.getAttribute("href");
        DroneUtils.getCurrentDriver().navigate().to(url);
        WaitUtils.waitForPageToLoad();
    }

    // If false, we don't expect form "Try another way" link available on the page. If true, we expect that it is available on the page
    public void assertTryAnotherWayLinkAvailability(boolean expectedAvailability) {
        try {
            driver.findElement(By.id("try-another-way"));
            Assert.assertTrue(expectedAvailability);
        } catch (NoSuchElementException nse) {
            Assert.assertFalse(expectedAvailability);
        }
    }

    public void clickTryAnotherWayLink() {
        tryAnotherWayLink.click();
    }

    public void assertAccountLinkAvailability(boolean expectedAvailability) {
        try {
            driver.findElement(By.id("account"));
            Assert.assertTrue(expectedAvailability);
        } catch (NoSuchElementException nse) {
            Assert.assertFalse(expectedAvailability);
        }
    }

    public void clickAccountLink() {
        accountLink.click();
    }

    // If false, we don't expect "attempted username" link available on the page. If true, we expect that it is available on the page
    public void assertAttemptedUsernameAvailability(boolean expectedAvailability) {
        assertAttemptedUsernameAvailability(driver, expectedAvailability);
    }

    public static void assertAttemptedUsernameAvailability(WebDriver driver, boolean expectedAvailability) {
        try {
            driver.findElement(By.id("kc-attempted-username"));
            Assert.assertTrue(expectedAvailability);
        } catch (NoSuchElementException nse) {
            Assert.assertFalse(expectedAvailability);
        }
    }

    public String getAttemptedUsername() {
        return attemptedUsernameLabel.getText();
    }

    public void clickResetLogin() {
        resetLoginLink.click();
    }
}
