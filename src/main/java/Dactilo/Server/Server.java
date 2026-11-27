package Dactilo.Server;

import java.io.*;
        import java.net.*;
        import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

        private static final int PORT = 1234;
        private static final int MAX_NB_LOBBIES = 20;
        private static final int MAX_NB_CLIENT_IN_A_GAME = 10;

        private static final String NOT_A_COMMAND = "ERROR 1";
        private static final String WRONG_FORMAT = "ERROR 2";
        private static final String WRONG_MOMENT = "ERROR 3";
        private static final String COMMAND_FAILED = "ERROR 4";

        public static void main(String[] args) {
                try (ServerSocket serverSocket = new ServerSocket(PORT);
                     ExecutorService executorClient = Executors.newFixedThreadPool(MAX_NB_LOBBIES*MAX_NB_CLIENT_IN_A_GAME);
                     ExecutorService executorGAME = Executors.newFixedThreadPool(MAX_NB_LOBBIES)) {
                        System.out.println("[Server] starting with id ");
                        System.out.println("[Server] listening on port " + PORT);
                        for (int i = 0; i < MAX_NB_LOBBIES; i++){
                                //todo read from a file and get a string with getline
                                new GameData("goal",MAX_NB_CLIENT_IN_A_GAME); //the creation places itself in the queue of games to be filled
                        }
                        while (!serverSocket.isClosed()) {
                                Socket clientSocket = serverSocket.accept();
                                executorClient.submit(new ClientHandler(clientSocket));
                        }
                } catch (IOException e) {
                        System.out.println("[Server] exception: " + e);
                }
        }

        static class GameHandler implements Runnable{
                private GameData data;

                public GameHandler(GameData data) {
                        this.data = data;
                }

                @Override
                public void run(){
                        //game management
                }
        }

        static class ClientHandler implements Runnable {
                private final Socket socket;
                private SimplePair<Integer,GameData> currentGame;

                public ClientHandler(Socket socket) {
                        this.socket = socket;
                }

                @Override
                public void run() {
                        try (socket; // This allows to use try-with-resources with the socket
                             BufferedReader in =
                                     new BufferedReader(
                                             new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                             BufferedWriter out =
                                     new BufferedWriter(
                                             new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
                                System.out.println(
                                        "[Server] new client connected from "
                                                + socket.getInetAddress().getHostAddress()
                                                + ":"
                                                + socket.getPort());

                                System.out.println(
                                        "[Server] received textual data from client: " + in.readLine());

                                try {
                                        System.out.println(
                                                "[Server] sleeping for 10 seconds to simulate a long operation");
                                        Thread.sleep(10000);
                                } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                }

                                System.out.println(
                                        "[Server] sending response to client: " + TEXTUAL_DATA);

                                out.write(TEXTUAL_DATA + "\n");
                                out.flush();

                                System.out.println("[Server] closing connection");
                        } catch (IOException e) {
                                System.out.println("[Server] exception: " + e);
                        }
                }
                private String commandInterpreter(String[] parsedInput) throws Exception { //todo implement the cases
                        switch (parsedInput[0]){
                                case null : throw new Exception("No commands to be interpreted");
                                case "JOIN":
                                        if (parsedInput.length != 1){
                                                return NOT_A_COMMAND;
                                        }
                                        else if (currentGame == null){
                                                currentGame = GameData.findAndJoinGame();
                                                if(currentGame == null){
                                                        return COMMAND_FAILED;
                                                }
                                                return "OK";
                                        }
                                case "COMMIT":
                                default:
                        }
                }
        }
}

