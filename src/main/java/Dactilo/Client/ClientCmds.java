package Dactilo.Client;

import Dactilo.config.NetworkConfig;
import picocli.CommandLine;

@CommandLine.Command(name = "client", description = "Démarre un client joueur avec interface Lanterna")
public class ClientCmds implements Runnable {

    @CommandLine.Option(names = { "--host" }, defaultValue = "localhost", description = "Adresse du serveur")
    private String host;

    @CommandLine.Option(names = { "--tcp-port" }, defaultValue = "5000", description = "Port TCP du serveur")
    private int tcpPort;

    @CommandLine.Option(names = { "--udp-port" }, defaultValue = "5001", description = "Port UDP du serveur")
    private int udpPort;

    @CommandLine.Option(names = { "-c", "--create" }, defaultValue = "false", description = "Créer la salle en tant qu'hôte")
    private boolean isHost;

    @CommandLine.Option(names = { "--room" }, required = true, description = "Nom de la salle")
    private String room;

    @CommandLine.Option(names = { "--user" }, required = true, description = "Pseudo du joueur")
    private String username;

    @Override
    public void run() {
        NetworkConfig config = new NetworkConfig(host, tcpPort, udpPort);
        Client client = new Client(config, room, username, isHost);
        client.start();
    }
}