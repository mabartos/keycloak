package org.keycloak.testsuite.console.page.fragment;

import org.jboss.arquillian.graphene.fragment.Root;
import org.jboss.logging.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class LocaleDropdown {
    protected Logger log = Logger.getLogger(this.getClass());

    @Root
    private WebElement root;

    @FindBy(tagName = "ul")
    private WebElement dropDownMenu;

    @FindBy(id = "kc-current-locale-link")
    private WebElement currentLocaleLink;

    public String getSelected() {
        return getTextFromElement(currentLocaleLink);
    }

    public void selectByText(String text) {
        // open the menu
        log.info("Opening the localization menu");
        currentLocaleLink.click();

        // click desired locale
        clickLink(dropDownMenu.findElement(By.xpath("./li/a[normalize-space(.)='" + text + "']")));
    }
}
