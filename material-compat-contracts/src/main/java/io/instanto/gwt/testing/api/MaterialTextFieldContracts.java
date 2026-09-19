package io.instanto.gwt.testing.api;

import static io.instanto.gwt.testing.api.ContractAssertions.*;

import com.google.gwt.event.logical.shared.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Label;
import java.util.ArrayList;
import java.util.List;

/** Shared API behaviour used by the original text-field addins. */
public final class MaterialTextFieldContracts {
  private MaterialTextFieldContracts() {}

  public static void initialisationHandlersKeepSourceAndCanBeRemoved() {
    class Source extends Label implements HasInitializeHandlers {
      public HandlerRegistration addInitializeHandler(InitializeHandler handler) {
        return addHandler(handler, InitializeEvent.getType());
      }
    }
    Source source = new Source();
    List<String> calls = new ArrayList<>();
    HandlerRegistration registration =
        source.addInitializeHandler(
            event -> {
              same(source, event.getSource(), "initialisation source");
              same(InitializeEvent.getType(), event.getAssociatedType(), "initialisation type");
              calls.add("first");
            });
    source.addInitializeHandler(event -> calls.add("second"));
    InitializeEvent.fire(source);
    registration.removeHandler();
    InitializeEvent.fire(source);
    equal("[first, second, second]", calls.toString(), "ordered delivery and disposal");
  }

  public static void sharedAndClientTimeFormatsRoundTrip() {
    for (String pattern : new String[] {"HH:mm", "hh:mm aa"}) {
      com.google.gwt.i18n.shared.DateTimeFormat shared =
          com.google.gwt.i18n.shared.DateTimeFormat.getFormat(pattern);
      com.google.gwt.i18n.shared.DateTimeFormat client =
          com.google.gwt.i18n.client.DateTimeFormat.getFormat(pattern);
      String text = pattern.equals("HH:mm") ? "23:07" : "11:07 PM";
      equal(text, shared.format(shared.parse(text)), "shared time round-trip");
      equal(text, client.format(shared.parse(text)), "client accepts shared parse");
      equal(text, shared.format(client.parse(text)), "shared accepts client parse");
    }
  }

  public static void nativeKeyEventsPreserveModifiersAndRelativeElement() {
    com.google.gwt.user.client.ui.TextBox source = new com.google.gwt.user.client.ui.TextBox();
    int[] calls = {0};
    source.addKeyUpHandler(
        event -> {
          equal(65, event.getNativeKeyCode(), "key code");
          isTrue(event.isControlKeyDown() && event.isShiftKeyDown(), "enabled modifiers");
          isTrue(!event.isAltKeyDown() && !event.isMetaKeyDown(), "disabled modifiers");
          same(source.getElement(), event.getRelativeElement(), "relative element");
          calls[0]++;
        });
    com.google.gwt.event.dom.client.DomEvent.fireNativeEvent(
        com.google.gwt.dom.client.Document.get().createKeyUpEvent(true, false, true, false, 65),
        source,
        source.getElement());
    equal(1, calls[0], "one native key delivery");
  }

  public static void suggestionsUseFactoriesAndDeliverSelectionWithoutValueChange() {
    class Oracle extends com.google.gwt.user.client.ui.MultiWordSuggestOracle {
      int factories;

      @Override
      protected MultiWordSuggestion createSuggestion(String replacement, String display) {
        factories++;
        return super.createSuggestion(replacement, display);
      }
    }
    class Display extends com.google.gwt.user.client.ui.SuggestBox.DefaultSuggestionDisplay {
      com.google.gwt.user.client.ui.SuggestBox.SuggestionCallback callback;
      com.google.gwt.user.client.ui.SuggestOracle.Suggestion choice;

      @Override
      protected void showSuggestions(
          com.google.gwt.user.client.ui.SuggestBox box,
          java.util.Collection<? extends com.google.gwt.user.client.ui.SuggestOracle.Suggestion>
              suggestions,
          boolean html,
          boolean autoSelect,
          com.google.gwt.user.client.ui.SuggestBox.SuggestionCallback callback) {
        this.callback = callback;
        choice = suggestions.iterator().next();
      }

      @Override
      public void hideSuggestions() {}

      @Override
      public boolean isSuggestionListShowing() {
        return callback != null;
      }
    }
    Oracle oracle = new Oracle();
    oracle.add("Alice Example");
    Display display = new Display();
    com.google.gwt.user.client.ui.SuggestBox box =
        new com.google.gwt.user.client.ui.SuggestBox(
            oracle, new com.google.gwt.user.client.ui.TextBox(), display);
    List<String> events = new ArrayList<>();
    HandlerRegistration selection =
        box.addSelectionHandler(
            e -> events.add("selected:" + e.getSelectedItem().getReplacementString()));
    box.addValueChangeHandler(e -> events.add("value"));
    com.google.gwt.user.client.ui.RootPanel.get().add(box);
    try {
      box.setText("Ali");
      box.showSuggestionList();
      equal(1, oracle.factories, "oracle factory hook");
      display.callback.onSuggestionSelected(display.choice);
      equal("Alice Example", box.getValue(), "selected value");
      equal("[selected:Alice Example]", events.toString(), "selection event only");
      selection.removeHandler();
      display.callback.onSuggestionSelected(display.choice);
      equal(1, events.size(), "selection handler removed");
    } finally {
      box.removeFromParent();
    }
  }
}
