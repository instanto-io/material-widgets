package io.instanto.material.testing;

import com.microsoft.playwright.Page;

/** Waits for the original Materialbox opening animation before testing Escape. */
final class LightboxAssertions {
  private LightboxAssertions() {}

  static void awaitOpen(Page page, String imageSelector) {
    page.waitForFunction(
        "selector => { const image=document.querySelector(selector);"
            + " const overlay=document.querySelector('#materialbox-overlay');"
            + " return image && overlay && image.classList.contains('active')"
            + " && !image.classList.contains('velocity-animating')"
            + " && !overlay.classList.contains('velocity-animating'); }",
        imageSelector);
  }
}
