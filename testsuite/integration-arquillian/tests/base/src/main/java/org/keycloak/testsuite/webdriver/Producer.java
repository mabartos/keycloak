package org.keycloak.testsuite.webdriver;

import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
@Startup
public class Producer {

    @Produces
    public String HERE = "Here";
}
