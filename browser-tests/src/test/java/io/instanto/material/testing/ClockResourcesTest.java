package io.instanto.material.testing;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Checks pointer and keyboard activation of both adapted upstream clock resources. */
public class ClockResourcesTest {
  @Test
  public void mouseClickAndKeyboardFocusKeepTheClockOpen() {
    Path jquery =
        Path.of(
            "../material-assets/target/classes/META-INF/resources/gwt-material/js/jquery-3.5.1.min.js");
    Path clock =
        Path.of(
            "../addins-teavm/target/classes/gwt/material/design/addins/client/timepicker/resources");
    try (Playwright playwright = Playwright.create();
        Browser browser = playwright.webkit().launch()) {
      for (String suffix : new String[] {".js", ".min.js"}) {
        try (Page page = browser.newPage(new Browser.NewPageOptions().setHasTouch(true))) {
          List<String> errors = new ArrayList<>();
          page.onPageError(errors::add);
          page.setContent(
              "<button id='before'>Before</button><input id='time' style='margin:80px;width:200px;height:40px'>");
          page.addScriptTag(new Page.AddScriptTagOptions().setPath(jquery));
          page.addStyleTag(
              new Page.AddStyleTagOptions().setPath(clock.resolve("css/timepicker.css")));
          page.addScriptTag(
              new Page.AddScriptTagOptions().setPath(clock.resolve("js/timepicker" + suffix)));
          // Preserve the original Material Java callback's blur behaviour.
          page.evaluate(
              "$('#time').lolliclock({beforeShow:function(){document.getElementById('time').blur();}})");
          page.locator("#time").click();
          page.waitForTimeout(400);
          assertEquals(true, page.evaluate("$('#time').data('lolliclock').isShown"));
          page.keyboard().press("Escape");
          waitForClosedClock(page);
          page.locator("#before").focus();
          page.keyboard().press("Tab");
          page.waitForTimeout(400);
          assertEquals(true, page.evaluate("$('#time').data('lolliclock').isShown"));
          page.keyboard().press("Escape");
          waitForClosedClock(page);
          page.locator("#time").tap();
          page.waitForTimeout(400);
          assertEquals(true, page.evaluate("$('#time').data('lolliclock').isShown"));
          page.keyboard().press("Escape");
          waitForClosedClock(page);
          page.evaluate("$('#time').lolliclock('remove')");
          page.locator("#time").tap();
          assertEquals(0, page.locator(".lolliclock-popover").count());
          assertEquals(suffix + " errors", List.of(), errors);
        }
      }
    }
  }

  private static void waitForClosedClock(Page page) {
    // Upstream releases the document listeners at the end of the closing animation.
    page.waitForFunction(
        "!$('#time').data('lolliclock').isShown && !$('#time').data('lolliclock').popover.is(':visible')");
  }
}
