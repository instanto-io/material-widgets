package io.instanto.material.testing;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.FilePayload;
import java.nio.file.Path;
import org.junit.Test;

/** Native browser checks supplement the TeaVM/Testkit scenarios. */
public class AddinsTest {
  @Test
  public void floatingWindowOpensAndCloses() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!addins-window",
        page -> {
          page.waitForSelector(
              "body[data-showcase-page=addins-window][data-showcase-state=rendered]");
          page.getByText("Open Window", new Page.GetByTextOptions().setExact(true)).click();
          page.waitForSelector(".window.open");
          page.locator(".window.open .window-action").first().click();
          page.waitForFunction("() => document.querySelectorAll('.window.open').length === 0");
          assertEquals(0, page.locator(".window.open").count());
        });
  }

  @Test
  public void carouselMethodButtonsChangeSlide() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!addins-carousel",
        page -> {
          page.waitForSelector(
              "body[data-showcase-page=addins-carousel][data-showcase-state=rendered]");
          page.getByText("Go to 2nd slide", new Page.GetByTextOptions().setExact(true)).click();
          page.waitForSelector(".slick-slider .slick-current[data-slick-index='1']");
          page.getByText("Get Current Slide Index", new Page.GetByTextOptions().setExact(true))
              .click();
          assertTrue(
              page.locator(".toast").allTextContents().stream()
                  .anyMatch(text -> text.contains("1 Current Slide Index")));
        });
  }

  @Test
  public void richEditorClearsAndInsertsText() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!addins-richeditor",
        page -> {
          page.waitForSelector(
              "body[data-showcase-page=addins-richeditor][data-showcase-state=rendered]");
          page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Reset")).click();
          page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Get Value")).click();
          page.waitForSelector(".toast:has-text('Empty')");
          page.getByRole(
                  AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Insert Material Design"))
              .click();
          page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Get Value")).click();
          page.waitForSelector(".toast:has-text('Material Design')");
        });
  }

  @Test
  public void treeViewExpandsAndSelectsOriginalItems() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!addins-treeview",
        page -> {
          page.waitForSelector(
              "body[data-showcase-page=addins-treeview][data-showcase-state=rendered]");
          Locator tree = page.locator("#catalogue-content .tree");
          int allItems = tree.locator(".tree-item").count();
          assertTrue(allItems > 3);
          page.locator("#catalogue-content [data-tooltip='Collapse']").click();
          page.waitForFunction(
              "total => Array.from(document.querySelectorAll('#catalogue-content .tree-item')).filter(item => item.getClientRects().length).length < total",
              allItems);
          page.locator("#catalogue-content [data-tooltip='Expand']").click();
          page.waitForFunction(
              "total => Array.from(document.querySelectorAll('#catalogue-content .tree-item')).filter(item => item.getClientRects().length).length === total",
              allItems);
          page.locator("#catalogue-content .tree > .tree-item:first-child > .tree-header").click();
          page.waitForSelector(".toast:has-text('Selected : Documents')");
          assertEquals(1, tree.locator(".tree-item.selected").count());
        });
  }

  @Test
  public void allAddinsRenderAndRemount() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!button",
        page -> {
          page.waitForSelector("a[data-page=addins-cropper]");
          var routes =
              page.locator("a[data-page^=addins-]")
                  .evaluateAll("links=>links.map(a=>a.getAttribute('data-page'))");
          assertEquals(34, ((java.util.List<?>) routes).size());
          for (Object route : (java.util.List<?>) routes) {
            for (int pass = 0; pass < 2; pass++) {
              page.locator("a[data-page='" + route + "']").click();
              page.waitForSelector(
                  "body[data-showcase-page='" + route + "'][data-showcase-state=rendered]");
              assertFalse(page.locator("#catalogue-content").textContent().isBlank());
              page.waitForFunction(
                  "Array.from(document.querySelectorAll('#catalogue-content img[src]')).every(i=>!i.getAttribute('src') || (i.complete && i.naturalWidth>0))");
              page.locator("a[data-page=button]").click();
              page.waitForSelector("body[data-showcase-page=button]");
            }
          }
        });
  }

  @Test
  public void signatureAcceptsNativePointerInput() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!addins-signature",
        page -> {
          var canvas = page.locator("#catalogue-content canvas").first();
          canvas.scrollIntoViewIfNeeded();
          var rect = canvas.boundingBox();
          page.mouse().move(rect.x + 20, rect.y + 20);
          page.mouse().down();
          page.mouse().move(rect.x + 100, rect.y + 70, new Mouse.MoveOptions().setSteps(6));
          page.mouse().up();
          page.waitForSelector("#catalogue-content :text('End Signature Event fired')");
          page.getByText("Get Image Data", new Page.GetByTextOptions().setExact(true)).click();
          page.waitForSelector(".modal img[src^='data:image/png;base64,']");
          page.getByText("Close", new Page.GetByTextOptions().setExact(true)).click();
        });
  }

  @Test
  public void cropperExportsAnImage() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!addins-cropper",
        page -> {
          page.waitForSelector(".croppie-container");
          page.waitForFunction("document.querySelector('.cr-image').style.opacity === '1'");
          if (Boolean.getBoolean("material.capture"))
            page.screenshot(
                new Page.ScreenshotOptions()
                    .setPath(Path.of("target/addins-cropper.png"))
                    .setFullPage(true));
          page.getByText("Crop", new Page.GetByTextOptions().setExact(true)).click();
          page.waitForSelector(".modal img[src^='data:image/']");
          page.getByText("Close", new Page.GetByTextOptions().setExact(true)).click();
        });
  }

  @Test
  public void uploaderQueuesWithoutSending() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!addins-fileuploader",
        page -> {
          page.waitForSelector(
              "input.dz-hidden-input",
              new Page.WaitForSelectorOptions()
                  .setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
          page.locator("input.dz-hidden-input")
              .first()
              .setInputFiles(
                  new FilePayload(
                      "sample.txt",
                      "text/plain",
                      "Local sample".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
          page.waitForSelector("[data-dz-name]:has-text('sample.txt')");
          page.waitForFunction(
              "Dropzone.instances.some(d=>d.options.autoProcessQueue===false && d.getQueuedFiles().length===1)");
        });
  }
}
