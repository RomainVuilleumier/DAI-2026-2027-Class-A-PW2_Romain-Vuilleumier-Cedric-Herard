package Dactilo.Client;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import Dactilo.Server.*;


class Client {

    private static final String HOST = "localhost";
    private static final int PORT = 1234;
    private static final String PSEUDO = "temp"; //todo take in the input of the command to display clearly in the terminal

    public static void main(String args[]) {
        System.out.println("[Client " + PSEUDO + "] starting with pseudo " + PSEUDO);
        System.out.println("[Client " + PSEUDO + "] connecting to " + HOST + ":" + PORT);
        String data;

        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader in =
                     new BufferedReader(
                             new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out =
                     new BufferedWriter(
                             new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)); ) {
            System.out.println("[Client " + PSEUDO + "] connected to " + HOST + ":" + PORT);
            System.out.println(
                    "[Client "
                            + PSEUDO
                            + "] sending textual data to server "
                            + HOST
                            + ":"
                            + PORT
                            + ": "
                            + data);

            out.write(data + "\n");
            out.flush();

            data = in.readLine();
            String[] parsed = data.split(" ");
            //String command = parsed[0];
            //todo analysis of the command
            //todo analysis of the given args
            String inputText = data.substring(parsed[0].length()+1); //we ignore the command and the space after it
            //todo compare with the actual text wanted

            System.out.println("[Client " + PSEUDO + "] response from server: " + in.readLine());

            System.out.println("[Client " + PSEUDO + "] closing connection");
        } catch (IOException e) {
            System.out.println("[Client " + PSEUDO + "] exception: " + e);
        }
    }

    private void commandInterpreter(String[] parsedInput) throws Exception {//todo implement the cases
        switch (parsedInput[0]){
            case null : throw new Exception("No commands to be interpreted");
            case "REDO":
            case "RESULT":
            case "OK":
            case "ERROR":
            default:
        }
    }
}