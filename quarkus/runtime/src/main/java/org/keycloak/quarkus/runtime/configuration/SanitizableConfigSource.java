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

import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Config source with the ability to sanitize its properties based on environment setup
 */
public interface SanitizableConfigSource {

    /**
     * Execute sanitization of the config source
     */
    void sanitizeConfigSource();

    /**
     * Specifies when the property should be removed
     *
     * @param entry checked entry
     * @return true when the condition is met
     */
    default boolean shouldBeRemoved(Map.Entry<String, String> entry) {
        return PropertyMappers.isDisabledMapper(entry.getKey());
    }

    /**
     * Record removed property after the sanitization
     *
     * @param entry removed entry
     */
    default void recordRemovedProperty(Map.Entry<String, String> entry) {
        SanitizedRemovedConfigContext.add(entry.getKey(), entry.getValue());
    }

    /**
     * Default sanitization approach for properties based on disabled mappers
     * Execute sanitization of the config source
     */
    default Map<String, String> sanitizeProperties(Map<String, String> properties) {
        return properties.entrySet()
                .stream()
                .filter((p) -> {
                    if (shouldBeRemoved(p)) {
                        recordRemovedProperty(p);
                        return false;
                    } else {
                        return true;
                    }
                })
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
