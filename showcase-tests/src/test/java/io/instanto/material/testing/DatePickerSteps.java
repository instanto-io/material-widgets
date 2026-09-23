package io.instanto.material.testing;

import static io.instanto.webapp.testkit.dom.Dom.*;
import static org.junit.Assert.*;

import io.instanto.cucumber.tea.CucumberSuite;
import io.instanto.cucumber.tea.Then;
import org.teavm.jso.dom.html.HTMLInputElement;

@CucumberSuite("features/date-pickers.feature")
public class DatePickerSteps extends MaterialSteps {
  @Then("each date picker selects a value after remounting")
  public void datePickersSelectValuesAfterRemount() {
    for (int pass = 0; pass < 2; pass++) {
      // The translation example restarts its native picker on the next scheduler turn.
      waitFor(
          () ->
              assertEquals(
                  15, findAll("#catalogue-content input.picker__input:not([disabled])").size()));
      var fields = findAll("#catalogue-content input.picker__input:not([disabled])");
      assertEquals(15, fields.size());
      assertTrue(
          ((HTMLInputElement) find("#years_to_display input.picker__input"))
              .getValue()
              .contains("1950"));
      for (var field : fields) {
        click(field);
        waitFor(() -> assertFalse(findAll(".picker--opened .picker__day").isEmpty()));
        click(findAll(".picker--opened .picker__day--infocus:not(.picker__day--disabled)").get(0));
        assertFalse(((HTMLInputElement) field).getValue().isEmpty());
        if (!findAll(".picker--opened").isEmpty()) click(find(".picker--opened .picker__close"));
        waitFor(() -> assertTrue(findAll(".picker--opened").isEmpty()));
      }
      assertTrue(
          ((HTMLInputElement) find("#date_limit input.picker__input")).getValue().contains("2017"));
      click(find("a[data-page=button]"));
      waitFor(() -> assertEquals("button", app.page().root().getAttribute("data-showcase-page")));
      assertTrue(findAll(".picker").isEmpty());
      click(find("a[data-page=datePicker]"));
      waitFor(
          () -> assertEquals("datePicker", app.page().root().getAttribute("data-showcase-page")));
    }
  }

  @Then("the selected date reaches the original value handler")
  public void selectedDateReachesTheOriginalValueHandler() {
    click(find("#open_and_close_control input.picker__input"));
    waitFor(() -> assertFalse(findAll(".picker--opened .picker__day").isEmpty()));
    click(findAll(".picker--opened .picker__day--infocus:not(.picker__day--disabled)").get(0));
    waitFor(() -> assertTrue(findAll(".picker--opened").isEmpty()));
    String messages = "";
    for (var toast : findAll(".toast")) messages += toast.getTextContent();
    assertTrue(messages, messages.contains("Date Selected "));
    assertFalse(messages, messages.contains("Date Selected null"));
    assertFalse(messages, messages.contains("Closed Date Picker with value null"));
  }
}
