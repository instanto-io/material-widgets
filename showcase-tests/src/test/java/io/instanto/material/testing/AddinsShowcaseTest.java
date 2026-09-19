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
public class AddinsShowcaseTest {
  private void check(String route) {
    var app =
        FramedApplication.open(
            "/resources/applications/material/index.html#!addins-" + route, 1280, 900);
    try {
      app.awaitReady(
          page -> ("addins-" + route).equals(page.root().getAttribute("data-showcase-page")));
      assertFalse(app.page().find("#catalogue-content").getTextContent().isEmpty());
      Dom.waitFor(() -> assertTrue("Images on " + route, imagesLoaded(app.page().root())));
      Dom.click(app.page().find("a[data-page=button]"));
      app.awaitReady(page -> "button".equals(page.root().getAttribute("data-showcase-page")));
      Dom.click(app.page().find("a[data-page=addins-" + route + "]"));
      app.awaitReady(
          page -> ("addins-" + route).equals(page.root().getAttribute("data-showcase-page")));
      Dom.waitFor(
          () -> assertTrue("Remounted images on " + route, imagesLoaded(app.page().root())));
      app.assertHealthy();
    } finally {
      app.close();
    }
  }

  @org.teavm.jso.JSBody(
      params = "body",
      script =
          "return Array.prototype.every.call(body.querySelectorAll('#catalogue-content img[src]'),function(i){return !i.getAttribute('src') || (i.complete && i.naturalWidth>0);});")
  private static native boolean imagesLoaded(org.teavm.jso.dom.html.HTMLElement body);

  @Test
  public void autocomplete() {
    check("autocomplete");
  }

  @Test
  public void avatar() {
    check("avatar");
  }

  @Test
  public void bubble() {
    check("bubble");
  }

  @Test
  public void camera() {
    check("camera");
  }

  @Test
  public void carousel() {
    check("carousel");
  }

  @Test
  public void circularprogress() {
    check("circularprogress");
  }

  @Test
  public void combobox() {
    check("combobox");
  }

  @Test
  public void countup() {
    check("countup");
  }

  @Test
  public void cropper() {
    check("cropper");
  }

  @Test
  public void cutouts() {
    check("cutouts");
  }

  @Test
  public void dnd() {
    check("dnd");
  }

  @Test
  public void docviewer() {
    check("docviewer");
  }

  @Test
  public void emptystates() {
    check("emptystates");
  }

  @Test
  public void fileuploader() {
    check("fileuploader");
  }

  @Test
  public void iconmorph() {
    check("iconmorph");
  }

  @Test
  public void inputmask() {
    check("inputmask");
  }

  @Test
  public void livestamp() {
    check("livestamp");
  }

  @Test
  public void masonry() {
    check("masonry");
  }

  @Test
  public void menubar() {
    check("menubar");
  }

  @Test
  public void overlay() {
    check("overlay");
  }

  @Test
  public void pathanimator() {
    check("pathanimator");
  }

  @Test
  public void rating() {
    check("rating");
  }

  @Test
  public void richeditor() {
    check("richeditor");
  }

  @Test
  public void scrollfire() {
    check("scrollfire");
  }

  @Test
  public void signature() {
    check("signature");
  }

  @Test
  public void splitpanel() {
    check("splitpanel");
  }

  @Test
  public void steppers() {
    check("steppers");
  }

  @Test
  public void subheaders() {
    check("subheaders");
  }

  @Test
  public void swipeable() {
    check("swipeable");
  }

  @Test
  public void timepickers() {
    check("timepickers");
  }

  @Test
  public void treeview() {
    check("treeview");
  }

  @Test
  public void waterfall() {
    check("waterfall");
  }

  @Test
  public void webp() {
    check("webp");
  }

  @Test
  public void window() {
    check("window");
  }
}
