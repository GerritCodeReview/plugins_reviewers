package com.googlesource.gerrit.plugins.reviewers.client;

import com.google.gerrit.client.rpc.Natives;
import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import java.util.HashSet;
import java.util.Set;

public class AddReviewersScreen extends HorizontalPanel {
  private static final String REMOVE_BUTTON_IMG =
      "plugins/reviewers/static/remove_reviewer.png";
  static class Factory implements Screen.EntryPoint {
    @Override
    public void onLoad(Screen screen) {
      screen.setPageTitle("Reviewers");
      screen.show(new AddReviewersScreen(screen.getToken()));
    }
  }

  private String projectName;
  private Panel entriesPanel;
  private TextBox filterBox;
  private TextBox reviewerBox;
  private Set<ReviewerEntry> rEntries;

  AddReviewersScreen(String projectName) {
    this.projectName = projectName;
    this.rEntries = new HashSet<>();
    this.entriesPanel = new VerticalPanel();
    this.filterBox = new TextBox();
    this.reviewerBox = new TextBox();
    onInitUI();

    new RestApi("projects").id(this.projectName).view("reviewers").get(
        new AsyncCallback<JsArray<ReviewerFilterSection>>() {

      @Override
      public void onSuccess(JsArray<ReviewerFilterSection> result) {
        display(result);
      }

      @Override
      public void onFailure(Throwable caught) {
      }
    });
  }

  void onInitUI() {
    this.setStyleName("reviewers-panel");
    this.filterBox.getElement().setPropertyString("placeholder", "filter");
    this.reviewerBox.getElement().setPropertyString("placeholder", "reviewer");
  }

  void reset() {
    this.clear();
    this.rEntries = new HashSet<>();
    this.entriesPanel.clear();
    resetInputBoxes();
  }

  void resetInputBoxes() {
    this.filterBox.setText("");
    this.reviewerBox.setText("");
  }

  void display(JsArray<ReviewerFilterSection> sections) {
    for (ReviewerFilterSection section : Natives.asList(sections)) {
      Label filter = new Label(section.filter());
      filter.addStyleName("reviewers-filterLabel");
      entriesPanel.add(filter);
      for (String reviewer : Natives.asList(section.reviewers())) {
        ReviewerEntry rEntry = new ReviewerEntry(section.filter(), reviewer);
        rEntries.add(rEntry);
        entriesPanel.add(createOneEntry(rEntry));
      }
    }
    add(entriesPanel);
    add(createInputPanel());
  }

  Panel createOneEntry(final ReviewerEntry e) {
    Label l = new Label(e.reviewer);
    l.setStyleName("reviewers-reviewerLabel");

    Image img = new Image(REMOVE_BUTTON_IMG);
    Button removeButton = Button.wrap(img.getElement());
    removeButton.setStyleName("reviewers-removeButton");
    removeButton.setTitle("remove reviewer");
    removeButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(final ClickEvent event) {
        doSave(Action.REMOVE, e);
      }
    });

    HorizontalPanel p = new HorizontalPanel();
    p.add(l);
    p.add(removeButton);
    return p;
  }

  Panel createInputPanel(){
    Panel p = new VerticalPanel();
    p.setStyleName("reviewers-inputPanel");
    p.add(this.filterBox);
    p.add(this.reviewerBox);
    Button addButton = new Button("Add");
    addButton.setStyleName("reviewers-addButton");
    addButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(final ClickEvent event) {
        ReviewerEntry e = new ReviewerEntry(filterBox.getValue(),
            reviewerBox.getValue());
        if (!rEntries.contains(e) && !e.filter.isEmpty() &&
            !e.reviewer.isEmpty()) {
          doSave(Action.ADD, e);
        }
        resetInputBoxes();
      }
    });
    addButton.setEnabled(true);
    p.add(addButton);
    return p;
  }

  void doSave(Action action, ReviewerEntry entry) {
    Input in = Input.create();
    in.setAction(action);
    in.setFilter(entry.filter);
    in.setReviewer(entry.reviewer);

    reset();
    new RestApi("projects").id(this.projectName).view("reviewers").put(
        in, new AsyncCallback<JsArray<ReviewerFilterSection>>() {

      @Override
      public void onSuccess(JsArray<ReviewerFilterSection> result) {
        display(result);
      }

      @Override
      public void onFailure(Throwable caught) {
      }
    });
  }

  static class ReviewerEntry {
    private String filter;
    private String reviewer;

    ReviewerEntry(String filter, String reviewer) {
      this.filter = filter;
      this.reviewer = reviewer;
    }

    @Override
    public int hashCode() {
      return 31 * filter.hashCode() * reviewer.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || !(o instanceof ReviewerEntry)) {
        return false;
      }
      ReviewerEntry other = (ReviewerEntry) o;
      if (this.filter != other.filter || this.reviewer != other.reviewer) {
        return false;
      }
      return true;
    }
  }
}
