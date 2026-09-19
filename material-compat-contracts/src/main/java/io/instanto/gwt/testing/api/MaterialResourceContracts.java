package io.instanto.gwt.testing.api;

import static io.instanto.gwt.testing.api.ContractAssertions.*;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.CodeDownloadException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.user.client.Timer;

/** Resource loading contracts shared by the native GWT and TeaVM runtimes. */
public final class MaterialResourceContracts {
  public static final String INVALID_SCRIPT_URL = "data:text/javascript;base64,%%%";

  private MaterialResourceContracts() {}

  public static void imageLoadAndErrorHandlersFollowBrowserEvents(
      Callback<Void, Throwable> completion) {
    com.google.gwt.user.client.ui.Image image = new com.google.gwt.user.client.ui.Image();
    int[] counts = new int[2];
    image.addLoadHandler(event -> counts[1]++).removeHandler();
    image.addErrorHandler(event -> counts[1]++).removeHandler();
    Timer timeout =
        new Timer() {
          @Override
          public void run() {
            image.removeFromParent();
            completion.onFailure(new AssertionError("Image events timed out"));
          }
        };
    image.addLoadHandler(
        event -> {
          counts[0]++;
          image.setUrl("data:image/png;base64,bm90LWFuLWltYWdl");
        });
    image.addErrorHandler(
        event -> {
          timeout.cancel();
          image.removeFromParent();
          try {
            equal(1, counts[0], "Valid image loads before invalid image fails");
            equal(0, counts[1], "Removed image handlers stay removed");
            completion.onSuccess(null);
          } catch (Throwable failure) {
            completion.onFailure(failure);
          }
        });
    com.google.gwt.user.client.ui.RootPanel.get().add(image);
    timeout.schedule(5000);
    image.setUrl("data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");
  }

  public static void documentQueriesRemainLive() {
    NodeList<Element> elements = Document.get().getElementsByTagName("instanto-resource-test");
    int initial = elements.getLength();
    Element element = Document.get().createElement("instanto-resource-test");
    Document.get().getBody().appendChild(element);
    equal(initial + 1, elements.getLength(), "document query updates after insertion");
    element.removeFromParent();
    equal(initial, elements.getLength(), "document query updates after removal");
  }

  public static void stylesheetQueuesFlushInGwtOrder() {
    StyleInjector.flush();
    NodeList<Element> styles = Document.get().getElementsByTagName("style");
    int initial = styles.getLength();
    String prefix = "/* instanto-style-contract */";
    Element sample = Document.get().createDivElement();
    sample.setId("instanto-style-contract");
    Document.get().getBody().appendChild(sample);
    try {
      StyleInjector.inject(prefix + "#instanto-style-contract { width: 101px; }");
      StyleInjector.injectAtEnd(prefix + "#instanto-style-contract { width: 202px; }");
      StyleInjector.injectAtStart(prefix + "#instanto-style-contract { width: 303px; }");
      equal(initial, styles.getLength(), "default injection is queued");
      StyleInjector.flush();
      equal(initial + 3, styles.getLength(), "one style per nonempty queue");
      equal(202, sample.getOffsetWidth(), "end queue wins the cascade");
      StyleInjector.injectAtStart(prefix + "#instanto-style-contract { width: 404px; }", true);
      equal(202, sample.getOffsetWidth(), "prepended style stays before existing rules");
      StyleElement latest =
          StyleInjector.injectStylesheet(prefix + "#instanto-style-contract { width: 505px; }");
      equal(505, sample.getOffsetWidth(), "new normal injection appends after prior flush");
      StyleInjector.setContents(latest, prefix + "#instanto-style-contract { width: 606px; }");
      equal(606, sample.getOffsetWidth(), "returned stylesheet can be replaced in place");
      latest.removeFromParent();
      equal(202, sample.getOffsetWidth(), "removing returned style restores previous rule");
      StyleElement empty = StyleInjector.injectStylesheet(null);
      equal("", empty.getInnerText(), "null CSS retains native array-join semantics");
      empty.removeFromParent();
    } finally {
      sample.removeFromParent();
      for (int i = styles.getLength() - 1; i >= 0; i--) {
        Element style = styles.getItem(i);
        if (style.getInnerText().contains(prefix)) style.removeFromParent();
      }
    }
  }

  public static void queuedStylesFlushAutomatically(Callback<Void, Throwable> completion) {
    StyleInjector.flush();
    Element sample = Document.get().createDivElement();
    sample.setId("instanto-auto-style");
    Document.get().getBody().appendChild(sample);
    StyleInjector.inject("/*instanto-auto-style*/ #instanto-auto-style { width: 177px; }");
    StyleInjector.inject("/*instanto-auto-style*/ #instanto-auto-style { height: 19px; }");
    new Timer() {
      @Override
      public void run() {
        Throwable failure = null;
        try {
          equal(177, sample.getOffsetWidth(), "queued width applied automatically");
          equal(19, sample.getOffsetHeight(), "queued height applied automatically");
        } catch (Throwable caught) {
          failure = caught;
        }
        sample.removeFromParent();
        NodeList<Element> styles = Document.get().getElementsByTagName("style");
        for (int i = styles.getLength() - 1; i >= 0; i--) {
          Element style = styles.getItem(i);
          if (style.getInnerText().contains("/*instanto-auto-style*/")) style.removeFromParent();
        }
        if (failure == null) completion.onSuccess(null);
        else completion.onFailure(failure);
      }
    }.schedule(1);
  }

  public static void inlineScriptsExecuteImmediatelyAndRespectRemoval() {
    Document document = Document.get();
    Element body = document.getBody();
    body.removeAttribute("data-resource-inline");
    JavaScriptObject removed =
        ScriptInjector.fromString("document.body.setAttribute('data-resource-inline','executed');")
            .setWindow(ScriptInjector.TOP_WINDOW)
            .inject();
    isTrue(removed != null, "inline injection returns a script handle");
    equal(
        "executed",
        body.getAttribute("data-resource-inline"),
        "inline script executes before returning");
    String scriptText =
        "/* instanto-retained-script */ document.body.setAttribute('data-resource-inline','retained');";
    NodeList<Element> scripts = document.getElementsByTagName("script");
    int initial = scripts.getLength();
    JavaScriptObject retainedHandle =
        ScriptInjector.fromString(scriptText)
            .setWindow(ScriptInjector.TOP_WINDOW)
            .setRemoveTag(false)
            .inject();
    equal(initial + 1, scripts.getLength(), "explicitly retained script remains attached");
    Element retained = Element.as(retainedHandle);
    equal(scriptText, retained.getPropertyString("text"), "retained script content");
    retained.removeFromParent();
    ScriptInjector.fromString("/* instanto-default-removed */")
        .setWindow(ScriptInjector.TOP_WINDOW)
        .inject();
    equal(initial, scripts.getLength(), "default inline injection removes its tag");
    body.removeAttribute("data-resource-inline");
  }

  public static void scriptNoncesPropagateFromTheTargetDocument() {
    Element head = Document.get().getElementsByTagName("head").getItem(0);
    Element seed = Document.get().createElement("script");
    seed.setAttribute("type", "application/json");
    seed.setAttribute("nonce", "instanto-resource-nonce");
    head.insertBefore(seed, head.getFirstChild());
    NodeList<Element> scripts = Document.get().getElementsByTagName("script");
    Element added = null;
    try {
      JavaScriptObject nonceHandle =
          ScriptInjector.fromString("/* instanto-nonce-contract */")
              .setWindow(ScriptInjector.TOP_WINDOW)
              .setRemoveTag(false)
              .inject();
      added = Element.as(nonceHandle);
      equal(
          "instanto-resource-nonce",
          added.getPropertyString("nonce"),
          "nonce copied from selected document");
    } finally {
      if (added != null) added.removeFromParent();
      seed.removeFromParent();
    }
  }

  public static void externalScriptCompletesAfterExecution(Callback<Void, Throwable> completion) {
    Element body = Document.get().getBody();
    body.removeAttribute("data-resource-async");
    int count = Document.get().getElementsByTagName("script").getLength();
    Completion guard = new Completion(completion);
    ScriptInjector.fromUrl(
            "data:text/javascript,document.body.setAttribute('data-resource-async','loaded');")
        .setWindow(ScriptInjector.TOP_WINDOW)
        .setRemoveTag(true)
        .setCallback(
            new Callback<Void, Exception>() {
              @Override
              public void onFailure(Exception reason) {
                guard.fail(reason);
              }

              @Override
              public void onSuccess(Void result) {
                guard.check(
                    () -> {
                      equal(
                          "loaded",
                          body.getAttribute("data-resource-async"),
                          "success runs after evaluation");
                      equal(
                          count,
                          Document.get().getElementsByTagName("script").getLength(),
                          "tag removed before callback");
                      body.removeAttribute("data-resource-async");
                    });
              }
            })
        .inject();
  }

  public static void failedScriptReportsDownloadException(Callback<Void, Throwable> completion) {
    int count = Document.get().getElementsByTagName("script").getLength();
    Completion guard = new Completion(completion);
    ScriptInjector.fromUrl(INVALID_SCRIPT_URL)
        .setWindow(ScriptInjector.TOP_WINDOW)
        .setRemoveTag(true)
        .setCallback(
            new Callback<Void, Exception>() {
              @Override
              public void onFailure(Exception reason) {
                guard.check(
                    () -> {
                      isTrue(
                          reason instanceof CodeDownloadException,
                          "failed load reports GWT exception type");
                      equal(
                          CodeDownloadException.Reason.TERMINATED,
                          ((CodeDownloadException) reason).getReason(),
                          "download reason");
                      equal(
                          count,
                          Document.get().getElementsByTagName("script").getLength(),
                          "failed tag removed before callback");
                    });
              }

              @Override
              public void onSuccess(Void result) {
                guard.fail(new AssertionError("invalid script loaded"));
              }
            })
        .inject();
  }

  private static final class Completion {
    private final Callback<Void, Throwable> callback;
    private boolean finished;
    private final Timer timeout;

    Completion(Callback<Void, Throwable> callback) {
      this.callback = callback;
      timeout =
          new Timer() {
            @Override
            public void run() {
              fail(new AssertionError("script callback timed out"));
            }
          };
      timeout.schedule(5000);
    }

    void check(Runnable assertion) {
      if (finished) throw new AssertionError("script callback called twice");
      try {
        assertion.run();
      } catch (Throwable failure) {
        fail(failure);
        return;
      }
      finished = true;
      timeout.cancel();
      callback.onSuccess(null);
    }

    void fail(Throwable failure) {
      if (finished) throw new AssertionError("script callback called twice", failure);
      finished = true;
      timeout.cancel();
      callback.onFailure(failure);
    }
  }
}
