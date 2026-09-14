package io.instanto.material.testing;

import static org.junit.Assert.*;

import io.instanto.webapp.testkit.app.ApplicationRule;
import io.instanto.webapp.testkit.dom.Dom;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class NavigationDrawerTest {
  @Rule
  public ApplicationRule app =
      new ApplicationRule("/resources/applications/material/index.html?pattern=sidenav_drawer")
          .readyWhen(page -> "sidenav_drawer".equals(page.root().getAttribute("data-pattern")))
          .sized(1280, 900);

  @Test
  public void drawerReopensAndOverlayClosesIt() {
    for (int pass = 0; pass < 2; pass++) {
      Dom.waitFor(() -> assertTrue(navigationIdle(app.application().page().root())));
      Dom.waitFor(() -> assertTrue(Dom.layout(Dom.find("#sidenavDrawer")).right() <= 1));
      watchOpened(app.application().page().root());
      Dom.click(Dom.find("nav .button-collapse"));
      Dom.waitFor(
          () ->
              assertEquals(
                  "true", app.application().page().root().getAttribute("data-test-drawer-opened")));
      Dom.waitFor(() -> assertEquals(0, Dom.layout(Dom.find("#sidenavDrawer")).left(), 1));
      Dom.click(Dom.find("#sidenav-overlay"));
      Dom.waitFor(() -> assertTrue(Dom.layout(Dom.find("#sidenavDrawer")).right() <= 1));
      Dom.waitFor(() -> assertTrue(Dom.findAll("#sidenav-overlay").isEmpty()));
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
