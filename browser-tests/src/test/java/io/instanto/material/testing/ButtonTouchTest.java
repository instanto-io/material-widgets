package io.instanto.material.testing;

import static org.junit.Assert.*;

import com.microsoft.playwright.Locator;
import org.junit.Test;

/** Touch input must reach the original double-click handler, including its toast. */
public class ButtonTouchTest {
  @Test
  public void doubleTapFiresOneToastAfterNavigation() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!button",
        true,
        page -> {
          page.waitForSelector("body[data-showcase-page=button][data-showcase-state=rendered]");
          for (int pass = 0; pass < 2; pass++) {
            page.locator("#events button").nth(0).tap();
            page.waitForSelector(".toast:has-text('Click Triggered')");
            Locator button = page.locator("#events button").nth(2);
            button.scrollIntoViewIfNeeded();
            assertEquals("manipulation", button.evaluate("e => getComputedStyle(e).touchAction"));
            var bounds = button.boundingBox();
            double x = bounds.x + bounds.width / 2;
            double y = bounds.y + bounds.height / 2;
            page.touchscreen().tap(x, y);
            assertTrue(button.textContent().contains("Double Click Me"));
            page.touchscreen().tap(x, y);
            page.waitForSelector(".toast:has-text('Double Click Triggered')");
            assertTrue(button.textContent().contains("Double Clicked"));
            assertEquals(1, page.locator(".toast:has-text('Double Click Triggered')").count());
            page.locator("a[data-page=checkbox]").tap();
            page.waitForSelector("body[data-showcase-page=checkbox]");
            page.locator("a[data-page=button]").tap();
            page.waitForSelector("body[data-showcase-page=button]");
            // Let the preceding toast finish before checking the new page's handler count.
            page.waitForFunction(
                "!Array.from(document.querySelectorAll('.toast')).some(e => e.textContent.includes('Double Click Triggered'))");
          }
        });
  }

  @Test
  public void slowTapsAndDisabledButtonsDoNotDoubleClick() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!button",
        true,
        page -> {
          page.waitForSelector("body[data-showcase-page=button][data-showcase-state=rendered]");
          Locator button = page.locator("#events button").nth(2);
          button.scrollIntoViewIfNeeded();
          var bounds = button.boundingBox();
          double x = bounds.x + bounds.width / 2;
          double y = bounds.y + bounds.height / 2;
          page.touchscreen().tap(x, y);
          page.waitForTimeout(500);
          page.touchscreen().tap(x, y);
          assertTrue(button.textContent().contains("Double Click Me"));
          assertEquals(0, page.locator(".toast:has-text('Double Click Triggered')").count());
          button.evaluate("e => e.setAttribute('disabled', '')");
          page.touchscreen().tap(x, y);
          page.touchscreen().tap(x, y);
          assertTrue(button.textContent().contains("Double Click Me"));
          assertEquals(0, page.locator(".toast:has-text('Double Click Triggered')").count());
          button.evaluate("e => e.removeAttribute('disabled')");
          // A mouse remains usable on a device with a touchscreen, even just after touch input.
          button.dblclick();
          page.waitForSelector(".toast:has-text('Double Click Triggered')");
          assertEquals(1, page.locator(".toast:has-text('Double Click Triggered')").count());
        });
  }

  @Test
  public void cancelledMovingAndMultiTouchGesturesDoNotDoubleClick() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!button",
        true,
        page -> {
          page.waitForSelector("body[data-showcase-page=button][data-showcase-state=rendered]");
          Locator button = page.locator("#events button").nth(2);
          button.scrollIntoViewIfNeeded();
          // Controlled event sequences exercise cancellation and movement without asking
          // Playwright's touchscreen.tap() to stand in for a drag or pinch.
          button.evaluate(
              """
              async element => {
                const box = element.getBoundingClientRect();
                const x = box.x + box.width / 2, y = box.y + box.height / 2;
                const touch = (id, dx = 0) => ({identifier: id, target: element,
                  clientX: x + dx, clientY: y});
                const a = touch(1), b = touch(2), moved = touch(1, 40);
                function send(type, touches, changedTouches) {
                  const event = new Event(type, {bubbles: true});
                  Object.defineProperties(event, {touches: {value: touches},
                    targetTouches: {value: touches}, changedTouches: {value: changedTouches}});
                  element.dispatchEvent(event);
                }
                function tap() {
                  send('touchstart', [a], [a]);
                  send('touchend', [], [a]);
                  element.dispatchEvent(new MouseEvent('click', {bubbles: true,
                    detail: 1, clientX: x, clientY: y}));
                }
                for (const invalid of ['cancel', 'move', 'multiple', 'long']) {
                  send('touchcancel', [], [a]);
                  tap();
                  send('touchstart', [a], [a]);
                  if (invalid === 'cancel') send('touchcancel', [], [a]);
                  if (invalid === 'move') send('touchmove', [moved], [moved]);
                  if (invalid === 'multiple') send('touchstart', [a, b], [b]);
                  if (invalid === 'long') await new Promise(resolve => setTimeout(resolve, 500));
                  send('touchend', [], [a]);
                  tap();
                  await new Promise(resolve => setTimeout(resolve, 20));
                }
              }
              """);
          assertTrue(button.textContent().contains("Double Click Me"));
          page.waitForTimeout(50);
          assertEquals(0, page.locator(".toast:has-text('Double Click Triggered')").count());
        });
  }

  @Test
  public void browserDoubleClickAfterDoubleTapDoesNotDuplicateToast() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!button",
        true,
        page -> {
          for (int delay : new int[] {0, 30}) {
            page.waitForSelector("body[data-showcase-page=button][data-showcase-state=rendered]");
            Locator button = page.locator("#events button").nth(2);
            button.scrollIntoViewIfNeeded();
            button.evaluate(
                """
              (element, delay) => {
                // Control the sequence here: Chromium already emits a native dblclick
                // for some touch taps, so adding another to real taps tests three events.
                const box = element.getBoundingClientRect();
                const touch = {identifier: 1, target: element,
                  clientX: box.x + 4, clientY: box.y + box.height / 2};
                for (let i = 0; i < 2; i++) {
                  for (const type of ['touchstart', 'touchend']) {
                    const event = new Event(type, {bubbles: true});
                    const touches = type === 'touchstart' ? [touch] : [];
                    Object.defineProperties(event, {touches: {value: touches},
                      targetTouches: {value: touches}, changedTouches: {value: [touch]}});
                    element.dispatchEvent(event);
                  }
                  element.dispatchEvent(new MouseEvent('click', {bubbles: true, detail: 1,
                    clientX: touch.clientX, clientY: touch.clientY}));
                }
                const emit = () => element.dispatchEvent(
                  new MouseEvent('dblclick', {bubbles: true, detail: 2}));
                if (delay) setTimeout(emit, delay); else emit();
              }
              """,
                delay);
            page.waitForSelector(".toast:has-text('Double Click Triggered')");
            page.waitForTimeout(60);
            assertEquals(1, page.locator(".toast:has-text('Double Click Triggered')").count());
            button.dblclick();
            assertEquals(2, page.locator(".toast:has-text('Double Click Triggered')").count());
            page.reload();
          }
        });
  }
}
