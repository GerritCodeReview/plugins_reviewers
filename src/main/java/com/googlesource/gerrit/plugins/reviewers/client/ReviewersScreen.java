// Copyright (C) 2014 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.reviewers.client;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.gerrit.client.rpc.NativeMap;
import com.google.gerrit.client.rpc.Natives;
import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwtexpui.globalkey.client.NpTextBox;

public class ReviewersScreen extends HorizontalPanel {
  private static final String REMOVE_BUTTON_IMG = "plugins/reviewers/static/remove_reviewer.png";

  static class Factory implements Screen.EntryPoint {
    @Override
    public void onLoad(Screen screen) {
      screen.setPageTitle("Reviewers");
      screen.show(new ReviewersScreen(URL.decodeQueryString(screen.getToken(1))));
    }
  }

  private boolean isOwner;
  private String projectName;
  private Set<ReviewerEntry> rEntries;

  ReviewersScreen(final String projectName) {
    setStyleName("reviewers-panel");
    this.projectName = projectName;
    this.rEntries = new HashSet<>();

    new RestApi("access/")
        .addParameter("project", projectName)
        .get(
            new AsyncCallback<NativeMap<ProjectAccessInfo>>() {

              @Override
              public void onSuccess(NativeMap<ProjectAccessInfo> result) {
                isOwner = result.get(projectName).isOwner();
                display();
              }

              @Override
              public void onFailure(Throwable caught) {}
            });
  }

  void display() {
    new RestApi("projects")
        .id(projectName)
        .view("reviewers")
        .get(
            new AsyncCallback<JsArray<ReviewerFilterSection>>() {

              @Override
              public void onSuccess(JsArray<ReviewerFilterSection> result) {
                display(result);
              }

              @Override
              public void onFailure(Throwable caught) {}
            });
  }

  void display(JsArray<ReviewerFilterSection> sections) {
    add(createEntriesPanel(sections));
    add(createInputPanel());
  }

  Panel createEntriesPanel(JsArray<ReviewerFilterSection> sections) {
    Panel p = new VerticalPanel();
    p.setStyleName("reviewers-filter-container");
    for (ReviewerFilterSection section : Natives.asList(sections)) {

      final String filter = section.filter();
      final List<String> reviewers = Natives.asList(section.reviewers());
      final List<String> excludedPaths = Natives.asList(section.excluded());

      Panel filterPanel = new FlowPanel();
      final Label filterLabel = new Label(filter);
      filterLabel.addStyleName("reviewers-filterLabel");
      filterPanel.add(filterLabel);

      Button editBtn = new Button("Edit");
      editBtn.setStyleName("reviewers-filterEditBtn");
      editBtn.addClickHandler(
          new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
              InputElement filterInput =
                  (InputElement) Document.get().getElementById("filtersTextBox");
              InputElement reviewersInput =
                  (InputElement) Document.get().getElementById("reviewerInputField");
              InputElement excludedPathInput =
                  (InputElement) Document.get().getElementById("excludedInputField");
              InputElement originFilter =
                  (InputElement) Document.get().getElementById("originFilterInputField");

              filterInput.setValue(filter);
              originFilter.setValue(filter);
              StringBuilder reviewersBuilder = new StringBuilder();
              for (String reviewer : reviewers) {
                if (reviewersBuilder.length() != 0) {
                  reviewersBuilder.append(", ");
                }
                reviewersBuilder.append(reviewer);
              }
              reviewersInput.setValue(reviewersBuilder.toString());

              StringBuilder excludedPathsBuilder = new StringBuilder();
              for (String path : excludedPaths) {
                if (excludedPathsBuilder.length() != 0) {
                  excludedPathsBuilder.append(", ");
                }
                excludedPathsBuilder.append(path);
              }

              excludedPathInput.setValue(excludedPathsBuilder.toString());
              // Scroll to top of the page where the form is
              Window.scrollTo(0, 0);
            }
          });

      Button removeFilterBtn = new Button("Remove");
      removeFilterBtn.setStyleName("reviewers-filterRemoveBtn");
      removeFilterBtn.addClickHandler(
          new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
              ReviewerEntry entry = new ReviewerEntry(filter);
              boolean confirm = Window.confirm("Are you sure to remove that filter");
              if (confirm) {
                doSave(Action.REMOVE_FILTER, entry);
              }
            }
          });

      filterPanel.add(removeFilterBtn);
      filterPanel.add(editBtn);
      List<String> excludedPathsList = Natives.asList(section.excluded());

      Label reviewersSection = new Label("Reviewers: ");
      reviewersSection.addStyleName("reviewers-reviewers-section");
      filterPanel.add(reviewersSection);
      for (String reviewer : Natives.asList(section.reviewers())) {

        // TODO mtfk build the panel always base on the reviewer entry ) also
        // the filter label from above
        ReviewerEntry rEntry = new ReviewerEntry(section.filter(), reviewer);
        rEntries.add(rEntry);
        filterPanel.add(createOneEntry(rEntry));
      }
      if (!excludedPathsList.isEmpty()) {
        Label excludedLabel = new Label("Excluded paths: ");
        excludedLabel.addStyleName("reviewers-excluded-label");
        filterPanel.add(excludedLabel);
        for (String path : excludedPathsList) {
          Label pathLabel = new Label(path);
          pathLabel.addStyleName("reviewers-excluded-path");
          filterPanel.add(pathLabel);
        }
      }
      p.add(filterPanel);
    }
    return p;
  }

  Panel createOneEntry(final ReviewerEntry e) {
    Label reviewerName = new Label(e.reviewer);
    reviewerName.setStyleName("reviewers-reviewerLabel");

    Image img = new Image(REMOVE_BUTTON_IMG);
    Button removeButton = Button.wrap(img.getElement());
    removeButton.setStyleName("reviewers-removeButton");
    removeButton.setTitle("remove reviewer");
    removeButton.addClickHandler(
        new ClickHandler() {
          @Override
          public void onClick(final ClickEvent event) {
            boolean confirm = Window.confirm("Are you sure to remove reviewer: " + e.reviewer);
            if (confirm) {
              doSave(Action.REMOVE, e);
            }
          }
        });
    removeButton.setVisible(isOwner);

    FlowPanel reviewerElement = new FlowPanel();
    reviewerElement.setStyleName("reviewers-container");
    reviewerElement.add(reviewerName);
    reviewerElement.add(removeButton);
    return reviewerElement;
  }

  Panel createInputPanel() {
    final NpTextBox reviewerBox = new NpTextBox();
    final NpTextBox excludedBox = new NpTextBox();

    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    oracle.add("file:");
    oracle.add("owner:");
    oracle.add("branch:");
    oracle.add("ownerin:");
    oracle.add("topic:");
    oracle.add("OR");
    oracle.add("AND");
    oracle.add("NOT");

    final SuggestBox filterBox = new SuggestBox(oracle, new NpTextBox());

    filterBox.getElement().setPropertyString("placeholder", "filter");
    // TODO rename id to filterInputField
    filterBox.getElement().setId("filtersTextBox");
    reviewerBox.getElement().setPropertyString("placeholder", "reviewer");
    reviewerBox.getElement().setId("reviewerInputField");
    excludedBox.getElement().setPropertyString("placeholder", "e.g. /lib/gen-src, /gen-src");
    excludedBox.getElement().setId("excludedInputField");

    // Hidden input field used for editing purpose
    final Hidden originFilter = new Hidden();
    originFilter.setID("originFilterInputField");

    Button addButton = new Button("Add");
    addButton.setStyleName("reviewers-addButton");
    addButton.addClickHandler(
        new ClickHandler() {
          @Override
          public void onClick(final ClickEvent event) {
            /*
             * TODO(mduft) Since we override getText() method in the AutoCompleteTexBox we
             * have to get value directly from the element property This[1]
             * probably can be reused to replace AutoCompleteTextBox
             *
             * [1] https://gerrit-review.googlesource.com/#/c/72050/
             */
            String filter = filterBox.getElement().getPropertyString("value");
            String originFilterValue = originFilter.getValue();
            ReviewerEntry e =
                new ReviewerEntry(
                    filter, reviewerBox.getValue(), excludedBox.getValue(), originFilterValue);
            if (!rEntries.contains(e) && !e.filter.isEmpty() && !e.reviewer.isEmpty()) {
              if (originFilterValue.isEmpty()) {
                doSave(Action.ADD, e);
              } else {
                doSave(Action.EDIT, e);
              }
            }
          }
        });
    filterBox.setEnabled(isOwner);
    reviewerBox.setEnabled(isOwner);
    addButton.setEnabled(isOwner);

    Panel p = new VerticalPanel();
    p.setStyleName("reviewers-inputPanel");
    p.add(new Label("Filter:"));
    p.add(filterBox);
    p.add(new Label("Reviewers:"));
    p.add(reviewerBox);
    p.add(new Label("Excluded paths:"));
    p.add(excludedBox);
    p.add(originFilter);

    p.add(addButton);
    return p;
  }

  void doSave(Action action, ReviewerEntry entry) {
    ChangeReviewersInput in = ChangeReviewersInput.create();
    in.setAction(action);
    in.setFilter(entry.filter);
    in.setReviewer(entry.reviewer);
    in.setExcluded(entry.excluded);
    in.setOrigin(entry.originFilter);

    sendRequest(in);
  }

  void removeFilter(String filter) {
    ChangeReviewersInput in = ChangeReviewersInput.create();
    in.setAction(Action.REMOVE_FILTER);
    in.setFilter(filter);

    sendRequest(in);
  }

  private void sendRequest(ChangeReviewersInput in) {
    new RestApi("projects")
        .id(projectName)
        .view("reviewers")
        .put(
            in,
            new AsyncCallback<JsArray<ReviewerFilterSection>>() {

              @Override
              public void onSuccess(JsArray<ReviewerFilterSection> result) {
                reset();
                display(result);
              }

              @Override
              public void onFailure(Throwable caught) {}
            });
  }

  void reset() {
    clear();
    rEntries = new HashSet<>();
  }

  static class ReviewerEntry {
    private final String filter;
    private final String reviewer;
    private final String excluded;
    private final String originFilter;

    // TODO find out if we need that
    // Used for removing filter
    ReviewerEntry(String filter) {
      this(filter, "", "", "");
    }

    ReviewerEntry(String filter, String reviewer, String excluded) {
      this(filter, reviewer, excluded, "");
    }

    ReviewerEntry(String filter, String reviewer) {
      this(filter, reviewer, "", "");
    }

    ReviewerEntry(String filter, String reviewer, String excluded, String originFilterValue) {
      this.filter = filter;
      this.reviewer = reviewer;
      this.excluded = excluded;
      this.originFilter = originFilterValue;
    }

    @Override
    public int hashCode() {
      return Objects.hash(filter, reviewer, excluded);
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || !(o instanceof ReviewerEntry)) {
        return false;
      }
      ReviewerEntry other = (ReviewerEntry) o;
      if (!this.filter.equals(other.filter) || !this.reviewer.equals(other.reviewer)) {
        return false;
      }
      return true;
    }
  }
}
