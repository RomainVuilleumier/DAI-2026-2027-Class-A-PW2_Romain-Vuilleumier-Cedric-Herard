package Dactilo.Client;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

class Client {

    private static final String HOST = "localhost";
    private static final int PORT = 1234;
    private static final String PSEUDO = "temp";

    public static void main(String args[]) {
        System.out.println("[Client " + PSEUDO + "] starting with pseudo " + PSEUDO);
        System.out.println("[Client " + PSEUDO + "] connecting to " + HOST + ":" + PORT);

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
                            + TEXTUAL_DATA);

            out.write(TEXTUAL_DATA + "\n");
            out.flush();

            System.out.println("[Client " + PSEUDO + "] response from server: " + in.readLine());

            System.out.println("[Client " + PSEUDO + "] closing connection");
        } catch (IOException e) {
            System.out.println("[Client " + PSEUDO + "] exception: " + e);
        }
    }
}