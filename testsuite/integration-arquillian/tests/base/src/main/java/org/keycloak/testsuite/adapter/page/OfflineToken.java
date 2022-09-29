package org.keycloak.testsuite.adapter.page;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.PageContext;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.net.URL;

import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
public class OfflineToken extends AbstractShowTokensPage {

    public static final String DEPLOYMENT_NAME = "offline-client";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    public OfflineToken(PageContext pageContext) {
        super(pageContext);
    }

    @Override
    public URL getInjectedUrl() {
        return url;
    }

    public void logout() {
        String uri = getUriBuilder().path("/logout").build().toASCIIString();
        log.info("Logging out, navigating to: " + uri);
        driver.navigate().to(uri);
        pause(300); // this is needed for FF for some reason
        waitUntilElement(ExpectedConditions.visibilityOf(driver.findElement(By.tagName("body"))));
    }
}