package io.instanto.compat;

import static org.junit.Assert.*;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.ScriptInjector;
import io.instanto.gwt.testing.api.MaterialResourceContracts;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class MaterialResourceCompatibilityTest {
  @Test
  public void documentQueriesRemainLive() {
    MaterialResourceContracts.documentQueriesRemainLive();
  }

  @Test
  public void stylesRetainGwtOrdering() {
    MaterialResourceContracts.stylesheetQueuesFlushInGwtOrder();
  }

  @Test
  public void inlineScriptsExecuteAndCleanUp() {
    MaterialResourceContracts.inlineScriptsExecuteImmediatelyAndRespectRemoval();
  }

  @Test
  public void noncesFollowTargetDocument() {
    MaterialResourceContracts.scriptNoncesPropagateFromTheTargetDocument();
  }

  @Test
  public void externalScriptLoads() {
    awaitScript(false);
  }

  @Test
  public void externalScriptFails() {
    awaitScript(true);
  }

  @Test
  public void queuedStylesFlushAutomatically() {
    awaitStyles();
  }

  @Async
  private static native void awaitStyles();

  private static void awaitStyles(AsyncCallback<Void> continuation) {
    MaterialResourceContracts.queuedStylesFlushAutomatically(
        new Callback<Void, Throwable>() {
          @Override
          public void onSuccess(Void result) {
            continuation.complete(null);
          }

          @Override
          public void onFailure(Throwable failure) {
            continuation.error(failure);
          }
        });
  }

  @Async
  private static native void awaitScript(boolean fail);

  private static void awaitScript(boolean fail, AsyncCallback<Void> continuation) {
    Callback<Void, Throwable> completion =
        new Callback<Void, Throwable>() {
          @Override
          public void onSuccess(Void result) {
            continuation.complete(null);
          }

          @Override
          public void onFailure(Throwable failure) {
            continuation.error(failure);
          }
        };
    if (fail) MaterialResourceContracts.failedScriptReportsDownloadException(completion);
    else MaterialResourceContracts.externalScriptCompletesAfterExecution(completion);
  }

  @Test
  public void selectedWindowReceivesScriptAndNonce() {
    JSObject frame = makeFrame();
    try {
      JSObject target = frameWindow(frame);
      JavaScriptObject script =
          ScriptInjector.fromString("document.body.dataset.selectedWindow='yes';")
              .setWindow(JavaScriptObject.of(target))
              .setRemoveTag(false)
              .inject();
      assertEquals("true|yes|frame-resource-nonce", executionState(script.unwrap(), target));
    } finally {
      removeFrame(frame);
    }
  }

  @Test
  public void scriptCallbacksRunOnceAndCaptureBuilderSettings() {
    List<String> calls = new ArrayList<>();
    ScriptInjector.FromUrl builder =
        ScriptInjector.fromUrl("data:text/javascript,/*resource-once*/")
            .setRemoveTag(true)
            .setCallback(record(calls, "first"));
    JavaScriptObject first = builder.inject();
    builder.setCallback(record(calls, "second"));
    JavaScriptObject second = builder.inject();
    dispatchCompletion(first.unwrap());
    dispatchCompletion(second.unwrap());
    assertEquals(List.of("first", "second"), calls);
  }

  private static Callback<Void, Exception> record(List<String> calls, String value) {
    return new Callback<Void, Exception>() {
      @Override
      public void onSuccess(Void result) {
        calls.add(value);
      }

      @Override
      public void onFailure(Exception reason) {
        throw new AssertionError(reason);
      }
    };
  }

  @JSBody(
      script =
          "var frame=document.createElement('iframe'); document.body.appendChild(frame); var seed=frame.contentDocument.createElement('script'); seed.type='application/json'; seed.setAttribute('nonce','frame-resource-nonce'); frame.contentDocument.head.appendChild(seed); return frame;")
  private static native JSObject makeFrame();

  @JSBody(params = "frame", script = "return frame.contentWindow;")
  private static native JSObject frameWindow(JSObject frame);

  @JSBody(params = "frame", script = "frame.remove();")
  private static native void removeFrame(JSObject frame);

  @JSBody(
      params = {"script", "target"},
      script =
          "return [script.ownerDocument===target.document, target.document.body.dataset.selectedWindow, script.nonce].join('|');")
  private static native String executionState(JSObject script, JSObject target);

  @JSBody(
      params = "script",
      script =
          "script.dispatchEvent(new Event('load')); script.dispatchEvent(new Event('error')); script.dispatchEvent(new Event('load'));")
  private static native void dispatchCompletion(JSObject script);
}
