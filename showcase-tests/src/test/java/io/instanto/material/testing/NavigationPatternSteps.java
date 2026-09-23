package io.instanto.material.testing;

import static io.instanto.webapp.testkit.dom.Dom.*;
import static org.junit.Assert.*;

import io.instanto.cucumber.tea.CucumberSuite;
import io.instanto.cucumber.tea.Then;

@CucumberSuite("features/navigation-patterns.feature")
public class NavigationPatternSteps extends MaterialSteps {
  @Then("the pattern renders with local navigation and images")
  public void originalPatternRenders() {
    assertFalse(findAll("header nav").isEmpty());
    assertEquals("Back to showcase", find(".pattern-back").getTextContent());
    assertTrue(findAll("a[href*='gwtmaterialdesign' i]").isEmpty());
    waitFor(() -> assertTrue("Local images", imagesLoaded(app.page().root())));
  }

  @org.teavm.jso.JSBody(
      params = "root",
      script =
          "return Array.from(root.querySelectorAll('img')).every(i => i.complete && i.naturalWidth>0);")
  private static native boolean imagesLoaded(org.teavm.jso.dom.html.HTMLElement root);
}
