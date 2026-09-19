package io.instanto.material.testing;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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
  public void infiniteTableSupportsRemoteSortAndCategoryFilter() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!table-infinite",
        page -> {
          page.waitForSelector("tbody tr.data-row");
          page.locator("thead th[id=col1]").click();
          page.waitForFunction(
              "() => {const names = Array.from(document.querySelectorAll('tbody tr.data-row td[id=col1]')).map(c => c.textContent.trim()).filter(Boolean); return names.length > 1;}");
          List<String> ascending =
              page.locator("tbody tr.data-row td[id=col1]").allTextContents().stream()
                  .map(String::trim)
                  .filter(value -> !value.isEmpty())
                  .collect(Collectors.toList());
          List<String> expectedAscending = new ArrayList<>(ascending);
          expectedAscending.sort(String.CASE_INSENSITIVE_ORDER);
          assertEquals(expectedAscending, ascending);
          page.locator("thead th[id=col1]").click();
          page.waitForFunction(
              "() => {const names = Array.from(document.querySelectorAll('tbody tr.data-row td[id=col1]')).map(c => c.textContent.trim()).filter(Boolean); return names.length > 1;}");
          List<String> descending =
              page.locator("tbody tr.data-row td[id=col1]").allTextContents().stream()
                  .map(String::trim)
                  .filter(value -> !value.isEmpty())
                  .collect(Collectors.toList());
          List<String> expectedDescending = new ArrayList<>(descending);
          expectedDescending.sort(String.CASE_INSENSITIVE_ORDER);
          Collections.reverse(expectedDescending);
          assertEquals(expectedDescending, descending);
        });

    BaselineTest.verify(
        "showcase-teavm",
        "/?categoryFilter=Category%201#!table-infinite",
        page -> {
          page.waitForSelector("tbody tr.data-row");
          page.locator(".table-body")
              .evaluate(
                  "el => { el.scrollTop = Math.max(0, el.scrollHeight - el.clientHeight); el.dispatchEvent(new Event('scroll')); }");
          page.waitForTimeout(500);
          assertTrue(page.locator("tbody tr.data-row").count() <= 30);
        });
  }

  @Test
  public void frozenColumnsRemainPinnedDuringHorizontalScroll() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!table-frozen",
        page -> {
          page.waitForSelector("tbody tr.data-row");
          Locator firstCell = page.locator("tbody tr.data-row").first().locator("td").first();
          Locator midCell = page.locator("tbody tr.data-row").first().locator("td").nth(4);
          assertTrue(firstCell.count() > 0);
          assertTrue(midCell.count() > 0);
          page.locator(".table-body")
              .evaluate("el => { el.scrollLeft = 0; el.dispatchEvent(new Event('scroll')); }");
          double firstBefore = firstCell.boundingBox().x;
          double midBefore = midCell.boundingBox().x;
          page.locator(".table-body")
              .evaluate(
                  "el => { el.scrollLeft = el.scrollWidth; el.dispatchEvent(new Event('scroll')); }");
          page.waitForFunction("() => document.querySelector('.table-body').scrollLeft > 0");
          page.waitForTimeout(350);
          double firstAfter = firstCell.boundingBox().x;
          double midAfter = midCell.boundingBox().x;
          assertTrue(Math.abs(firstAfter - firstBefore) < 1.0);
          assertTrue(Math.abs(midAfter - midBefore) > 20.0);
        });
  }

  @Test
  public void tableSelectionCanBeToggledByKeyboardAndTouch() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!table-standard",
        page -> {
          page.locator("#catalogue-content select").first().selectOption("MULTIPLE");
          page.waitForSelector("tbody td.selection input");
          Locator rowInput = page.locator("tbody td.selection input").first();
          rowInput.focus();
          page.keyboard().press("Space");
          page.waitForFunction(
              "() => document.querySelectorAll('tbody tr.data-row.selected').length === 1");
          page.keyboard().press("Space");
          page.waitForFunction(
              "() => document.querySelectorAll('tbody tr.data-row.selected').length === 0");
          rowInput.tap();
          page.waitForFunction(
              "() => document.querySelectorAll('tbody tr.data-row.selected').length === 1");
        });
  }

  @Test
  public void infiniteTableFailureModeSurfacesErrorState() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/?serviceFailure=true#!table-infinite",
        page -> {
          page.waitForSelector("body[data-showcase-page=table-infinite]");
          page.waitForTimeout(900);
          assertEquals(0, page.locator("tbody tr.data-row").count());
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
