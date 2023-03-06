package org.keycloak.it.cli;

import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLITest;
import org.keycloak.quarkus.runtime.cli.CommandContext;
import org.keycloak.quarkus.runtime.cli.command.Export;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import java.util.List;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@CLITest
public class CommandContextTest {

    @Test
    public void presentAssociatedMappers() {
        CommandContext ctx = CommandContext.init(List.of("export"));

        assertThat(ctx, notNullValue());
        assertThat(ctx.is(Export.class), is(true));
        assertThat(ctx.getCommand(), notNullValue());
        assertThat(ctx.getCommand().getMappers(), is(new PropertyMappers[]{}));

        CommandContext ctx2 = CommandContext.init(List.of("export2"));

        assertThat(ctx, notNullValue());
    }

}
