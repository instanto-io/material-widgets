package io.instanto.material.testing;

import static org.junit.Assert.*;

import io.instanto.webapp.testkit.app.FramedApplication;
import io.instanto.webapp.testkit.dom.Dom;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class NavigationPatternsTest {
  @Test
  public void originalFullPagePatternsStartWithLocalResources() throws Throwable {
    for (String token :
        new String[] {
          "navbar_default",
          "navbar_fixed",
          "navbar_tall",
          "navbar_extend",
          "navbar_tab",
          "navbar_shrink",
          "navbar_tab_push",
          "sidenav_fixed",
          "sidenav_drawer",
          "sidenav_drawer_header",
          "sidenav_push",
          "sidenav_push_header",
          "sidenav_card",
          "sidenav_mini",
          "sidenav_mini_expandable",
          "sidenav_edge",
          "sidenav_colaps",
          "sidenav_content"
        }) {
      var application =
          FramedApplication.open(
                  "/resources/applications/material/index.html?pattern=" + token, 1280, 900)
              .awaitReady(page -> token.equals(page.root().getAttribute("data-pattern")));
      Throwable failure = null;
      try {
        Dom.use(application.page().root());
        assertTrue(token, Dom.findAll("header nav").size() > 0);
        assertEquals("Back to showcase", Dom.find(".pattern-back").getTextContent());
        assertTrue(token, Dom.findAll("a[href*='gwtmaterialdesign' i]").isEmpty());
        Dom.waitFor(
            () -> assertTrue(token + " local images", imagesLoaded(application.page().root())));
      } catch (Throwable error) {
        failure = error;
      } finally {
        try {
          application.close();
        } catch (Throwable error) {
          if (failure == null) failure = error;
        }
        Dom.reset();
      }
      if (failure != null) throw failure;
    }
  }

  @org.teavm.jso.JSBody(
      params = "root",
      script =
          "return Array.from(root.querySelectorAll('img')).every(i => i.complete && i.naturalWidth>0);")
  private static native boolean imagesLoaded(org.teavm.jso.dom.html.HTMLElement root);
}
