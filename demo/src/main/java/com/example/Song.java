package com.example;

public class Song {

  private String name;
  private String artist;
  private String fileName;
  private Integer year;
  private String genre;

  @Override
  public String toString() {
    return name + " — " + artist;
  }

  public String name() {
    return this.name;
  }

  public String artist() {
    return this.artist;
  }

  public String fileName() {
    return this.fileName;
  }

  public Integer year() {
    return this.year;
  }

  public String genre() {
    return this.genre;
  }
}
