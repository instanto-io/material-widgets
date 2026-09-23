package io.instanto.material.testing;

import static io.instanto.webapp.testkit.dom.Dom.*;
import static org.junit.Assert.*;

import io.instanto.cucumber.tea.CucumberSuite;
import io.instanto.cucumber.tea.Then;

@CucumberSuite("features/tables.feature")
public class TableSteps extends MaterialSteps {
  private static final String DATA_ROWS = "tbody tr.data-row";
  private static final String TABLE_VIEW = ".table-container";
  private static final int PAGE_SIZE = 5;
  private static final int PAGED_FIXTURE_ROWS = 52;
  private static final int INFINITE_FIXTURE_ROWS = 125;

  @Then("expansion fills the viewport and restores page scrolling")
  public void tableExpandsAndRestoresWithoutLeavingPageScrollLocked() {
    open("standard");
    double width = layout(find(TABLE_VIEW)).width();
    click(find("#stretch"));
    waitFor(() -> assertTrue(fillsViewport(find(TABLE_VIEW))));
    click(find("#stretch"));
    waitFor(() -> assertEquals(width, layout(find(TABLE_VIEW)).width(), 1));
    assertFalse(app.page().root().getClassName().contains("overflow-hidden"));
    click(find("#stretch"));
    navigate("button");
    assertFalse(app.page().root().getClassName().contains("overflow-hidden"));
  }

  @org.teavm.jso.JSBody(
      params = "table",
      script =
          "const r=table.getBoundingClientRect(), w=table.ownerDocument.defaultView; return Math.abs(r.x)<1 && Math.abs(r.y)<1 && Math.abs(r.width-w.innerWidth)<1 && Math.abs(r.height-w.innerHeight)<1;")
  private static native boolean fillsViewport(org.teavm.jso.dom.html.HTMLElement table);

  @Then("the original {string} table renders after remounting")
  public void originalTableRendersAfterRemount(String token) {
    for (int pass = 0; pass < 2; pass++) {
      open(token);
      assertFalse("Rows for " + token, findAll(DATA_ROWS).isEmpty());
      waitFor(() -> assertTrue("Local table images loaded", imagesLoaded(app.page().root())));
      assertTrue(findAll("#catalogue-content a[href*='gwtmaterialdesign' i]").isEmpty());
      navigate("button");
    }
  }

  @org.teavm.jso.JSBody(
      params = "root",
      script =
          "return Array.from(root.querySelectorAll('#catalogue-content img')).every(i => i.complete && i.naturalWidth > 0);")
  private static native boolean imagesLoaded(org.teavm.jso.dom.html.HTMLElement root);

  private void open(String token) {
    navigate("table-" + token);
    waitFor(() -> assertFalse("Rows for " + token, findAll(DATA_ROWS).isEmpty()), 10_000);
  }

  @Then("the pager reaches the last row and returns to the first")
  public void pagerMovesThroughAllRowsAndBack() {
    open("paged");
    assertEquals(PAGE_SIZE, findAll(DATA_ROWS).size());
    String first = find(DATA_ROWS + ":first-child").getTextContent();
    int lastPage = (PAGED_FIXTURE_ROWS + PAGE_SIZE - 1) / PAGE_SIZE;
    for (int page = 2; page <= lastPage; page++) {
      click(find(".action-page-panel .arrow-next"));
      int firstRow = (page - 1) * PAGE_SIZE + 1;
      int lastRow = Math.min(page * PAGE_SIZE, PAGED_FIXTURE_ROWS);
      String expected = firstRow + "-" + lastRow + " of " + PAGED_FIXTURE_ROWS;
      waitFor(() -> assertEquals(expected, find(".action-page-panel > span").getTextContent()));
    }
    assertEquals(PAGED_FIXTURE_ROWS % PAGE_SIZE, findAll(DATA_ROWS).size());
    for (int page = lastPage - 1; page >= 1; page--) {
      click(find(".action-page-panel .arrow-prev"));
    }
    waitFor(() -> assertEquals("1-5 of 52", find(".action-page-panel > span").getTextContent()));
    assertEquals(first, find(DATA_ROWS + ":first-child").getTextContent());
  }

  @Then("the original pager responds to keyboard controls")
  public void pagerRespondsToKeyboardControls() {
    open("paged");
    assertEquals("1-5 of 52", find(".action-page-panel > span").getTextContent());
    press(find(".action-page-panel .arrow-next"), "Enter");
    waitFor(() -> assertEquals("6-10 of 52", find(".action-page-panel > span").getTextContent()));
    press(find(".action-page-panel .arrow-prev"), "Enter");
    waitFor(() -> assertEquals("1-5 of 52", find(".action-page-panel > span").getTextContent()));
  }

  @Then("sorting and row controls update the standard table")
  public void standardTableSortsAndAddsAndClearsRows() {
    open("standard");
    click(find("thead.tableFloatingHeaderOriginal th[id=col1]"));
    waitFor(
        () ->
            assertTrue(
                find("#catalogue-content").getTextContent().contains("ColumnSortEvent - Sorted:")));
    click(findByText("Add Row"));
    waitFor(() -> assertEquals(1, findAll("tbody tr.data-row").size()));
    click(findByText("Clear Table"));
    waitFor(() -> assertEquals(0, findAll("tbody tr.data-row").size()));
    click(findByText("Add All Rows"));
    waitFor(() -> assertTrue(findAll("tbody tr.data-row").size() > 0));
  }

  @Then("the first column can be hidden and restored")
  public void columnsCanBeHiddenAndRestored() {
    open("paged");
    click(find("#columnToggle"));
    click(find("label[for$='-col0']"));
    waitFor(
        () ->
            assertEquals(
                "none",
                computedStyle(find("tbody tr.data-row:first-child td[id=col0]"), "display")));
    click(find("label[for$='-col0']"));
    waitFor(
        () ->
            assertNotEquals(
                "none",
                computedStyle(find("tbody tr.data-row:first-child td[id=col0]"), "display")));
  }

  @Then("selection and density use the original option handlers")
  public void selectionAndDensityUseTheOriginalOptionHandlers() {
    open("standard");
    select(findAll("#catalogue-content select").get(0), "MULTIPLE");
    waitFor(() -> assertTrue(findAll("tbody td.selection input").size() > 0));
    click(findAll("tbody td.selection label").get(0));
    waitFor(() -> assertEquals(1, findAll("tbody tr.data-row.selected").size()));
    click(findAll("tbody td.selection label").get(0));
    waitFor(() -> assertEquals(0, findAll("tbody tr.data-row.selected").size()));
    double before = layout(findAll("tbody tr.data-row").get(0)).height();
    select(findAll("#catalogue-content select").get(1), "COMPACT");
    waitFor(() -> assertTrue(layout(findAll("tbody tr.data-row").get(0)).height() < before));
  }

  @Then("categorized rows can be opened and closed")
  public void categorizedRowsOpenAndCloseThroughTheOriginalControls() {
    open("categorized");
    click(findByText("Open All"));
    waitFor(() -> assertTrue(visibleRows(app.page().root()) > 0));
    click(findByText("Close All"));
    waitFor(() -> assertEquals(0, visibleRows(app.page().root())));
    click(findByText("Open All"));
    waitFor(() -> assertTrue(visibleRows(app.page().root()) > 0));
  }

  @Then("scrolling loads another bounded row window")
  public void infiniteTableLoadsAnotherWindowOfLocalRows() {
    open("infinite");
    String first = findAll("tbody tr.data-row").get(0).getTextContent();
    assertTrue(findAll(DATA_ROWS).size() < INFINITE_FIXTURE_ROWS);
    scrollTable(find(".table-body"));
    waitFor(
        () -> {
          assertTrue(findAll("tbody tr.data-row").size() > 0);
          assertNotEquals(first, findAll("tbody tr.data-row").get(0).getTextContent());
        },
        5_000);
    assertTrue(
        "Infinite view keeps a bounded row window",
        findAll(DATA_ROWS).size() < INFINITE_FIXTURE_ROWS);
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
