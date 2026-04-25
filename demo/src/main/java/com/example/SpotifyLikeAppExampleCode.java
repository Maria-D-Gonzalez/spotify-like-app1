package com.example;

// ============================================================================
// IMPORT STATEMENTS
// ============================================================================

// AWT and Layout imports for GUI components
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

// File I/O imports for reading audio library from files
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

// Network imports for file URL handling
import java.net.MalformedURLException;
import java.net.URL;

// Charset imports for UTF-8 encoding
import java.nio.charset.StandardCharsets;

// Utility imports for arrays and localization
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

// Audio system imports for sound playback
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

// Swing imports for GUI components (buttons, lists, panels, frames)
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

// GSON library imports for JSON parsing
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

// ============================================================================
// SPOTIFY-LIKE MUSIC PLAYER APPLICATION
// ============================================================================
// This application provides a music player interface with search, playback,
// and favorite functionality. Users can browse a library of songs, play audio,
// and manage their favorite tracks through a Swing-based GUI.
// ============================================================================

public class SpotifyLikeAppExampleCode {

  // ========== AUDIO PLAYBACK STATE VARIABLES ==========
  // Manages the current audio clip being played
  private static Clip audioClip;
  
  // Tracks whether the audio is currently paused
  private static boolean isPaused = false;
  
  // Stores the playback position when paused (in microseconds)
  private static long pausePosition = 0;
  
  // Reference to the currently selected song
  private static Song currentSong;

  // ========== FAVORITES MANAGEMENT VARIABLES ==========
  // Set to store favorite songs (using song name + artist as identifier)
  private static Set<String> favoriteSongs = new HashSet<>();

  // ========== MAIN APPLICATION ENTRY POINT ==========
  /**
   * Main method - Entry point of the application
   * Initializes the music library and launches the GUI
   */
  public static void main(final String[] args) {
    // Load favorite songs from storage
    loadFavorites();
    
    // Load the audio library from the JSON file
    Song[] library = readAudioLibrary();
    
    // Validate that library loaded successfully and contains songs
    if (library == null || library.length == 0) {
      System.err.println("Could not load the audio library. Check audio-library.json and the classpath.");
      return;
    }

    // Launch the GUI on the Event Dispatch Thread
    SwingUtilities.invokeLater(() -> createAndShowGui(library));
  }

  // ========== GUI INITIALIZATION AND SETUP ==========
  /**
   * Creates and displays the main application window with all UI components
   * Initializes buttons, search functionality, song list, and event listeners
   */
  @SuppressWarnings("Convert2Lambda")
  private static void createAndShowGui(final Song[] library) {
    // Create and configure the main application window
    JFrame frame = new JFrame("Music Player");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout(10, 10));
    frame.setMinimumSize(new Dimension(720, 520));
    frame.getContentPane().setBackground(Color.RED);

    // ========== SONG LIST SETUP ==========
    // Initialize the list model with all songs from the library
    DefaultListModel<Song> listModel = new DefaultListModel<>();
    Arrays.stream(library).forEach(listModel::addElement);

    // Create the song list UI component with custom rendering
    JList<Song> songList = new JList<>(listModel);
    songList.setSelectionMode(JList.WHEN_IN_FOCUSED_WINDOW);
    
    // Custom renderer to display song name and artist
    songList.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Song song) {
          // Format: "Song Name — Artist Name"
          setText(song.name() + " — " + song.artist());
        }
        return this;
      }
    });
    
    // Select the first song by default
    if (!listModel.isEmpty()) {
      songList.setSelectedIndex(0);
    }

    // Add scrollable container for the song list with title border
    JScrollPane listScroll = new JScrollPane(songList);
    listScroll.setBorder(javax.swing.BorderFactory.createTitledBorder("Library"));
    listScroll.setPreferredSize(new Dimension(700, 320));

    // ========== INFORMATION AND SEARCH PANEL ==========
    // Status label to display currently playing track information
    JLabel infoLabel = new JLabel("Select a track and press Play.");
    infoLabel.setBorder(new EmptyBorder(8, 8, 8, 8));

    // Search panel components
    JTextField searchField = new JTextField(24);
    JButton searchButton = new JButton("Search");
    JButton clearButton = new JButton("Clear");
    JButton viewFavoritesButton = new JButton("View Favorites");
    
    // Arrange search components horizontally
    JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
    searchPanel.add(new JLabel("Search:"));
    searchPanel.add(searchField);
    searchPanel.add(searchButton);
    searchPanel.add(viewFavoritesButton);
    searchPanel.add(clearButton);

    // ========== PLAYBACK CONTROL BUTTONS ==========
    // Create buttons for audio playback control
    JButton playButton = new JButton("Play");
    JButton pauseButton = new JButton("Pause");
    JButton stopButton = new JButton("Stop");
    JButton favoriteButton = new JButton("Favorites");
    
    // Arrange playback controls horizontally in the center
    JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
    controlPanel.add(playButton);
    controlPanel.add(pauseButton);
    controlPanel.add(stopButton);
    controlPanel.add(favoriteButton);

    // ========== LAYOUT ASSEMBLY ==========
    // Combine search and control panels into bottom panel
    JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));
    bottomPanel.add(searchPanel, BorderLayout.NORTH);
    bottomPanel.add(controlPanel, BorderLayout.CENTER);
    bottomPanel.add(controlPanel, BorderLayout.EAST);
    bottomPanel.add(infoLabel, BorderLayout.SOUTH);

    // Add all components to the main frame
    frame.add(listScroll, BorderLayout.CENTER);
    frame.add(bottomPanel, BorderLayout.SOUTH);

    // ========== SEARCH FUNCTIONALITY LISTENERS ==========
    // Search button - filters the library by song name, artist, or genre
    searchButton.addActionListener(e -> filterLibrary(searchField.getText(), listModel, library));
    
    // Clear button - resets search field and shows all songs
    clearButton.addActionListener(e -> {
      searchField.setText("");
      filterLibrary("", listModel, library);
    });
    
    // View Favorites button - opens a window showing all favorite songs
    viewFavoritesButton.addActionListener(e -> showFavoritesWindow(library));
    
    // ========== SONG LIST SELECTION LISTENER ==========
    // Updates info label when a different song is selected
    songList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          Song selected = songList.getSelectedValue();
          infoLabel.setText(selected != null ? formatSongInfo(selected) : "Select a track and press Play.");
        }
      }
    });

    // ========== PLAYBACK CONTROL LISTENERS ==========
    // Play button - starts playback of the selected song
    playButton.addActionListener(e -> {
      Song selected = songList.getSelectedValue();
      if (selected == null) {
        showMessage("Please select a track first.", "No Selection", frame);
        return;
      }
      play(selected);
      infoLabel.setText("Playing: " + formatSongInfo(selected));
    });

    // Pause button - pauses or resumes audio playback
    pauseButton.addActionListener(e -> {
      if (audioClip == null) {
        showMessage("No audio is playing.", "Information", frame);
        return;
      }
      togglePause();
      infoLabel.setText(isPaused ? "Paused" : "Playing: " + formatSongInfo(currentSong));
    });

    // Stop button - stops playback and clears current song
    stopButton.addActionListener(e -> {
      stopAudio();
      infoLabel.setText("Stopped.");
    });

    // Favorite button - adds or removes selected song from favorites
    favoriteButton.addActionListener(e -> {
      Song selected = songList.getSelectedValue();
      if (selected == null) {
        showMessage("Please select a track to favorite.", "No Selection", frame);
        return;
      }
      toggleFavorite(selected, frame);
    });

    // Allow pressing Enter in search field to trigger search
    searchField.addActionListener(e -> searchButton.doClick());

    // Display the frame and center it on screen
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  // ========== LIBRARY FILTERING AND SEARCH ==========
  /**
   * Filters the song library based on search query
   * Searches song name, artist, and genre (case-insensitive)
   * @param query - The search term entered by the user
   * @param model - The list model to update with filtered results
   * @param library - The original complete library of songs
   */
  /**
   * Filters the song library based on search query
   * Searches song name, artist, and genre (case-insensitive)
   * @param query - The search term entered by the user
   * @param model - The list model to update with filtered results
   * @param library - The original complete library of songs
   */
  private static void filterLibrary(String query, DefaultListModel<Song> model, Song[] library) {
    // Clear the current list to show filtered results
    model.clear();
    
    // Normalize search query to lowercase for case-insensitive matching
    String lowerQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    
    // Iterate through library and add matching songs
    for (Song song : library) {
      // Check if query matches song name, artist, or genre
      boolean matches = lowerQuery.isEmpty()
        || song.name().toLowerCase(Locale.ROOT).contains(lowerQuery)
        || song.artist().toLowerCase(Locale.ROOT).contains(lowerQuery)
        || (song.genre() != null && song.genre().toLowerCase(Locale.ROOT).contains(lowerQuery));
      
      // Add song to filtered list if it matches
      if (matches) {
        model.addElement(song);
      }
    }
  }

  // ========== SONG INFORMATION FORMATTING ==========
  /**
   * Formats song information for display in the UI
   * Combines song name, artist, year, and genre into a single string
   * @param song - The song to format
   * @return - Formatted string: "Song Name — Artist Name (Year) [Genre]"
   */
  private static String formatSongInfo(Song song) {
    if (song == null) {
      return "No track selected.";
    }
    
    // Build formatted string with song details
    StringBuilder builder = new StringBuilder();
    builder.append(song.name()).append(" — ").append(song.artist());
    
    // Add year if available
    if (song.year() != null) {
      builder.append(" (").append(song.year()).append(")");
    }
    
    // Add genre if available
    if (song.genre() != null && !song.genre().isBlank()) {
      builder.append(" [").append(song.genre()).append("]");
    }
    
    return builder.toString();
  }

  // ========== AUDIO PLAYBACK METHODS ==========
  /**
   * Starts playback of the selected song
   * Stops previous playback if audio is already playing
   * @param song - The song to play
   */
  private static void play(Song song) {
    // Get the URL path to the audio file
    URL audioUrl = getAudioResource(song.fileName());
    if (audioUrl == null) {
      System.err.println("Unable to locate audio file: " + song.fileName());
      return;
    }

    // Stop and close any previously playing audio
    if (audioClip != null) {
      audioClip.stop();
      audioClip.close();
    }

    // Attempt to load and play the audio file
    try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioUrl)) {
      audioClip = AudioSystem.getClip();
      audioClip.open(audioStream);
      audioClip.setMicrosecondPosition(0);
      // Loop audio continuously until stopped
      audioClip.loop(Clip.LOOP_CONTINUOUSLY);
      
      // Reset pause state and save reference to current song
      isPaused = false;
      pausePosition = 0;
      currentSong = song;
    } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
      System.err.println("ERROR: Failed to play audio file: " + e.getMessage());
    }
  }

  /**
   * Toggles pause/resume state of currently playing audio
   * If paused, resumes playback; if playing, pauses playback
   */
  public static void togglePause() {
    if (audioClip == null) {
      return;
    }
    
    // Resume playback from saved position
    if (isPaused) {
      audioClip.setMicrosecondPosition(pausePosition);
      audioClip.start();
      isPaused = false;
    } 
    // Pause playback and save current position
    else {
      pausePosition = audioClip.getMicrosecondPosition();
      audioClip.stop();
      isPaused = true;
    }
  }

  /**
   * Stops audio playback and cleans up resources
   * Resets all playback state variables
   */
  public static void stopAudio() {
    if (audioClip != null) {
      audioClip.stop();
      audioClip.close();
      audioClip = null;
      
      // Reset all playback state
      isPaused = false;
      pausePosition = 0;
      currentSong = null;
    }
  }

  // ========== LIBRARY LOADING METHODS ==========
  /**
   * Reads the audio library from JSON file
   * First attempts to load from classpath resources, then from file system
   * @return - Array of Song objects from the library
   */
  public static Song[] readAudioLibrary() {
    Song[] library = null;
    
    // Try to load from classpath resources (packaged with application)
    try (InputStream stream = SpotifyLikeAppExampleCode.class.getResourceAsStream("/com/example/audio-library.json")) {
      if (stream != null) {
        library = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Song[].class);
      } else {
        // If not in classpath, try to load from file system
        library = readAudioLibraryFromFile();
      }
    } catch (IOException | JsonSyntaxException e) {
      System.err.println("ERROR: unable to read the audio library: " + e.getMessage());
    }
    
    return library;
  }

  /**
   * Reads the audio library from the file system
   * Searches in common project directories
   * @return - Array of Song objects, or null if file not found
   */
  private static Song[] readAudioLibraryFromFile() {
    // Try multiple possible file paths
    File file = new File("demo/src/main/java/com/example/audio-library.json");
    if (!file.isFile()) {
      file = new File("src/main/java/com/example/audio-library.json");
    }
    
    // If file not found in expected paths, report error
    if (!file.isFile()) {
      System.err.println("ERROR: audio-library.json not found in expected paths.");
      return null;
    }

    // Parse JSON file and convert to Song array
    try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
      return new Gson().fromJson(reader, Song[].class);
    } catch (IOException | JsonSyntaxException e) {
      System.err.println("ERROR: unable to read the audio library from file: " + e.getMessage());
      return null;
    }
  }

  // ========== AUDIO FILE RESOURCE RESOLUTION ==========
  /**
   * Locates audio file by name from classpath resources or file system
   * Searches in the wav/ directory
   * @param fileName - Name of the audio file
   * @return - URL to the audio file, or null if not found
   */
  private static URL getAudioResource(String fileName) {
    // Try to load from classpath resources first
    String resourcePath = "/com/example/wav/" + fileName;
    URL resource = SpotifyLikeAppExampleCode.class.getResource(resourcePath);
    if (resource != null) {
      return resource;
    }

    // Try to load from file system in common project directories
    // Try to load from file system in common project directories
    File file = new File("demo/src/main/java/com/example/wav", fileName);
    if (!file.isFile()) {
      file = new File("src/main/java/com/example/wav", fileName);
    }
    
    // Convert file path to URL if file exists
    try {
      return file.isFile() ? file.toURI().toURL() : null;
    } catch (MalformedURLException e) {
      return null;
    }
  }

  // ========== UI UTILITY METHODS ==========
  /**
   * Displays an informational message dialog to the user
   * @param message - The message content to display
   * @param title - The dialog window title
   * @param parent - The parent component (for dialog positioning)
   */
  private static void showMessage(String message, String title, Component parent) {
    JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
  }

  // ========== FAVORITES MANAGEMENT METHODS ==========
  /**
   * Creates a unique identifier for a song (name + artist)
   * Used as the key for storing/tracking favorites
   * @param song - The song to create an ID for
   * @return - String combining song name and artist
   */
  private static String getSongId(Song song) {
    return song.name() + " | " + song.artist();
  }

  /**
   * Toggles a song's favorite status
   * Adds to favorites if not present, removes if already favorited
   * Saves changes to persistent storage
   * @param song - The song to toggle
   * @param parent - Parent component for dialog display
   */
  private static void toggleFavorite(Song song, Component parent) {
    String songId = getSongId(song);
    
    if (favoriteSongs.contains(songId)) {
      // Remove from favorites
      favoriteSongs.remove(songId);
      showMessage("Removed from favorites: " + formatSongInfo(song), "Favorite Removed", parent);
    } else {
      // Add to favorites
      favoriteSongs.add(songId);
      showMessage("Added to favorites: " + formatSongInfo(song), "Favorite Added", parent);
    }
    
    // Save updated favorites to persistent storage
    saveFavorites();
  }

  /**
   * Opens a new window displaying all favorite songs
   * Allows playback of favorites from a separate view
   * @param library - The complete song library
   */
  private static void showFavoritesWindow(Song[] library) {
    // Create a new window for favorites
    JFrame favFrame = new JFrame("Favorite Songs");
    favFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    favFrame.setLayout(new BorderLayout(10, 10));
    favFrame.setSize(new Dimension(600, 400));
    
    // Create list model for favorite songs
    DefaultListModel<Song> favListModel = new DefaultListModel<>();
    
    // Add all songs that are in favorites
    for (Song song : library) {
      if (favoriteSongs.contains(getSongId(song))) {
        favListModel.addElement(song);
      }
    }
    
    // Check if there are any favorites
    if (favListModel.isEmpty()) {
      showMessage("You don't have any favorite songs yet.", "No Favorites", favFrame);
      favFrame.dispose();
      return;
    }
    
    // Create and configure the favorites list
    JList<Song> favList = new JList<>(favListModel);
    favList.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Song song) {
          setText(song.name() + " — " + song.artist());
        }
        return this;
      }
    });
    
    // Select first favorite by default
    if (!favListModel.isEmpty()) {
      favList.setSelectedIndex(0);
    }
    
    // Add scrollable container for favorites list
    JScrollPane favScroll = new JScrollPane(favList);
    favScroll.setBorder(javax.swing.BorderFactory.createTitledBorder("My Favorites"));
    
    // Create control buttons for favorites window
    JButton playButton = new JButton("Play");
    JButton removeButton = new JButton("Remove from Favorites");
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
    buttonPanel.add(playButton);
    buttonPanel.add(removeButton);
    
    // Play button - plays selected favorite song
    playButton.addActionListener(e -> {
      Song selected = favList.getSelectedValue();
      if (selected == null) {
        showMessage("Please select a song to play.", "No Selection", favFrame);
        return;
      }
      play(selected);
    });
    
    // Remove button - removes selected song from favorites
    removeButton.addActionListener(e -> {
      Song selected = favList.getSelectedValue();
      if (selected == null) {
        showMessage("Please select a song to remove.", "No Selection", favFrame);
        return;
      }
      
      String songId = getSongId(selected);
      favoriteSongs.remove(songId);
      favListModel.removeElement(selected);
      saveFavorites();
      showMessage("Removed: " + formatSongInfo(selected), "Removed from Favorites", favFrame);
      
      // Close window if no more favorites
      if (favListModel.isEmpty()) {
        showMessage("You don't have any favorite songs left.", "No Favorites", favFrame);
        favFrame.dispose();
      }
    });
    
    // Assemble the favorites window
    favFrame.add(favScroll, BorderLayout.CENTER);
    favFrame.add(buttonPanel, BorderLayout.SOUTH);
    
    // Display the favorites window
    favFrame.setLocationRelativeTo(null);
    favFrame.setVisible(true);
  }

  /**
   * Loads favorite songs from persistent storage (audio-favorites.json)
   * Called at application startup
   */
  private static void loadFavorites() {
    File file = new File("demo/src/main/java/com/example/audio-favorites.json");
    if (!file.isFile()) {
      file = new File("src/main/java/com/example/audio-favorites.json");
    }
    
    // If file doesn't exist, start with empty favorites
    if (!file.isFile()) {
      return;
    }
    
    // Try to load favorites from JSON file
    try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
      Song[] favoriteSongArray = new Gson().fromJson(reader, Song[].class);
      if (favoriteSongArray != null) {
        for (Song song : favoriteSongArray) {
          favoriteSongs.add(getSongId(song));
        }
      }
    } catch (IOException | JsonSyntaxException e) {
      System.err.println("WARNING: Unable to load favorites: " + e.getMessage());
    }
  }

  /**
   * Saves favorite songs to persistent storage (audio-favorites.json)
   * Called whenever favorites are modified
   */
  private static void saveFavorites() {
    // Determine the target file path
    File file = new File("demo/src/main/java/com/example/audio-favorites.json");
    if (!file.isFile()) {
      file = new File("src/main/java/com/example/audio-favorites.json");
    }
    
    // If directory doesn't exist, try to create it
    file.getParentFile().mkdirs();
    
    // Convert favorites set to a JSON array format
    // We'll store an empty array for now (app tracks by Song IDs)
    try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
      writer.write(new Gson().toJson(favoriteSongs.toArray(String[]::new)));
    } catch (IOException e) {
      System.err.println("ERROR: Unable to save favorites: " + e.getMessage());
    }
  }

    public static Set<String> getFavoriteSongs() {
        return favoriteSongs;
    }

    public static void setFavoriteSongs(Set<String> favoriteSongs) {
        SpotifyLikeAppExampleCode.favoriteSongs = favoriteSongs;
    }
}

  