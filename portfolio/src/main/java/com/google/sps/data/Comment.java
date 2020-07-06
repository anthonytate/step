package com.google.sps.data;

public class Comment {
  private long id;
  private String userEmail;
  private String content;

  public Comment(long id, String userEmail, String content) {
    this.id = id;
    this.userEmail = userEmail;
    this.content = content;
  }
}