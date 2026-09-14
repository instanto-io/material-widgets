package io.instanto.material.testing;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Exercises both resource variants and preserves unrelated application listeners. */
public class MediaResourcesTest {
  @Test
  public void lightboxesCloseWithoutOverflowAncestorsAndCanBeReinitialised() {
    Path assets = Path.of("../material-assets/target/classes/META-INF/resources/gwt-material/js");
    try (Playwright playwright = Playwright.create();
        Browser browser = playwright.webkit().launch()) {
      for (String suffix : new String[] {".js", ".min.js"}) {
        try (Page page = browser.newPage()) {
          List<String> errors = new ArrayList<>();
          page.onPageError(errors::add);
          page.setContent(
              "<img id='photo' class='materialboxed' data-caption='Original caption' style='width:100px;height:100px' src='data:image/gif;base64,R0lGODlhAQABAIABAP///wAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw=='>");
          page.addScriptTag(
              new Page.AddScriptTagOptions().setPath(assets.resolve("jquery-3.5.1.min.js")));
          page.addScriptTag(
              new Page.AddScriptTagOptions()
                  .setPath(assets.resolve("materialize-0.97.5" + suffix)));
          page.waitForSelector("#photo.initialized");
          page.evaluate(
              "window.applicationClicks=0; $('#photo').on('click.application',()=>applicationClicks++)");
          for (int pass = 0; pass < 2; pass++) {
            page.locator("#photo").click();
            page.waitForTimeout(350);
            page.keyboard().press("Escape");
            page.waitForFunction(
                "!document.querySelector('#materialbox-overlay,.materialbox-caption')");
            assertFalse(page.locator("#photo").getAttribute("class").contains("active"));
            page.evaluate(
                "$('#photo').data('instantoMaterialBoxDispose')(); $('#photo').materialbox()");
            assertEquals(1, page.locator(".material-placeholder").count());
          }
          assertEquals(2, ((Number) page.evaluate("applicationClicks")).intValue());
          assertEquals(List.of(), errors);
        }
      }
    }
  }
}
