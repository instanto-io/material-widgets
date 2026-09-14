package io.instanto.material.client;

import com.google.gwt.dom.client.Element;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/** Releases only the handlers and animations owned by the adapted Materialize tabs plugin. */
public final class TabLifecycle {
  private TabLifecycle() {}

  public static void unload(Element tab) {
    unloadNative(tab.unwrap());
  }

  // The bundled Velocity cannot cancel queue:false delay timers. Detach keeps
  // their data valid until they finish; removing the node would discard it.
  @JSBody(
      params = "element",
      script =
          "var tab = window.jQuery(element); "
              + "var ns = tab.data('instantoMaterialTabsNamespace'); "
              + "if (ns) window.jQuery(window).off('resize' + ns); "
              + "tab.off('click.instantoMaterialTabs', 'a'); "
              + "tab.find('.indicator').velocity('stop', true).detach();")
  private static native void unloadNative(JSObject element);
}
