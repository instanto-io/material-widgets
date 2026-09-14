package io.instanto.material.testing;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Checks the adapted debug and minified upstream plugins independently of the Java launcher. */
public class TabResourcesTest {
  @Test
  public void reinitialisationKeepsOneIndicatorAndPreservesApplicationHandlers() {
    Path assets = Path.of("../material-assets/target/classes/META-INF/resources/gwt-material/js");
    try (Playwright playwright = Playwright.create();
        Browser browser = playwright.webkit().launch()) {
      for (String suffix : new String[] {".js", ".min.js"}) {
        try (Page page = browser.newPage()) {
          List<String> errors = new ArrayList<>();
          page.onPageError(errors::add);
          page.setContent(
              "<ul id='tabs' class='tabs'><li class='tab'><a href='#one'>One</a></li>"
                  + "<li class='tab'><a href='#two'>Two</a></li></ul><div id='one'>First</div><div id='two'>Second</div>");
          page.addScriptTag(
              new Page.AddScriptTagOptions().setPath(assets.resolve("jquery-3.5.1.min.js")));
          page.addScriptTag(
              new Page.AddScriptTagOptions()
                  .setPath(assets.resolve("materialize-0.97.5" + suffix)));
          page.waitForSelector(
              "#tabs .indicator",
              new Page.WaitForSelectorOptions()
                  .setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
          page.evaluate(
              "window.applicationClicks=0; $('#tabs').on('click.application','a',function(){window.applicationClicks++;});");
          for (int pass = 0; pass < 4; pass++) {
            page.evaluate(
                "$('#tabs').tabs(); $('#tabs a[href=\"#two\"]').trigger('click'); $('#tabs').tabs(); $(window).trigger('resize');");
            assertEquals(1, page.locator("#tabs .indicator").count());
            assertTrue(page.locator("#two").isVisible());
          }
          // Allow the plugin's 90 ms delayed animations and 300 ms tweens to finish.
          page.waitForTimeout(500);
          assertEquals(4, ((Number) page.evaluate("window.applicationClicks")).intValue());
          assertEquals(suffix + " browser errors", List.of(), errors);
        }
      }
    }
  }
}
