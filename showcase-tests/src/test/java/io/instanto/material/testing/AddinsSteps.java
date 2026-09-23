package io.instanto.material.testing;

import static io.instanto.webapp.testkit.dom.Dom.*;
import static org.junit.Assert.*;

import io.instanto.cucumber.tea.CucumberSuite;
import io.instanto.cucumber.tea.Then;
import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLElement;

@CucumberSuite("features/addins.feature")
public class AddinsSteps extends MaterialSteps {
  @Then("the original addin renders and survives remounting")
  public void rendersAndRemounts() {
    String route = app.page().root().getAttribute("data-showcase-page");
    assertFalse(find("#catalogue-content").getTextContent().isEmpty());
    waitFor(() -> assertTrue("Images on " + route, imagesLoaded(app.page().root())));
    navigate("button");
    navigate(route);
    waitFor(() -> assertTrue("Remounted images on " + route, imagesLoaded(app.page().root())));
  }

  @JSBody(
      params = "body",
      script =
          "return Array.prototype.every.call(body.querySelectorAll('#catalogue-content img[src]'),function(i){return !i.getAttribute('src') || (i.complete && i.naturalWidth>0);});")
  private static native boolean imagesLoaded(HTMLElement body);
}
