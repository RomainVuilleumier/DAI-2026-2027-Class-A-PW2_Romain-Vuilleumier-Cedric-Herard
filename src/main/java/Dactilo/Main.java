package Dactilo;

import Dactilo.Client.Client;
import Dactilo.Server.Server;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "dactilo",
        mixinStandardHelpOptions = true,
        version = "Dactilo 1.0",
        subcommands = { Server.class, Client.class }
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