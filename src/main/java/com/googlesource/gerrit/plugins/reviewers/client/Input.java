package com.googlesource.gerrit.plugins.reviewers.client;

import com.google.gwt.core.client.JavaScriptObject;

public class Input extends JavaScriptObject {
  public static Input create() {
    return (Input) createObject();
  }

  protected Input() {
  }

  final void setAction(Action a) {
    setActionRaw(a.name());
  }

  private final native void setActionRaw(String a)
  /*-{ if(a)this.action=a; }-*/;

  final native void setFilter(String f)
  /*-{ if(f)this.filter=f; }-*/;

  final native void setReviewer(String r)
  /*-{ if(r)this.reviewer=r; }-*/;
}
