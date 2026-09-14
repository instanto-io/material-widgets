package io.instanto.material.client;

import com.google.gwt.dom.client.Element;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/** Releases the timers and namespaced handlers owned by the bundled media plugins. */
public final class MediaLifecycle {
  private MediaLifecycle() {}

  public static void unloadSlider(Element element) {
    unloadSliderNative(element.unwrap());
  }

  public static void unloadImage(Element element) {
    unloadImageNative(element.unwrap());
  }

  @JSBody(
      params = "element",
      script =
          """
      var slider=window.jQuery(element);
      slider.triggerHandler('sliderPause.instantoMaterialSlider');
      slider.off('.instantoMaterialSlider');
      slider.find('*').velocity('stop',true);
      slider.children('.indicators').detach();
      var hammer=slider.data('hammer');
      if(hammer) { hammer.destroy(); slider.removeData('hammer'); }
      """)
  private static native void unloadSliderNative(JSObject element);

  @JSBody(
      params = "element",
      script =
          """
      var dispose=window.jQuery(element).data('instantoMaterialBoxDispose');
      if(dispose) dispose();
      """)
  private static native void unloadImageNative(JSObject element);
}
