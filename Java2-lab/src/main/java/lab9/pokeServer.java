package lab9;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class pokeServer {


    public static void main(String[] args) {
        final int PORT = 8082; // Choose a port number
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running...");
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept incoming client connections
                System.out.println("Connected to client: " + clientSocket);

                // Handling client request in a separate thread
                new Thread(() -> handleClientRequest(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClientRequest(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            String pokemonName;

            while ((pokemonName = in.readLine()) != null && !pokemonName.equals("quit")) {
                System.out.println("Received request for: " + pokemonName);

                try {
                    // Make a REST request to PokeAPI
                    URL pokeApiUrl = new URL("https://pokeapi.co/api/v2/pokemon/" + pokemonName.toLowerCase());
                    String jsonResponse = new BufferedReader(new InputStreamReader(pokeApiUrl.openStream()))
                            .lines().collect(Collectors.joining());

                    JSONObject jsonObject = JSONObject.parseObject(jsonResponse);
                    String name = jsonObject.getString("name");
                    int height = jsonObject.getIntValue("height");
                    int weight = jsonObject.getIntValue("weight");
                    JSONArray abilities = jsonObject.getJSONArray("abilities");

                    // Construct the response message
                    String response = "Name: " + name + "\n"
                            + "Height: " + height + "\n"
                            + "Weight: " + weight + "\n"
                            + "Abilities: " + abilities.stream()
                            .map(obj -> ((JSONObject) obj).getJSONObject("ability").getString("name"))
                            .collect(Collectors.joining(", "));
                    out.println(response); // Send the response message to the client
                    System.out.println("Info of " + pokemonName + " sent.");

                } catch (FileNotFoundException e) {
                    out.println("Pokemon not found.");
                    continue;
                }


            }
            System.out.println("Client quits.");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}



