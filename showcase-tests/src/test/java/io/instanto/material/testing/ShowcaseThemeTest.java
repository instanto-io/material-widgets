package io.instanto.material.testing;

import static org.junit.Assert.*;

import io.instanto.webapp.testkit.app.ApplicationRule;
import io.instanto.webapp.testkit.dom.Dom;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class ShowcaseThemeTest {
  @Rule
  public ApplicationRule app =
      new ApplicationRule("/resources/applications/material/index.html#!button")
          .readyWhen(page -> "rendered".equals(page.root().getAttribute("data-showcase-state")))
          .sized(1280, 900);

  @Test
  public void themeCoversShellAndTableAndReturnsToLight() {
    assertEquals("false", Dom.find("#showcase-theme").getAttribute("aria-pressed"));
    try {
      Dom.click(Dom.find("#showcase-theme"));
      assertEquals("true", Dom.find("#showcase-theme").getAttribute("aria-pressed"));
      assertEquals(
          "rgb(41, 47, 54)", Dom.computedStyle(Dom.find("#catalogue-toolbar"), "background-color"));
      Dom.click(Dom.find("a[data-page=table-standard]"));
      Dom.waitFor(() -> assertTrue(Dom.findAll("tbody tr.data-row").size() > 0));
      assertEquals(
          "rgb(66, 66, 66)", Dom.computedStyle(Dom.find(".table-body"), "background-color"));
      Dom.click(Dom.find("#stretch"));
      assertEquals(
          "rgb(66, 66, 66)", Dom.computedStyle(Dom.find(".table-container"), "background-color"));
      Dom.click(Dom.find("#stretch"));
    } finally {
      if ("true".equals(Dom.find("#showcase-theme").getAttribute("aria-pressed")))
        Dom.click(Dom.find("#showcase-theme"));
    }
    assertEquals(
        "rgb(255, 255, 255)",
        Dom.computedStyle(Dom.find("#catalogue-toolbar"), "background-color"));
    assertEquals("false", Dom.find("#showcase-theme").getAttribute("aria-pressed"));
  }
}
