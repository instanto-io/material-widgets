package io.instanto.material.testing;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;
import java.nio.file.*;
import org.junit.Test;

public class TablesTest {
  @Test
  public void tableExpandsToTheViewportAndRestoresItsLayout() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!table-standard",
        page -> {
          page.waitForSelector("tbody tr.data-row");
          double width = page.locator(".table-container").boundingBox().width;
          page.locator("#stretch").click();
          page.waitForFunction(
              "(() => {const r=document.querySelector('.table-container').getBoundingClientRect(); return Math.abs(r.x)<1 && Math.abs(r.y)<1 && Math.abs(r.width-innerWidth)<1 && Math.abs(r.height-innerHeight)<1;})()");
          page.locator("#stretch").click();
          assertEquals(width, page.locator(".table-container").boundingBox().width, 1);
          assertFalse(page.locator("body").getAttribute("class").contains("overflow-hidden"));
        });
  }

  @Test
  public void columnMenuTogglesTheOriginalCells() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!table-paged",
        page -> {
          page.waitForSelector("tbody tr.data-row");
          page.locator("#columnToggle").click();
          page.locator("label[for$='-col0']").click();
          page.waitForFunction(
              "getComputedStyle(document.querySelector('tbody tr.data-row td[id=col0]')).display === 'none'");
        });
  }

  @Test
  public void originalTableViewsRenderAndRemount() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!button",
        page -> {
          java.util.List<String> consoleErrors = new java.util.ArrayList<>();
          page.onConsoleMessage(
              message -> {
                if (message.type().equals("error") || message.type().equals("warning"))
                  consoleErrors.add(message.text());
              });
          for (int pass = 0; pass < 2; pass++) {
            for (String token :
                new String[] {
                  "standard", "paged", "categorized", "frozen", "customized", "infinite"
                }) {
              page.locator("a[data-page='table-" + token + "']").click();
              page.waitForSelector(
                  "body[data-showcase-page='table-" + token + "'][data-showcase-state=rendered]");
              try {
                page.waitForFunction(
                    "document.querySelectorAll('#catalogue-content tbody tr.data-row').length > 0");
              } catch (RuntimeException e) {
                try {
                  Files.writeString(Path.of("target/table-" + token + ".html"), page.content());
                } catch (Exception ignored) {
                }
                throw e;
              }
              assertEquals(
                  0, page.locator("#catalogue-content a[href*='gwtmaterialdesign' i]").count());
              page.waitForFunction(
                  "Array.from(document.querySelectorAll('#catalogue-content img')).every(i => i.complete && i.naturalWidth > 0)");
              if (pass == 0)
                try {
                  Files.writeString(Path.of("target/table-" + token + ".html"), page.content());
                } catch (Exception failure) {
                  throw new RuntimeException(failure);
                }
              if (pass == 0)
                page.screenshot(
                    new Page.ScreenshotOptions()
                        .setPath(Path.of("target/table-" + token + ".png"))
                        .setFullPage(true));
              page.locator("a[data-page=button]").click();
              page.waitForSelector("body[data-showcase-page=button]");
              assertEquals("Table setup and plugin errors", java.util.List.of(), consoleErrors);
            }
          }
        });
  }
}
