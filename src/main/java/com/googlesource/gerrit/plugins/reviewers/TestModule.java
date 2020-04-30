package com.googlesource.gerrit.plugins.reviewers;

public class TestModule extends Module {

  public TestModule() {
    super(true, false);
  }

  @Override
  protected void bindWorkQueue() {
    bind(ReviewerWorkQueue.class).to(ReviewerWorkQueue.Direct.class);
  }
}
