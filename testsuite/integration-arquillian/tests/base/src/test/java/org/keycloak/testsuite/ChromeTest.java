package org.keycloak.testsuite;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import static org.hamcrest.MatcherAssert.assertThat;

public class ChromeTest {

    WebDriver driver;

    @BeforeClass
    public static void setupClass() {
        //WebDriverManager.getInstance(FirefoxDriver.class).setup();
    }

    @Before
    public void setupTest() {
        driver = WebDriverManager.getInstance(FirefoxDriver.class).create();
    }

    @After
    public void teardown() {
        driver.quit();
    }

    @Test
    public void test() {
        // Exercise
        driver.get("https://bonigarcia.dev/selenium-webdriver-java/");
        String title = driver.getTitle();

        // Verify
        assertThat(title, CoreMatchers.containsString("Selenium WebDriver"));
    }
}

