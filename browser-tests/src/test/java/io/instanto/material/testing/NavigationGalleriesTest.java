package io.instanto.material.testing;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;
import java.util.List;
import org.junit.Test;

/** Checks local navigation examples without links to unported external demos. */
public class NavigationGalleriesTest {
  @Test
  public void navigationPagesLinkToLocalPatternsAndSource() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!navbar",
        page -> {
          for (int pass = 0; pass < 2; pass++) {
            for (String token : List.of("navbar", "sidenavs")) {
              page.locator("a[data-page=" + token + "]").click();
              page.waitForSelector(
                  "body[data-showcase-page=" + token + "][data-showcase-state=rendered]");
              assertTrue(
                  page.locator(".catalogue-pattern-note")
                      .textContent()
                      .contains("running on TeaVM"));
              assertEquals(0, page.locator("#catalogue-content img:visible").count());
              int patterns = token.equals("navbar") ? 6 : 11;
              assertEquals(patterns, page.locator("a[href^='?pattern=']").count());
              assertEquals(patterns, page.locator("a[href^='pattern-sources/']").count());
              for (Locator link : page.locator("a[href^='pattern-sources/']").all()) {
                String url =
                    java.net.URI.create(page.url()).resolve(link.getAttribute("href")).toString();
                var response = page.request().get(url);
                assertEquals(url, 200, response.status());
                response.dispose();
              }
              assertEquals(
                  0,
                  page.locator(
                          "a[href*='gwtmaterialdesign.github.io'], a[href*='github.com/GwtMaterialDesign']")
                      .count());
              if (token.equals("navbar")) assertTrue(page.locator("#selection_event").count() > 0);
              else
                assertTrue(
                    page.locator("#catalogue-content")
                        .textContent()
                        .contains("SideNav Properties"));
            }
            page.locator("a[data-page=button]").click();
            page.waitForSelector("body[data-showcase-page=button]");
          }
        });
  }

  @Test
  public void originalNavbarSelectionHandlerFiresOnceAfterRemount() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!navbar",
        page -> {
          for (int pass = 0; pass < 2; pass++) {
            page.waitForSelector("body[data-showcase-page=navbar][data-showcase-state=rendered]");
            Locator section = page.locator("#selection_event");
            section.getByText("Account", new Locator.GetByTextOptions().setExact(true)).click();
            page.waitForSelector(".toast:has-text('0 Selected Index')");
            assertEquals(1, page.locator(".toast:has-text('0 Selected Index')").count());
            section.getByText("Refresh", new Locator.GetByTextOptions().setExact(true)).click();
            page.waitForSelector(".toast:has-text('1 Selected Index')");
            assertEquals(1, page.locator(".toast:has-text('1 Selected Index')").count());
            page.locator("a[data-page=button]").click();
            page.waitForSelector("body[data-showcase-page=button]");
            page.waitForFunction("document.querySelectorAll('.toast').length === 0");
            page.locator("a[data-page=navbar]").click();
          }
        });
  }
}
