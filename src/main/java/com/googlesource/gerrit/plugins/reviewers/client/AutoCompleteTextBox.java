package com.googlesource.gerrit.plugins.reviewers.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwtexpui.globalkey.client.GlobalKey;

/**
 * MultipleTextBox allow to have multiple suggestions at a result Base on
 * example from Viktor Zaprudnev
 * 
 * Usage: SuggestBox(oracle, new MultipleTextBox());
 */

public class AutoCompleteTextBox extends TextBoxBase {
  /**
   * Creates an empty multiple text box. Stop key propagation (this prevent from
   * jumping into search field in main UI
   */
  public AutoCompleteTextBox() {
    this(Document.get().createTextInputElement(), "gwt-TextBox");
    addKeyPressHandler(GlobalKey.STOP_PROPAGATION);
  }

  /**
   * This constructor may be used by subclasses to explicitly use an existing
   * element. This element must be an <input> element whose type is 'text'.
   *
   * @param element the element to be used
   */
  protected AutoCompleteTextBox(Element element) {
    super(element);
    assert InputElement.as(element).getType().equalsIgnoreCase("text");
    addKeyPressHandler(GlobalKey.STOP_PROPAGATION);
  }

  AutoCompleteTextBox(Element element, String styleName) {
    super(element);
    if (styleName != null) {
      setStyleName(styleName);
    }
  }

  @Override
  public String getText() {
    String wholeString = super.getText();
    String lastString = wholeString;
    if (wholeString != null && !wholeString.trim().equals("")) {
      int lastComma = wholeString.trim().lastIndexOf(" ");
      if (lastComma > 0) {
        lastString = wholeString.trim().substring(lastComma + 1);
      }
    }
    return lastString;
  }

  public String getFullText() {
    return super.getText();
  }

  @Override
  public void setText(String text) {
    String wholeString = super.getText();
    if (text != null && text.equals("")) {
      super.setText(text);
    } else {
      // Clean last text, to replace with new value, for example, if new
      // text is v.zaprudnevd@gmail.com:
      // "manuel@we-r-you.com, v" need to be replaced with:
      // "manuel@we-r-you.com, v.zaprudnevd@gmail.com, "

      if (wholeString != null) {
        int lastComma = wholeString.trim().lastIndexOf(" ");
        if (lastComma > 0) {
          wholeString = wholeString.trim().substring(0, lastComma);
        } else {
          wholeString = "";
        }

        if (!wholeString.trim().endsWith(" ")
            && !wholeString.trim().equals("")) {
          wholeString = wholeString + " ";
        }

        wholeString = wholeString + text;
        super.setText(wholeString);
      }
    }
  }
}
