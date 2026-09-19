package io.instanto.gwt.testing.api;

import static io.instanto.gwt.testing.api.ContractAssertions.*;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.event.shared.UmbrellaException;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import java.util.ArrayList;
import java.util.List;

/** Event delivery and HTML handling shared between original GWT and TeaVM. */
public final class MaterialEventContracts {
  private MaterialEventContracts() {}

  public static void eventSourcesFilterHandlersAndPreserveOrder() {
    for (EventBus bus : buses()) {
      Object first = new Object();
      Object second = new Object();
      List<String> calls = new ArrayList<>();
      bus.addHandler(Notice.TYPE, event -> calls.add("global:" + event.text));
      HandlerRegistration registration =
          bus.addHandlerToSource(
              Notice.TYPE,
              first,
              event -> {
                same(first, event.getSource(), "source reaches handler");
                calls.add("first:" + event.text);
              });
      bus.addHandlerToSource(Notice.TYPE, second, event -> calls.add("second:" + event.text));
      bus.fireEventFromSource(new Notice("one"), first);
      bus.fireEventFromSource(new Notice("two"), second);
      bus.fireEvent(new Notice("none"));
      registration.removeHandler();
      bus.fireEventFromSource(new Notice("removed"), first);
      equal(
          "[first:one, global:one, second:two, global:two, global:none, global:removed]",
          calls.toString(),
          "filtered handlers precede global handlers and can be removed");
      boolean rejected = false;
      try {
        bus.fireEventFromSource(new Notice("null"), null);
      } catch (NullPointerException expected) {
        rejected = true;
      }
      isTrue(rejected, "explicit null source rejected");
    }
  }

  public static void handlerChangesWaitForNestedDispatchToFinish() {
    for (EventBus bus : buses()) {
      List<String> calls = new ArrayList<>();
      HandlerRegistration[] second = new HandlerRegistration[1];
      bus.addHandler(
          Notice.TYPE,
          event -> {
            calls.add("a:" + event.text);
            if (event.text.equals("outer")) {
              second[0].removeHandler();
              bus.addHandler(Notice.TYPE, next -> calls.add("c:" + next.text));
              bus.fireEvent(new Notice("nested"));
            }
          });
      second[0] = bus.addHandler(Notice.TYPE, event -> calls.add("b:" + event.text));
      bus.fireEvent(new Notice("outer"));
      bus.fireEvent(new Notice("next"));
      equal(
          "[a:outer, a:nested, b:nested, b:outer, a:next, c:next]",
          calls.toString(),
          "handler changes take effect after outer dispatch");
    }
  }

  public static void handlerFailuresAreCollectedAfterAllHandlersRun() {
    SimpleEventBus bus = new SimpleEventBus();
    RuntimeException first = new IllegalStateException("first");
    RuntimeException second = new IllegalArgumentException("second");
    HandlerRegistration one =
        bus.addHandler(
            Notice.TYPE,
            event -> {
              throw first;
            });
    HandlerRegistration two =
        bus.addHandler(
            Notice.TYPE,
            event -> {
              throw second;
            });
    List<String> calls = new ArrayList<>();
    bus.addHandler(Notice.TYPE, event -> calls.add(event.text));
    boolean rejected = false;
    try {
      bus.fireEvent(new Notice("failure"));
    } catch (UmbrellaException expected) {
      rejected = true;
      equal(2, expected.getCauses().size(), "both errors retained");
      equal(1, expected.getSuppressed().length, "additional failure retained as suppressed");
      isTrue(expected.getCauses().contains(first), "first original exception retained");
      isTrue(expected.getCauses().contains(second), "second original exception retained");
    }
    isTrue(rejected, "legacy event bus translates umbrella exception");
    one.removeHandler();
    two.removeHandler();
    bus.fireEvent(new Notice("recovered"));
    equal("[failure, recovered]", calls.toString(), "later handlers run and bus recovers");
  }

  public static void sanitizationAllowsOnlyUpstreamMarkupSubset() {
    equal(
        "<b>bold</b><br><em>emphasis</em>",
        SimpleHtmlSanitizer.sanitizeHtml("<b>bold</b><br><em>emphasis</em>").asString(),
        "attribute-free allowed markup preserved");
    equal(
        "&lt;img src=x onerror=&#39;alert(1)&#39;&gt;&lt;script&gt;alert(2)&lt;/script&gt;",
        SimpleHtmlSanitizer.sanitizeHtml("<img src=x onerror='alert(1)'><script>alert(2)</script>")
            .asString(),
        "active markup escaped");
    equal(
        "&lt;b title=&quot;unsafe&quot;&gt;text</b>",
        SimpleHtmlSanitizer.getInstance().sanitize("<b title=\"unsafe\">text</b>").asString(),
        "attributes disqualify even an allowed opening tag");
    equal(
        "&amp; &#39; &#x3c; &amp;bad1; &amp;#X3c; &quot; &#39;",
        SafeHtmlUtils.htmlEscapeAllowEntities("&amp; &#39; &#x3c; &bad1; &#X3c; \" '"),
        "upstream entity grammar and quote escaping");
    equal(
        "&lt;<b>",
        SimpleHtmlSanitizer.sanitizeHtml("<<b>").asString(),
        "leading malformed delimiter is escaped");
    boolean rejected = false;
    try {
      SimpleHtmlSanitizer.sanitizeHtml(null);
    } catch (NullPointerException expected) {
      rejected = true;
    }
    isTrue(rejected, "null sanitizer input rejected");
  }

  private static EventBus[] buses() {
    return new EventBus[] {
      new SimpleEventBus(), new com.google.web.bindery.event.shared.SimpleEventBus()
    };
  }

  public static void throwableConstructorsInitializeSuppression() {
    Throwable cause = new IllegalStateException("cause");
    Throwable[] values = {
      new Throwable(),
      new Throwable("message"),
      new Throwable(cause),
      new Throwable("message", cause),
      new ConfigurableThrowable(true),
      new ConfigurableThrowable(false)
    };
    for (int index = 0; index < values.length; index++) {
      Throwable value = values[index];
      equal(0, value.getSuppressed().length, "suppressed exceptions initially empty");
      value.addSuppressed(cause);
      if (index == values.length - 1) {
        equal(0, value.getSuppressed().length, "disabled suppression remains empty");
      } else {
        equal(1, value.getSuppressed().length, "suppressed exception added");
        same(cause, value.getSuppressed()[0], "suppressed identity retained");
      }
    }
  }

  private static final class ConfigurableThrowable extends Throwable {
    ConfigurableThrowable(boolean enabled) {
      super("configured", null, enabled, false);
    }
  }

  private interface NoticeHandler extends EventHandler {
    void onNotice(Notice notice);
  }

  private static final class Notice extends GwtEvent<NoticeHandler> {
    static final Type<NoticeHandler> TYPE = new Type<>();
    final String text;

    Notice(String text) {
      this.text = text;
    }

    public Type<NoticeHandler> getAssociatedType() {
      return TYPE;
    }

    protected void dispatch(NoticeHandler handler) {
      handler.onNotice(this);
    }
  }
}
