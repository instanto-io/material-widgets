package io.instanto.material.testing;

import static io.instanto.webapp.testkit.dom.Dom.*;
import static org.junit.Assert.*;

import io.instanto.cucumber.tea.CucumberSuite;
import io.instanto.cucumber.tea.Then;

@CucumberSuite("features/showcase-theme.feature")
public class ShowcaseThemeSteps extends MaterialSteps {
  @Then("dark theme covers the shell and table and returns to light")
  public void themeCoversShellAndTableAndReturnsToLight() {
    assertEquals("false", find("#showcase-theme").getAttribute("aria-pressed"));
    try {
      click(find("#showcase-theme"));
      assertEquals("true", find("#showcase-theme").getAttribute("aria-pressed"));
      assertEquals(
          "rgb(41, 47, 54)", computedStyle(find("#catalogue-toolbar"), "background-color"));
      click(find("a[data-page=table-standard]"));
      waitFor(() -> assertTrue(findAll("tbody tr.data-row").size() > 0));
      assertEquals("rgb(66, 66, 66)", computedStyle(find(".table-body"), "background-color"));
      click(find("#stretch"));
      assertEquals("rgb(66, 66, 66)", computedStyle(find(".table-container"), "background-color"));
      click(find("#stretch"));
    } finally {
      if ("true".equals(find("#showcase-theme").getAttribute("aria-pressed")))
        click(find("#showcase-theme"));
    }
    assertEquals(
        "rgb(255, 255, 255)", computedStyle(find("#catalogue-toolbar"), "background-color"));
    assertEquals("false", find("#showcase-theme").getAttribute("aria-pressed"));
  }
}
