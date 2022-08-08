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

package org.keycloak.testsuite.model;

import org.keycloak.utils.StringUtil;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public enum MapStoreProvider {
    CHM("chm") {
        @Override
        public Optional<String> getDbUrl() {
            return Optional.of("");
        }

        @Override
        public Optional<String> getDbUsername() {
            return Optional.of("");
        }

        @Override
        public Optional<String> getDbPassword() {
            return Optional.of("");
        }
    },
    JPA("jpa") {
        @Override
        public Optional<String> getDbUrl() {
            return Optional.ofNullable(System.getProperty("keycloak.map.storage.connectionsJpa.url"));
        }

        @Override
        public Optional<String> getDbUsername() {
            return Optional.ofNullable(System.getProperty("keycloak.map.storage.connectionsJpa.user"));
        }

        @Override
        public Optional<String> getDbPassword() {
            return Optional.ofNullable(System.getProperty("keycloak.map.storage.connectionsJpa.password"));
        }
    },
    HOTROD("hotrod") {
        @Override
        public Optional<String> getDbUrl() {
            return Optional.ofNullable(System.getProperty("keycloak.map.storage.connectionsHotRod.url"));
        }

        @Override
        public Optional<String> getDbUsername() {
            return Optional.ofNullable(System.getProperty("keycloak.map.storage.connectionsHotRod.user"));
        }

        @Override
        public Optional<String> getDbPassword() {
            return Optional.ofNullable(System.getProperty("keycloak.map.storage.connectionsHotRod.password"));
        }
    };

    private static final String AUTH_SERVER_QUARKUS_MAP_STORAGE_PROFILE = "auth.server.quarkus.mapStorage.profile.config";

    private final String alias;

    public abstract Optional<String> getDbUrl();

    public abstract Optional<String> getDbUsername();

    public abstract Optional<String> getDbPassword();


    MapStoreProvider(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    public static Optional<MapStoreProvider> getCurrentProvider() {
        return getProviderByAlias(System.getProperty(AUTH_SERVER_QUARKUS_MAP_STORAGE_PROFILE, ""));
    }

    public static Optional<MapStoreProvider> getProviderByAlias(String alias) {
        if (StringUtil.isBlank(alias)) return Optional.empty();

        return Arrays.stream(MapStoreProvider.values())
                .filter(f -> alias.equals(f.getAlias()))
                .findFirst();
    }

    public static boolean isMapStoreDefined() {
        return getCurrentProvider().isPresent();
    }
}
