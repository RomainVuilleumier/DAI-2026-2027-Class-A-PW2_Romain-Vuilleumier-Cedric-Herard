package Dactilo.Server;

import Dactilo.config.NetworkConfig;
import picocli.CommandLine;

@CommandLine.Command(name = "server", description = "Démarre le serveur de jeu")
public class ServerCmd implements Runnable {

        @CommandLine.Option(names = { "--tcp-port" }, defaultValue = "5000", description = "Port TCP")
        private int tcpPort;

        @CommandLine.Option(names = { "--udp-port" }, defaultValue = "5001", description = "Port UDP")
        private int udpPort;

        @Override
        public void run() {
                NetworkConfig config = new NetworkConfig();
                Server server = new Server();
                server.start();
        }
}