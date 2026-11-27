package Dactilo;

import Dactilo.Client.ClientCmds;
import Dactilo.Server.ServerCmds;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "dactilo",
        mixinStandardHelpOptions = true,
        version = "Dactilo 1.0",
        subcommands = { ServerCmds.class, ClientCmds.class }
)
public class Main implements Runnable {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}