package io.airlift.airline.help.cli.bash;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import io.airlift.airline.CompletionBehaviour;
import io.airlift.airline.help.AbstractGlobalUsageGenerator;
import io.airlift.airline.model.CommandGroupMetadata;
import io.airlift.airline.model.CommandMetadata;
import io.airlift.airline.model.GlobalMetadata;
import io.airlift.airline.model.OptionMetadata;

public class BashCompletionGenerator extends AbstractGlobalUsageGenerator {

    private static final char NEWLINE = '\n';
    private static final String DOUBLE_NEWLINE = "\n\n";
    private final boolean withDebugging;

    public BashCompletionGenerator() {
        this(false);
    }

    /**
     * Creates a new completion generator
     * 
     * @param enableDebugging
     *            Whether to enable debugging, when true the generated script
     *            will do {@code set -o xtrace} in its functions and
     *            {@code set +o xtrace} at the end of its functions
     */
    public BashCompletionGenerator(boolean enableDebugging) {
        this.withDebugging = enableDebugging;
    }

    @Override
    public void usage(GlobalMetadata global, OutputStream output) throws IOException {
        Writer writer = new OutputStreamWriter(output);

        // Bash Header
        writer.append("#!/bin/bash").append(DOUBLE_NEWLINE);
        writer.append("# Generated by airline BashCompletionGenerator").append(DOUBLE_NEWLINE);

        // Helper functions
        writer.append("containsElement () {\n");
        writer.append("  # This function from http://stackoverflow.com/a/8574392/107591\n");
        writer.append("  local e\n");
        writer.append("  for e in \"${@:2}\"; do [[ \"$e\" == \"$1\" ]] && return 0; done\n");
        writer.append("  return 1\n");
        writer.append("}\n\n");

        // If there are multiple groups then we will need to generate a function
        // for each
        boolean hasGroups = global.getCommandGroups().size() > 1 || global.getDefaultGroupCommands().size() == 0;
        if (hasGroups) {
            for (CommandGroupMetadata group : global.getCommandGroups()) {
                generateGroupCompletionFunction(writer, global, group);
                for (CommandMetadata command : group.getCommands()) {
                    generateCommandCompletionFunction(writer, global, group, command);
                }
            }
        } else {
            for (CommandMetadata command : global.getDefaultGroupCommands()) {
                if (command.isHidden())
                    continue;

                generateCommandCompletionFunction(writer, global, null, command);
            }
        }

        // Start main completion function
        writer.append("_complete_").append(bashize(global.getName())).append("() {").append('\n');

        writer.append("  # Get completion data").append('\n');
        writer.append("  CURR_WORD=${COMP_WORDS[COMP_CWORD]}").append('\n');
        writer.append("  PREV_WORD=${COMP_WORDS[COMP_CWORD-1]}").append('\n');
        writer.append("  CURR_CMD=").append('\n');
        writer.append("  if [[ ${COMP_CWORD} -ge 1 ]]; then").append('\n');
        writer.append("    CURR_CMD=${COMP_WORDS[1]}").append('\n');
        writer.append("  fi").append(DOUBLE_NEWLINE);

        // Prepare list of top level commands and groups
        Set<String> commandNames = new HashSet<>();
        for (CommandMetadata command : global.getDefaultGroupCommands()) {
            if (command.isHidden())
                continue;
            commandNames.add(command.getName());
        }
        if (hasGroups) {
            for (CommandGroupMetadata group : global.getCommandGroups()) {
                commandNames.add(group.getName());
            }
        }
        writeWordListVariable(writer, 2, "COMMANDS", commandNames.iterator());

        // Firstly check whether we are only completing the group or command
        writer.append("  if [[ ${COMP_CWORD} -eq 1 ]]; then").append('\n');
        writer.append("    COMPREPLY=()").append('\n');
        writeCompletionGeneration(writer, 4, false, CompletionBehaviour.NONE, "COMMANDS");
        writer.append("  fi").append(DOUBLE_NEWLINE);

        // Otherwise we must be in a specific group/command
        // Use a switch statement to provide group/command specific completion
        writer.append("  case ${CURR_CMD} in ").append('\n');
        if (hasGroups) {
            for (CommandGroupMetadata group : global.getCommandGroups()) {
                // Add case for the group
                indent(writer, 4);
                writer.append(group.getName()).append(')').append('\n');
                indent(writer, 6);

                // Just call the group function and pass its value back up
                writer.append("COMPREPLY=( $( _completion_group_").append(bashize(group.getName())).append(" ) )").append('\n');
                indent(writer, 6);
                writer.append("return $?").append('\n');
                indent(writer, 6);
                writer.append(";;").append('\n');
            }
        } else {
            for (CommandMetadata command : global.getDefaultGroupCommands()) {
                if (command.isHidden())
                    continue;

                // Add case for the command
                indent(writer, 4);
                writer.append(command.getName()).append(')').append('\n');
                indent(writer, 6);

                // Just call the command function and pass its value back up
                writer.append("COMPREPLY=( $( _completion_command_").append(bashize(command.getName()))
                        .append(" \"${COMMANDS}\" ) )").append('\n');
                indent(writer, 6);
                writer.append("return $?").append('\n');
                indent(writer, 6);
                writer.append(";;").append('\n');
            }
        }

        writer.append("  esac").append(DOUBLE_NEWLINE);

        // End Function
        if (this.withDebugging) {
            writer.append("  set +o xtrace").append('\n');
        }
        writer.append("}").append(DOUBLE_NEWLINE);

        // Completion setup
        writer.append("complete -F _complete_").append(bashize(global.getName())).append(" ").append(global.getName());

        // Flush the output
        writer.flush();
        output.flush();
    }

    private void generateGroupCompletionFunction(Writer writer, GlobalMetadata global, CommandGroupMetadata group)
            throws IOException {
        // Start Function
        writer.append("_completion_group_").append(bashize(group.getName())).append("() {").append('\n');

        // Prepare variables
        writer.append("  # Get completion data").append('\n');
        writer.append("  COMPREPLY=()").append('\n');
        writer.append("  CURR_WORD=${COMP_WORDS[COMP_CWORD]}").append('\n');
        writer.append("  PREV_WORD=${COMP_WORDS[COMP_CWORD-1]}").append("\n");
        writer.append("  CURR_CMD=").append('\n');
        writer.append("  if [[ ${COMP_CWORD} -ge 2 ]]; then").append('\n');
        writer.append("    CURR_CMD=${COMP_WORDS[2]}").append('\n');
        writer.append("  fi").append(DOUBLE_NEWLINE);

        // Prepare list of group commands
        Set<String> commandNames = new HashSet<>();
        for (CommandMetadata command : group.getCommands()) {
            if (command.isHidden())
                continue;
            commandNames.add(command.getName());
        }
        writeWordListVariable(writer, 2, "COMMANDS", commandNames.iterator());

        // Check if we are completing a group
        writer.append("  if [[ ${COMP_CWORD} -eq 2 ]]; then").append('\n');
        writeCompletionGeneration(writer, 4, true, CompletionBehaviour.NONE, "COMMANDS");
        writer.append("  fi").append(DOUBLE_NEWLINE);

        // Otherwise we must be in a specific command
        // Use a switch statement to provide command specific completion
        writer.append("  case ${CURR_CMD} in").append('\n');
        for (CommandMetadata command : group.getCommands()) {
            if (command.isHidden())
                continue;

            // Add case for the command
            indent(writer, 4);
            writer.append(command.getName()).append(')').append('\n');
            indent(writer, 6);

            // Just call the command function and pass its value back up
            //@formatter:off
            writer.append("COMPREPLY=( $( _completion_")
                  .append(bashize(group.getName()))
                  .append("_command_")
                  .append(bashize(command.getName()))
                  .append(" \"${COMMANDS}\" ) )")
                  .append('\n');
            //@formatter:on
            indent(writer, 6);
            writer.append("echo ${COMPREPLY[@]}").append('\n');
            indent(writer, 6);
            writer.append("return $?").append('\n');
            indent(writer, 6);
            writer.append(";;").append('\n');
        }
        writer.append("  esac").append('\n');

        // End Function
        writer.append('}').append(DOUBLE_NEWLINE);
    }

    private void generateCommandCompletionFunction(Writer writer, GlobalMetadata global, CommandGroupMetadata group,
            CommandMetadata command) throws IOException {
        // Start Function
        writer.append("_completion_");
        if (group != null) {
            writer.append(bashize(group.getName())).append("_");
        }
        writer.append("command_").append(bashize(command.getName())).append("() {").append('\n');

        // Prepare variables
        writer.append("  # Get completion data").append('\n');
        writer.append("  COMPREPLY=()").append('\n');
        writer.append("  CURR_WORD=${COMP_WORDS[COMP_CWORD]}").append('\n');
        writer.append("  PREV_WORD=${COMP_WORDS[COMP_CWORD-1]}").append('\n');
        writer.append("  COMMANDS=$1").append(DOUBLE_NEWLINE);

        // Prepare the option information
        Set<String> flagOpts = new HashSet<>();
        Set<String> argOpts = new HashSet<>();
        for (OptionMetadata option : command.getAllOptions()) {
            if (option.isHidden())
                continue;

            if (option.getArity() == 0) {
                flagOpts.addAll(option.getOptions());
            } else {
                argOpts.addAll(option.getOptions());
            }
        }
        writeWordListVariable(writer, 2, "FLAG_OPTS", flagOpts.iterator());
        writeWordListVariable(writer, 2, "ARG_OPTS", argOpts.iterator());
        writer.append('\n');

        // Check whether we are completing a value for an argument flag
        if (argOpts.size() > 0) {
            writer.append("  $( containsElement ${PREV_WORD} ${ARG_OPTS[@]} )").append('\n');
            writer.append("  SAW_ARG=$?").append('\n');

            // If we previously saw an argument then we are completing that
            // argument
            writer.append("  if [[ ${SAW_ARG} -eq 0 ]]; then").append('\n');
            writer.append("    ARG_VALUES=").append('\n');
            writer.append("    ARG_GENERATED_VALUES=").append('\n');
            writer.append("    case ${PREV_WORD} in").append('\n');
            for (OptionMetadata option : command.getAllOptions()) {
                if (option.isHidden() || option.getArity() == 0)
                    continue;

                // Add cases for the names
                indent(writer, 6);
                Iterator<String> names = option.getOptions().iterator();
                while (names.hasNext()) {
                    writer.append(names.next());
                    if (names.hasNext())
                        writer.append('|');
                }
                writer.append(")\n");

                // Then generate the completions for the option
                if (StringUtils.isNotEmpty(option.getCompletionCommand())) {
                    indent(writer, 8);
                    writer.append("ARG_GENERATED_VALUES=$( ").append(option.getCompletionCommand()).append(" )")
                            .append('\n');
                }
                if (option.getAllowedValues() != null && option.getAllowedValues().size() > 0) {
                    writeWordListVariable(writer, 8, "ARG_VALUES", option.getAllowedValues().iterator());
                }
                writeCompletionGeneration(writer, 8, true, option.getCompletionBehaviours(), "ARG_VALUES",
                        "ARG_GENERATED_VALUES");
                indent(writer, 8);
                writer.append(";;").append('\n');
            }
            writer.append("    esac").append('\n');
            writer.append("  fi").append(DOUBLE_NEWLINE);
        }

        // If we previously saw a flag we could see another option or an
        // argument if supported
        int behaviour = CompletionBehaviour.NONE;
        if (command.getArguments() != null) {
            if (StringUtils.isNotEmpty(command.getArguments().getCompletionCommand())) {
                writer.append("  ARGUMENTS=$( ").append(command.getArguments().getCompletionCommand()).append(" )")
                        .append('\n');
            } else {
                writer.append("  ARGUMENTS=").append('\n');
            }
            behaviour = command.getArguments().getCompletionBehaviours();
        } else {
            writer.append("  ARGUMENTS=").append('\n');
        }
        writeCompletionGeneration(writer, 2, true, behaviour, "FLAG_OPTS", "ARG_OPTS", "ARGUMENTS");

        // End Function
        writer.append('}').append(DOUBLE_NEWLINE);
    }

    private void indent(Writer writer, int indent) throws IOException {
        repeat(writer, indent, ' ');
    }

    private void repeat(Writer writer, int count, char c) throws IOException {
        if (count <= 0)
            return;
        for (int i = 0; i < count; i++) {
            writer.append(c);
        }
    }

    private void writeWordListVariable(Writer writer, int indent, String varName, Iterator<String> words)
            throws IOException {
        indent(writer, indent);
        writer.append(varName).append("=\"");
        while (words.hasNext()) {
            writer.append(words.next());
            if (words.hasNext())
                writer.append(' ');
        }
        writer.append('"').append('\n');
    }

    private void writeCompletionGeneration(Writer writer, int indent, boolean isNestedFunction, int behaviour,
            String... varNames) throws IOException {
        indent(writer, indent);
        writer.append("COMPREPLY=( $(compgen ");

        // Add -o flag as appropriate
        switch (behaviour) {
        case CompletionBehaviour.FILENAMES:
            writer.append("-o default ");
            break;
        case CompletionBehaviour.DIRECTORIES:
            writer.append("-o dirnames ");
            break;
        case CompletionBehaviour.AS_FILENAMES:
            writer.append("-o filenames ");
            break;
        case CompletionBehaviour.AS_DIRECTORIES:
            writer.append("-o plusdirs ");
            break;
        case CompletionBehaviour.SYSTEM_COMMANDS:
            writer.append("-c ");
            break;
        }

        // Build a word list from available variables
        writer.append("-W \"");
        for (int i = 0; i < varNames.length; i++) {
            writer.append("${").append(varNames[i]).append("}");
            if (i < varNames.length - 1)
                writer.append(' ');
        }
        if (behaviour == CompletionBehaviour.CLI_COMMANDS) {
            writer.append(" ${COMMANDS}");
        }
        writer.append("\" -- ${CURR_WORD}) )").append('\n');

        // Echo is necessary due when using a nested function calls
        if (isNestedFunction) {
            indent(writer, indent);
            writer.append("echo ${COMPREPLY[@]}").append('\n');
        }
        indent(writer, indent);
        writer.append("return 0").append('\n');
    }

    private String bashize(String value) {
        StringBuilder builder = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_') {
                builder.append(c);
            }
        }
        return builder.toString();
    }

}
