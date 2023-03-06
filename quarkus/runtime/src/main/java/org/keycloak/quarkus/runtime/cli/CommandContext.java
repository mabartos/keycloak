/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.quarkus.runtime.cli;

import org.keycloak.common.util.CollectionUtil;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.command.AbstractCommand;
import org.keycloak.quarkus.runtime.cli.command.Main;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

import static org.keycloak.quarkus.runtime.Environment.isRebuildCheck;
import static picocli.CommandLine.Model.UsageMessageSpec.SECTION_KEY_COMMAND_LIST;

public class CommandContext {
    private final CommandLine commandLine;
    private final CommandLine.Model.CommandSpec spec;
    private final AbstractCommand command;
    private final List<String> args;

    private CommandContext(List<String> args) {
        this.args = args;
        this.spec = initCommandSpec(args, CommandLine.Model.CommandSpec.forAnnotatedObject(new Main(), new DefaultFactory()).name(Environment.getCommand()));
        this.commandLine = initCommandLine(spec);
        this.command = initAbstractCommand(commandLine);
    }

    public static CommandContext init(List<String> args) {
        CommandContext commandContext = new CommandContext(args);
        final AbstractCommand command = commandContext.getCommand();

        if (command == null) {
            final ExecutionExceptionHandler errorHandler = new ExecutionExceptionHandler();
            final PrintWriter errStream = new PrintWriter(System.err, true);
            errorHandler.error(errStream, "Provided command does NOT exist or does NOT implement 'AbstractCommand' class", null);
            exitOnFailure(-1, commandContext.getCommandLine());
        }

        return commandContext;
    }

    public CommandLine getCommandLine() {
        return commandLine;
    }

    public CommandLine.Model.CommandSpec getSpec() {
        return spec;
    }

    public AbstractCommand getCommand() {
        return command;
    }

    public List<String> getArgs() {
        return CollectionUtil.isNotEmpty(args) ? args : Collections.EMPTY_LIST;
    }

    public boolean is(Class<? extends AbstractCommand> commandType) {
        return commandType != null && commandType.isAssignableFrom(command.getClass());
    }

    public void exitOnFailure(int exitCode) {
        exitOnFailure(exitCode, commandLine);
    }

    public void println(String message) {
        println(commandLine, message);
    }

    public static void println(CommandLine cmd, String message) {
        cmd.getOut().println(message);
    }

    private static void exitOnFailure(int exitCode, CommandLine cmd) {
        if (exitCode != cmd.getCommandSpec().exitCodeOnSuccess() && !Environment.isTestLaunchMode() || isRebuildCheck()) {
            // hard exit wanted, as build failed and no subsequent command should be executed. no quarkus involved.
            System.exit(exitCode);
        }
    }

    private static CommandLine initCommandLine(CommandLine.Model.CommandSpec spec) {
        CommandLine cmd = new CommandLine(spec);
        cmd.setExecutionExceptionHandler(new ExecutionExceptionHandler());
        cmd.setParameterExceptionHandler(new ShortErrorMessageHandler());
        cmd.setHelpFactory(new HelpFactory());
        cmd.getHelpSectionMap().put(SECTION_KEY_COMMAND_LIST, new SubCommandListRenderer());
        cmd.setErr(new PrintWriter(System.err, true));
        return cmd;
    }

    private static AbstractCommand initAbstractCommand(CommandLine cmdLine) {
        if (cmdLine != null && cmdLine.getCommand() instanceof AbstractCommand) {
            return cmdLine.getCommand();
        } else {
            return null;
        }
    }

    private static CommandLine.Model.CommandSpec initCommandSpec(List<String> args, CommandLine.Model.CommandSpec parentSpec) {
        for (String arg : args) {
            CommandLine command = parentSpec.subcommands().get(arg);

            if (command != null) {
                return command.getCommandSpec();
            }
        }

        return null;
    }
}