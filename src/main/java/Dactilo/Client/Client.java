package Dactilo.Client;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import Dactilo.Server.*;


class Client {

    private static final String HOST = "localhost";
    private static final int PORT = 1234;
    private static final String PSEUDO = "temp"; //todo take in the input of the command to display clearly in the terminal
    private static Boolean shouldWait = false;

    public static void main(String args[]) {
        System.out.println("[Client " + PSEUDO + "] starting with pseudo " + PSEUDO);
        System.out.println("[Client " + PSEUDO + "] connecting to " + HOST + ":" + PORT);
        String data;
        String answer;

        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader in =
                     new BufferedReader(
                             new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out =
                     new BufferedWriter(
                             new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)); ) {
            System.out.println("[Client " + PSEUDO + "] connected to " + HOST + ":" + PORT);

            while (!socket.isClosed()) {
                Join(out);
                answer = in.readLine();
                AnswerToJoin(answer,out,in);
                answer = in.readLine();

                if (answer == null || !answer.startsWith("START ")) {
                    throw new Exception("Expected START message");
                }

                System.out.println("Game started: " + answer);
                Commit(out,getCommitAnswer());
                answer = in.readLine();
                int pos = AnswerToCommit(answer,out,in);
                System.out.println("The position in the classment of the client is " + pos);
            }

            System.out.println("[Client " + PSEUDO + "] closing connection");
        } catch (Exception e) {
            System.out.println("[Client " + PSEUDO + "] exception: " + e);
        }
    }

    private static void AnswerToJoin(String input, BufferedWriter out,BufferedReader in) throws Exception {
        String[] parsedInput = input.split(" ");
        switch (parsedInput[0]){
            case null : throw new Exception("No commands to be interpreted");
            case "OK":
                return;
            case "ERROR":
                if (parsedInput.length == 1){
                    throw new Exception("no ERROR code received");
                } else {
                    switch (parsedInput[1]){
                        case null: throw new Exception("no ERROR code received");
                        case "4":
                            System.out.println("Failed to find a server, trying again");
                            Join(out);
                            input = in.readLine();
                            AnswerToJoin(input, out, in);
                        default: throw new Exception("unExpected answer");
                    }
                }
            default:
                throw new Exception("Unknown command");
        }

    }

    private static int AnswerToCommit(String input, BufferedWriter out,BufferedReader in) throws Exception {
        String[] parsedInput = input.split(" ");
        switch (parsedInput[0]){
            case null : throw new Exception("No commands to be interpreted");
            case "REDO":
                Commit(out,getCommitAnswer());
                input = in.readLine();
                return AnswerToCommit(input, out, in);
            case "RESULT":
                return Integer.parseInt(parsedInput[1]);
            case "ERROR":
                switch (parsedInput[1]){
                    case "2":
                        Commit(out,getCommitAnswer());
                        input = in.readLine();
                        return AnswerToCommit(input, out, in);
                    case "3":
                        throw new Exception("Tried to commit while not even in a game or having waited for it to start");
                }
            default:
                throw new Exception("Unknown command");
        }
    }

    private static void Commit(BufferedWriter out, String textToCommit) throws IOException {
        out.write("COMMIT " + textToCommit + "\n");
        out.flush();
    }
    private static void Join(BufferedWriter out) throws IOException {
        out.write("JOIN\n");
        out.flush();
    }

    static String getCommitAnswer(){
        String answer = "";
        //todo implement the interaction with the cli
        return answer;
    }
}