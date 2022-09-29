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

package org.keycloak.testsuite.arquillian.provider;

import org.apache.http.client.utils.URIBuilder;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.impl.enricher.resource.URLResourceProvider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.jboss.logging.Logger;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.arquillian.TestContext;
import org.keycloak.testsuite.arquillian.annotation.AppServerBrowserContext;
import org.keycloak.testsuite.arquillian.annotation.AppServerContext;
import org.keycloak.testsuite.arquillian.annotation.AuthServerBrowserContext;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContext;
import org.keycloak.testsuite.util.ServerURLs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.keycloak.testsuite.util.ServerURLs.APP_SERVER_HOST;
import static org.keycloak.testsuite.util.ServerURLs.APP_SERVER_PORT;
import static org.keycloak.testsuite.util.ServerURLs.APP_SERVER_SCHEME;

public class URLProvider extends URLResourceProvider {

    protected final Logger log = Logger.getLogger(this.getClass());

    @Inject
    Instance<SuiteContext> suiteContext;
    @Inject
    Instance<TestContext> testContext;

    public void handlePageInitializationBeforeClass(@Observes(precedence = 100) Before event) {
        Consumer<Class<?>> initFields = (clazz) ->
                Arrays.stream(clazz.getDeclaredFields())
                        .filter(Context::containsSupportedAnnotation)
                        .forEach(f -> {
                            f.setAccessible(true);
                            URL url = Context.processUrl(f.getAnnotations(),this);
                            //Object object = Optional.ofNullable(initPageWithContext(f)).orElseGet(() -> initPageWithoutContext(f));
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

    @Override
    public Object doLookup(ArquillianResource resource, Annotation... qualifiers) {
        URL url = (URL) super.doLookup(resource, qualifiers);

        if (url == null) {
            String appServerContextRoot = ServerURLs.getAppServerContextRoot();
            try {
                for (Annotation a : qualifiers) {
                    if (OperateOnDeployment.class.isAssignableFrom(a.annotationType())) {
                        return new URL(appServerContextRoot + "/" + ((OperateOnDeployment) a).value() + "/");
                    }
                }
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }

        //URL myUrl = Context.processUrl()

        // inject context roots if annotation present
        for (Annotation a : qualifiers) {
            if (AuthServerContext.class.isAssignableFrom(a.annotationType())) {
                return suiteContext.get().getAuthServerInfo().getContextRoot();
            }
            if (AppServerContext.class.isAssignableFrom(a.annotationType())) {
                //standalone
                ContainerInfo appServerInfo = testContext.get().getAppServerInfo();
                if (appServerInfo != null) return appServerInfo.getContextRoot();

                //cluster
                List<ContainerInfo> appServerBackendsInfo = testContext.get().getAppServerBackendsInfo();
                if (appServerBackendsInfo.isEmpty()) throw new IllegalStateException("Both testContext's appServerInfo and appServerBackendsInfo not set.");

                return appServerBackendsInfo.get(0).getContextRoot();
            }
            if (AuthServerBrowserContext.class.isAssignableFrom(a.annotationType())) {
                return suiteContext.get().getAuthServerInfo().getBrowserContextRoot();
            }
            if (AppServerBrowserContext.class.isAssignableFrom(a.annotationType())) {
                //standalone
                ContainerInfo appServerInfo = testContext.get().getAppServerInfo();
                if (appServerInfo != null) return appServerInfo.getBrowserContextRoot();

                //cluster
                List<ContainerInfo> appServerBackendsInfo = testContext.get().getAppServerBackendsInfo();
                if (appServerBackendsInfo.isEmpty()) throw new IllegalStateException("Both testContext's appServerInfo and appServerBackendsInfo not set.");

                return appServerBackendsInfo.get(0).getBrowserContextRoot();
            }
        }

        // fix injected URL
        if (url != null) {
            try {
                url = new URIBuilder(url.toURI())
                        .setScheme(APP_SERVER_SCHEME)
                        .setHost(APP_SERVER_HOST)
                        .setPort(Integer.parseInt(APP_SERVER_PORT))
                        .build().toURL();
            } catch (URISyntaxException | MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }

        return url;
    }

    private enum Context {
        AUTH_SERVER(AuthServerContext.class) {
            @Override
            public URL getUrl(Annotation annotation, URLProvider provider) {
                return provider.suiteContext.get().getAuthServerInfo().getContextRoot();
            }
        },
        APP_SERVER(AppServerContext.class) {
            @Override
            public URL getUrl(Annotation annotation, URLProvider provider) {
                ContainerInfo appServerInfo = provider.testContext.get().getAppServerInfo();
                if (appServerInfo != null) return appServerInfo.getContextRoot();

                //cluster
                List<ContainerInfo> appServerBackendsInfo = provider.testContext.get().getAppServerBackendsInfo();
                if (appServerBackendsInfo.isEmpty()) {
                    throw new IllegalStateException("Both testContext's appServerInfo and appServerBackendsInfo not set.");
                }

                return appServerBackendsInfo.get(0).getContextRoot();
            }
        },
        AUTH_SERVER_BROWSER(AuthServerBrowserContext.class) {
            @Override
            public URL getUrl(Annotation annotation, URLProvider provider) {
                return provider.suiteContext.get().getAuthServerInfo().getBrowserContextRoot();
            }
        },
        APP_SERVER_BROWSER(AppServerBrowserContext.class) {
            @Override
            public URL getUrl(Annotation annotation, URLProvider provider) {
                //standalone
                ContainerInfo appServerInfo = provider.testContext.get().getAppServerInfo();
                if (appServerInfo != null) return appServerInfo.getBrowserContextRoot();

                //cluster
                List<ContainerInfo> appServerBackendsInfo = provider.testContext.get().getAppServerBackendsInfo();
                if (appServerBackendsInfo.isEmpty()) {
                    throw new IllegalStateException("Both testContext's appServerInfo and appServerBackendsInfo not set.");
                }

                return appServerBackendsInfo.get(0).getBrowserContextRoot();
            }
        },
        OPERATE_ON_DEPLOYMENT(OperateOnDeployment.class) {
            @Override
            public URL getUrl(Annotation annotation, URLProvider provider) {
                String appServerContextRoot = ServerURLs.getAppServerContextRoot();
                try {
                    return new URL(appServerContextRoot + "/" + ((OperateOnDeployment) annotation).value() + "/");
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        private final Class<? extends Annotation> annotation;

        public abstract URL getUrl(Annotation annotation, URLProvider provider);

        Context(Class<? extends Annotation> annotation) {
            this.annotation = annotation;
        }

        public static Optional<URL> processUrl(Annotation[] annotations, URLProvider urlProvider) {
            return Arrays.stream(Context.values())
                    .filter(f -> f.annotation.isAssignableFrom(annotation.annotationType()))
                    .findFirst()
                    .map(f -> f.getUrl(annotation, urlProvider));
        }




        public static boolean containsSupportedAnnotation(Field field) {
            Annotation[] annotations = field.getAnnotations();
            if (annotations.length == 0) return false;

            final Set<Context> contextValues = Arrays.stream(Context.values()).collect(Collectors.toSet());
            return Arrays.stream(annotations).anyMatch(contextValues::contains);
        }

       /* public static Optional<Context> getSupportedContext(Field field) {
            Set<Class<? extends Annotation>> supportedAnnotations = Arrays.stream(Context.values()).map(f -> f.annotation).collect(Collectors.toSet());
            return Arrays.stream(field.getAnnotations()).anyMatch(supportedAnnotations::contains);


            Collections.disjoint(Arrays.stream(Context.values()).map(f -> f.annotation).collect(Collectors()), field.getAnnotations())
            final Set<Class<? extends Annotation>> annotations = Arrays.stream(Context.values())
                    .map(f -> f.annotation)
                    .collect(Collectors.toSet());
            return Arrays.stream(field.getAnnotations()).filter(annotations::contains).findFirst();
        }*/

    }
}
