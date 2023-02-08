// Copyright (C) 2020 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.reviewers;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import com.google.gerrit.server.FanOutExecutor;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

interface ReviewerWorkQueue {
  void submit(AddReviewers addReviewers);

  class Scheduled implements ReviewerWorkQueue {
    private final WorkQueue workQueue;

    @Inject
    Scheduled(WorkQueue workQueue) {
      this.workQueue = workQueue;
    }

    @Override
    public void submit(AddReviewers addReviewers) {
      workQueue.getDefaultQueue().submit(addReviewers);
    }
  }

  class Direct implements ReviewerWorkQueue {
    @Override
    public void submit(AddReviewers addReviewers) {
      directExecutor().execute(addReviewers);
    }
  }

  class ScheduledFanOut implements ReviewerWorkQueue {
    private final ExecutorService executor;

    @Inject
    ScheduledFanOut(@FanOutExecutor ExecutorService executor) {
      this.executor = executor;
    }

    @Override
    public void submit(AddReviewers addReviewers) {
      @SuppressWarnings("unused")
      Future<?> ignored = executor.submit(addReviewers);
    }
  }
}
