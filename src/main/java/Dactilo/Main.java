package Dactilo;
import Dactilo.Client.*;
import Dactilo.Server.*;
import java.io.File;
import picocli.CommandLine;

@CommandLine.Command(
        name = "Dactilo-CHD-RMV",
        description = "Dactylo_Race",
        version = "1.0",
        showDefaultValues = true,
        subcommands = {
        },
        scope = CommandLine.ScopeType.INHERIT,
        mixinStandardHelpOptions = true)
public class Main {

    static void main(String[] args) {
        String jarFilename =
                new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath())
                        .getName();

        int exitCode = new CommandLine(new Main()).setCommandName(jarFilename).execute(args);

        System.exit(exitCode);
    }
}