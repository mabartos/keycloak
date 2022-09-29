package org.keycloak.testsuite.console.page.clients;

import org.keycloak.testsuite.page.Page;
import org.keycloak.testsuite.console.page.AdminConsoleCreate;

/**
 *
 * @author tkyjovsk
 */
public class CreateClient extends AdminConsoleCreate {

    public CreateClient() {
        setEntity("client");
    }
    
    @Page
    private CreateClientForm form;
    
    public CreateClientForm form() {
        return form;
    }
    
}
