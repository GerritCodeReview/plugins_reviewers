package com.googlesource.gerrit.plugins.reviewers.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class ReviewerFilterSection extends JavaScriptObject {
  public final native String filter() /*-{ return this.filter; }-*/;
  public final native JsArrayString reviewers() /*-{ return this.reviewers; }-*/;

  protected ReviewerFilterSection() {
  }
}
