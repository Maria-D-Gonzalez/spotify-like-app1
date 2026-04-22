package com.example;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class SpotifyLikeAppExampleCode {

  private static Clip audioClip;
  private static boolean isPaused = false;
  private static long pausePosition = 0;
  private static Song currentSong;
    private static Song[] favorites;
  /*UI */
  public static void main1(final String[] args) {
    Song[] library = readAudioLibrary();
    if (library == null || library.length == 0) {
      System.err.println("Could not load the audio library. Check audio-library.json and the classpath.");
      return;
    }

    SwingUtilities.invokeLater(() -> createAndShowGui(library));
  }
  @SuppressWarnings("Convert2Lambda")
  private static void createAndShowGui(final Song[] library) {
      /*changed the title of app */
    JFrame frame = new JFrame("Music Player");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout(10, 10));
    frame.setMinimumSize(new Dimension(720, 520));

    DefaultListModel<Song> listModel = new DefaultListModel<>();
    Arrays.stream(library).forEach(listModel::addElement);

    JList<Song> songList = new JList<>(listModel);
    songList.setSelectionMode(JList.WHEN_IN_FOCUSED_WINDOW);
    songList.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Song song) {
          setText(song.name() + " — " + song.artist());
        }
        return this;
      }
    });
    if (!listModel.isEmpty()) {
      songList.setSelectedIndex(0);
    }

    JScrollPane listScroll = new JScrollPane(songList);
    listScroll.setBorder(javax.swing.BorderFactory.createTitledBorder("Library"));
    listScroll.setPreferredSize(new Dimension(700, 320));

    JLabel infoLabel = new JLabel("Select a track and press Play.");
    infoLabel.setBorder(new EmptyBorder(8, 8, 8, 8));

    JTextField searchField = new JTextField(24);
    JButton searchButton = new JButton("Search");
    JButton clearButton = new JButton("Clear");
    JButton FavoriteButton = new JButton("Favorites");
    JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
    searchPanel.add(new JLabel("Search:"));
    searchPanel.add(searchField);
    searchPanel.add(searchButton);
    searchPanel.add(FavoriteButton);
    searchPanel.add(clearButton);

    JButton playButton = new JButton("Play");
    JButton pauseButton = new JButton("Pause");
    JButton stopButton = new JButton("Stop");
    JButton favoriteButton = new JButton("Favorites");
    JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
    controlPanel.add(playButton);
    controlPanel.add(pauseButton);
    controlPanel.add(stopButton);
    controlPanel.add(favoriteButton);

    JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));
    bottomPanel.add(searchPanel, BorderLayout.NORTH);
    bottomPanel.add(controlPanel, BorderLayout.CENTER);
    bottomPanel.add(controlPanel, BorderLayout.EAST);
    bottomPanel.add(infoLabel, BorderLayout.SOUTH);

    frame.add(listScroll, BorderLayout.CENTER);
    frame.add(bottomPanel, BorderLayout.SOUTH);

    searchButton.addActionListener(e -> filterLibrary(searchField.getText(), listModel, library));
    clearButton.addActionListener(e -> {
      searchField.setText("");
      filterLibrary("", listModel, library);
    });
    favoriteButton.addActionListener(e -> {
      Song selected = songList.getSelectedValue();
      if (selected == null) {
        showMessage("Please select a track to favorite.", "No Selection", frame);
        return;
      }
      showMessage("Added to favorites: " + formatSongInfo(selected), "Favorite Added", frame);
    });
    
    songList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          Song selected = songList.getSelectedValue();
          infoLabel.setText(selected != null ? formatSongInfo(selected) : "Select a track and press Play.");
        }
      }
    });

    playButton.addActionListener(e -> {
      Song selected = songList.getSelectedValue();
      if (selected == null) {
        showMessage("Please select a track first.", "No Selection", frame);
        return;
      }
      play(selected);
      infoLabel.setText("Playing: " + formatSongInfo(selected));
    });

    pauseButton.addActionListener(e -> {
      if (audioClip == null) {
        showMessage("No audio is playing.", "Information", frame);
        return;
      }
      togglePause();
      infoLabel.setText(isPaused ? "Paused" : "Playing: " + formatSongInfo(currentSong));
    });

    stopButton.addActionListener(e -> {
      stopAudio();
      infoLabel.setText("Stopped.");
    });

    searchField.addActionListener(e -> searchButton.doClick());

    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static void filterLibrary(String query, DefaultListModel<Song> model, Song[] library) {
    model.clear();
    String lowerQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    for (Song song : library) {
      boolean matches = lowerQuery.isEmpty()
        || song.name().toLowerCase(Locale.ROOT).contains(lowerQuery)
        || song.artist().toLowerCase(Locale.ROOT).contains(lowerQuery)
        || (song.genre() != null && song.genre().toLowerCase(Locale.ROOT).contains(lowerQuery));
      if (matches) {
        model.addElement(song);
      }
    }
  }

  private static String formatSongInfo(Song song) {
    if (song == null) {
      return "No track selected.";
    }
    StringBuilder builder = new StringBuilder();
    builder.append(song.name()).append(" — ").append(song.artist());
    if (song.year() != null) {
      builder.append(" (").append(song.year()).append(")");
    }
    if (song.genre() != null && !song.genre().isBlank()) {
      builder.append(" [").append(song.genre()).append("]");
    }
    return builder.toString();
  }

  private static void play(Song song) {
    URL audioUrl = getAudioResource(song.fileName());
    if (audioUrl == null) {
      System.err.println("Unable to locate audio file: " + song.fileName());
      return;
    }

    if (audioClip != null) {
      audioClip.stop();
      audioClip.close();
    }

    try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioUrl)) {
      audioClip = AudioSystem.getClip();
      audioClip.open(audioStream);
      audioClip.setMicrosecondPosition(0);
      audioClip.loop(Clip.LOOP_CONTINUOUSLY);
      isPaused = false;
      pausePosition = 0;
      currentSong = song;
    } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
      System.err.println("ERROR: Failed to play audio file: " + e.getMessage());
    }
  }

  public static void togglePause() {
    if (audioClip == null) {
      return;
    }
    if (isPaused) {
      audioClip.setMicrosecondPosition(pausePosition);
      audioClip.start();
      isPaused = false;
    } else {
      pausePosition = audioClip.getMicrosecondPosition();
      audioClip.stop();
      isPaused = true;
    }
  }

  public static void stopAudio() {
    if (audioClip != null) {
      audioClip.stop();
      audioClip.close();
      audioClip = null;
      isPaused = false;
      pausePosition = 0;
      currentSong = null;
    }
  }

  public static Song[] readAudioLibrary() {
    Song[] library = null;
    try (InputStream stream = SpotifyLikeAppExampleCode.class.getResourceAsStream("/com/example/audio-library.json")) {
      if (stream != null) {
        library = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Song[].class);
      } else {
        library = readAudioLibraryFromFile();
      }
    } catch (IOException | JsonSyntaxException e) {
      System.err.println("ERROR: unable to read the audio library: " + e.getMessage());
    }
    return library;
  }

  private static Song[] readAudioLibraryFromFile() {
    File file = new File("demo/src/main/java/com/example/audio-library.json");
    if (!file.isFile()) {
      file = new File("src/main/java/com/example/audio-library.json");
    }
    if (!file.isFile()) {
      System.err.println("ERROR: audio-library.json not found in expected paths.");
      return null;
    }

    try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
      return new Gson().fromJson(reader, Song[].class);
    } catch (IOException | JsonSyntaxException e) {
      System.err.println("ERROR: unable to read the audio library from file: " + e.getMessage());
      return null;
    }
  }

  private static URL getAudioResource(String fileName) {
    String resourcePath = "/com/example/wav/" + fileName;
    URL resource = SpotifyLikeAppExampleCode.class.getResource(resourcePath);
    if (resource != null) {
      return resource;
    }

    File file = new File("demo/src/main/java/com/example/wav", fileName);
    if (!file.isFile()) {
      file = new File("src/main/java/com/example/wav", fileName);
    }
    try {
      return file.isFile() ? file.toURI().toURL() : null;
    } catch (MalformedURLException e) {
      return null;
    }
  }

  private static void showMessage(String message, String title, Component parent) {
    JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
  
  }
  public static void main(final String[] args) {
    if (favorites == null || favorites.length == 0) {
    }

    SwingUtilities.invokeLater(() -> ShowGui(favorites))
    ;
  }

    private static void ShowGui(Song[] favorites) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}