/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.adapters;

import org.apache.http.params.CoreConnectionPNames;

/**
 * Defines extended custom parameter names for connections in HttpCore used in Keycloak.
 */
interface KcCoreConnectionPNames extends CoreConnectionPNames {

    /**
     * Defines the connection time-to-live ({@code CONNECTION_TTL}) in milliseconds.
     * <p>
     * This parameter expects a value of type {@link Long}.
     * </p>
     */
    String CONNECTION_TTL = "http.connection.ttl";
}
