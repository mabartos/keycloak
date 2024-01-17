package org.keycloak.quarkus.runtime.cli;

import org.keycloak.config.Option;
import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.command.AbstractCommand;
import org.keycloak.quarkus.runtime.cli.command.Build;
import org.keycloak.quarkus.runtime.cli.command.Main;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.keycloak.quarkus.runtime.Environment.isRebuildCheck;
import static org.keycloak.quarkus.runtime.cli.Picocli.getIncludeOptions;
import static org.keycloak.quarkus.runtime.configuration.Configuration.OPTION_PART_SEPARATOR;
import static picocli.CommandLine.Model.UsageMessageSpec.SECTION_KEY_COMMAND_LIST;

public class CommandLineFactory {
    private List<String> cliArgs;
    private CommandLine.Model.CommandSpec spec;
    private boolean created = false;

    public CommandLine createCommandLine(List<String> cliArgs) {
        this.cliArgs = cliArgs;
        this.spec = CommandLine.Model.CommandSpec.forAnnotatedObject(new Main(), new DefaultFactory()).name(Environment.getCommand());
        this.created = true;

        for (CommandLine subCommand : spec.subcommands().values()) {
            CommandLine.Model.CommandSpec subCommandSpec = subCommand.getCommandSpec();

            // help option added to any subcommand
            subCommandSpec.addOption(CommandLine.Model.OptionSpec.builder(Help.OPTION_NAMES)
                    .usageHelp(true)
                    .description("This help message.")
                    .build());
        }

        return reuseCommandLine();
    }

    public CommandLine reuseCommandLine() {
        if (!created) throw new IllegalStateException("You have to call `createCommandLine(...)` method first");

        addCommandOptions(cliArgs, getCurrentCommandSpec(cliArgs, spec));

        if (isRebuildCheck()) {
            // build command should be available when running re-aug
            addCommandOptions(cliArgs, spec.subcommands().get(Build.NAME));
        }

        CommandLine cmd = new CommandLine(spec);

        cmd.setExecutionExceptionHandler(new ExecutionExceptionHandler());
        cmd.setParameterExceptionHandler(new ShortErrorMessageHandler());
        cmd.setHelpFactory(new HelpFactory());
        cmd.getHelpSectionMap().put(SECTION_KEY_COMMAND_LIST, new SubCommandListRenderer());
        cmd.setErr(new PrintWriter(System.err, true));

        return cmd;
    }

    private static void addCommandOptions(List<String> cliArgs, CommandLine command) {
        if (command != null && command.getCommand() instanceof AbstractCommand) {
            Picocli.IncludeOptions options = getIncludeOptions(cliArgs, command.getCommand(), command.getCommandName());

            if (!options.includeBuildTime && !options.includeRuntime) {
                return;
            }

            addOptionsToCli(command, options);
        }
    }

    static CommandLine getCurrentCommandSpec(List<String> cliArgs, CommandLine.Model.CommandSpec spec) {
        for (String arg : cliArgs) {
            CommandLine command = spec.subcommands().get(arg);

            if (command != null) {
                return command;
            }
        }

        return null;
    }

    private static void addOptionsToCli(CommandLine commandLine, Picocli.IncludeOptions includeOptions) {
        final Map<OptionCategory, List<PropertyMapper<?>>> mappers = new EnumMap<>(OptionCategory.class);

        if (includeOptions.includeRuntime) {
            mappers.putAll(PropertyMappers.getRuntimeMappers());

            if (includeOptions.includeDisabled) {
                combinePropertyMappers(mappers, PropertyMappers.getDisabledRuntimeMappers());
            }
        }

        if (includeOptions.includeBuildTime) {
            combinePropertyMappers(mappers, PropertyMappers.getBuildTimeMappers());

            if (includeOptions.includeDisabled) {
                combinePropertyMappers(mappers, PropertyMappers.getDisabledBuildTimeMappers());
            }
        }

        addMappedOptionsToArgGroups(commandLine, mappers);
    }

    private static <T extends Map<OptionCategory, List<PropertyMapper<?>>>> void combinePropertyMappers(T origMappers, T additionalMappers) {
        for (Map.Entry<OptionCategory, List<PropertyMapper<?>>> entry : additionalMappers.entrySet()) {
            final List<PropertyMapper<?>> result = new ArrayList<>(origMappers.getOrDefault(entry.getKey(), Collections.emptyList()));
            result.addAll(entry.getValue());
            origMappers.put(entry.getKey(), result);
        }
    }

    private static void addMappedOptionsToArgGroups(CommandLine commandLine, Map<OptionCategory, List<PropertyMapper<?>>> propertyMappers) {
        CommandLine.Model.CommandSpec cSpec = commandLine.getCommandSpec();
        for (OptionCategory category : ((AbstractCommand) commandLine.getCommand()).getOptionCategories()) {
            List<PropertyMapper<?>> mappersInCategory = propertyMappers.get(category);

            if (mappersInCategory == null) {
                //picocli raises an exception when an ArgGroup is empty, so ignore it when no mappings found for a category.
                continue;
            }

            CommandLine.Model.ArgGroupSpec.Builder argGroupBuilder = CommandLine.Model.ArgGroupSpec.builder()
                    .heading(category.getHeading() + ":")
                    .order(category.getOrder())
                    .validate(false);

            for (PropertyMapper<?> mapper : mappersInCategory) {
                String name = mapper.getCliFormat();
                String description = mapper.getDescription();

                if (description == null || cSpec.optionsMap().containsKey(name) || name.endsWith(OPTION_PART_SEPARATOR)) {
                    //when key is already added or has no description, don't add.
                    continue;
                }

                CommandLine.Model.OptionSpec.Builder optBuilder = CommandLine.Model.OptionSpec.builder(name)
                        .description(getDecoratedOptionDescription(mapper))
                        .paramLabel(mapper.getParamLabel())
                        .completionCandidates(new Iterable<String>() {
                            @Override
                            public Iterator<String> iterator() {
                                return mapper.getExpectedValues().iterator();
                            }
                        })
                        .parameterConsumer(PropertyMapperParameterConsumer.INSTANCE)
                        .hidden(mapper.isHidden());

                if (mapper.getDefaultValue().isPresent()) {
                    optBuilder.defaultValue(Option.getDefaultValueString(mapper.getDefaultValue().get()));
                }

                if (mapper.getType() != null) {
                    optBuilder.type(mapper.getType());
                } else {
                    optBuilder.type(String.class);
                }

                argGroupBuilder.addArg(optBuilder.build());
            }

            if (argGroupBuilder.args().isEmpty()) {
                continue;
            }

            cSpec.addArgGroup(argGroupBuilder.build());
        }
    }

    private static String getDecoratedOptionDescription(PropertyMapper<?> mapper) {
        StringBuilder transformedDesc = new StringBuilder(mapper.getDescription());

        if (mapper.getType() != Boolean.class && !mapper.getExpectedValues().isEmpty()) {
            transformedDesc.append(" Possible values are: " + String.join(", ", mapper.getExpectedValues()) + ".");
        }

        mapper.getDefaultValue()
                .map(d -> Option.getDefaultValueString(d).replaceAll("%", "%%")) // escape formats
                .map(d -> " Default: " + d + ".")
                .ifPresent(transformedDesc::append);

        mapper.getEnabledWhen().map(e -> format(" %s.", e)).ifPresent(transformedDesc::append);

        mapper.getDeprecatedMetadata().ifPresent(deprecatedMetadata -> {
            List<String> deprecatedDetails = new ArrayList<>();
            String note = deprecatedMetadata.getNote();
            if (note != null) {
                if (!note.endsWith(".")) {
                    note += ".";
                }
                deprecatedDetails.add(note);
            }
            if (!deprecatedMetadata.getNewOptionsKeys().isEmpty()) {
                String s = deprecatedMetadata.getNewOptionsKeys().size() > 1 ? "s" : "";
                deprecatedDetails.add("Use the following option" + s + " instead: " + String.join(", ", deprecatedMetadata.getNewOptionsKeys()) + ".");
            }

            transformedDesc.insert(0, "@|bold DEPRECATED.|@ ");
            if (!deprecatedDetails.isEmpty()) {
                transformedDesc
                        .append(" @|bold ")
                        .append(String.join(" ", deprecatedDetails))
                        .append("|@");
            }
        });

        return transformedDesc.toString();
    }

}
