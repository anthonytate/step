package com.google.sps.data;

/** Holds whether or not the user is logged in and the link to log in/out. */
public class LogInStatus {
  private boolean loggedIn;
  private String link;

  public LogInStatus(boolean loggedIn, String link) {
    this.loggedIn = loggedIn;
    this.link = link;
  }
}