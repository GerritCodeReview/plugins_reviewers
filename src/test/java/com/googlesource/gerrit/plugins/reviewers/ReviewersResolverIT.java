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

import com.google.common.collect.ImmutableSet;
import com.google.gerrit.acceptance.AbstractDaemonTest;
import com.google.gerrit.acceptance.NoHttpd;
import com.google.gerrit.acceptance.TestAccount;
import com.google.gerrit.acceptance.testsuite.group.GroupOperations;
import com.google.gerrit.reviewdb.client.Account;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

@NoHttpd
public class ReviewersResolverIT extends AbstractDaemonTest {

  @Inject private GroupOperations groupOperations;
  @Inject private ReviewersResolver resolver;
  private int change;

  @Before
  public void setUp() {
    change = 1;
  }

  @Test
  public void testUploaderSkippedAsReviewer() throws Exception {
    Set<Account.Id> reviewers =
        resolver.resolve(
            Collections.singleton(user.email()),
            project,
            change,
            gApi.accounts().id(user.id().get()).get());
    assertThat(reviewers).isEmpty();
  }

  @Test
  public void testAccountResolve() throws Exception {
    Set<Account.Id> reviewers =
        resolver.resolve(
            ImmutableSet.of(user.email(), admin.email()),
            project,
            change,
            gApi.accounts().id(admin.id().get()).get());
    assertThat(reviewers).containsExactly(user.id());
  }

  @Test
  public void testAccountGroupResolve() throws Exception {
    String group1 = "group1";
    groupOperations.newGroup().name(group1).create();
    TestAccount foo = createTestAccount("foo", group1);
    TestAccount bar = createTestAccount("bar", group1);

    String group2 = "group2";
    groupOperations.newGroup().name(group2).create();
    TestAccount baz = createTestAccount("baz", group2);
    TestAccount qux = createTestAccount("qux", group2);

    TestAccount system = createTestAccount("system", "Administrators");

    Set<Account.Id> reviewers =
        resolver.resolve(
            ImmutableSet.of(system.email(), group1, group2),
            project,
            change,
            gApi.accounts().id(admin.id().get()).get());
    assertThat(reviewers).containsExactly(system.id(), foo.id(), bar.id(), baz.id(), qux.id());
  }

  private TestAccount createTestAccount(String name, String group) throws Exception {
    name = name(name);
    return accountCreator.create(name, name + "@example.com", name + " full name", group);
  }
}
