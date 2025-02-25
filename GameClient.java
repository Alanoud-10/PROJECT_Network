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
    private static final String SERVER_IP = "Localhost";
    private static final int SERVER_PORT = 4567;
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

    public GameClient(String username) {
        this.username = username;
        createGUI();
        connectToServer();
    }

private void createGUI() {
        JFrame mainFrame = new JFrame("Multiplayer Game Client");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(800, 600);
        mainFrame.setLayout(new BorderLayout());
       

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        
        CardLayout cardLayout = new CardLayout();
       
        JPanel cardPanel = new JPanel(cardLayout) {
        @Override
        protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        ImageIcon background = new ImageIcon("math.jpg");
        g.drawImage(background.getImage(), 0, 0, getWidth(), getHeight(), this);
    }
};
        JLabel titleLabel = new JLabel("قائمة المتصلين في العد السريع", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Dubai", Font.BOLD, 40)); 
        titleLabel.setForeground(new Color(255, 255, 255)); 
        titleLabel.setBorder(BorderFactory.createEmptyBorder(50, 0, 10, 0));
       
        JPanel connectedPlayersPanel = new JPanel(new BorderLayout());
        playerListModel = new DefaultListModel<>();
        JList<String> playerList = new JList<>(playerListModel);
        JScrollPane playerScrollPane = new JScrollPane(playerList);
        JPanel playerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,0,50));
        playerScrollPane.setPreferredSize(new Dimension(400, 300));
        playerPanel.add(playerScrollPane);
        playerList.setBackground(new Color(240, 248, 255)); 
        playerList.setForeground(new Color(25, 25, 112)); 
        playerList.setSelectionBackground(new Color(100, 149, 237)); 

        playerList.setFont(new Font("Dubai", Font.PLAIN, 20)); 
        playerScrollPane.setBorder(BorderFactory.createLineBorder(new Color(70, 130, 180), 2));



        playerScrollPane.getVerticalScrollBar().setUnitIncrement(16);
;

        //JPanel buttonPanel = new JPanel();
        JButton pairRequestButton = new JButton("ابدأ اللعبة ");
        pairRequestButton.setOpaque(true);
        pairRequestButton.setContentAreaFilled(true);


        //buttonPanel.add(pairRequestButton);
        // Enhance the appearance of the "Join Game" button
pairRequestButton.setBackground(new Color(240,248,255)); // Set a vibrant blue background color
pairRequestButton.setForeground(new Color(25,25,112)); // Set the text color to white for better contrast
pairRequestButton.setFont(new Font("Dubai", Font.BOLD, 20)); // Increase font size and make it bold
pairRequestButton.setFocusPainted(false); // Remove focus border when clicked
pairRequestButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Add internal padding
// Add rounded corners using a custom Border
pairRequestButton.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(70, 130, 180), 2, true), // Rounded border with darker blue color
        BorderFactory.createEmptyBorder(10, 20, 10, 20) // Internal padding for better spacing
));

// Add hover effect to change background color on mouse over
pairRequestButton.addMouseListener(new java.awt.event.MouseAdapter() {
    @Override
    public void mouseEntered(java.awt.event.MouseEvent evt) {
        pairRequestButton.setBackground(new Color(244, 255, 255)); // Darker shade on hover
    }

    @Override
    public void mouseExited(java.awt.event.MouseEvent evt) {
        pairRequestButton.setBackground(new Color(240, 248, 255)); // Revert to the original color
    }
});

        
        playRoomListModel = new DefaultListModel<>();
        JList<String> playRoomList = new JList<>(playRoomListModel);
        JScrollPane playRoomScrollPane = new JScrollPane(playRoomList);
        playerPanel.setOpaque(false);
        playerScrollPane.setOpaque(false);
        playerList.setOpaque(false);


       
        connectedPlayersPanel.setOpaque(false);
        connectedPlayersPanel.add(titleLabel, BorderLayout.NORTH);
        connectedPlayersPanel.add(playerPanel, BorderLayout.CENTER);
        connectedPlayersPanel.add(pairRequestButton, BorderLayout.SOUTH);

// Create a panel for the title of the waiting room
JLabel waitingRoomTitle = new JLabel("قائمة الانتظار في العد السريع", SwingConstants.CENTER);
waitingRoomTitle.setFont(new Font("Dubai", Font.BOLD, 40)); // Adjust font size and style
waitingRoomTitle.setForeground(new Color(255, 255, 255)); // Set text color
waitingRoomTitle.setBorder(BorderFactory.createEmptyBorder(50, 0, 0, 0)); // Add padding for spacing
waitingRoomTitle.setAlignmentX(Component.CENTER_ALIGNMENT);


        JPanel waitingRoomContainer = new JPanel(new BorderLayout());
        waitingRoomContainer.setLayout(new BoxLayout(waitingRoomContainer, BoxLayout.Y_AXIS));
        waitingRoomContainer.setOpaque(false); // Make it transparent to blend with background

         waitingListModel = new DefaultListModel<>();
         JList<String> waitingList = new JList<>(waitingListModel);
         JScrollPane waitingScrollPane = new JScrollPane(waitingList);
         
waitingScrollPane.setPreferredSize(new Dimension(400, 300)); // Same width as connected players list
waitingList.setBackground(new Color(240, 248, 255)); 
waitingList.setForeground(new Color(25, 25, 112)); 
waitingList.setSelectionBackground(new Color(100, 149, 237)); 
waitingList.setFont(new Font("Dubai", Font.PLAIN, 20)); 
waitingScrollPane.setBorder(BorderFactory.createLineBorder(new Color(70, 130, 180), 2));
waitingScrollPane.getVerticalScrollBar().setUnitIncrement(16);
// Create a panel to hold the list and apply centered alignment
JPanel waitingRoomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 50));
waitingRoomPanel.add(waitingScrollPane);
waitingRoomPanel.setOpaque(false); // Make it transparent to blend with background


// Add title and list to the container
waitingRoomContainer.add(Box.createVerticalStrut(20)); // Add spacing above the title
waitingRoomContainer.add(waitingRoomTitle);
waitingRoomContainer.add(Box.createVerticalStrut(10)); // Add spacing below the title
waitingRoomContainer.add(waitingRoomPanel);
waitingRoomContainer.add(Box.createVerticalStrut(10)); // Add spacing below the list

// Add the waiting room container to the main panel
connectedPlayersPanel.add(waitingRoomContainer, BorderLayout.EAST);
 
         cardPanel.add(connectedPlayersPanel, "ConnectedPlayers");
         cardPanel.add(waitingRoomContainer, "WaitingRoom");
         cardLayout.show(cardPanel, "ConnectedPlayers");
        
         mainFrame.add(cardPanel);
         mainFrame.setLocationRelativeTo(null);
         mainFrame.setVisible(true);
         
      

       // JPanel listPanel = new JPanel(new GridLayout(1, 3));
      //  listPanel.add(createLabeledPanel("Connected Players", playerScrollPane));
        //listPanel.add(createLabeledPanel("Waiting Room", waitingScrollPane));
       // listPanel.add(createLabeledPanel("Game Rooms", playRoomScrollPane));

      //  frame2.add(chatScrollPane, BorderLayout.CENTER);
      //  frame2.add(buttonPanel, BorderLayout.SOUTH);
      //  frame2.add(listPanel, BorderLayout.EAST);
       // frame2.add(createLabeledPanel("Connected Players",playerScrollPane ), BorderLayout.CENTER);
       // Create background panel with image

       pairRequestButton.addActionListener(e -> {
            if (out != null) {
                 out.println("PAIR_REQUEST " + username);
               cardLayout.show(cardPanel, "WaitingRoom");
            }
                    
        });

        
    }
                
     
                
   
    
    private JPanel createLabeledPanel(String title, JScrollPane content) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(title, JLabel.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(label, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
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
        SwingUtilities.invokeLater(LoginScreen::new);  // Launch the GUI on the Event Dispatch Thread
   
    }
}

class LoginScreen {
    public LoginScreen() {
        JFrame frame = new JFrame("Game Login");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ImageIcon background = new ImageIcon("math.jpg");
                g.drawImage(background.getImage(), 0, 0, getWidth(), getHeight(), this);
            }
        };
        panel.setLayout(new GridBagLayout());
        
         GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); 
         gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JLabel welcomeLabel = new JLabel("مرحبا بكم في العد السريع ", SwingConstants.CENTER);
        welcomeLabel.setForeground(new Color(255, 255, 255)); 
        welcomeLabel.setFont(new Font("Dubai", Font.BOLD, 40)); 
        welcomeLabel.setBorder(BorderFactory.createEmptyBorder(50, 0, 10, 0));
        welcomeLabel.setOpaque(false);


        panel.add(welcomeLabel, gbc);

       
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        JTextField usernameField = new JTextField(20);
        panel.add(usernameField, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
       

        JLabel usernameLabel = new JLabel("أدخل الاسم للبدأ :");
        usernameLabel.setForeground(Color.WHITE);
        usernameLabel.setFont(new Font("Dubai", Font.BOLD, 25));
        panel.add(usernameLabel, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL; 
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2; 

        gbc.anchor = GridBagConstraints.CENTER;
        
     

        JButton connectButton = new JButton("إتصل");
        connectButton.setFont(new Font("Dubai", Font.BOLD, 25));
        connectButton.setForeground(new Color(25,25,112));
        panel.add(connectButton, gbc);

   
        connectButton.setPreferredSize(new Dimension(150, 50)); 
       
        frame.revalidate();
        frame.repaint();

        frame.add(panel, BorderLayout.CENTER);

        connectButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            if (!username.isEmpty()) {
                frame.dispose();
                new GameClient(username);
            }
        });
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}