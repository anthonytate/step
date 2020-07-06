package com.google.sps.data;

public class Comment {
  private String content;
  private String userEmail;
  private long id;

  public Comment(String content, String userEmail, long id) {
    this.content = content;
    this.userEmail = userEmail;
    this.id = id;
  }
}