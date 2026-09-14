package io.instanto.material.probe;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import gwt.material.design.jquery.client.api.Functions.EventFunc;
import gwt.material.design.jquery.client.api.Functions.Func;
import gwt.material.design.jquery.client.api.JQuery;
import gwt.material.design.jquery.client.api.JQueryElement;
import org.teavm.jso.*;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

/** Exercises the original Material jQuery declarations through generated TeaVM bridges. */
public final class InteropProbe {
  public static void main(String[] args) {
    Element element = Document.get().createDivElement();
    element.setId("interop-node");
    Document.get().getBody().appendChild(element);
    JQueryElement query = JQuery.$(element);
    require(query.length() == 1, "jQuery selected exactly one node");
    require(
        same(query.get(0).unwrap(), element.unwrap()),
        "GWT argument unwrapped to the original DOM node");
    Element roundtrip = query.get(0);
    require(
        same(roundtrip.unwrap(), element.unwrap()),
        "native return wrapped without cloning the node");
    Element[] results = query.get();
    require(
        results.length == 1 && same(results[0].unwrap(), element.unwrap()),
        "native array returns wrapped individually");
    query.text("Shared DOM identity");
    int[] eachCalls = {0};
    query.each(
        (index, node) -> {
          require(
              index instanceof Double && ((Double) index) == 0.0,
              "Generic callback number is boxed");
          require(same(node.unwrap(), element.unwrap()), "Generic callback element is wrapped");
          eachCalls[0]++;
        });
    require(eachCalls[0] == 1, "Original each callback runs once");
    require(
        element.getInnerText().equals("Shared DOM identity"),
        "jQuery writes visible through GWT wrapper");
    Element input = Document.get().createTextInputElement();
    JQueryElement inputQuery = JQuery.$(input);
    inputQuery.val("1000");
    Object value = inputQuery.val();
    require(
        value instanceof String && ((String) value).length() == 4,
        "Object return becomes a Java String");
    require(query.prop("nodeType") instanceof Number, "Object number return is boxed");
    require(inputQuery.prop("disabled") instanceof Boolean, "Object boolean return is boxed");
    inputQuery.prop("materialNumber", (Object) Integer.valueOf(42));
    inputQuery.prop("disabled", (Object) Boolean.TRUE);
    inputQuery.prop("materialBoolean", (Object) Boolean.FALSE);
    inputQuery.prop("materialString", (Object) "native text");
    require(nativeObjectArguments(input.unwrap()), "Object arguments have native primitive types");
    require(
        Boolean.TRUE.equals(inputQuery.prop("disabled")),
        "Object boolean argument becomes a native boolean");
    require(
        "native text".equals(inputQuery.prop("materialString")),
        "Object string argument becomes native text");
    inputQuery.prop("materialString", (Object) null);
    require(inputQuery.prop("materialString") == null, "Object null argument stays null");
    require(query.prop("missingMaterialProperty") == null, "undefined Object return becomes null");
    require(
        same(jsinterop.base.Js.asAny(query.prop("ownerDocument")), HTMLDocument.current()),
        "opaque Object return retains native identity");
    int[] calls = {0};
    EventFunc handler =
        event -> {
          require(same(target(event), element.unwrap()), "native event target identity");
          require(event.getType().equals("click"), "original event property getter");
          calls[0]++;
          event.preventDefault();
          return null;
        };
    query.on("click.material-probe", handler);
    query.trigger("click", new Object[0]);
    require(calls[0] == 1, "native event callback reaches Java");
    query.off("click.material-probe", handler);
    query.trigger("click", new Object[0]);
    require(calls[0] == 1, "the same callback removes its listener");
    gwt.material.design.jquery.client.api.Functions.EventFunc1<Object> generic =
        (event, payload) -> {
          require(
              payload instanceof Double && ((Double) payload) == 7.0,
              "Erased callback payload is boxed");
          calls[0]++;
          return false;
        };
    query.on("payload.material-probe", generic);
    triggerNativePayload(query);
    require(calls[0] == 2, "Generic listener is invoked");
    query.off("payload.material-probe", generic);
    triggerNativePayload(query);
    require(calls[0] == 2, "Generic callback adapter preserves removal identity");
    element.removeFromParent();
    Document.get().getBody().appendChild(element);
    require(
        same(JQuery.$(element).get(0).unwrap(), query.get(0).unwrap()),
        "detach and reattach preserve node identity");
    require(
        JQuery.$(JQuery.window()).length() == 1, "upstream window overlay selects the host window");
    int[] proxyCalls = {0};
    JQuery.proxy(() -> proxyCalls[0]++, (Object) null).call();
    require(proxyCalls[0] == 1, "namespaced method accepts a parameter named function");
    JQuery.proxy(captureArguments(), (Object) null, "first", 2).call();
    require(
        "first:2".equals(Document.get().getBody().getAttribute("data-proxy-arguments")),
        "namespaced varargs pass each value to the native function");
    HTMLDocument.current().getBody().setAttribute("data-probe", "passed");
  }

  @org.teavm.jso.JSBody(params = "query", script = "query.trigger('payload', [7]);")
  private static native void triggerNativePayload(
      gwt.material.design.jquery.client.api.JQueryElement query);

  private static void require(boolean value, String message) {
    if (!value) {
      Document.get().getBody().setAttribute("data-probe-error", message);
      throw new AssertionError(message);
    }
  }

  @JSBody(
      params = {"a", "b"},
      script = "return a === b;")
  private static native boolean same(JSObject a, JSObject b);

  @JSBody(
      params = "element",
      script =
          "return typeof element.materialNumber === 'number' && element.materialNumber === 42 && typeof element.materialBoolean === 'boolean' && element.materialBoolean === false && typeof element.materialString === 'string' && element.materialString === 'native text';")
  private static native boolean nativeObjectArguments(JSObject element);

  @JSBody(params = "event", script = "return event.target;")
  private static native HTMLElement target(JSObject event);

  @JSBody(
      script =
          "return function(first, second) { document.body.setAttribute('data-proxy-arguments', first + ':' + second); };")
  private static native Func captureArguments();
}
