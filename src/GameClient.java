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
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameClient {

    // Constants for server connection and game configuration
    private static final String SERVER_IP = "Localhost" ; // IP address of the server
    private static final int SERVER_PORT = 9090; // Port number of the server
    private static final int MIN_PLAYERS = 2; // Minimum players required to start a game
    private static final int MAX_PLAYERS = 5; // Maximum players allowed in a game
    private static final int GAME_START_DELAY = 30; // Delay in seconds before starting a game if there are too many players
    private static int roomCounter = 1; // Counter to generate unique room IDs (not used in client)

    // Networking and GUI components
    private Socket socket; // Socket for communication with the server
    private BufferedReader in; // Input stream to receive messages from the server
    private PrintWriter out; // Output stream to send messages to the server
    private String username; // Username of the player

    // GUI components
    private JTextArea chatArea; // Text area to display chat and server messages
    private DefaultListModel<String> playerListModel; // List model for connected players
    private DefaultListModel<String> waitingListModel; // List model for players in the waiting room
    private DefaultListModel<String> playRoomListModel; // List model for players in game rooms

    // Data structures to track players and game rooms (not used in client)
    private static List<String> waitingRoom = new ArrayList<>();
    private static HashMap<String, List<String>> gameRooms = new HashMap<>();

    // Constructor to initialize the GUI
    public GameClient() {
        createGUI();
    }

    // Method to create the graphical user interface
    private void createGUI() {
        JFrame frame = new JFrame("Multiplayer Game Client"); // Main window
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close the application on window close
        frame.setSize(600, 600); // Set window size
        frame.setLayout(new BorderLayout()); // Use BorderLayout for layout management

        // Top panel for username input and connect button
        JPanel topPanel = new JPanel();
        JTextField usernameField = new JTextField(10); // Text field for entering username
        JButton connectButton = new JButton("Connect"); // Button to connect to the server
        topPanel.add(new JLabel("Username: ")); // Label for the username field
        topPanel.add(usernameField); // Add username field to the panel
        topPanel.add(connectButton); // Add connect button to the panel

        // Chat area to display messages
        chatArea = new JTextArea();
        chatArea.setEditable(false); // Make chat area read-only
        JScrollPane chatScrollPane = new JScrollPane(chatArea); // Add scroll bars to the chat area

        // Button panel for game-related actions
        JPanel buttonPanel = new JPanel();
        JButton pairRequestButton = new JButton("Join Game"); // Button to request joining a game
        buttonPanel.add(pairRequestButton); // Add button to the panel

        // Player list to display connected players
        playerListModel = new DefaultListModel<>();
        JList<String> playerList = new JList<>(playerListModel);
        JScrollPane playerScrollPane = new JScrollPane(playerList);

        // Waiting list to display players in the waiting room
        waitingListModel = new DefaultListModel<>();
        JList<String> waitingList = new JList<>(waitingListModel);
        JScrollPane waitingScrollPane = new JScrollPane(waitingList);

        // Play room list to display players in game rooms
        playRoomListModel = new DefaultListModel<>();
        JList<String> playRoomList = new JList<>(playRoomListModel);
        JScrollPane playRoomScrollPane = new JScrollPane(playRoomList);

        // Panel to hold all lists with labels
        JPanel listPanel = new JPanel(new GridLayout(1, 3)); // Use GridLayout for equal spacing

        // Player List Section
        JPanel playerListPanel = new JPanel(new BorderLayout());
        playerListPanel.add(new JLabel("Connected Players"), BorderLayout.NORTH); // Label for player list
        playerListPanel.add(playerScrollPane, BorderLayout.CENTER); // Add player list

        // Waiting Room Section
        JPanel waitingRoomPanel = new JPanel(new BorderLayout());
        waitingRoomPanel.add(new JLabel("Waiting Room"), BorderLayout.NORTH); // Label for waiting room
        waitingRoomPanel.add(waitingScrollPane, BorderLayout.CENTER); // Add waiting list

        // Play Room Section
        JPanel playRoomPanel = new JPanel(new BorderLayout());
        playRoomPanel.add(new JLabel("Game Rooms"), BorderLayout.NORTH); // Label for play room
        playRoomPanel.add(playRoomScrollPane, BorderLayout.CENTER); // Add play room list

        // Add all sections to the list panel
        listPanel.add(playerListPanel); // Add player list section
        listPanel.add(waitingRoomPanel); // Add waiting room section
        listPanel.add(playRoomPanel); // Add play room section

        // Add all components to the main window
        frame.add(topPanel, BorderLayout.NORTH); // Add top panel to the top of the window
        frame.add(chatScrollPane, BorderLayout.CENTER); // Add chat area to the center
        frame.add(buttonPanel, BorderLayout.SOUTH); // Add button panel to the bottom
        frame.add(listPanel, BorderLayout.EAST); // Add list panel to the right

        // Action listener for the connect button
        connectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                username = usernameField.getText().trim(); // Get the username from the text field
                if (!username.isEmpty()) {
                    connectToServer(); // Connect to the server if the username is not empty
                }
            }
        });

        // Action listener for the pair request button
        pairRequestButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (out != null) {
                    out.println("PAIR_REQUEST " + username); // Send a pair request to the server
                }
            }
        });

        frame.setVisible(true); // Make the window visible
    }

    // Method to connect to the server
    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT); // Create a socket connection to the server
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Set up input stream
            out = new PrintWriter(socket.getOutputStream(), true); // Set up output stream

            out.println("CONNECT " + username); // Send the username to the server
            chatArea.append("Connected to the server as " + username + "\n"); // Display connection message

            new Thread(new ServerListener()).start(); // Start a thread to listen for server messages
        } catch (IOException e) {
            chatArea.append("Failed to connect to server.\n"); // Display error message if connection fails
        }
    }

    // Inner class to listen for messages from the server
    private class ServerListener implements Runnable {
        public void run() {
            try {
                String response;
                while ((response = in.readLine()) != null) { // Continuously read messages from the server
                    if (response.startsWith("PLAYER_LIST")) {
                        updatePlayerList(response.substring(12)); // Update the player list
                    } else if (response.startsWith("WAITING_ROOM")) {
                        updateWaitingList(response.substring(13)); // Update the waiting list
                    } else if (response.startsWith("PLAY_ROOM")) {
                        updatePlayRoomList(response.substring(10)); // Update the play room list
                    } else if (response.startsWith("PLAYER_JOINED")) {
                        chatArea.append(response.substring(14) + " joined the waiting room.\n"); // Notify when a player joins
                    } else if (response.startsWith("GAME_START")) {
                        chatArea.append("Game started! Room: " + response.substring(11) + "\n"); // Notify when a game starts
                    } else {
                        chatArea.append(response + "\n"); // Display any other messages
                    }
                }
            } catch (IOException e) {
                chatArea.append("Disconnected from server.\n"); // Notify when disconnected from the server
            }
        }
    }

    // Method to update the player list in the GUI
    private void updatePlayerList(String players) {
        playerListModel.clear(); // Clear the current player list
        for (String player : players.split(", ")) {
            playerListModel.addElement(player); // Add each player to the list
        }
    }

    // Method to update the waiting list in the GUI
    private void updateWaitingList(String waitingPlayers) {
        waitingListModel.clear(); // Clear the current waiting list
        for (String player : waitingPlayers.split(", ")) {
            waitingListModel.addElement(player); // Add each player to the list
        }
    }

    // Method to update the play room list in the GUI
    private void updatePlayRoomList(String playRoomPlayers) {
        playRoomListModel.clear(); // Clear the current play room list
        for (String player : playRoomPlayers.split(", ")) {
            playRoomListModel.addElement(player); // Add each player to the list
        }
    }

    // Main method to start the client
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameClient::new); // Launch the GUI on the Event Dispatch Thread
    }
}