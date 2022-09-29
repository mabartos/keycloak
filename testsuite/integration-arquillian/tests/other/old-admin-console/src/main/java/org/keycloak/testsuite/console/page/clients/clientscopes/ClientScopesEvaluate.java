/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.console.page.clients.clientscopes;

import org.keycloak.testsuite.page.Page;
import org.keycloak.testsuite.console.page.clients.Client;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientScopesEvaluate extends Client {

    @Page
    private ClientScopesEvaluateForm form;

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/client-scopes/evaluate-scopes";
    }

    public ClientScopesEvaluateForm form() {
        return form;
    }
}
