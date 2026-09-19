package io.instanto.compat;

import static org.junit.Assert.*;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.GestureChangeEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import io.instanto.gwt.testing.api.MaterialChoiceContracts;
import io.instanto.gwt.testing.api.MaterialDomContracts;
import io.instanto.gwt.testing.api.MaterialEventContracts;
import io.instanto.gwt.testing.api.MaterialGwtContracts;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class MaterialCompatibilityTest {
  @Test
  public void tableNumberFormatting() {
    io.instanto.gwt.testing.api.MaterialTableContracts.numberPatternsPreserveValuesAndPrecision();
  }

  @Test
  public void tableDom() {
    io.instanto.gwt.testing.api.MaterialTableContracts.tableRowsExposeLiveTypedCells();
  }

  @Test
  public void nativeKeyEvents() {
    io.instanto.gwt.testing.api.MaterialTextFieldContracts
        .nativeKeyEventsPreserveModifiersAndRelativeElement();
  }

  @Test
  public void suggestionSelection() {
    io.instanto.gwt.testing.api.MaterialTextFieldContracts
        .suggestionsUseFactoriesAndDeliverSelectionWithoutValueChange();
  }

  @Test
  public void initialisationEvents() {
    io.instanto.gwt.testing.api.MaterialTextFieldContracts
        .initialisationHandlersKeepSourceAndCanBeRemoved();
  }

  @Test
  public void sharedTimeFormatting() {
    io.instanto.gwt.testing.api.MaterialTextFieldContracts.sharedAndClientTimeFormatsRoundTrip();
  }

  @Test
  public void panelLifecycleOrder() {
    MaterialDomContracts.panelsLoadAfterTheirChildrenAndUnloadBeforeThem();
    MaterialDomContracts.panelInsertionUsesContainerElementsAfterPluginWrapping();
    MaterialDomContracts.elementTextIgnoresPresentationAndPreservesLineBreaks();
  }

  @Test
  public void buttonSafeHtmlUsesSubclassRendering() {
    MaterialChoiceContracts.buttonSafeHtmlUsesSubclassRendering();
  }

  @Test
  public void listInsertionAndSelectionPreserveTextAndValues() {
    MaterialChoiceContracts.listInsertionAndSelectionPreserveTextAndValues();
  }

  @Test
  public void listDirectionWrappingDoesNotLeakIntoPublicText() {
    MaterialChoiceContracts.listDirectionWrappingDoesNotLeakIntoPublicText();
  }

  @Test
  public void directionalLabelsRestoreContextAndEscapePlainText() {
    MaterialChoiceContracts.directionalLabelsRestoreContextAndEscapePlainText();
  }

  @Test
  public void radioConstructorsPreserveSafeHtmlAndDirection() {
    MaterialChoiceContracts.radioConstructorsPreserveSafeHtmlAndDirection();
  }

  @Test
  public void suppressionRetainsTeaVmDefensiveCopies() {
    Throwable failure = new Throwable();
    Throwable suppressed = new IllegalStateException("suppressed");
    failure.addSuppressed(suppressed);
    failure.getSuppressed()[0] = null;
    assertSame(suppressed, failure.getSuppressed()[0]);
  }

  @Test
  public void nativeLoadAndErrorEventsCanBeRemoved() {
    Label widget = new Label("resource");
    java.util.List<String> calls = new java.util.ArrayList<>();
    HandlerRegistration loaded =
        widget.addDomHandler(
            event -> calls.add("load"), com.google.gwt.event.dom.client.LoadEvent.getType());
    HandlerRegistration failed =
        widget.addDomHandler(
            event -> calls.add("error"), com.google.gwt.event.dom.client.ErrorEvent.getType());
    dispatchResourceEvent(widget.getElement().unwrap(), "load");
    dispatchResourceEvent(widget.getElement().unwrap(), "error");
    loaded.removeHandler();
    failed.removeHandler();
    dispatchResourceEvent(widget.getElement().unwrap(), "load");
    dispatchResourceEvent(widget.getElement().unwrap(), "error");
    assertEquals(java.util.List.of("load", "error"), calls);
  }

  @Test
  public void nativeKeyboardCodesAndModifiersReachHandlers() {
    TextBox input = new TextBox();
    int[] calls = {0};
    input.addKeyDownHandler(
        event -> {
          assertEquals(13, event.getNativeKeyCode());
          assertTrue(event.isControlKeyDown());
          calls[0]++;
        });
    input.addKeyUpHandler(
        event -> {
          assertEquals(13, event.getNativeKeyCode());
          calls[0]++;
        });
    input.addKeyPressHandler(
        event -> {
          assertEquals('a', event.getCharCode());
          calls[0]++;
        });
    dispatchKeyboardCodes(input.getElement().unwrap(), "keydown");
    dispatchKeyboardCodes(input.getElement().unwrap(), "keyup");
    dispatchKeyboardCodes(input.getElement().unwrap(), "keypress");
    assertEquals(3, calls[0]);
  }

  @JSBody(
      params = {"element", "type"},
      script = "element.dispatchEvent(new Event(type));")
  private static native void dispatchResourceEvent(HTMLElement element, String type);

  @JSBody(
      params = {"element", "type"},
      script =
          "var event = new KeyboardEvent(type, {ctrlKey:true}); Object.defineProperties(event, {keyCode:{value:13}, charCode:{value:97}}); element.dispatchEvent(event);")
  private static native void dispatchKeyboardCodes(HTMLElement element, String type);

  @Test
  public void suppression() {
    MaterialEventContracts.throwableConstructorsInitializeSuppression();
  }

  @Test
  public void resources() {
    MaterialDomContracts.typedResourceElementsRetainNativeProperties();
  }

  @Test
  public void options() {
    MaterialDomContracts.optionViewsTrackInsertionSelectionAndDisabledState();
  }

  @Test
  public void attach() {
    MaterialDomContracts.attachHandlerInterfaceReportsWidgetLifecycle();
  }

  @Test
  public void eventSources() {
    MaterialEventContracts.eventSourcesFilterHandlersAndPreserveOrder();
  }

  @Test
  public void nestedEventDispatch() {
    MaterialEventContracts.handlerChangesWaitForNestedDispatchToFinish();
  }

  @Test
  public void eventHandlerErrors() {
    MaterialEventContracts.handlerFailuresAreCollectedAfterAllHandlersRun();
  }

  @Test
  public void sanitization() {
    MaterialEventContracts.sanitizationAllowsOnlyUpstreamMarkupSubset();
  }

  @Test
  public void typedValues() {
    MaterialGwtContracts.typedValuesReportErrorsAndRecover();
  }

  @Test
  public void textDirection() {
    MaterialGwtContracts.textDirectionFollowsContentAndCanBeDisabled();
  }

  @Test
  public void inputSelection() {
    MaterialGwtContracts.inputSelectionTracksCursorAndRejectsInvalidRanges();
  }

  @Test
  public void javascriptDates() {
    MaterialGwtContracts.javascriptDatesPreserveNativeRolloverAndInvalidValues();
  }

  @Test
  public void keyboardCancellationAndDirectionListenerRemoval() {
    TextBox input = new TextBox();
    RootPanel.get().add(input);
    try {
      HandlerRegistration registration = input.addKeyDownHandler(event -> input.cancelKey());
      assertFalse(dispatchKey(input.getElement().unwrap(), "keydown"));
      registration.removeHandler();
      input.cancelKey();
      assertTrue(dispatchKey(input.getElement().unwrap(), "keydown"));
      input.setDirectionEstimator(true);
      input.getElement().setPropertyString("value", "שלום");
      dispatchKey(input.getElement().unwrap(), "keyup");
      assertEquals(Direction.RTL, input.getDirection());
      input.setDirectionEstimator(false);
      input.getElement().setPropertyString("value", "hello");
      dispatchKey(input.getElement().unwrap(), "keyup");
      assertEquals(Direction.RTL, input.getDirection());
    } finally {
      input.removeFromParent();
    }
  }

  @JSBody(
      params = {"element", "type"},
      script =
          "return element.dispatchEvent(new KeyboardEvent(type, {bubbles:true, cancelable:true, key:'a'}));")
  private static native boolean dispatchKey(HTMLElement element, String type);

  @Test
  public void nativeTouchPayloadCancellationAndRemoval() {
    Label widget = new Label("touch");
    int[] calls = {0};
    HandlerRegistration registration =
        widget.addDomHandler(
            event -> {
              calls[0]++;
              assertTrue(event.isAltKeyDown());
              assertEquals(1, event.getTouches().length());
              assertEquals(17, event.getTouches().get(0).getIdentifier());
              assertEquals(31, event.getChangedTouches().get(0).getClientX());
              Element.as(event.getTargetTouches().get(0).getTarget())
                  .setAttribute("data-touch", "same");
              event.preventDefault();
            },
            TouchStartEvent.getType());
    assertFalse(dispatchTouch(widget.getElement().unwrap()));
    assertEquals("same", widget.getElement().getAttribute("data-touch"));
    registration.removeHandler();
    assertTrue(dispatchTouch(widget.getElement().unwrap()));
    assertEquals(1, calls[0]);
  }

  @Test
  public void nativeGesturePayloadAndListenerRemoval() {
    Label widget = new Label("gesture");
    int[] calls = {0};
    HandlerRegistration registration =
        widget.addDomHandler(
            event -> {
              calls[0]++;
              assertEquals(2.5, event.getScale(), 0);
              assertEquals(15, event.getRotation(), 0);
            },
            GestureChangeEvent.getType());
    dispatchGesture(widget.getElement().unwrap());
    registration.removeHandler();
    dispatchGesture(widget.getElement().unwrap());
    assertEquals(1, calls[0]);
  }

  @JSBody(
      params = "element",
      script =
          "var e = new Event('touchstart', {bubbles:true, cancelable:true}); var t = {identifier:17, clientX:31, clientY:42, pageX:31, pageY:42, target:element}; Object.defineProperties(e, {touches:{value:[t]}, changedTouches:{value:[t]}, targetTouches:{value:[t]}, altKey:{value:true}}); return element.dispatchEvent(e);")
  private static native boolean dispatchTouch(HTMLElement element);

  @JSBody(
      params = "element",
      script =
          "var e = new Event('gesturechange'); Object.defineProperties(e, {scale:{value:2.5}, rotation:{value:15}}); element.dispatchEvent(e);")
  private static native void dispatchGesture(HTMLElement element);

  @Test
  public void styles() {
    MaterialGwtContracts.stylePropertiesRetainValuesAndClearIndependently();
  }

  @Test
  public void domChildren() {
    MaterialGwtContracts.mixedChildTraversalAndRemovalPreserveTheDom();
  }

  @Test
  public void inputSize() {
    MaterialGwtContracts.textBoxVisibleLengthUsesTheNativeInputSize();
  }

  @Test
  public void lifecycleEvents() {
    MaterialGwtContracts.openAndCloseEventsRetainTargetSourceAndDisposal();
  }
}
