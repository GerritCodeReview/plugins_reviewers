package com.googlesource.gerrit.plugins.reviewers;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;

public interface ReviewerWorkQueue {
  public void submit(AddReviewers addReviewers);

  public static class Scheduled implements ReviewerWorkQueue {
    private final WorkQueue workQueue;

    @Inject
    Scheduled(WorkQueue workQueue) {
      this.workQueue = workQueue;
    }

    @Override
    public void submit(AddReviewers addReviewers) {
      workQueue.getDefaultQueue().submit(addReviewers);
      return;
    }
  }

  public static class Direct implements ReviewerWorkQueue {

    @Override
    public void submit(AddReviewers addReviewers) {
      directExecutor().execute(addReviewers);
      return;
    }
  }
}
