package org.keycloak.it.cli;

import org.keycloak.config.Option;
import org.keycloak.config.OptionBuilder;
import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.cli.command.AbstractCommand;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import picocli.CommandLine;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

@CommandLine.Command(name = "custom-command",
        header = "Custom Command",
        description = "Custom testing command")
public final class CustomCommand extends AbstractCommand implements Runnable {
    @Override
    public void run() {

    }

    public PropertyMapper<?>[] getMappers() {
        return new PropertyMapper[]{
                fromOption(SOME_OPTION)
                        .to("kc.spi-connections-infinispan-quarkus-stack")
                        .paramLabel("stack")
                        .build()
        };
    }

    public static final Option<String> SOME_OPTION = new OptionBuilder<>("some-option", String.class)
            .category(OptionCategory.HTTP)
            .hidden()
            .description("Some Options")
            .defaultValue("default")
            .build();
}
