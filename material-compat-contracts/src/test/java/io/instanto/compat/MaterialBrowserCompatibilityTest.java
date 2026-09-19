package io.instanto.compat;

import static org.junit.Assert.*;

import com.google.gwt.core.client.Callback;
import com.google.gwt.storage.client.Storage;
import io.instanto.gwt.testing.api.MaterialBrowserContracts;
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
public class MaterialBrowserCompatibilityTest {
  @Test
  public void imageEvents() {
    awaitImageEvents();
  }

  @Async
  private static native void awaitImageEvents();

  private static void awaitImageEvents(AsyncCallback<Void> continuation) {
    io.instanto.gwt.testing.api.MaterialResourceContracts
        .imageLoadAndErrorHandlersFollowBrowserEvents(
            new Callback<Void, Throwable>() {
              @Override
              public void onSuccess(Void value) {
                continuation.complete(null);
              }

              @Override
              public void onFailure(Throwable failure) {
                continuation.error(failure);
              }
            });
  }

  @Test
  public void locationAndNavigator() {
    MaterialBrowserContracts.locationAndNavigatorReadTheHostWindow();
    awaitNavigation();
  }

  @Test
  public void storage() {
    MaterialBrowserContracts.storageSupportMatchesAvailableStores();
  }

  @Test
  public void metadata() {
    MaterialBrowserContracts.metadataAndFieldsetsUseNativeElements();
  }

  @Test
  public void geometry() {
    MaterialBrowserContracts.widgetCoordinatesAccountForScrolledContainers();
  }

  @Test
  public void suggestions() {
    MaterialBrowserContracts.suggestionSubclassesCanUseTheDefaultConstructor();
  }

  @Test
  public void scrolling() {
    awaitScroll();
  }

  @Async
  private static native void awaitNavigation();

  private static void awaitNavigation(AsyncCallback<Void> continuation) {
    MaterialBrowserContracts.sameDocumentNavigationCompletes(
        new Callback<Void, Throwable>() {
          @Override
          public void onSuccess(Void value) {
            continuation.complete(null);
          }

          @Override
          public void onFailure(Throwable failure) {
            continuation.error(failure);
          }
        });
  }

  @Async
  private static native void awaitScroll();

  private static void awaitScroll(AsyncCallback<Void> continuation) {
    MaterialBrowserContracts.windowScrollEventsReportPositionAndCanBeRemoved(
        new Callback<Void, Throwable>() {
          @Override
          public void onSuccess(Void value) {
            continuation.complete(null);
          }

          @Override
          public void onFailure(Throwable failure) {
            continuation.error(failure);
          }
        });
  }

  @Test
  public void inaccessibleStorageReportsUnsupported() {
    JSObject descriptors = denyStorage();
    try {
      assertFalse(Storage.isLocalStorageSupported());
      assertFalse(Storage.isSessionStorageSupported());
      assertNull(Storage.getLocalStorageIfSupported());
      assertNull(Storage.getSessionStorageIfSupported());
    } finally {
      restoreStorage(descriptors);
    }
  }

  @JSBody(
      script =
          "var saved={}; ['localStorage','sessionStorage'].forEach(function(name){saved[name]=Object.getOwnPropertyDescriptor(window,name); Object.defineProperty(window,name,{configurable:true,get:function(){throw new DOMException('Test blocked storage','SecurityError');}});}); return saved;")
  private static native JSObject denyStorage();

  @JSBody(
      params = "saved",
      script =
          "['localStorage','sessionStorage'].forEach(function(name){if(saved[name]) Object.defineProperty(window,name,saved[name]); else delete window[name];});")
  private static native void restoreStorage(JSObject saved);
}
