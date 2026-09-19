package io.instanto.gwt.testing.api;

import static io.instanto.gwt.testing.api.ContractAssertions.*;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.i18n.shared.BidiFormatter;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.safehtml.client.HasSafeHtml;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.DirectionalTextHelper;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.RootPanel;

/** Selection and directional labels used by Material's original form controls. */
public final class MaterialChoiceContracts {
  private MaterialChoiceContracts() {}

  public static void listInsertionAndSelectionPreserveTextAndValues() {
    ListBox list = new ListBox(true);
    isTrue(list.isMultipleSelect(), "multiple selection constructor");
    list.addItem("Last", "last");
    list.insertItem("First", 0);
    list.insertItem("Middle", Direction.LTR, "middle", 1);
    list.insertItem("Appended", 99);
    equal(4, list.getItemCount(), "insertion count");
    equal("First", list.getItemText(0), "insert before existing option");
    equal("First", list.getValue(0), "default submitted value");
    equal("middle", list.getValue(1), "explicit submitted value");
    equal("Appended", list.getItemText(3), "out of range insertion appends");
    list.setItemSelected(0, true);
    list.setItemSelected(2, true);
    isTrue(list.isItemSelected(0) && list.isItemSelected(2), "multiple selections retained");
    list.setSelectedIndex(1);
    equal("Middle", list.getSelectedItemText(), "selected text");
    equal("middle", list.getSelectedValue(), "selected value");
    list.setItemText(1, "Changed");
    equal("Changed", list.getSelectedItemText(), "text replacement");
    equal("middle", list.getSelectedValue(), "text replacement preserves value");
    list.removeItem(0);
    equal("Changed", list.getItemText(0), "removal shifts indexes");
    list.clear();
    equal(0, list.getItemCount(), "clear removes options");
    equal(null, list.getSelectedItemText(), "empty selected text");
    equal(null, list.getSelectedValue(), "empty selected value");
    rejectsIndex(() -> list.getItemText(0));
    rejectsIndex(() -> list.setItemText(0, "invalid"));
    rejectsIndex(() -> list.getValue(-1));
    rejectsIndex(() -> list.setValue(0, "invalid"));
    rejectsIndex(() -> list.isItemSelected(0));
    rejectsIndex(() -> list.setItemSelected(0, true));
    rejectsIndex(() -> list.removeItem(0));
    list.addItem("Valid");
    boolean rejected = false;
    try {
      list.setItemText(0, null);
    } catch (NullPointerException expected) {
      rejected = true;
    }
    isTrue(rejected, "null replacement text rejected");
  }

  public static void listDirectionWrappingDoesNotLeakIntoPublicText() {
    ListBox list = new ListBox();
    list.addItem("שלום", Direction.RTL, "he");
    OptionElement option = SelectElement.as(list.getElement()).getOptions().getItem(0);
    equal("\u202Bשלום\u202C", option.getText(), "RTL option wrapped in default LTR locale");
    equal("שלום", list.getItemText(0), "wrapping hidden from API");
    equal("he", list.getValue(0), "wrapping excluded from submitted value");
    list.setItemText(0, "Hello", Direction.LTR);
    equal("Hello", option.getText(), "same direction needs no wrapper");
    isTrue(!option.hasAttribute("bidiwrapped"), "obsolete wrapper marker removed");
    list.setDirectionEstimator(true);
    list.addItem("שלום");
    equal(
        "\u202Bשלום\u202C",
        SelectElement.as(list.getElement()).getOptions().getItem(1).getText(),
        "estimated RTL");
    list.setDirectionEstimator(false);
    equal("שלום", list.getItemText(1), "estimator change preserves existing options");
    list.setItemText(1, "שלום");
    equal(
        "שלום",
        SelectElement.as(list.getElement()).getOptions().getItem(1).getText(),
        "disabled estimator removes wrapper on next update");
    equal(
        "\u202AHello\u202C",
        BidiFormatter.getInstance(Direction.RTL)
            .unicodeWrapWithKnownDir(Direction.LTR, "Hello", false, false),
        "explicit RTL context supports LTR embedding");
  }

  public static void directionalLabelsRestoreContextAndEscapePlainText() {
    Element element = Document.get().createSpanElement();
    element.setAttribute("dir", "ltr");
    DirectionalTextHelper helper = new DirectionalTextHelper(element, true);
    helper.setText("שלום <b>", Direction.RTL);
    equal("שלום <b>", helper.getText(), "plain text stays literal");
    isTrue(helper.getHtml().contains("&lt;b&gt;"), "plain markup escaped");
    equal(
        "rtl", element.getFirstChildElement().getAttribute("dir"), "nested span carries direction");
    equal("ltr", element.getAttribute("dir"), "inline context retained");
    helper.setDirectionEstimator(true);
    equal(Direction.RTL, helper.getTextDirection(), "explicit direction survives estimator change");
    helper.setText("Hello");
    equal(Direction.LTR, helper.getTextDirection(), "next implicit content uses estimator");
    helper.setHtml(SafeHtmlUtils.fromSafeConstant("<b>שלום</b>"));
    equal("<b>שלום</b>", helper.getHtml(), "safe HTML excludes helper wrapper");
    equal(Direction.RTL, helper.getTextDirection(), "HTML direction ignores tags");
    helper.setDirectionEstimator(false);
    equal(
        Direction.LTR,
        helper.getTextDirection(),
        "disabling estimation restores initial direction");
    equal("b", element.getFirstChildElement().getTagName().toLowerCase(), "helper span removed");
    Element block = Document.get().createDivElement();
    DirectionalTextHelper blockHelper = new DirectionalTextHelper(block, false);
    blockHelper.setText("שלום", Direction.RTL);
    equal("rtl", block.getAttribute("dir"), "block direction belongs on block");
    equal(null, block.getFirstChildElement(), "block text needs no nested span");
  }

  public static void radioConstructorsPreserveSafeHtmlAndDirection() {
    SafeHtml markup = SafeHtmlUtils.fromSafeConstant("<b>שלום</b>");
    RadioButton plain = new RadioButton("choices", "<b>plain</b>", false);
    equal("<b>plain</b>", plain.getText(), "plain constructor escapes markup");
    equal("&lt;b&gt;plain&lt;/b&gt;", plain.getHTML(), "escaped label HTML");
    RadioButton html = new RadioButton("choices", "<b>bold</b>", true);
    equal("bold", html.getText(), "HTML constructor renders markup");
    RadioButton safe = new RadioButton("choices", markup);
    equal("<b>שלום</b>", safe.getHTML(), "safe HTML constructor");
    RadioButton directed = new RadioButton("choices", markup, Direction.RTL);
    equal(Direction.RTL, directed.getTextDirection(), "safe HTML explicit direction");
    equal("שלום", directed.getText(), "directional wrapper excluded from label");
    RadioButton estimated = new RadioButton("choices", markup, WordCountDirectionEstimator.get());
    equal(Direction.RTL, estimated.getTextDirection(), "safe HTML estimated direction");
    equal(
        Direction.RTL,
        new RadioButton("choices", "Hello", Direction.RTL).getTextDirection(),
        "explicit plain direction");
    equal(
        Direction.RTL,
        new RadioButton("choices", "שלום", WordCountDirectionEstimator.get()).getTextDirection(),
        "estimated plain direction");
    HasSafeHtml api = directed;
    api.setHTML(SafeHtmlUtils.fromSafeConstant("<i>Changed</i>"));
    equal("<i>Changed</i>", directed.getHTML(), "SafeHtml interface dispatch");
    RootPanel.get().add(plain);
    RootPanel.get().add(html);
    try {
      plain.setValue(true);
      html.setValue(true);
      isTrue(!plain.getValue() && html.getValue(), "native radio group exclusivity");
      equal("choices", html.getName(), "radio group name");
    } finally {
      plain.removeFromParent();
      html.removeFromParent();
    }
  }

  public static void buttonSafeHtmlUsesSubclassRendering() {
    com.google.gwt.user.client.ui.Button button =
        new com.google.gwt.user.client.ui.Button() {
          @Override
          public void setHTML(String html) {
            super.setHTML("prefix" + html);
          }
        };
    HasSafeHtml api = button;
    api.setHTML(SafeHtmlUtils.fromSafeConstant("<b>value</b>"));
    equal(
        "prefix<b>value</b>", button.getHTML(), "base SafeHtml overload invokes subclass renderer");
  }

  private static void rejectsIndex(Runnable operation) {
    boolean rejected = false;
    try {
      operation.run();
    } catch (IndexOutOfBoundsException expected) {
      rejected = true;
    }
    isTrue(rejected, "invalid option index rejected");
  }
}
