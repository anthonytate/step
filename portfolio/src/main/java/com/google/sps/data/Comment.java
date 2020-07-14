package com.google.sps.data;

public class Comment {
  private long id;
  private String userEmail;
  private String content;
  private String imgUrl;

  public Comment(long id, String userEmail, String content, String imgUrl) {
    this.id = id;
    this.userEmail = userEmail;
    this.content = content;
    this.imgUrl = imgUrl;
  }
}