package io.instanto.material.testing;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.Test;

/** Exercises the original text-field handlers and the selected addins in WebKit. */
public class TextFieldsTest {
  @Test
  public void originalContactOracleSelectsAnAutocompleteChip() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!textfields",
        page -> {
          page.waitForSelector("body[data-showcase-page=textfields][data-showcase-state=rendered]");
          Locator autocomplete = page.locator("#field_types_and_states .autocomplete");
          // The upstream template deliberately uses display=NONE. Reveal only for this interaction
          // check.
          assertFalse(autocomplete.isVisible());
          autocomplete.evaluate("e => e.style.display = 'block'");
          autocomplete
              .locator("input")
              .pressSequentially("Luis", new Locator.PressSequentiallyOptions().setDelay(30));
          autocomplete
              .locator(".dropdown-item")
              .filter(new Locator.FilterOptions().setHasText("Luis Hoppe"))
              .click();
          assertTrue(autocomplete.locator(".chip").textContent().contains("Luis Hoppe"));
          assertEquals("", autocomplete.locator("input").inputValue());
          page.locator("a[data-page=button]").click();
          page.waitForSelector("body[data-showcase-page=button]");
          assertEquals(0, page.locator(".multiValueSuggestBox-list").count());
        });
  }

  private static Locator button(Locator section, String name) {
    return section.getByRole(
        AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name).setExact(true));
  }

  @Test
  public void originalValuesEventsAndRemountWork() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!textfields",
        page -> {
          for (int pass = 0; pass < 2; pass++) {
            page.waitForSelector(
                "body[data-showcase-page=textfields][data-showcase-state=rendered]");
            Locator values = page.locator("#setting_value");
            assertEquals("Some Value", values.locator("input").first().inputValue());
            Locator setters = button(values, "Set Value");
            Locator events = button(values, "Set Value with Event");
            setters.nth(0).click();
            assertEquals("Text Box", values.locator("input").first().inputValue());
            events.nth(0).click();
            assertEquals("Text Box 2", values.locator("input").first().inputValue());
            page.waitForSelector(".toast:has-text('Value Text Box 2')");
            events.nth(1).click();
            assertEquals("Text Area 2", values.locator("textarea").inputValue());
            double[] numbers = {1000, 1000, 10.5, 10.5};
            for (int n = 0; n < numbers.length; n++) {
              events.nth(n + 2).click();
              assertEquals(
                  numbers[n],
                  Double.parseDouble(values.locator("input").nth(n + 1).inputValue()),
                  0.0001);
            }
            values.locator("input").first().fill("Typed value");
            button(values, "Get Value").first().click();
            page.waitForSelector(".toast:has-text('Typed value')");
            page.locator("a[data-page=button]").click();
            page.waitForSelector("body[data-showcase-page=button]");
            page.locator("a[data-page=textfields]").click();
          }
        });
  }

  @Test
  public void fieldStatesValidationAndSensitivityWork() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!textfields",
        page -> {
          page.waitForSelector("body[data-showcase-page=textfields][data-showcase-state=rendered]");
          Locator states = page.locator("#field_types_and_states");
          Locator text = states.locator(".row").nth(1).locator("input").first();
          button(states, "Validate").click();
          assertTrue(
              text.evaluate(
                      "e => e.classList.contains('invalid') || e.parentElement.classList.contains('invalid')")
                  .equals(true));
          states
              .locator("select")
              .nth(1)
              .selectOption(
                  new com.microsoft.playwright.options.SelectOption().setLabel("READ_ONLY"),
                  new Locator.SelectOptionOptions().setForce(true));
          assertTrue((Boolean) text.evaluate("e => e.readOnly"));
          states
              .locator("select")
              .nth(1)
              .selectOption("DEFAULT", new Locator.SelectOptionOptions().setForce(true));
          assertFalse((Boolean) text.evaluate("e => e.readOnly"));
          states
              .locator("select")
              .nth(1)
              .selectOption("DISABLED", new Locator.SelectOptionOptions().setForce(true));
          assertFalse(text.isEnabled());
          states
              .locator("select")
              .nth(1)
              .selectOption("DEFAULT", new Locator.SelectOptionOptions().setForce(true));
          assertTrue(text.isEnabled());
          text.fill("Reset me");
          button(states, "Reset").click();
          assertEquals("", text.inputValue());
          Locator sensitive = page.locator("#field_sensitivity");
          sensitive.getByText("Sensitive", new Locator.GetByTextOptions().setExact(true)).click();
          assertEquals("password", sensitive.locator("input").first().getAttribute("type"));
          sensitive.getByText("Sensitive", new Locator.GetByTextOptions().setExact(true)).click();
          assertEquals("text", sensitive.locator("input").first().getAttribute("type"));
          Locator nulls = page.locator("#returnvalueasnull");
          button(nulls, "Get Value").click();
          page.waitForSelector(".toast:has-text('Value is null')");
          nulls.locator(".lever").click();
          nulls.locator(".lever").click();
          button(nulls, "Get Value").click();
          page.waitForSelector(".toast:has-text('Value is empty')");
        });
  }

  @Test
  public void inputMaskComboBoxesAndTimePickerWork() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!textfields",
        page -> {
          page.waitForSelector("body[data-showcase-page=textfields][data-showcase-state=rendered]");
          Locator states = page.locator("#field_types_and_states");
          Locator time = states.locator(".timepicker input");
          time.click();
          page.waitForSelector(".lolliclock-popover:visible");
          // Clock hands cover the ticks; wait for the dial animation before clicking their
          // coordinates.
          page.waitForTimeout(400);
          page.locator(".lolliclock-popover:visible .lolliclock-tick")
              .filter(
                  new Locator.FilterOptions().setHasText(java.util.regex.Pattern.compile("^3$")))
              .first()
              .click(new Locator.ClickOptions().setForce(true));
          page.waitForSelector(".lolliclock-dial-minutes:not(.lolliclock-dial-out)");
          page.waitForTimeout(400);
          page.locator(".lolliclock-popover:visible .lolliclock-tick")
              .filter(
                  new Locator.FilterOptions().setHasText(java.util.regex.Pattern.compile("^15$")))
              .first()
              .click(new Locator.ClickOptions().setForce(true));
          page.locator(".lolliclock-popover:visible .lolliclock-button")
              .filter(new Locator.FilterOptions().setHasText("OK"))
              .click();
          assertTrue(time.inputValue(), time.inputValue().matches("0?3:15 (AM|PM)"));
          Locator mask = states.getByPlaceholder("eg: 0000-0000-0000-0000");
          mask.pressSequentially(
              "1234567890123456", new Locator.PressSequentiallyOptions().setDelay(30));
          assertEquals("1234-5678-9012-3456", mask.inputValue());
          Locator combo = states.locator(".select2-selection--single");
          combo.click();
          page.locator(".select2-results__option:visible")
              .filter(new Locator.FilterOptions().setHasText("Option 2"))
              .click();
          assertTrue(combo.textContent().contains("Option 2"));
          Locator multiple = states.locator(".select2-selection--multiple");
          multiple.click();
          page.locator(".select2-results__option:visible")
              .filter(new Locator.FilterOptions().setHasText("Option 1"))
              .click();
          assertTrue(multiple.textContent().contains("Option 1"));
          page.locator("a[data-page=button]").click();
          page.waitForSelector("body[data-showcase-page=button]");
          assertEquals(0, page.locator(".select2-container--open").count());
          assertEquals(0, page.locator(".lolliclock-popover:visible").count());
        });
  }
}
