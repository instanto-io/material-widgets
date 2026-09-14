package io.instanto.material.testing;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;
import java.nio.file.Path;
import org.junit.Test;

public class NavigationPatternsTest {
  @Test
  public void drawerOpensAndClosesAndMiniNavigationExpands() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/?pattern=sidenav_drawer",
        page -> {
          String base = page.url().split("\\?")[0];
          page.waitForSelector("body[data-pattern=sidenav_drawer]");
          page.waitForFunction(
              "document.querySelector('#sidenavDrawer').getBoundingClientRect().right <= 1");
          page.locator("nav .button-collapse").click();
          page.waitForFunction(
              "Math.abs(document.querySelector('#sidenavDrawer').getBoundingClientRect().left) < 1");
          page.locator("#sidenav-overlay").click(new Locator.ClickOptions().setPosition(800, 100));
          page.waitForFunction(
              "document.querySelector('#sidenavDrawer').getBoundingClientRect().right <= 1");
          page.navigate(base + "?pattern=sidenav_mini_expandable");
          page.waitForSelector("body[data-pattern=sidenav_mini_expandable]");
          page.waitForFunction(
              "Math.abs(document.querySelector('#sidenavMiniExpand').getBoundingClientRect().width-64) < 1");
          page.evaluate(
              "window.patternOpened=false; jQuery('nav .button-collapse').one('side-nav-opened',()=>window.patternOpened=true)");
          page.locator("nav .button-collapse").click();
          page.waitForFunction("window.patternOpened === true");
          page.waitForFunction(
              "Math.abs(document.querySelector('#sidenavMiniExpand').getBoundingClientRect().width-300) < 1");
          page.locator("nav .button-collapse").click();
          page.waitForFunction(
              "Math.abs(document.querySelector('#sidenavMiniExpand').getBoundingClientRect().width-64) < 1");
        });
  }

  @Test
  public void allOriginalFullPagePatternsLoad() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/?pattern=navbar_default",
        page -> {
          String base = page.url().split("\\?")[0];
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
            page.navigate(base + "?pattern=" + token);
            page.waitForSelector("body[data-pattern='" + token + "']");
            assertTrue(token, page.locator("header nav").count() > 0);
            assertEquals(0, page.locator("a[href*='gwtmaterialdesign' i]").count());
            page.waitForFunction(
                "Array.from(document.images).every(i => i.complete && i.naturalWidth>0)");
            page.waitForFunction(
                "!document.querySelector('.side-nav.velocity-animating, #sidenav-overlay.velocity-animating, header.velocity-animating')");
            page.screenshot(
                new Page.ScreenshotOptions().setPath(Path.of("target", token + ".png")));
          }
          page.locator(".pattern-back").click();
          page.waitForSelector("body[data-showcase-page=sidenavs]");
          assertEquals(11, page.locator("a[href^='?pattern=sidenav_']").count());
        });
  }
}
