package com.googlesource.gerrit.plugins.reviewers;

public enum ReviewerType {
  REVIEWER("reviewer"),
  CC("cc");

  String name;

  ReviewerType(String name) {
    this.name = name;
  }
}
