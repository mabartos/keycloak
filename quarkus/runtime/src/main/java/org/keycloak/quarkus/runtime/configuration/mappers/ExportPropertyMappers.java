package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import org.keycloak.config.ExportOptions;

import java.util.Optional;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class ExportPropertyMappers {

    private ExportPropertyMappers() {
    }

    public static PropertyMapper<?>[] getMappers() {
        return new PropertyMapper[] {
                fromOption(ExportOptions.FILE)
                        .to("kc.spi-export-exporter")
                        .transformer(ExportPropertyMappers::transformExporter)
                        .paramLabel("file")
                        .build(),
                fromOption(ExportOptions.FILE)
                        .to("kc.spi-export-single-file-file")
                        .paramLabel("file")
                        .build(),
                fromOption(ExportOptions.DIR)
                        .to("kc.spi-export-dir-dir")
                        .paramLabel("dir")
                        .build(),
                fromOption(ExportOptions.REALM)
                        .to("kc.spi-export-single-file-realm-id")
                        .paramLabel("realm")
                        .build(),
                fromOption(ExportOptions.REALM)
                        .to("kc.spi-export-dir-realm-id")
                        .paramLabel("realm")
                        .build()
        };
    }

    private static Optional<String> transformExporter(Optional<String> option, ConfigSourceInterceptorContext context) {
        if (option.isPresent()) {
            return Optional.of("singleFile");
        }
        ConfigValue dirConfigValue = context.proceed("kc.spi-export-dir-dir");
        if (dirConfigValue != null && dirConfigValue.getValue() != null) {
            return Optional.of("dir");
        }
        return Optional.empty();
    }

}
