package io.instanto.material.testing;

import static io.instanto.webapp.testkit.dom.Dom.*;
import static org.junit.Assert.*;

import io.instanto.cucumber.tea.AfterScenario;
import io.instanto.cucumber.tea.Given;
import io.instanto.webapp.testkit.app.FramedApplication;

/** Shared lifecycle and navigation for browser scenarios against the built TeaVM showcase. */
public abstract class MaterialSteps {
  protected FramedApplication app;

  @Given("the Material showcase page {string} is open")
  public void openShowcase(String route) {
    open("/resources/applications/material/index.html#!" + route, "data-showcase-page", route);
  }

  @Given("the Material navigation pattern {string} is open")
  public void openPattern(String pattern) {
    open("/resources/applications/material/index.html?pattern=" + pattern, "data-pattern", pattern);
  }

  private void open(String url, String attribute, String expected) {
    app = FramedApplication.open(url, 1280, 900);
    app.awaitReady(page -> expected.equals(page.root().getAttribute(attribute)));
    use(app.page().root());
  }

  protected void navigate(String route) {
    click(find("a[data-page='" + route + "']"));
    waitFor(() -> assertEquals(route, app.page().root().getAttribute("data-showcase-page")));
  }

  @AfterScenario
  public void close() {
    try {
      if (app != null) app.close();
    } finally {
      reset();
    }
  }
}
