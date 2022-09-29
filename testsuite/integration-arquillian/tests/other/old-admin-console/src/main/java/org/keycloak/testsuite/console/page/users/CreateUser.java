package org.keycloak.testsuite.console.page.users;

import org.keycloak.testsuite.page.Page;
import org.keycloak.testsuite.console.page.AdminConsoleCreate;

/**
 *
 * @author tkyjovsk
 */
public class CreateUser extends AdminConsoleCreate {

    public CreateUser() {
        setEntity("user");
    }
    
    @Page
    private UserAttributesForm form;
    
    public UserAttributesForm form() {
        return form;
    }

}
