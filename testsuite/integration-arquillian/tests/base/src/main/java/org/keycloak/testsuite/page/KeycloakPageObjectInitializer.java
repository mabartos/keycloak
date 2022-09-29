/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.page;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.logging.Logger;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.arquillian.TestContext;
import org.keycloak.testsuite.util.OAuthClient;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

public class KeycloakPageObjectInitializer {

    @Inject
    private Instance<TestContext> testContextInstance;
    @Inject
    private Instance<SuiteContext> suiteContextInstance;
    @Inject
    private Instance<OAuthClient> oauthClient;
    private WebDriver webDriver;


    private static final Logger log = Logger.getLogger(KeycloakPageObjectInitializer.class);

    public void setUpDriver(@Observes BeforeClass event) {
        this.webDriver = WebDriverManager.chromedriver().create();
    }

    public void handlePageInitializationBeforeClass(@Observes(precedence = 100) Before event) {
        Consumer<Class<?>> initFields = (clazz) ->
                Arrays.stream(clazz.getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(Page.class))
                        .forEach(f -> {
                            f.setAccessible(true);
                            Object object = Optional.ofNullable(initPageWithContext(f)).orElseGet(() -> initPageWithoutContext(f));
                            try {
                                f.set(event.getTestInstance(), object);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        });

        final Class<?> currentClass = event.getTestClass().getJavaClass();
        initFields.accept(currentClass);

        Class<?> clazz = currentClass.getSuperclass();
        while (clazz != null) {
            initFields.accept(clazz);
            clazz = clazz.getSuperclass();
        }
    }

    private Object initPageWithContext(Field field) {
        final Constructor<?> constructor;
        try {
            constructor = field.getType().getConstructor(PageContext.class);
        } catch (NoSuchMethodException e) {
            log.debug("Page does not contain constructor with parameter of type 'PageContext'");
            return null;
        }

        final PageContext pageContext = new PageContext(oauthClient.get(), suiteContextInstance.get(), testContextInstance.get(), webDriver);
        try {
            return constructor.newInstance(pageContext);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.error("Cannot initialize page with the pageContext");
            log.error(e);
            return null;
        }
    }

    private Object initPageWithoutContext(Field field) {
        final Constructor<?> constructor;
        try {
            constructor = field.getType().getConstructor();
        } catch (NoSuchMethodException e) {
            log.debug("Page does not contain constructor without parameters");
            return null;
        }

        try {
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.error("Cannot initialize page");
            log.error(e);
            return null;
        }
    }
}
