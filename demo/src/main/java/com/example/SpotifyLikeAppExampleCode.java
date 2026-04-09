package com.example;

import java.io.File;
import java.io.FileReader;
import java.util.Scanner;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

// declares a class for the app
public class SpotifyLikeAppExampleCode {

  // the current audio clip
  private static Clip audioClip;

  // track if audio is paused
  private static boolean isPaused = false;

  /*replaced path
    */

  private static final String DIRECTORY_PATH =
    "C:\\Users\\maria\\OneDrive\\Documents\\GitHub\\spotify-like-app1\\demo\\src\\main\\java\\com\\example";

  // "main" makes this class a java app that can be executed
  public static void main(final String[] args) {
    // reading audio library from json file
    Song[] library = readAudioLibrary();

    // create a scanner for user input
    try (Scanner input = new Scanner(System.in)) {
      String userInput = "";
      while (!userInput.equals("q")) {
        menu();

        // get input
        userInput = input.nextLine();

        // accept upper or lower case commands
        userInput = userInput.toLowerCase();

        // do something
        handleMenu(userInput, library);
      }
    }
  }

  /*
   * displays the menu for the app
   */
  public static void menu() {
    System.out.println("---- SpotifyLikeApp ----");
    System.out.println("[H]ome");
    System.out.println("[S]earch by title");
    System.out.println("[L]ibrary");
    System.out.println("[P]lay");
    System.out.println("[Q]uit");
    System.out.println("[T]pause");

    System.out.println("");
    System.out.print("Enter q to Quit:");
  }

  /*
   * handles the user input for the app
   */
  public static void handleMenu(String userInput, Song[] library) {
    switch (userInput) {
      case "h" -> System.out.println("-->Home<--");
      case "s" -> System.out.println("-->Search by title<--");
      case "l" -> System.out.println("-->Library<--");
      case "p" -> play(library);
      case "q" -> System.out.println("-->Quit<--");
      case "t" -> togglePause();
      default -> {}
    }
  }

  /*
   * toggles pause/resume for the audio
   */
  public static void togglePause() {
    if (audioClip != null) {
      if (isPaused) {
        audioClip.start();
        isPaused = false;
        System.out.println("-->Resumed<--");
      } else {
        audioClip.stop();
        isPaused = true;
        System.out.println("-->Paused<--");
      }
    } else {
      System.out.println("No audio is playing.");
    }
  }

  /*
   * plays an audio file
   */
  public static void play(Song[] library) {
    // open the audio file

    // get the filePath and open a audio file
    final Integer i = 3;
    final String filename = library[i].fileName();
    final String filePath = DIRECTORY_PATH + "/wav/" + filename;
    final File file = new File(filePath);

    // stop the current song from playing, before playing the next one
    if (audioClip != null) {
      audioClip.close();
    }

    try {
      // create clip
      audioClip = AudioSystem.getClip();

      // get input stream
      final AudioInputStream in = AudioSystem.getAudioInputStream(file);

      audioClip.open(in);
      audioClip.setMicrosecondPosition(0);
      audioClip.loop(Clip.LOOP_CONTINUOUSLY);
      isPaused = false;
    } catch (javax.sound.sampled.UnsupportedAudioFileException | 
             java.io.IOException | 
             javax.sound.sampled.LineUnavailableException e) {
      System.err.println("ERROR: Failed to play audio file: " + e.getMessage());
    }
  }

  // read the audio library of music
  public static Song[] readAudioLibrary() {
    // get the file path
    final String jsonFileName = "audio-library.json";
    final String filePath = DIRECTORY_PATH + "/" + jsonFileName;

    Song[] library = null;
    try {
      System.out.println("Reading the file " + filePath);
      JsonReader reader = new JsonReader(new FileReader(filePath));
      library = new Gson().fromJson(reader, Song[].class);
    } catch (java.io.IOException | com.google.gson.JsonSyntaxException e) {
      System.out.printf("ERROR: unable to read the file %s\n", filePath);
      System.out.println();
    }

    return library;
  }

  public static void playMP3(String path) {
    try {
      ProcessBuilder builder = new ProcessBuilder();
      builder.command(new String[] { "cmd", "/c", "start", "", path });
      builder.start();
    } catch (java.io.IOException e) {
      System.out.println("ERROR: Failed to play MP3 file: " + e.getMessage());
    }
  }
}
