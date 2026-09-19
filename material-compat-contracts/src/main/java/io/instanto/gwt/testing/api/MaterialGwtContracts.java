package io.instanto.gwt.testing.api;

import static io.instanto.gwt.testing.api.ContractAssertions.*;

import com.google.gwt.core.client.JsDate;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.editor.client.EditorDelegate;
import com.google.gwt.editor.ui.client.adapters.ValueBoxEditor;
import com.google.gwt.event.logical.shared.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ValueBox;
import com.google.gwt.user.client.ui.Widget;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/** GWT API behavior used by Material, shared unchanged between both compilers. */
public final class MaterialGwtContracts {
  private MaterialGwtContracts() {}

  public static void typedValuesReportErrorsAndRecover() {
    NumericInput input = new NumericInput();
    ValueBoxEditor<Integer> editor = input.asEditor();
    same(editor, input.asEditor(), "cached editor");
    List<String> errors = new ArrayList<>();
    editor.setDelegate(
        new EditorDelegate<Integer>() {
          public String getPath() {
            return "quantity";
          }

          public void recordError(String message, Object value, Object userData) {
            equal("oops", value, "rejected input retained for error reporting");
            isTrue(userData instanceof ParseException, "original parse exception retained");
            errors.add(message);
          }

          public void setDirty(boolean dirty) {}

          public HandlerRegistration subscribe() {
            return () -> {};
          }
        });
    editor.setValue(42);
    equal("42", input.getText(), "editor renders value");
    input.setText("oops");
    equal(null, input.getValue(), "invalid value is null");
    boolean rejected = false;
    try {
      input.getValueOrThrow();
    } catch (ParseException expected) {
      rejected = true;
    }
    isTrue(rejected, "explicit parser access reports errors");
    equal(42, editor.getValue(), "editor retains last valid value on parse error");
    equal(1, errors.size(), "one editor error");
    input.setText("17");
    equal(17, editor.getValue(), "valid input recovers");
    input.setText("");
    equal(null, editor.getValue(), "empty typed input is null");
    equal("", new TextBox().getValue(), "empty text input remains an empty string");
  }

  public static void textDirectionFollowsContentAndCanBeDisabled() {
    TextBox input = new TextBox();
    input.setDirectionEstimator(true);
    input.setText("שלום עולם");
    equal(Direction.RTL, input.getDirection(), "Hebrew input direction");
    input.setValue("hello world");
    equal(Direction.LTR, input.getDirection(), "Latin value direction");
    input.setText("123");
    equal(Direction.LTR, input.getDirection(), "numeric input direction");
    input.setText("...");
    equal(Direction.DEFAULT, input.getDirection(), "neutral input direction");
    input.setDirectionEstimator(false);
    input.setDirection(Direction.RTL);
    input.setText("hello");
    equal(Direction.RTL, input.getDirection(), "disabled estimator preserves explicit direction");
    input.setDirectionEstimator(true);
    equal(Direction.LTR, input.getDirection(), "reenabling immediately refreshes direction");
    equal(
        Direction.RTL,
        WordCountDirectionEstimator.get()
            .estimateDirection("<span title='hello world'>שלום</span>", true),
        "HTML markup excluded from direction estimation");
  }

  public static void inputSelectionTracksCursorAndRejectsInvalidRanges() {
    TextBox input = new TextBox();
    RootPanel.get().add(input);
    try {
      input.setText("material");
      input.setSelectionRange(1, 3);
      equal("ate", input.getSelectedText(), "selected substring");
      input.setCursorPos(5);
      equal(5, input.getCursorPos(), "cursor position");
      equal(0, input.getSelectionLength(), "moving cursor clears selection");
      boolean rejected = false;
      try {
        input.setSelectionRange(7, 2);
      } catch (IndexOutOfBoundsException expected) {
        rejected = true;
      }
      isTrue(rejected, "selection beyond end rejected");
      input.selectAll();
      equal("material", input.getSelectedText(), "select all");
    } finally {
      input.removeFromParent();
    }
  }

  public static void javascriptDatesPreserveNativeRolloverAndInvalidValues() {
    double epoch = JsDate.UTC(2024, 1, 29, 12, 34, 56, 789);
    JsDate date = JsDate.create(epoch);
    equal(epoch, date.getTime(), "epoch roundtrip");
    equal(29, date.getUTCDate(), "leap day");
    date.setUTCDate(30);
    equal(2, date.getUTCMonth(), "day overflow advances month");
    equal(1, date.getUTCDate(), "day overflow normalizes date");
    date.setUTCHours(5);
    equal(34, date.getUTCMinutes(), "omitted setter arguments preserved");
    equal(789, date.getUTCMilliseconds(), "milliseconds preserved");
    equal(date.getTime(), date.valueOf(), "native valueOf");
    JsDate local = JsDate.create(2023, 12, 1);
    equal(2024, local.getFullYear(), "local month overflow advances year");
    equal(0, local.getMonth(), "local month overflow normalizes month");
    equal(1924, JsDate.create(24, 0).getFullYear(), "native short-year constructor semantics");
    isTrue(Double.isNaN(JsDate.parse("not a date")), "invalid parse remains NaN");
    isTrue(Double.isNaN(JsDate.create("not a date").getTime()), "invalid date remains NaN");
  }

  private static final class NumericInput extends ValueBox<Integer> {
    NumericInput() {
      super(
          Document.get().createTextInputElement(),
          new Renderer<Integer>() {
            public String render(Integer value) {
              return value == null ? "" : value.toString();
            }

            public void render(Integer value, Appendable output) throws IOException {
              output.append(render(value));
            }
          },
          text -> {
            if (text.isEmpty()) return null;
            try {
              return Integer.valueOf(text.toString());
            } catch (NumberFormatException error) {
              throw new ParseException("Expected integer", 0);
            }
          });
    }
  }

  public static void stylePropertiesRetainValuesAndClearIndependently() {
    Style style = Document.get().createDivElement().getStyle();
    style.setVisibility(Style.Visibility.HIDDEN);
    style.setFontWeight(Style.FontWeight.BOLD);
    style.setFloat(Style.Float.RIGHT);
    style.setVerticalAlign(Style.VerticalAlign.TEXT_TOP);
    style.setOverflow(Style.Overflow.AUTO);
    style.setCursor(Style.Cursor.COL_RESIZE);
    style.setDisplay(Style.Display.TABLE_CELL);
    equal("hidden", style.getVisibility(), "visibility");
    equal("bold", style.getFontWeight(), "font weight");
    equal("right", style.getProperty("cssFloat"), "float");
    equal("text-top", style.getVerticalAlign(), "vertical alignment");
    equal("auto", style.getOverflow(), "overflow");
    equal("col-resize", style.getCursor(), "cursor");
    equal("table-cell", style.getDisplay(), "table display");
    style.clearVisibility();
    style.clearFloat();
    equal("", style.getVisibility(), "cleared visibility");
    equal("", style.getProperty("cssFloat"), "cleared float");
    equal("bold", style.getFontWeight(), "unrelated style survives clearing");
    style.setProperty("marginTop", "12px");
    equal("12px", style.getProperty("marginTop"), "camel case CSS name");
    style.clearProperty("marginTop");
    equal("", style.getProperty("marginTop"), "camel case CSS clearing");
    style.setHeight(100, Style.Unit.PCT);
    style.setZIndex(9999);
    style.setBottom(0, Style.Unit.PX);
    style.setLeft(12, Style.Unit.PX);
    style.clearHeight();
    equal("", style.getProperty("height"), "fullscreen height cleared");
    equal("9999", style.getProperty("zIndex"), "clearing height preserves stacking");
    style.clearZIndex();
    style.clearBottom();
    style.clearLeft();
    equal("", style.getProperty("zIndex"), "fullscreen stacking cleared");
    equal("", style.getProperty("bottom"), "fullscreen bottom cleared");
    equal("", style.getProperty("left"), "fullscreen left cleared");
    equal("bold", style.getFontWeight(), "clearing fullscreen styles preserves unrelated values");
  }

  public static void mixedChildTraversalAndRemovalPreserveTheDom() {
    Element parent = Document.get().createDivElement();
    parent.appendChild(Document.get().createTextNode("before"));
    Element span = Document.get().createSpanElement();
    span.setInnerText("after");
    parent.appendChild(span);
    equal(2, parent.getChildCount(), "text nodes are included");
    Node text = parent.getChild(0);
    text.removeFromParent();
    equal(1, parent.getChildCount(), "node removal changes original parent");
    equal("after", parent.getInnerText(), "element survives text removal");
    parent.getChild(0).removeFromParent();
    equal(0, parent.getChildCount(), "element removal changes original parent");
    text.removeFromParent();
    equal(0, parent.getChildCount(), "detached node removal is harmless");
  }

  public static void textBoxVisibleLengthUsesTheNativeInputSize() {
    TextBox input = new TextBox();
    input.setVisibleLength(37);
    equal(37, input.getVisibleLength(), "input size roundtrip");
    equal("37", input.getElement().getAttribute("size"), "native input size attribute");
    input.setValue("kept");
    input.setVisibleLength(12);
    equal("kept", input.getValue(), "size update preserves value");
  }

  public static void openAndCloseEventsRetainTargetSourceAndDisposal() {
    LifecycleWidget widget = new LifecycleWidget();
    List<String> calls = new ArrayList<>();
    HandlerRegistration opened =
        widget.addOpenHandler(
            event -> {
              same(widget, event.getSource(), "open event source");
              equal("dialog", event.getTarget(), "open event target");
              calls.add("open");
            });
    widget.addCloseHandler(
        event -> {
          same(widget, event.getSource(), "close event source");
          equal("dialog", event.getTarget(), "close event target");
          calls.add(event.isAutoClosed() ? "auto" : "close");
        });
    OpenEvent.fire(widget, "dialog");
    CloseEvent.fire(widget, "dialog");
    CloseEvent.fire(widget, "dialog", true);
    opened.removeHandler();
    OpenEvent.fire(widget, "dialog");
    equal("[open, close, auto]", calls.toString(), "ordered event delivery and removal");
  }

  private static final class LifecycleWidget extends Widget
      implements HasOpenHandlers<String>, HasCloseHandlers<String> {
    LifecycleWidget() {
      setElement(Document.get().createDivElement());
    }

    public HandlerRegistration addOpenHandler(OpenHandler<String> handler) {
      return addHandler(handler, OpenEvent.getType());
    }

    public HandlerRegistration addCloseHandler(CloseHandler<String> handler) {
      return addHandler(handler, CloseEvent.getType());
    }
  }
}
