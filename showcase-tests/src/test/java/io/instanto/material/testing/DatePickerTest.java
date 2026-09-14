package io.instanto.material.testing;

import static org.junit.Assert.*;

import io.instanto.webapp.testkit.app.ApplicationRule;
import io.instanto.webapp.testkit.dom.Dom;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class DatePickerTest {
  @Rule
  public ApplicationRule app =
      new ApplicationRule("/resources/applications/material/index.html#!datePicker")
          .readyWhen(page -> "datePicker".equals(page.root().getAttribute("data-showcase-page")))
          .sized(1280, 900);

  @Test
  public void datePickersSelectValuesAfterRemount() {
    for (int pass = 0; pass < 2; pass++) {
      // The translation example restarts its native picker on the next scheduler turn.
      Dom.waitFor(
          () ->
              assertEquals(
                  15,
                  Dom.findAll("#catalogue-content input.picker__input:not([disabled])").size()));
      var fields = Dom.findAll("#catalogue-content input.picker__input:not([disabled])");
      assertEquals(15, fields.size());
      assertTrue(
          ((HTMLInputElement) Dom.find("#years_to_display input.picker__input"))
              .getValue()
              .contains("1950"));
      for (var field : fields) {
        Dom.click(field);
        Dom.waitFor(() -> assertFalse(Dom.findAll(".picker--opened .picker__day").isEmpty()));
        Dom.click(
            Dom.findAll(".picker--opened .picker__day--infocus:not(.picker__day--disabled)")
                .get(0));
        assertFalse(((HTMLInputElement) field).getValue().isEmpty());
        if (!Dom.findAll(".picker--opened").isEmpty())
          Dom.click(Dom.find(".picker--opened .picker__close"));
        Dom.waitFor(() -> assertTrue(Dom.findAll(".picker--opened").isEmpty()));
      }
      assertTrue(
          ((HTMLInputElement) Dom.find("#date_limit input.picker__input"))
              .getValue()
              .contains("2017"));
      Dom.click(Dom.find("a[data-page=button]"));
      Dom.waitFor(
          () ->
              assertEquals(
                  "button", app.application().page().root().getAttribute("data-showcase-page")));
      assertTrue(Dom.findAll(".picker").isEmpty());
      Dom.click(Dom.find("a[data-page=datePicker]"));
      Dom.waitFor(
          () ->
              assertEquals(
                  "datePicker",
                  app.application().page().root().getAttribute("data-showcase-page")));
    }
  }

  @Test
  public void selectedDateReachesTheOriginalValueHandler() {
    Dom.click(Dom.find("#open_and_close_control input.picker__input"));
    Dom.waitFor(() -> assertFalse(Dom.findAll(".picker--opened .picker__day").isEmpty()));
    Dom.click(
        Dom.findAll(".picker--opened .picker__day--infocus:not(.picker__day--disabled)").get(0));
    Dom.waitFor(() -> assertTrue(Dom.findAll(".picker--opened").isEmpty()));
    String messages = "";
    for (var toast : Dom.findAll(".toast")) messages += toast.getTextContent();
    assertTrue(messages, messages.contains("Date Selected "));
    assertFalse(messages, messages.contains("Date Selected null"));
    assertFalse(messages, messages.contains("Closed Date Picker with value null"));
  }
}
