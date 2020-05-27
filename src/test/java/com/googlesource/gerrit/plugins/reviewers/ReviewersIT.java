// Copyright (C) 2018 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.extensions.client.ReviewerState.CC;
import static com.google.gerrit.extensions.client.ReviewerState.REVIEWER;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.ImmutableSet;
import com.google.gerrit.acceptance.NoHttpd;
import com.google.gerrit.acceptance.PushOneCommit;
import com.google.gerrit.acceptance.TestAccount;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.extensions.api.changes.AddReviewerInput;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.client.ChangeStatus;
import com.google.gerrit.extensions.client.ReviewerState;
import com.google.gerrit.extensions.common.ChangeInfo;
import java.util.Set;
import org.junit.Test;

@NoHttpd
@TestPlugin(name = "reviewers", sysModule = "com.googlesource.gerrit.plugins.reviewers.TestModule")
public class ReviewersIT extends AbstractReviewersPluginTest {

  @Test
  public void addReviewers() throws Exception {
    TestAccount user2 = accountCreator.user2();
    createFilters(filter("*").reviewer(user).cc(user2));
    String changeId = createChange().getChangeId();
    assertThat(reviewersFor(changeId)).containsExactlyElementsIn(ImmutableSet.of(user.id()));
    assertThat(ccsFor(changeId)).containsExactlyElementsIn(ImmutableSet.of(user2.id()));
  }

  @Test
  public void addReviewerMatchingReviewerAndCc() throws Exception {
    TestAccount user2 = accountCreator.user2();
    createFilters(filter("*").cc(user).cc(user2), filter("path:^a.txt").reviewer(user2));
    String changeId = createChange().getChangeId();
    assertThat(reviewersFor(changeId)).containsExactlyElementsIn(ImmutableSet.of(user2.id()));
    assertThat(ccsFor(changeId)).containsExactlyElementsIn(ImmutableSet.of(user.id()));
  }

  @Test
  public void addReviewersMatchMultipleSections() throws Exception {
    TestAccount user2 = accountCreator.user2();
    createFilters(filter("*").reviewer(user), filter("path:^a.txt").reviewer(user2));
    String changeId = createChange().getChangeId();
    assertThat(reviewersFor(changeId))
        .containsExactlyElementsIn(ImmutableSet.of(user.id(), user2.id()));
  }

  @Test
  public void doNotAddReviewersOrCcFromNonMatchingFilters() throws Exception {
    TestAccount user2 = accountCreator.user2();
    createFilters(filter("branch:master").reviewer(user).cc(user2));
    createBranch(BranchNameKey.create(project, "other-branch"));
    // Create a change that matches the filter section.
    createChange("refs/for/master");
    // The actual change we want to test
    String changeId = createChange("refs/for/other-branch").getChangeId();
    assertNoReviewersAddedFor(changeId);
  }

  @Test
  public void addReviewersFromMatchingFilters() throws Exception {
    createFilters(filter("branch:other-branch").reviewer(user));
    // Create a change that doesn't match the filter section.
    createChange("refs/for/master");
    // The actual change we want to test
    createBranch(BranchNameKey.create(project, "other-branch"));
    String changeId = createChange("refs/for/other-branch").getChangeId();
    assertThat(reviewersFor(changeId)).containsExactlyElementsIn(ImmutableSet.of(user.id()));
  }

  @Test
  public void dontAddReviewersForPrivateChange() throws Exception {
    createFilters(filter("*").reviewer(user));
    PushOneCommit.Result r = createChange("refs/for/master%private");
    assertThat(r.getChange().change().isPrivate()).isTrue();
    assertNoReviewersAddedFor(r.getChangeId());
  }

  @Test
  public void privateBitFlippedAndReviewersAddedOnSubmit() throws Exception {
    createFilters(filter("*").reviewer(user));
    PushOneCommit.Result r = createChange("refs/for/master%private");
    assertThat(r.getChange().change().isPrivate()).isTrue();
    String changeId = r.getChangeId();
    assertNoReviewersAddedFor(changeId);
    // This adds admin as reviewer
    gApi.changes().id(changeId).current().review(ReviewInput.approve());
    gApi.changes().id(changeId).current().submit();
    assertThat(reviewersFor(changeId))
        .containsExactlyElementsIn(ImmutableSet.of(admin.id(), user.id()));
    ChangeInfo info = gApi.changes().id(changeId).get();
    assertThat(info.status).isEqualTo(ChangeStatus.MERGED);
    assertThat(info.isPrivate).isNull();
  }

  @Test
  public void reviewerAddedOnPrivateBitFlip() throws Exception {
    createFilters(filter("*").reviewer(user));
    PushOneCommit.Result r = createChange("refs/for/master%private");
    assertThat(r.getChange().change().isPrivate()).isTrue();
    String changeId = r.getChangeId();
    assertNoReviewersAddedFor(changeId);
    gApi.changes().id(changeId).setPrivate(false);
    ChangeInfo info = gApi.changes().id(changeId).get();
    assertThat(info.isPrivate).isNull();
    assertThat(reviewersFor(changeId)).containsExactlyElementsIn(ImmutableSet.of(user.id()));
  }

  @Test
  public void dontChangeExistingStateOfReviewer() throws Exception {
    Set<Account.Id> userSet = ImmutableSet.of(user.id());
    TestAccount user2 = accountCreator.user2();
    Set<Account.Id> user2Set = ImmutableSet.of(user2.id());

    createFilters(filter("*").reviewer(user2).cc(user));
    String changeId = createChange("refs/for/master").getChangeId();
    assertThat(ccsFor(changeId)).containsExactlyElementsIn(userSet);
    assertThat(reviewersFor(changeId)).containsExactlyElementsIn(user2Set);

    addReviewer(changeId, user, ReviewerState.REVIEWER);
    addReviewer(changeId, user2, ReviewerState.CC);
    assertThat(reviewersFor(changeId)).containsExactlyElementsIn(userSet);
    assertThat(ccsFor(changeId)).containsExactlyElementsIn(user2Set);

    amendChange(changeId);
    assertThat(reviewersFor(changeId)).containsExactlyElementsIn(userSet);
    assertThat(ccsFor(changeId)).containsExactlyElementsIn(user2Set);
  }

  @Test
  public void addAsReviewerIfConfiguredAsReviewerAndCc() throws Exception {
    createFilters(filter("*").cc(user), filter("branch:master").reviewer(user));
    String changeId = createChange("refs/for/master").getChangeId();
    assertThat(reviewersFor(changeId)).containsExactlyElementsIn(ImmutableSet.of(user.id()));
    assertThat(gApi.changes().id(changeId).get().reviewers.get(CC)).isNull();
  }

  private void addReviewer(String changeId, TestAccount user, ReviewerState state)
      throws Exception {
    AddReviewerInput input = new AddReviewerInput();
    input.reviewer = user.id().toString();
    input.state = state;
    gApi.changes().id(changeId).addReviewer(input);
  }

  private Set<Account.Id> ccsFor(String changeId) throws Exception {
    return reviewersFor(changeId, CC);
  }

  private Set<Account.Id> reviewersFor(String changeId) throws Exception {
    return reviewersFor(changeId, REVIEWER);
  }

  private Set<Account.Id> reviewersFor(String changeId, ReviewerState reviewerState)
      throws Exception {
    return gApi.changes().id(changeId).get().reviewers.get(reviewerState).stream()
        .map(a -> Account.id(a._accountId))
        .collect(toSet());
  }

  private void assertNoReviewersAddedFor(String changeId) throws Exception {
    assertThat(gApi.changes().id(changeId).get().reviewers.get(REVIEWER)).isNull();
    assertThat(gApi.changes().id(changeId).get().reviewers.get(CC)).isNull();
  }
}
