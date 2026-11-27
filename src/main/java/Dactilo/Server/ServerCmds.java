package Dactilo.Server;

import Dactilo.config.NetworkConfig;
import picocli.CommandLine;

@CommandLine.Command(name = "server", description = "Démarre le serveur de jeu")
public class ServerCmds implements Runnable {

        @CommandLine.Option(names = { "--tcp-port" }, defaultValue = "5000", description = "Port TCP")
        private int tcpPort;

        @CommandLine.Option(names = { "--udp-port" }, defaultValue = "5001", description = "Port UDP")
        private int udpPort;

        @Override
        public void run() {
                NetworkConfig config = new NetworkConfig("0.0.0.0", tcpPort, udpPort);
                Server server = new Server(config);
                server.start();
        }
}