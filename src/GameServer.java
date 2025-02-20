/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author reema
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    // Constants for server configuration
    private static final int PORT = 9090; // Port number the server listens on
    private static final int MIN_PLAYERS = 2; // Minimum players required to start a game
    private static final int MAX_PLAYERS = 5; // Maximum players allowed in a game
    private static final int GAME_START_DELAY = 30; // Delay in seconds before starting a game if there are too many players

    // Variables to manage game rooms and players
    private static int roomCounter = 1; // Counter to generate unique room IDs
    private static Set<String> connectedPlayers = new HashSet<>(); // Set of all connected players
    private static List<String> waitingRoom = new ArrayList<>(); // List of players waiting to start a game
    private static Map<String, List<String>> gameRooms = new HashMap<>(); // Map of game rooms and their players
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); // Scheduler for delayed game starts
    private static List<ClientHandler> clients = new ArrayList<>(); // List of active client handlers

    public static void main(String[] args) {
        System.out.println("Game Server is running...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            // Continuously accept new client connections
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept a new client connection
                ClientHandler clientHandler = new ClientHandler(clientSocket); // Create a handler for the client
                clients.add(clientHandler); // Add the handler to the list of clients
                new Thread(clientHandler).start(); // Start a new thread to handle the client
            }
        } catch (IOException e) {
            e.printStackTrace(); // Handle any errors that occur
        }
    }

    // Inner class to handle communication with a single client
    private static class ClientHandler implements Runnable {
        private Socket socket; // Socket for communication with the client
        private PrintWriter out; // Output stream to send messages to the client
        private BufferedReader in; // Input stream to receive messages from the client
        private String username; // Username of the connected player

        public ClientHandler(Socket socket) {
            this.socket = socket; // Initialize the socket
        }

        public void run() {
            try {
                // Set up input and output streams
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String message;
                // Continuously read messages from the client
                while ((message = in.readLine()) != null) {
                    handleClientMessage(message); // Handle the received message
                }
            } catch (IOException e) {
                e.printStackTrace(); // Handle any errors that occur
            } finally {
                disconnect(); // Clean up when the client disconnects
            }
        }

        // Method to handle different types of messages from the client
        private void handleClientMessage(String message) {
            if (message.startsWith("CONNECT")) {
                // Handle player connection
                username = message.substring(8); // Extract the username
                connectedPlayers.add(username); // Add the player to the connected players set
                sendToAll("PLAYER_LIST " + String.join(", ", connectedPlayers)); // Broadcast updated player list
            } else if (message.startsWith("PAIR_REQUEST")) {
                // Handle player request to join the waiting room
                addToWaitingRoom();
            }
        }

        // Method to add a player to the waiting room
        private void addToWaitingRoom() {
            waitingRoom.add(username); // Add the player to the waiting room
            sendToAll("PLAYER_JOINED " + username); // Notify all players that a new player has joined
            sendToAll("WAITING_ROOM " + String.join(", ", waitingRoom)); // Broadcast updated waiting room list
            if (waitingRoom.size() >= MIN_PLAYERS) {
                startGameIfReady(); // Start the game if enough players are waiting
            }
        }

        // Method to start a game if the conditions are met
        private void startGameIfReady() {
            if (waitingRoom.size() >= MIN_PLAYERS && waitingRoom.size() <= MAX_PLAYERS) {
                // Create a new game room
                String roomId = "Room-" + roomCounter++; // Generate a unique room ID
               gameRooms.put(roomId, new ArrayList<>(waitingRoom)); // Add the room to the game rooms map
                waitingRoom.clear(); // Clear the waiting room
                sendToAll("GAME_START " + roomId); // Notify all players that the game has started
                sendToAll("WAITING_ROOM " + String.join(", ", waitingRoom)); // Broadcast updated waiting room list
            } else if (waitingRoom.size() > MAX_PLAYERS) {
                scheduleGameStart(); // Schedule a delayed game start if there are too many players
            }
        }

        // Method to schedule a delayed game start
        private void scheduleGameStart() {
            scheduler.schedule(() -> {
                if (waitingRoom.size() >= MIN_PLAYERS) {
                    startGameIfReady(); // Start the game after the delay
                }
            }, GAME_START_DELAY, TimeUnit.SECONDS);
        }

        // Method to handle client disconnection
        private void disconnect() {
            if (username != null) {
                connectedPlayers.remove(username); // Remove the player from the connected players set
                waitingRoom.remove(username); // Remove the player from the waiting room
                sendToAll("PLAYER_LIST " + String.join(", ", connectedPlayers)); // Broadcast updated player list
                sendToAll("WAITING_ROOM " + String.join(", ", waitingRoom)); // Broadcast updated waiting room list
            }
            try {
                socket.close(); // Close the socket
            } catch (IOException e) {
                e.printStackTrace(); // Handle any errors that occur
            }
        }

        // Method to send a message to all connected clients
        private void sendToAll(String message) {
            for (ClientHandler client : clients) {
                client.out.println(message); // Send the message to each client
            }
        }
    }
}