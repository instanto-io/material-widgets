package io.instanto.gwt.testing.api;

import static io.instanto.gwt.testing.api.ContractAssertions.*;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.MetaElement;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle.MultiWordSuggestion;
import com.google.gwt.user.client.ui.RootPanel;

/** Browser services used by original Material widgets, without framework-specific adapters. */
public final class MaterialBrowserContracts {
  private MaterialBrowserContracts() {}

  public static void locationAndNavigatorReadTheHostWindow() {
    Element body = Document.get().getBody();
    body.setAttribute("data-window-original-url", Window.Location.getHref());
    try {
      script(
          "history.replaceState(null,'',location.pathname+'?material=one%20two#material%20hash');"
              + "['href','host','hostname','pathname','port','protocol','search','hash'].forEach(function(p){document.body.setAttribute('data-location-'+p,location[p]);});"
              + "['appCodeName','appName','appVersion','platform','userAgent'].forEach(function(p){document.body.setAttribute('data-navigator-'+p,navigator[p]);});");
      equal(body.getAttribute("data-location-href"), Window.Location.getHref(), "host URL");
      equal(body.getAttribute("data-location-host"), Window.Location.getHost(), "host and port");
      equal(body.getAttribute("data-location-hostname"), Window.Location.getHostName(), "hostname");
      equal(body.getAttribute("data-location-pathname"), Window.Location.getPath(), "path");
      equal(body.getAttribute("data-location-port"), Window.Location.getPort(), "port");
      equal(body.getAttribute("data-location-protocol"), Window.Location.getProtocol(), "protocol");
      equal("?material=one%20two", Window.Location.getQueryString(), "encoded query retained");
      equal("#material%20hash", Window.Location.getHash(), "encoded fragment retained");
      equal(
          body.getAttribute("data-navigator-appCodeName"),
          Window.Navigator.getAppCodeName(),
          "app code name");
      equal(body.getAttribute("data-navigator-appName"), Window.Navigator.getAppName(), "app name");
      equal(
          body.getAttribute("data-navigator-appVersion"),
          Window.Navigator.getAppVersion(),
          "app version");
      equal(
          body.getAttribute("data-navigator-platform"), Window.Navigator.getPlatform(), "platform");
      equal(
          body.getAttribute("data-navigator-userAgent"),
          Window.Navigator.getUserAgent(),
          "user agent");
    } finally {
      script(
          "history.replaceState(null,'',document.body.getAttribute('data-window-original-url'));"
              + "Array.from(document.body.attributes).forEach(function(a){if(a.name.indexOf('data-location-')===0||a.name.indexOf('data-navigator-')===0)document.body.removeAttribute(a.name);});");
      body.removeAttribute("data-window-original-url");
    }
  }

  public static void sameDocumentNavigationCompletes(Callback<Void, Throwable> completion) {
    String originalUrl = Window.Location.getHref();
    Document.get().getBody().setAttribute("data-window-navigation-url", originalUrl);
    String base = originalUrl.split("#", 2)[0];
    Window.Location.assign(base + "#material-assigned");
    new Timer() {
      private int attempts;
      private boolean replacing;

      @Override
      public void run() {
        try {
          if (!replacing && "#material-assigned".equals(Window.Location.getHash())) {
            replacing = true;
            Window.Location.replace(base + "#material-replaced");
          } else if (replacing && "#material-replaced".equals(Window.Location.getHash())) {
            finish(null);
            return;
          }
          if (++attempts >= 500) finish(new AssertionError("fragment navigation timed out"));
        } catch (Throwable failure) {
          finish(failure);
        }
      }

      private void finish(Throwable failure) {
        cancel();
        script(
            "history.replaceState(null,'',document.body.getAttribute('data-window-navigation-url'));document.body.removeAttribute('data-window-navigation-url');");
        if (failure == null) completion.onSuccess(null);
        else completion.onFailure(failure);
      }
    }.scheduleRepeating(10);
  }

  public static void storageSupportMatchesAvailableStores() {
    equal(
        Storage.isLocalStorageSupported(),
        Storage.getLocalStorageIfSupported() != null,
        "local support agrees with accessor");
    equal(
        Storage.isSessionStorageSupported(),
        Storage.getSessionStorageIfSupported() != null,
        "session support agrees with accessor");
    Storage local = Storage.getLocalStorageIfSupported();
    Storage session = Storage.getSessionStorageIfSupported();
    isTrue(local != null && session != null, "test page provides both storage types");
    String key = "instanto-material-contract-" + Document.get().createUniqueId();
    try {
      local.setItem(key, "local-value");
      session.setItem(key, "session-value");
      equal("local-value", local.getItem(key), "local round trip");
      equal("session-value", session.getItem(key), "stores remain independent");
      local.removeItem(key);
      equal(null, local.getItem(key), "local removal");
      equal("session-value", session.getItem(key), "local removal preserves session value");
    } finally {
      local.removeItem(key);
      session.removeItem(key);
    }
  }

  public static void metadataAndFieldsetsUseNativeElements() {
    MetaElement meta = Document.get().createMetaElement();
    meta.setName("theme-color");
    meta.setContent("#123456");
    meta.setHttpEquiv("test-header");
    equal("meta", meta.getTagName().toLowerCase(), "native meta tag");
    equal("theme-color", meta.getAttribute("name"), "metadata name reflected");
    MetaElement view = MetaElement.as(meta);
    equal("test-header", view.getHttpEquiv(), "http equivalent property");
    view.setContent("#abcdef");
    equal("#abcdef", meta.getContent(), "metadata views share state");
    Element fieldset = DOM.createFieldSet();
    equal("fieldset", fieldset.getTagName().toLowerCase(), "legacy fieldset factory");
    fieldset.setPropertyBoolean("disabled", true);
    isTrue(fieldset.hasAttribute("disabled"), "fieldset uses native reflected disabled property");
    RootPanel.getBodyElement().setAttribute("data-root-view", "shared");
    equal(
        "shared",
        Document.get().getBody().getAttribute("data-root-view"),
        "legacy body view shares host body");
    Document.get().getBody().removeAttribute("data-root-view");
  }

  public static void widgetCoordinatesAccountForScrolledContainers() {
    FlowPanel outer = new FlowPanel();
    outer
        .getElement()
        .setAttribute(
            "style",
            "position:absolute;left:101px;top:137px;width:100px;height:100px;overflow:scroll;border:0;padding:0;");
    Element extent = Document.get().createDivElement();
    extent.setAttribute("style", "width:500px;height:500px;");
    outer.getElement().appendChild(extent);
    Label child = new Label("coordinates");
    child.getElement().setAttribute("style", "position:absolute;left:23px;top:29px;");
    outer.add(child);
    RootPanel.get().add(outer);
    try {
      equal(outer.getAbsoluteLeft() + 23, child.getAbsoluteLeft(), "child absolute left");
      equal(outer.getAbsoluteTop() + 29, child.getAbsoluteTop(), "child absolute top");
      outer.getElement().setScrollLeft(13);
      outer.getElement().setScrollTop(17);
      equal(
          outer.getAbsoluteLeft() + 10,
          child.getAbsoluteLeft(),
          "container horizontal scrolling reflected");
      equal(
          outer.getAbsoluteTop() + 12,
          child.getAbsoluteTop(),
          "container vertical scrolling reflected");
    } finally {
      outer.removeFromParent();
    }
  }

  public static void suggestionSubclassesCanUseTheDefaultConstructor() {
    MultiWordSuggestion empty = new MultiWordSuggestion();
    equal(null, empty.getDisplayString(), "default display string");
    equal(null, empty.getReplacementString(), "default replacement string");
    MultiWordSuggestion custom =
        new MultiWordSuggestion() {
          @Override
          public String getDisplayString() {
            return "label";
          }

          @Override
          public String getReplacementString() {
            return "value";
          }
        };
    equal("label", custom.getDisplayString(), "subclass supplies display");
    equal("value", custom.getReplacementString(), "subclass supplies replacement");
  }

  public static void windowScrollEventsReportPositionAndCanBeRemoved(
      Callback<Void, Throwable> completion) {
    new ScrollCheck(completion).start();
  }

  private static final class ScrollCheck {
    private final Callback<Void, Throwable> completion;
    private final Element extent = Document.get().createDivElement();
    private final int originalLeft = Window.getScrollLeft();
    private final int originalTop = Window.getScrollTop();
    private HandlerRegistration registration;
    private int calls;
    private boolean finished;
    private final Timer timeout =
        new Timer() {
          @Override
          public void run() {
            finish(new AssertionError("window scroll timed out"));
          }
        };

    ScrollCheck(Callback<Void, Throwable> completion) {
      this.completion = completion;
    }

    void start() {
      extent.setAttribute(
          "style",
          "position:absolute;left:0;top:0;width:5000px;height:5000px;pointer-events:none;");
      Document.get().getBody().appendChild(extent);
      registration =
          Window.addWindowScrollHandler(
              event -> {
                calls++;
                if (event.getScrollLeft() != 41 || event.getScrollTop() != 83) return;
                try {
                  equal(Window.getScrollLeft(), event.getScrollLeft(), "event horizontal position");
                  equal(Window.getScrollTop(), event.getScrollTop(), "event vertical position");
                  equal(null, event.getSource(), "window scroll has no widget source");
                  registration.removeHandler();
                  int callsAtRemoval = calls;
                  Window.scrollTo(originalLeft, originalTop);
                  new Timer() {
                    @Override
                    public void run() {
                      try {
                        equal(callsAtRemoval, calls, "removed scroll handler stays silent");
                        finish(null);
                      } catch (Throwable failure) {
                        finish(failure);
                      }
                    }
                  }.schedule(50);
                } catch (Throwable failure) {
                  finish(failure);
                }
              });
      timeout.schedule(5000);
      Window.scrollTo(41, 83);
    }

    void finish(Throwable failure) {
      if (finished) return;
      finished = true;
      timeout.cancel();
      if (registration != null) registration.removeHandler();
      extent.removeFromParent();
      Window.scrollTo(originalLeft, originalTop);
      if (failure == null) completion.onSuccess(null);
      else completion.onFailure(failure);
    }
  }

  private static void script(String source) {
    ScriptInjector.fromString(source).setWindow(ScriptInjector.TOP_WINDOW).inject();
  }
}
