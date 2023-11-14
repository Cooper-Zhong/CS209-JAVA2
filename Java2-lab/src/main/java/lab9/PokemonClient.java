package lab9;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class PokemonClient {

    public static void main(String[] args) {
        final String SERVER_ADDRESS = "127.0.0.1"; // Replace with server IP address
        final int PORT = 8082; // Replace with server port

        try (
            Socket socket = new Socket(SERVER_ADDRESS, PORT);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            System.out.println("Connected to server.");

            while (true) {
                System.out.print("Enter the pokemon name (or type 'QUIT' to exit): ");
                String userInputStr = userInput.readLine();

                if (userInputStr.equalsIgnoreCase("quit")) {
                    out.println("quit");
                    System.out.println("Client quits.");
                    break;
                }

                out.println(userInputStr); // Send the Pokemon name to the server

                // Receive and display information from the server
                for (int i = 0; i<4; i++) {
                    String response = in.readLine();
                    System.out.println(response);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
