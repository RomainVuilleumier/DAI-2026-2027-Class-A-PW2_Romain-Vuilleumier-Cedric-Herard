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


                                while (!socket.isClosed()){
                                        String input = in.readLine(); //we disigned the whole system so that the \n is the end of the message
                                        System.out.println("[Server] received textual data from client: " + input);
                                        if (socket.isClosed()) continue;
                                        SimplePair<String,Boolean> output = commandInterpreter(input,currentGame.getSecond().goal);
                                        System.out.println("[Server] sends it's reply to the client: " + output.getFirst());
                                        if (socket.isClosed()) continue;
                                        out.write(output.getFirst() + "\n");
                                        out.flush();
                                        if (output.getSecond()){ //joined a game
                                                System.out.println("[Server] waits for the game to start");
                                                try {
                                                        currentGame.getSecond().gate.await();
                                                        System.out.println("[Server] game started");
                                                        if (socket.isClosed()) continue;
                                                        out.write("START " + currentGame.getSecond().goal + "\n");
                                                        out.flush();
                                                } catch (InterruptedException e){
                                                        System.out.println("Stopped waiting without the game starting");
                                                        Thread.currentThread().interrupt();
                                                }
                                        }
                                }

                                System.out.println("[Server] closing connection");
                        } catch (Exception e) {
                                System.out.println("[Server] exception: " + e);
                        }
                }
                private SimplePair<String,Boolean> commandInterpreter(String data, String expectedText) throws Exception {
                        String[] parsedInput = data.split(" ");
                        switch (parsedInput[0]){
                                case null : throw new Exception("No commands to be interpreted");
                                case "JOIN":
                                        if (parsedInput.length != 1){
                                                return new SimplePair<>(WRONG_FORMAT,false);
                                        }
                                        else if (currentGame != null){
                                                return new SimplePair<>(WRONG_MOMENT,false);
                                        }
                                        currentGame = GameData.findAndJoinGame();
                                        if(currentGame == null){
                                                return new SimplePair<>(COMMAND_FAILED,false);
                                        }
                                        return new SimplePair<>("OK",true);
                                case "COMMIT":
                                        if (parsedInput.length == 1){
                                                return new SimplePair<>(WRONG_FORMAT,false);
                                        }
                                        else if (currentGame == null || !currentGame.getSecond().Started()){
                                                return new SimplePair<>(WRONG_MOMENT,false);
                                        }else {
                                                String inputText = data.substring(parsedInput[0].length()+1); //we ignore the command and the space after it
                                                if (inputText.compareTo(expectedText) == 0){
                                                        SimplePair<String,Boolean> temp = new SimplePair<>("RESULT " + currentGame.getSecond().getPosition(),false);
                                                        currentGame.getSecond().leaveGame();
                                                        currentGame = null;
                                                        return temp;
                                                } else {
                                                        return new SimplePair<>("REDO",false);
                                                }
                                        }
                                default:
                                        return new SimplePair<>(NOT_A_COMMAND,false);
                        }
                }
        }
}

