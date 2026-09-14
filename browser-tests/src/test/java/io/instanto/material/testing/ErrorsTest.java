package io.instanto.material.testing;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.Test;

/** Exercises the original error-label handlers and validators in WebKit. */
public class ErrorsTest {
  private static Locator button(Locator section, String name) {
    return section.getByRole(
        AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name).setExact(true));
  }

  private static void ready(Page page) {
    page.waitForSelector("body[data-showcase-page=errors][data-showcase-state=rendered]");
  }

  private static void message(Locator section, String kind, String expected) {
    // Upstream ComboBox keeps a separate helper label alongside its status label.
    String selector =
        kind.equals("helper") && "combo_box".equals(section.getAttribute("id"))
            ? ".help-description:visible"
            : ".field-" + kind + "-label:visible";
    Locator label = section.locator(selector);
    assertEquals(expected, label.textContent());
    for (String other : new String[] {"error", "success", "helper"}) {
      if (!other.equals(kind)) {
        assertEquals(0, section.locator(".field-" + other + "-label:visible").count());
      }
    }
  }

  @Test
  public void statusLabelsSwitchClearAndSurviveRemount() throws Exception {
    String[][] examples = {
      {"autocomplete", "autocomplete.", "for autocomplete", "autocomplete"},
      {"date_picker", "date picker.", "for date picker.", "date picker."},
      {"time_picker", "time picker.", "for time picker.", "time picker."},
      {"range", "range.", "for range.", "range"},
      {"switch", "switch.", "for switch.", "switch"},
      {"text_area", "text area.", "text area.", "text area."},
      {"text_box", "text box.", "for text box.", "text box."},
      {"combo_box", "ComboBox.", "for ComboBox.", "ComboBox."},
      {"list_box", "ListBox.", "for ListBox.", "ListBox."}
    };
    BaselineTest.verify(
        "showcase-teavm",
        "/#!errors",
        page -> {
          for (int pass = 0; pass < 2; pass++) {
            ready(page);
            for (String[] example : examples) {
              Locator section = page.locator("#" + example[0]);
              assertEquals(
                  0,
                  section
                      .locator(
                          ".field-error-label:visible, .field-success-label:visible, .field-helper-label:visible")
                      .count());
              button(section, "Error").click();
              message(section, "error", "This is an error message for " + example[1]);
              button(section, "Success").click();
              message(section, "success", "This is a success message " + example[2]);
              button(section, "Clear").click();
              assertEquals(
                  0,
                  section
                      .locator(".field-error-label:visible, .field-success-label:visible")
                      .count());
              button(section, "Helper").click();
              String helper = "This is a helper text for " + example[3];
              message(section, "helper", helper);
              button(section, "Error").click();
              message(section, "error", "This is an error message for " + example[1]);
              button(section, "Clear").click();
              // Upstream clearStatusText restores configured helper text.
              message(section, "helper", helper);
            }
            page.locator("a[data-page=button]").click();
            page.waitForSelector("body[data-showcase-page=button]");
            assertEquals(0, page.locator("#catalogue-content .field-error-label:visible").count());
            page.locator("a[data-page=errors]").click();
          }
        });
  }

  @Test
  public void originalEmailValidatorHandlesBlurAndCorrection() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!errors",
        page -> {
          ready(page);
          Locator section = page.locator("#validation");
          Locator email = section.locator("input");
          button(section, "Validate").click();
          // Upstream gives the email validator priority over the blank validator.
          message(section, "error", "Not a valid email address.");
          email.fill("not-an-email");
          email.press("Tab");
          message(section, "error", "Not a valid email address.");
          email.fill("carl@example.org");
          button(section, "Validate").click();
          assertEquals(
              0, section.locator(".field-error-label:visible, .field-error, .invalid").count());
          email.fill("invalid-again");
          button(section, "Validate").click();
          message(section, "error", "Not a valid email address.");
        });
  }

  @Test
  public void requiredLabelsAppearAndTextAndNumericValuesClearThem() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!errors",
        page -> {
          ready(page);
          Locator section = page.locator("#required");
          button(section, "Validate").click();
          Locator errors = section.locator(".field-error-label:visible");
          assertEquals(8, errors.count());
          for (String text : errors.allTextContents()) assertEquals("Field cannot be blank", text);
          section.locator("input").nth(0).fill("Present");
          section.locator("textarea").fill("Present too");
          for (int index = 1; index <= 4; index++) section.locator("input").nth(index).fill("12");
          button(section, "Validate").click();
          assertEquals(2, errors.count());
          section.locator("input").nth(0).fill("");
          section.locator("input").nth(0).press("Tab");
          assertEquals(3, errors.count());
        });
  }
}
