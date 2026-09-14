package io.instanto.material.testing;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;
import java.nio.file.Path;
import org.junit.Test;

public class ShowcaseThemeTest {
  @Test
  public void themeSupportsKeyboardReloadPatternsAndRestoration() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!button",
        page -> {
          page.waitForSelector("body[data-showcase-state=rendered]");
          assertEquals("light", page.locator("body").getAttribute("data-theme"));
          int styles = page.locator("head style").count();
          page.locator("#showcase-theme").focus();
          page.keyboard().press("Space");
          assertEquals("true", page.locator("#showcase-theme").getAttribute("aria-pressed"));
          page.screenshot(
              new Page.ScreenshotOptions().setPath(Path.of("target", "dark-buttons.png")));
          page.keyboard().press("Enter");
          assertEquals("false", page.locator("#showcase-theme").getAttribute("aria-pressed"));
          assertEquals(styles, page.locator("head style").count());
          page.locator("#showcase-theme").click();
          page.reload();
          page.waitForSelector("body[data-theme=dark][data-showcase-state=rendered]");
          page.locator("a[data-page=textfields]").click();
          page.waitForSelector("body[data-showcase-page=textfields]");
          assertEquals(
              "rgb(206, 214, 224)",
              page.locator(".input-field .prefix")
                  .first()
                  .evaluate("e => getComputedStyle(e).color"));
          page.screenshot(
              new Page.ScreenshotOptions().setPath(Path.of("target", "dark-textfields.png")));
          page.locator("a[data-page=table-standard]").click();
          page.waitForSelector("tbody tr.data-row");
          page.screenshot(
              new Page.ScreenshotOptions().setPath(Path.of("target", "dark-table.png")));
          page.navigate(page.url().split("#")[0] + "?pattern=sidenav_drawer");
          page.waitForSelector("body[data-pattern=sidenav_drawer][data-theme=dark]");
          page.locator("#showcase-theme").click();
          page.locator(".pattern-back").click();
          page.waitForSelector("body[data-theme=light][data-showcase-page=sidenavs]");
          page.setViewportSize(375, 812);
          assertTrue(page.locator("#showcase-theme").isVisible());
          var box = page.locator("#showcase-theme").boundingBox();
          assertTrue(box.x >= 0 && box.x + box.width <= 375);
        });
  }

  @Test
  public void themeStillWorksWithoutBrowserStorage() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!button",
        page -> {
          page.addInitScript(
              "Object.defineProperty(window,'localStorage',{get(){throw new Error('Storage unavailable');}})");
          page.reload();
          page.waitForSelector("body[data-showcase-state=rendered]");
          page.locator("#showcase-theme").click();
          assertEquals("dark", page.locator("body").getAttribute("data-theme"));
          page.locator("#showcase-theme").click();
          assertEquals("light", page.locator("body").getAttribute("data-theme"));
        });
  }
}
