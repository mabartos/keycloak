/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.configuration;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores removed properties after sanitization of config sources
 */
public class SanitizedRemovedConfigContext {
    private static final Map<String, String> REMOVED_SANITIZED_PROPS = new ConcurrentHashMap<>();

    public static void add(String key, String value) {
        REMOVED_SANITIZED_PROPS.put(key, value);
    }

    public static void clear() {
        REMOVED_SANITIZED_PROPS.clear();
    }

    public static Map<String, String> getRemovedSanitizedProperties() {
        return Collections.unmodifiableMap(REMOVED_SANITIZED_PROPS);
    }
}
