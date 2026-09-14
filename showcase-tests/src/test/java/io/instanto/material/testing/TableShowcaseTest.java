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
public class TableShowcaseTest {
  @Test
  public void tableExpandsAndRestoresWithoutLeavingPageScrollLocked() {
    open("standard");
    double width = Dom.layout(Dom.find(".table-container")).width();
    Dom.click(Dom.find("#stretch"));
    Dom.waitFor(() -> assertTrue(fillsViewport(Dom.find(".table-container"))));
    Dom.click(Dom.find("#stretch"));
    Dom.waitFor(() -> assertEquals(width, Dom.layout(Dom.find(".table-container")).width(), 1));
    assertFalse(app.application().page().root().getClassName().contains("overflow-hidden"));
    Dom.click(Dom.find("#stretch"));
    Dom.click(Dom.find("a[data-page=button]"));
    Dom.waitFor(
        () ->
            assertEquals(
                "button", app.application().page().root().getAttribute("data-showcase-page")));
    assertFalse(app.application().page().root().getClassName().contains("overflow-hidden"));
  }

  @org.teavm.jso.JSBody(
      params = "table",
      script =
          "const r=table.getBoundingClientRect(), w=table.ownerDocument.defaultView; return Math.abs(r.x)<1 && Math.abs(r.y)<1 && Math.abs(r.width-w.innerWidth)<1 && Math.abs(r.height-w.innerHeight)<1;")
  private static native boolean fillsViewport(org.teavm.jso.dom.html.HTMLElement table);

  @Rule
  public ApplicationRule app =
      new ApplicationRule("/resources/applications/material/index.html#!button")
          .readyWhen(page -> "rendered".equals(page.root().getAttribute("data-showcase-state")))
          .sized(1280, 900);

  @Test
  public void originalTablesRenderAfterNavigationAndRemount() {
    for (int pass = 0; pass < 2; pass++) {
      for (String token :
          new String[] {"standard", "paged", "categorized", "frozen", "customized", "infinite"}) {
        open(token);
        assertTrue(token, Dom.findAll("#catalogue-content tbody tr.data-row").size() > 0);
        Dom.waitFor(
            () ->
                assertTrue(
                    "Local table images loaded", imagesLoaded(app.application().page().root())));
        assertTrue(Dom.findAll("#catalogue-content a[href*='gwtmaterialdesign' i]").isEmpty());
        Dom.click(Dom.find("a[data-page=button]"));
        Dom.waitFor(
            () ->
                assertEquals(
                    "button", app.application().page().root().getAttribute("data-showcase-page")));
      }
    }
  }

  @org.teavm.jso.JSBody(
      params = "root",
      script =
          "return Array.from(root.querySelectorAll('#catalogue-content img')).every(i => i.complete && i.naturalWidth > 0);")
  private static native boolean imagesLoaded(org.teavm.jso.dom.html.HTMLElement root);

  private void open(String token) {
    Dom.click(Dom.find("a[data-page='table-" + token + "']"));
    Dom.waitFor(
        () -> {
          assertEquals(
              "table-" + token, app.application().page().root().getAttribute("data-showcase-page"));
          assertTrue(
              "Rows for " + token, Dom.findAll("#catalogue-content tbody tr.data-row").size() > 0);
        },
        10_000);
  }

  @Test
  public void pagerMovesThroughAllRowsAndBack() {
    open("paged");
    assertEquals(5, Dom.findAll("tbody tr.data-row").size());
    String first = Dom.find("tbody tr.data-row:first-child").getTextContent();
    for (int page = 2; page <= 11; page++) {
      Dom.click(Dom.find(".action-page-panel .arrow-next"));
      String expected =
          page == 11 ? "51-52 of 52" : ((page - 1) * 5 + 1) + "-" + page * 5 + " of 52";
      Dom.waitFor(
          () -> assertEquals(expected, Dom.find(".action-page-panel > span").getTextContent()));
    }
    assertEquals(2, Dom.findAll("tbody tr.data-row").size());
    for (int page = 10; page >= 1; page--) {
      Dom.click(Dom.find(".action-page-panel .arrow-prev"));
    }
    Dom.waitFor(
        () -> assertEquals("1-5 of 52", Dom.find(".action-page-panel > span").getTextContent()));
    assertEquals(first, Dom.find("tbody tr.data-row:first-child").getTextContent());
  }

  @Test
  public void standardTableSortsAndAddsAndClearsRows() {
    open("standard");
    Dom.click(Dom.find("thead.tableFloatingHeaderOriginal th[id=col1]"));
    Dom.waitFor(
        () ->
            assertTrue(
                Dom.find("#catalogue-content")
                    .getTextContent()
                    .contains("ColumnSortEvent - Sorted:")));
    Dom.click(Dom.findByText("Add Row"));
    Dom.waitFor(() -> assertEquals(1, Dom.findAll("tbody tr.data-row").size()));
    Dom.click(Dom.findByText("Clear Table"));
    Dom.waitFor(() -> assertEquals(0, Dom.findAll("tbody tr.data-row").size()));
    Dom.click(Dom.findByText("Add All Rows"));
    Dom.waitFor(() -> assertTrue(Dom.findAll("tbody tr.data-row").size() > 0));
  }

  @Test
  public void columnsCanBeHiddenAndRestored() {
    open("paged");
    Dom.click(Dom.find("#columnToggle"));
    Dom.click(Dom.find("label[for$='-col0']"));
    Dom.waitFor(
        () ->
            assertEquals(
                "none",
                Dom.computedStyle(
                    Dom.find("tbody tr.data-row:first-child td[id=col0]"), "display")));
    Dom.click(Dom.find("label[for$='-col0']"));
    Dom.waitFor(
        () ->
            assertNotEquals(
                "none",
                Dom.computedStyle(
                    Dom.find("tbody tr.data-row:first-child td[id=col0]"), "display")));
  }

  @Test
  public void selectionAndDensityUseTheOriginalOptionHandlers() {
    open("standard");
    Dom.select(Dom.findAll("#catalogue-content select").get(0), "MULTIPLE");
    Dom.waitFor(() -> assertTrue(Dom.findAll("tbody td.selection input").size() > 0));
    Dom.click(Dom.findAll("tbody td.selection label").get(0));
    Dom.waitFor(() -> assertEquals(1, Dom.findAll("tbody tr.data-row.selected").size()));
    Dom.click(Dom.findAll("tbody td.selection label").get(0));
    Dom.waitFor(() -> assertEquals(0, Dom.findAll("tbody tr.data-row.selected").size()));
    double before = Dom.layout(Dom.findAll("tbody tr.data-row").get(0)).height();
    Dom.select(Dom.findAll("#catalogue-content select").get(1), "COMPACT");
    Dom.waitFor(
        () -> assertTrue(Dom.layout(Dom.findAll("tbody tr.data-row").get(0)).height() < before));
  }

  @Test
  public void categorizedRowsOpenAndCloseThroughTheOriginalControls() {
    open("categorized");
    Dom.click(Dom.findByText("Open All"));
    Dom.waitFor(() -> assertTrue(visibleRows(app.application().page().root()) > 0));
    Dom.click(Dom.findByText("Close All"));
    Dom.waitFor(() -> assertEquals(0, visibleRows(app.application().page().root())));
    Dom.click(Dom.findByText("Open All"));
    Dom.waitFor(() -> assertTrue(visibleRows(app.application().page().root()) > 0));
  }

  @Test
  public void infiniteTableLoadsAnotherWindowOfLocalRows() {
    open("infinite");
    String first = Dom.findAll("tbody tr.data-row").get(0).getTextContent();
    assertTrue(Dom.findAll("tbody tr.data-row").size() < 125);
    scrollTable(Dom.find(".table-body"));
    Dom.waitFor(
        () -> {
          assertTrue(Dom.findAll("tbody tr.data-row").size() > 0);
          assertNotEquals(first, Dom.findAll("tbody tr.data-row").get(0).getTextContent());
        },
        5_000);
    assertTrue(
        "Infinite view keeps a bounded row window", Dom.findAll("tbody tr.data-row").size() < 125);
  }

  @org.teavm.jso.JSBody(
      params = "root",
      script =
          "return Array.from(root.querySelectorAll('tbody tr.data-row')).filter(r => r.getClientRects().length && r.ownerDocument.defaultView.getComputedStyle(r).display !== 'none').length;")
  private static native int visibleRows(org.teavm.jso.dom.html.HTMLElement root);

  @org.teavm.jso.JSBody(
      params = "body",
      script =
          "body.scrollTop = Math.min(body.scrollHeight-body.clientHeight, 2800); body.dispatchEvent(new body.ownerDocument.defaultView.Event('scroll')); ")
  private static native void scrollTable(org.teavm.jso.dom.html.HTMLElement body);
}
