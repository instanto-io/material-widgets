package io.instanto.material.testing;

import static io.instanto.webapp.testkit.dom.Dom.*;
import static org.junit.Assert.*;

import io.instanto.cucumber.tea.CucumberSuite;
import io.instanto.cucumber.tea.Then;

@CucumberSuite("features/navigation-drawer.feature")
public class NavigationDrawerSteps extends MaterialSteps {
  @Then("the drawer reopens and its overlay closes it")
  public void drawerReopensAndOverlayClosesIt() {
    for (int pass = 0; pass < 2; pass++) {
      waitFor(() -> assertTrue(navigationIdle(app.page().root())));
      waitFor(() -> assertTrue(layout(find("#sidenavDrawer")).right() <= 1));
      watchOpened(app.page().root());
      click(find("nav .button-collapse"));
      waitFor(
          () -> assertEquals("true", app.page().root().getAttribute("data-test-drawer-opened")));
      waitFor(() -> assertEquals(0, layout(find("#sidenavDrawer")).left(), 1));
      click(find("#sidenav-overlay"));
      waitFor(() -> assertTrue(layout(find("#sidenavDrawer")).right() <= 1));
      waitFor(() -> assertTrue(findAll("#sidenav-overlay").isEmpty()));
    }
  }

  @org.teavm.jso.JSBody(
      params = "root",
      script =
          "return !root.querySelector('.side-nav.velocity-animating, #sidenav-overlay.velocity-animating');")
  private static native boolean navigationIdle(org.teavm.jso.dom.html.HTMLElement root);

  @org.teavm.jso.JSBody(
      params = "root",
      script =
          "root.removeAttribute('data-test-drawer-opened'); root.ownerDocument.defaultView.jQuery(root.querySelector('nav .button-collapse')).one('side-nav-opened',()=>root.setAttribute('data-test-drawer-opened','true'));")
  private static native void watchOpened(org.teavm.jso.dom.html.HTMLElement root);
}
