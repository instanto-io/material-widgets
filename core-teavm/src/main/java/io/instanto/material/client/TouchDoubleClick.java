package io.instanto.material.client;

import com.google.gwt.event.shared.HandlerRegistration;
import gwt.material.design.client.base.MaterialWidget;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/** Adds touch double-taps to Material's existing, enabled-state-checked double-click handlers. */
public final class TouchDoubleClick {
  private TouchDoubleClick() {}

  public static HandlerRegistration register(MaterialWidget widget, HandlerRegistration handler) {
    JSObject state = retain(widget.getElement().unwrap());
    HandlerRegistration lifecycle = widget.addAttachHandler(event -> reset(state));
    return new HandlerRegistration() {
      private boolean removed;

      @Override
      public void removeHandler() {
        if (!removed) {
          removed = true;
          handler.removeHandler();
          lifecycle.removeHandler();
          release(state);
        }
      }
    };
  }

  @JSBody(
      params = "element",
      script =
          """
      var key = '__instantoMaterialDoubleTap';
      var state = element[key];
      if (state) { state.users++; return state; }
      var start = null, previous = null, pending = null, synthetic = null, duplicate = null;
      var timer = null;
      var interval = 400, distance = 24;
      var touchAction = element.style.getPropertyValue('touch-action');
      var priority = element.style.getPropertyPriority('touch-action');
      // Suppress double-tap zoom on this widget while preserving pan and pinch zoom.
      // Respect an application's explicit touch-action policy.
      var managesTouchAction = false;
      function reset() {
        if (timer !== null) clearTimeout(timer);
        timer = null;
        start = previous = pending = duplicate = null;
      }
      function attached() {
        reset();
        // UiBinder registers handlers before the widget is attached and styled.
        if (!managesTouchAction && element.isConnected
            && getComputedStyle(element).touchAction === 'auto') {
          managesTouchAction = true;
          element.style.setProperty('touch-action', 'manipulation');
        }
      }
      attached();
      function enabled() {
        return element.isConnected && !element.disabled && !element.hasAttribute('disabled')
          && !element.classList.contains('disabled');
      }
      function near(a, b) {
        var x = a.clientX - b.clientX, y = a.clientY - b.clientY;
        return x * x + y * y <= distance * distance;
      }
      function point(touch) {
        return { clientX: touch.clientX, clientY: touch.clientY,
          identifier: touch.identifier, time: performance.now() };
      }
      function down(event) {
        pending = duplicate = null;
        if (!enabled() || event.touches.length !== 1) { reset(); return; }
        start = point(event.touches[0]);
      }
      function move(event) {
        if (!start || event.touches.length !== 1 || !near(start, event.touches[0])) reset();
      }
      function up(event) {
        if (!enabled() || !start || event.touches.length !== 0
            || event.changedTouches.length !== 1) { reset(); return; }
        var end = point(event.changedTouches[0]);
        if (end.identifier !== start.identifier || !near(start, end)
            || end.time - start.time > interval) { reset(); return; }
        start = null;
        if (previous && end.time - previous.time <= interval && near(previous, end)) {
          pending = end;
          previous = null;
        } else previous = end;
      }
      function click(event) {
        // Wait for the browser's click so ordinary tap activation still runs normally.
        if (!pending) return;
        var tap = pending;
        pending = null;
        if (event[key] || !enabled() || event.detail === 0 || performance.now() - tap.time > interval
            || !near(tap, event)) return;
        event[key] = true;
        var target = event.target;
        var doubleEvent = new MouseEvent('dblclick', { bubbles: true, cancelable: true,
          view: window, detail: 2, button: 0, buttons: 0,
          clientX: event.clientX, clientY: event.clientY,
          screenX: event.screenX, screenY: event.screenY,
          ctrlKey: event.ctrlKey, altKey: event.altKey,
          shiftKey: event.shiftKey, metaKey: event.metaKey });
        // Run after the normal click handlers; prefer a native dblclick if one follows.
        timer = setTimeout(function() {
          timer = null;
          if (!enabled()) return;
          if (!element.contains(target)) target = element;
          duplicate = { target: target, time: performance.now() };
          synthetic = doubleEvent;
          target.dispatchEvent(doubleEvent);
          synthetic = null;
        }, 0);
      }
      function doubleClick(event) {
        // Some browsers also emit a native dblclick after the touch click.
        if (event !== synthetic && timer !== null) {
          clearTimeout(timer);
          timer = null;
        }
        if (event !== synthetic && duplicate && duplicate.target === event.target
            && performance.now() - duplicate.time <= interval) {
          event.preventDefault();
          event.stopImmediatePropagation();
        }
        previous = pending = null;
      }
      function pointerDown(event) {
        // A real mouse/pen interaction must not be suppressed after a touch gesture.
        if (event.pointerType !== 'touch') reset();
      }
      var listeners = { touchstart: down, touchmove: move, touchend: up,
        touchcancel: reset, pointerdown: pointerDown, click: click, dblclick: doubleClick };
      Object.keys(listeners).forEach(function(type) {
        element.addEventListener(type, listeners[type], { capture: true,
          passive: type.indexOf('touch') === 0 });
      });
      state = { users: 1, reset: attached, dispose: function() {
        Object.keys(listeners).forEach(function(type) {
          element.removeEventListener(type, listeners[type], true);
        });
        if (managesTouchAction && element.style.getPropertyValue('touch-action') === 'manipulation') {
          if (touchAction) element.style.setProperty('touch-action', touchAction, priority);
          else element.style.removeProperty('touch-action');
        }
        reset();
        delete element[key];
      }};
      element[key] = state;
      return state;
      """)
  private static native JSObject retain(JSObject element);

  @JSBody(params = "state", script = "state.reset();")
  private static native void reset(JSObject state);

  @JSBody(params = "state", script = "if (--state.users === 0) state.dispose();")
  private static native void release(JSObject state);
}
