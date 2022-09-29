package org.keycloak.testsuite.webdriver;


import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.safari.SafariDriver;

import java.util.Arrays;

public enum KcSupportedWebDriver {
    CHROME("chrome", ChromeDriver.class),
    EDGE("edge", EdgeDriver.class),
    FIREFOX("firefox", FirefoxDriver.class),
    SAFARI("safari", SafariDriver.class);

    private final String browserName;
    private final Class<? extends WebDriver> webDriver;

    KcSupportedWebDriver(String browserName, Class<? extends WebDriver> webDriver) {
        this.browserName = browserName;
        this.webDriver = webDriver;
    }

    public String getBrowserName() {
        return browserName;
    }

    public Class<? extends WebDriver> getWebDriver() {
        return webDriver;
    }

/*
    public static Class<? extends WebDriver> getCurrentDriverClass() {
        final String browserProperty = System.getProperty("browser", CHROME.getBrowserName());

        return Arrays.stream(KcSupportedWebDriver.values())
                .filter(f -> browserProperty.equals(f.browserName))
                .findFirst()
                .map(KcSupportedWebDriver::getWebDriver)
                ;
    }*/


}
