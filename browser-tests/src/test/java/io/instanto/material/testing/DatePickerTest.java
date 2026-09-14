package io.instanto.material.testing;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;
import java.nio.file.Path;
import org.junit.Test;

public class DatePickerTest {
  @Test
  public void navigatingAwayDisposesAnOpenBodyPicker() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!button",
        page -> {
          page.setDefaultTimeout(5000);
          page.locator("a[data-page=datePicker]").click();
          page.waitForSelector("body[data-showcase-page=datePicker]");
          Locator input = page.locator("#self_or_body_container input.picker__input").nth(1);
          input.evaluate("e => e.scrollIntoView({block:'center'})");
          var box = input.boundingBox();
          page.mouse().click(box.x + box.width / 2, box.y + box.height / 2);
          page.waitForSelector(".picker--opened .picker__day");
          page.goBack();
          page.waitForSelector("body[data-showcase-page=button]");
          assertEquals(0, page.locator(".picker").count());
          assertNotEquals(
              "hidden", page.locator("html").evaluate("e => getComputedStyle(e).overflow"));
        });
  }

  @Test
  public void everyEnabledPickerOpensAndSelectsADate() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!datePicker",
        page -> {
          page.setDefaultTimeout(5000);
          page.waitForSelector("body[data-showcase-page=datePicker]");
          Locator fields = page.locator("#catalogue-content input.picker__input:not([disabled])");
          page.locator("a[data-page=button]").click();
          page.waitForSelector("body[data-showcase-page=button]");
          page.locator("a[data-page=datePicker]").click();
          page.waitForSelector("body[data-showcase-page=datePicker]");
          page.waitForFunction(
              "document.querySelectorAll('#catalogue-content input.picker__input:not([disabled])').length === 15");
          assertTrue(page.locator("#disabled_styles input.picker__input").isDisabled());
          assertTrue(
              page.locator("#years_to_display input.picker__input").inputValue().contains("1950"));
          for (boolean dark : new boolean[] {false, true}) {
            if (dark) page.locator("#showcase-theme").click();
            for (int index = 0; index < fields.count(); index++) {
              Locator field = fields.nth(index);
              String section = (String) field.evaluate("e => e.closest('.scrollspy').id");
              field.evaluate("e => e.scrollIntoView({block:'center'})");
              var bounds = field.boundingBox();
              page.mouse().click(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
              page.waitForSelector(".picker--opened .picker__day");
              page.waitForFunction(
                  "document.querySelector('.picker--opened .picker__holder').getBoundingClientRect().top < 1 && Number(getComputedStyle(document.querySelector('.picker--opened .picker__frame')).opacity) > 0.99");
              String selectedYear = null;
              if (section.equals("date_picker_selection_types")) {
                selectedYear =
                    page.locator(".picker--opened .picker__select--year option")
                        .first()
                        .getAttribute("value");
                page.locator(".picker--opened .picker__select--year").selectOption(selectedYear);
              }
              page.screenshot(
                  new Page.ScreenshotOptions()
                      .setPath(Path.of("target", "datepicker-" + index + ".png")));
              Locator day =
                  page.locator(".picker--opened .picker__day--infocus:not(.picker__day--disabled)")
                      .first();
              day.click();
              assertFalse(section, field.inputValue().isBlank());
              if (selectedYear != null) assertTrue(field.inputValue().contains(selectedYear));
              if (section.equals("date_limit")) assertTrue(field.inputValue().contains("2017"));
              if (page.locator(".picker--opened").count() > 0) {
                page.locator(".picker--opened .picker__close").click();
              }
              page.waitForFunction("!document.querySelector('.picker--opened')");
              page.waitForFunction(
                  "Array.from(document.querySelectorAll('.picker__holder')).every(e=>e.getBoundingClientRect().height < 1)");
            }
          }
        });
  }

  @Test
  public void programmaticOpenAndDateEventsKeepTheirOriginalBehaviour() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!datePicker",
        page -> {
          page.setDefaultTimeout(5000);
          page.waitForSelector("body[data-showcase-page=datePicker]");
          page.getByText("Open DatePicker", new Page.GetByTextOptions().setExact(true)).click();
          page.waitForSelector(".picker--opened .picker__day");
          page.locator(".picker--opened .picker__day--infocus:not(.picker__day--disabled)")
              .first()
              .click();
          page.waitForFunction("!document.querySelector('.picker--opened')");
          assertFalse(
              page.locator("#open_and_close_control input.picker__input").inputValue().isBlank());
          String toasts = String.join(" ", page.locator(".toast").allTextContents());
          assertTrue(toasts, toasts.contains("Date Selected "));
          assertFalse(toasts, toasts.contains("Date Selected null"));
          assertFalse(toasts, toasts.contains("Closed Date Picker with value null"));
        });
  }
}
