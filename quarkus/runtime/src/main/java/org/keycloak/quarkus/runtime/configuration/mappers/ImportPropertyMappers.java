/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import org.keycloak.config.ImportOptions;
import org.keycloak.config.Option;
import org.keycloak.config.OptionBuilder;
import org.keycloak.config.OptionCategory;
import org.keycloak.exportimport.Strategy;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.util.Optional;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public final class ImportPropertyMappers {

    private ImportPropertyMappers() {
    }

    public static final String IMPORTER_PROPERTY = "kc.spi-import-importer";

    public static PropertyMapper<?>[] getMappers() {
        return new PropertyMapper[]{
                fromOption(IMPORTER_PLACEHOLDER)
                        .to(IMPORTER_PROPERTY)
                        .transformer(ImportPropertyMappers::transformImporter)
                        .paramLabel("file")
                        .build(),
                fromOption(ImportOptions.FILE)
                        .to("kc.spi-import-single-file-file")
                        .paramLabel("file")
                        .build(),
                fromOption(ImportOptions.DIR)
                        .to("kc.spi-import-dir-dir")
                        .paramLabel("dir")
                        .build(),
                fromOption(ImportOptions.OVERRIDE)
                        .to("kc.spi-import-single-file-strategy")
                        .transformer(ImportPropertyMappers::transformOverride)
                        .isEnabled(ImportPropertyMappers::isSingleFileProvider)
                        .build(),
                fromOption(ImportOptions.OVERRIDE)
                        .to("kc.spi-import-dir-strategy")
                        .transformer(ImportPropertyMappers::transformOverride)
                        .isEnabled(ImportPropertyMappers::isDirProvider)
                        .build(),
        };
    }

    private static final Option<String> IMPORTER_PLACEHOLDER = new OptionBuilder<>("importer", String.class)
            .category(OptionCategory.IMPORT)
            .description("Placeholder for determining import mode")
            .buildTime(false)
            .hidden()
            .build();

    public static boolean isProperImporter() {
        return Configuration.getOptionalValue(IMPORTER_PROPERTY).isPresent();
    }

    private static boolean isSingleFileProvider() {
        return isProvider(SINGLE_FILE);
    }

    private static boolean isDirProvider() {
        return isProvider(DIR);
    }

    private static boolean isProvider(String provider) {
        return Configuration.getOptionalValue(IMPORTER_PROPERTY)
                .filter(provider::equals)
                .isPresent();
    }

    private static final String SINGLE_FILE = "singleFile";
    private static final String DIR = "dir";

    private static Optional<String> transformOverride(Optional<String> option, ConfigSourceInterceptorContext context) {
        if (option.isPresent() && Boolean.parseBoolean(option.get())) {
            return Optional.of(Strategy.OVERWRITE_EXISTING.name());
        } else {
            return Optional.of(Strategy.IGNORE_EXISTING.name());
        }
    }

    private static Optional<String> transformImporter(Optional<String> option, ConfigSourceInterceptorContext context) {
        ConfigValue importer = context.proceed(IMPORTER_PROPERTY);
        if (importer != null) {
            return Optional.of(importer.getValue());
        }

        var file = Configuration.getOptionalValue("kc.spi-import-single-file-file").map(f -> SINGLE_FILE);
        var dir = Configuration.getOptionalValue("kc.spi-import-dir-dir")
                .or(() -> Configuration.getOptionalValue("kc.dir"))
                .map(f -> DIR);

        // Only one option can be specified
        boolean xor = file.isPresent() ^ dir.isPresent();

        return xor ? file.or(() -> dir) : Optional.empty();
    }

}
