package io.instanto.material.testing;

import static org.junit.Assert.*;

import org.junit.Test;

/** Exercises original Material widgets compiled to JavaScript by TeaVM. */
public class TeaVmWidgetsTest {
  @Test
  public void originalWidgetsAcceptInputAndDispatchClicks() throws Exception {
    BaselineTest.verify(
        System.getProperty("material.widget.module", "showcase-teavm"),
        "/?fixture",
        page -> {
          page.waitForSelector("body[data-ready=true]");
          page.locator("#material-name input").fill("TeaVM User");
          page.locator("#material-greet").click();
          assertEquals("Hello TeaVM User", page.locator("#material-result").textContent());
          page.locator("#material-name input").fill("Again");
          page.locator("#material-greet").click();
          assertEquals("Hello Again", page.locator("#material-result").textContent());
        });
  }
}
