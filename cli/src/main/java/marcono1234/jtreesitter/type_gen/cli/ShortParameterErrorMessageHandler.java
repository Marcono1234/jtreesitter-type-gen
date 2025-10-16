package marcono1234.jtreesitter.type_gen.cli;

import picocli.CommandLine;

import java.io.PrintWriter;

/**
 * Parameter exception handler which unlike the default one does not print the full usage help,
 * making it less verbose.
 */
// Based on example from `picocli.CommandLine#getParameterExceptionHandler`
class ShortParameterErrorMessageHandler implements CommandLine.IParameterExceptionHandler {
    @Override
    public int handleParseException(CommandLine.ParameterException ex, String[] args) {
        CommandLine cmd = ex.getCommandLine();
        PrintWriter writer = cmd.getErr();

        writer.println(ex.getMessage());
        CommandLine.UnmatchedArgumentException.printSuggestions(ex, writer);
        writer.print(cmd.getHelp().fullSynopsis());

        CommandLine.Model.CommandSpec spec = cmd.getCommandSpec();
        writer.printf("%nTry '--help' for more information.%n");

        return cmd.getExitCodeExceptionMapper() != null
            ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
            : spec.exitCodeOnInvalidInput();
    }
}
