package io.instanto.material.testing;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.Test;

/** Exercises retained upstream examples and the replacement catalogue shell in WebKit. */
public class ShowcaseTest {
  @Test
  public void originalTabsSelectAddResizeAndResetOnNavigation() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!tabs",
        page -> {
          page.waitForSelector("body[data-showcase-page=tabs][data-showcase-state=rendered]");
          for (int pass = 0; pass < 2; pass++) {
            assertEquals(2, page.locator("#dynamic_tabs li.tab").count());
            page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Add other Tab").setExact(true))
                .click();
            assertEquals(3, page.locator("#dynamic_tabs li.tab").count());
            page.locator("#dynamic_tabs a[href='#dynamicTab3']").click();
            page.waitForSelector("#dynamicTab3:visible");
            assertEquals("Content 3", page.locator("#dynamicTab3").textContent());
            page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Set Tab Index to 2").setExact(true))
                .click();
            page.waitForFunction(
                "document.querySelectorAll('#set_tab_index_method li.tab a')[1].classList.contains('active')");
            page.setViewportSize(1000, 800);
            page.waitForFunction(
                "Array.from(document.querySelectorAll('#dynamic_tabs .indicator')).some(e => e.getBoundingClientRect().width > 0)");
            page.locator("a[data-page=button]").click();
            page.waitForSelector("body[data-showcase-page=button]");
            assertEquals(0, page.locator("#dynamicTab3").count());
            page.locator("a[data-page=tabs]").click();
            page.waitForSelector("body[data-showcase-page=tabs]");
          }
        });
  }

  @Test
  public void originalCollectionAndLoaderHandlersWork() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!collection",
        page -> {
          page.waitForSelector("body[data-showcase-page=collection]");
          page.getByText("Wifi", new Page.GetByTextOptions().setExact(true)).click();
          page.waitForSelector(".toast:has-text('Wifi Network')");
          page.locator("a[data-page=loaders]").click();
          page.waitForSelector("body[data-showcase-page=loaders]");
          Locator panel = page.locator("#specific_panels");
          Locator loader =
              panel.getByRole(
                  AriaRole.BUTTON,
                  new Locator.GetByRoleOptions().setName("Show Loader").setExact(true));
          Locator progress =
              panel.getByRole(
                  AriaRole.BUTTON,
                  new Locator.GetByRoleOptions().setName("Show Progress").setExact(true));
          loader.click();
          assertFalse(progress.isEnabled());
          page.waitForFunction(
              "Array.from(document.querySelectorAll('#specific_panels button')).find(b => b.textContent === 'Show Progress').disabled === false");
          progress.click();
          assertFalse(loader.isEnabled());
          page.waitForFunction(
              "Array.from(document.querySelectorAll('#specific_panels button')).find(b => b.textContent === 'Show Loader').disabled === false");
        });
  }

  @Test
  public void originalPagesRenderAndCanBeRemounted() throws Exception {
    BaselineTest.verify(
        System.getProperty("showcase.module", "showcase-teavm"),
        "/",
        page -> {
          page.waitForSelector("body[data-showcase-page=button]");
          String[] pages = {
            "animation",
            "badge",
            "breadcrumb",
            "button",
            "cards",
            "checkbox",
            "chips",
            "collapsible",
            "collection",
            "color",
            "datePicker",
            "dialogs",
            "dropdown",
            "fab",
            "footer",
            "icon",
            "listbox",
            "layout",
            "loaders",
            "media",
            "pushpin",
            "radioButton",
            "range",
            "scrollspy",
            "search",
            "security",
            "shadow",
            "tabs",
            "textfields",
            "errors",
            "navbar",
            "sidenavs",
            "switches"
          };
          for (int pass = 0; pass < 2; pass++) {
            for (String token : pages) {
              page.locator("a[data-page='" + token + "']").click();
              page.waitForSelector(
                  "body[data-showcase-page='" + token + "'][data-showcase-state=rendered]");
              assertTrue(
                  token, page.locator("#catalogue-content").textContent().trim().length() > 20);
              assertEquals(
                  token + " has no upstream demo links",
                  0,
                  page.locator("a[href*='gwtmaterialdesign' i]").count());
              if (java.util.Set.of("cards", "collection", "layout", "loaders", "shadow")
                  .contains(token)) {
                page.waitForFunction(
                    "(() => { const images = Array.from(document.querySelectorAll('#catalogue-content img[src$=\".webp\"]')); return images.length > 0 && images.every(image => image.complete && image.naturalWidth > 0); })()");
              }
              if ("media".equals(token)) {
                // Finish loading the embedded sample before this catalogue sweep navigates away.
                page.waitForFunction(
                    "(() => { const frame = document.querySelector('#responsive_videos iframe'); const doc = frame && frame.contentDocument; return doc && doc.querySelector('video') && doc.readyState === 'complete' && Array.from(document.querySelectorAll('#catalogue-content img')).every(image => image.complete && image.naturalWidth > 0); })()");
              }
              assertEquals(
                  "Current page",
                  "page",
                  page.locator("a[data-page='" + token + "']").getAttribute("aria-current"));
            }
          }
        });
  }

  @Test
  public void originalButtonHandlersStillRunAfterNavigation() throws Exception {
    BaselineTest.verify(
        System.getProperty("showcase.module", "showcase-teavm"),
        "/#!button",
        page -> {
          page.waitForSelector("body[data-showcase-page=button]");
          for (int pass = 0; pass < 2; pass++) {
            assertTrue(
                "Source examples retain line breaks",
                page.locator("#raised pre").textContent().contains("\n"));
            page.locator("#events button").nth(0).click();
            assertTrue(page.locator("#events button").nth(0).textContent().contains("Clicked"));
            page.locator("#events button").nth(2).dblclick();
            assertTrue(
                page.locator("#events button").nth(2).textContent().contains("Double Clicked"));
            assertFalse(page.locator("#disabled button").nth(0).isEnabled());
            page.locator("a[data-page=checkbox]").click();
            page.waitForSelector("body[data-showcase-page=checkbox]");
            page.locator("a[data-page=button]").click();
            page.waitForSelector("body[data-showcase-page=button]");
            assertTrue(page.locator("#events button").nth(0).textContent().contains("Click Me"));
          }
        });
  }

  @Test
  public void originalCheckboxAndDialogHandlersWork() throws Exception {
    BaselineTest.verify(
        System.getProperty("showcase.module", "showcase-teavm"),
        "/#!checkbox",
        page -> {
          page.waitForSelector("body[data-showcase-page=checkbox]");
          page.getByText("Check all", new Page.GetByTextOptions().setExact(true)).click();
          assertEquals(6, page.locator("#check_box .col").nth(1).locator("input:checked").count());
          page.getByText("Check all", new Page.GetByTextOptions().setExact(true)).click();
          assertEquals(0, page.locator("#check_box .col").nth(1).locator("input:checked").count());
          page.locator("a[data-page=dialogs]").click();
          page.waitForSelector("body[data-showcase-page=dialogs]");
          page.locator("#dialogs")
              .getByRole(
                  AriaRole.BUTTON,
                  new Locator.GetByRoleOptions().setName("Show Dialog").setExact(true))
              .click();
          page.waitForSelector(".modal:visible");
          page.locator(".modal:visible")
              .getByRole(
                  AriaRole.BUTTON,
                  new Locator.GetByRoleOptions().setName("Close Dialog").setExact(true))
              .click();
          page.locator(".modal:visible")
              .waitFor(
                  new Locator.WaitForOptions()
                      .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));
          page.waitForFunction("document.querySelectorAll('.lean-overlay').length === 0");
          page.locator("a[data-page=button]").click();
          page.waitForSelector("body[data-showcase-page=button]");
        });
  }

  @Test
  public void catalogueSupportsSearchHistoryAndMedia() throws Exception {
    BaselineTest.verify(
        System.getProperty("showcase.module", "showcase-teavm"),
        "/#!button",
        page -> {
          page.waitForSelector("body[data-showcase-page=button]");
          page.getByLabel("Find a component").fill("check");
          assertTrue(page.locator("a[data-page=checkbox]").isVisible());
          assertFalse(page.locator("a[data-page=button]").isVisible());
          page.getByLabel("Find a component").fill("");
          page.locator("a[data-page=media]").click();
          page.waitForSelector("body[data-showcase-page=media][data-showcase-state=rendered]");
          assertEquals(0, page.locator(".pending-page").count());
          page.goBack();
          page.waitForSelector("body[data-showcase-page=button]");
        });
  }

  @Test
  public void smallScreensKeepThePageScrollableAndNavigationAvailable() throws Exception {
    BaselineTest.verify(
        System.getProperty("showcase.module", "showcase-teavm"),
        "/#!button",
        page -> {
          page.setViewportSize(390, 844);
          page.waitForSelector("body[data-showcase-page=button]");
          page.getByRole(
                  AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Toggle component menu"))
              .click();
          page.locator("a[data-page=checkbox]").click();
          page.waitForSelector("body[data-showcase-page=checkbox]");
          assertFalse(page.locator("body").getAttribute("class").contains("menu-open"));
          assertEquals(
              "pan-y", page.locator("body").evaluate("e => getComputedStyle(e).touchAction"));
          assertEquals(true, page.evaluate("document.documentElement.scrollHeight > innerHeight"));
          assertEquals(true, page.evaluate("document.documentElement.scrollWidth <= innerWidth"));
          page.mouse().move(320, 700);
          page.mouse().wheel(0, 650);
          page.waitForFunction("window.scrollY > 100");
        });
  }
}
