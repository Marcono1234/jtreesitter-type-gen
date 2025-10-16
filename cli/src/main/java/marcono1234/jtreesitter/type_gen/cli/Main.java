package marcono1234.jtreesitter.type_gen.cli;

import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.io.PrintWriter;

public class Main {
    public static void main(String[] args) {
        int exitCode = main(args, null, null);
        System.exit(exitCode);
    }

    // Visible for testing
    static int main(String[] args, @Nullable PrintWriter stdOut, @Nullable PrintWriter stdErr) {
        var commandLineBuilder = new CommandLine(new CommandGenerate())
            .setParameterExceptionHandler(new ShortParameterErrorMessageHandler())
            .setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
                commandLine.getErr().println("[ERROR] Code generation failed");
                ex.printStackTrace(commandLine.getErr());
                return commandLine.getCommandSpec().exitCodeOnExecutionException();
            });
        if (stdOut != null) {
            commandLineBuilder.setOut(stdOut);
        }
        if (stdErr != null) {
            commandLineBuilder.setErr(stdErr);
        }

        return commandLineBuilder.execute(args);
    }
}
