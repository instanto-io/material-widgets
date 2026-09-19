package io.instanto.gwt.testing.api;

import static io.instanto.gwt.testing.api.ContractAssertions.*;

import com.google.gwt.dom.client.*;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.DOM;
import java.math.BigDecimal;

/** Portable checks for the number formatting and native table APIs used by Material Table. */
public final class MaterialTableContracts {
  private MaterialTableContracts() {}

  public static void numberPatternsPreserveValuesAndPrecision() {
    equal(
        "1,234.568",
        NumberFormat.getDecimalFormat().format(1234.5678),
        "Decimal grouping and rounding");
    equal("11%", NumberFormat.getPercentFormat().format(0.1126132), "Percent scaling");
    equal("-12.50", NumberFormat.getFormat("0.00").format(-12.5), "Negative fixed precision");
    equal(
        "12345678901234567890.12",
        NumberFormat.getFormat("0.00").format(new BigDecimal("12345678901234567890.12")),
        "BigDecimal precision");
    equal(
        "1,234.50",
        NumberFormat.getDecimalFormat().overrideFractionDigits(2).format(1234.5),
        "Fraction override");
    equal(1234.5, NumberFormat.getDecimalFormat().parse("1,234.5"), "Grouped parsing");
    equal(12.0, NumberFormat.getPercentFormat().parse("12%"), "GWT percent suffix parsing");
    int[] cursor = {2};
    equal(12.5, NumberFormat.getDecimalFormat().parse("xx12.5 tail", cursor), "Parse offset");
    equal(6, cursor[0], "Parse cursor");
    equal("", SafeHtmlUtils.EMPTY_SAFE_HTML.asString(), "Empty renderer value");
  }

  public static void tableRowsExposeLiveTypedCells() {
    Element table = Document.get().createTableElement();
    Element body = Document.get().createElement("tbody");
    table.appendChild(body);
    TableRowElement first = TableRowElement.as(DOM.createTR());
    TableRowElement second = TableRowElement.as(DOM.createTR());
    body.appendChild(first);
    body.appendChild(second);
    Element lookedUp = body.getFirstChildElement();
    equal(true, first.equals(lookedUp), "Native row identity survives DOM lookup");
    equal(first.hashCode(), lookedUp.hashCode(), "Equal native rows have equal hashes");
    equal(false, first.equals(second), "Different rows remain distinct");
    java.util.Map<Element, String> byRow = new java.util.HashMap<>();
    byRow.put(first, "first");
    equal("first", byRow.get(lookedUp), "Native row works as a map key after lookup");
    NodeList<TableCellElement> cells = first.getCells();
    first.appendChild(DOM.createTH());
    first.appendChild(DOM.createTD());
    equal(2, cells.getLength(), "Live cells include header and data cells");
    equal("TH", cells.getItem(0).getTagName(), "Header cell type");
    equal("TD", cells.getItem(1).getTagName(), "Data cell type");
    equal(1, second.getSectionRowIndex(), "Section row index");
    Element footer = Document.get().createTFootElement();
    table.appendChild(footer);
    TableRowElement last = TableRowElement.as(DOM.createTR());
    footer.appendChild(last);
    equal(2, last.getRowIndex(), "Table index across sections");
    equal(0, last.getSectionRowIndex(), "Footer row index");
    table.getStyle().setTableLayout(Style.TableLayout.FIXED);
    equal("fixed", table.getStyle().getProperty("tableLayout"), "Table layout");
    table.getStyle().clearTableLayout();
    equal("", table.getStyle().getProperty("tableLayout"), "Cleared table layout");
  }
}
