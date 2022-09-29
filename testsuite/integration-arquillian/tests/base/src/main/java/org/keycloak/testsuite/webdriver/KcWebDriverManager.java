package org.keycloak.testsuite.webdriver;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.quarkus.arc.DefaultBean;
import org.openqa.selenium.WebDriver;

import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

@Dependent
@Startup
public class KcWebDriverManager {

    @Produces
    @DefaultBean
    public WebDriver webDriver() {
        System.out.println("HERE123");
        return WebDriverManager.chromedriver().create();
    }
}
