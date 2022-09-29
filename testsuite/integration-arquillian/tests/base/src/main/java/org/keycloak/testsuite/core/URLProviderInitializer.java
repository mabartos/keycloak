package org.keycloak.testsuite.core;

import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.keycloak.testsuite.page.Page;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

public class URLProviderInitializer {

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


}
