package io.instanto.material.testing;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.Test;

/** Exercises retained Media handlers, plugin disposal and responsive video embedding. */
public class MediaTest {
  @Test
  public void slidersStartPauseSelectAndCloseFullscreenWithoutLeakingOnNavigation()
      throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!media",
        page -> {
          for (int pass = 0; pass < 2; pass++) {
            page.waitForSelector("body[data-showcase-page=media][data-showcase-state=rendered]");
            assertEquals(2, page.locator(".slider").count());
            assertEquals(8, page.locator(".slider .indicator-item").count());
            button(page, "PAUSE").click();
            page.waitForTimeout(400);
            Object paused =
                page.locator("#options .slides")
                    .evaluate(
                        "e => Array.from(e.children).findIndex(s => s.classList.contains('active'))");
            page.waitForTimeout(1700);
            assertEquals(
                paused,
                page.locator("#options .slides")
                    .evaluate(
                        "e => Array.from(e.children).findIndex(s => s.classList.contains('active'))"));
            button(page, "START").click();
            page.waitForFunction(
                "previous => Array.from(document.querySelector('#options .slides').children).findIndex(s => s.classList.contains('active')) !== previous",
                paused);
            button(page, "PAUSE").click();
            page.locator("#options .indicator-item").nth(2).click();
            page.waitForSelector("#options .indicator-item:nth-child(3).active");
            button(page, "Fullscreen Slider").click();
            page.waitForFunction(
                "getComputedStyle(document.querySelector('#material_slider .slider')).position === 'fixed'");
            assertEquals(
                true,
                page.locator("#material_slider .slider")
                    .evaluate(
                        "e => { const b=e.getBoundingClientRect(); return Math.abs(b.width-innerWidth)<1 && Math.abs(b.height-innerHeight)<1 && Math.abs(b.top)<1 && Math.abs(b.left)<1; }"));
            button(page, "Close Fullscreen").click();
            assertNotEquals(
                "fixed",
                page.locator("#material_slider .slider")
                    .evaluate("e => getComputedStyle(e).position"));
            page.evaluate(
                "window.oldSliders=Array.from(document.querySelectorAll('.slider')); window.oldSlides=document.querySelector('#options .slides')");
            page.locator("a[data-page=button]").click();
            page.waitForSelector("body[data-showcase-page=button]");
            assertEquals(
                true,
                page.evaluate(
                    "oldSliders.every(e => !e.isConnected && !jQuery(e).data('hammer') && !e.querySelector('.indicators'))"));
            Object detached = page.evaluate("oldSlides.innerHTML");
            page.waitForTimeout(1700);
            assertEquals(
                "Detached sliders stop changing", detached, page.evaluate("oldSlides.innerHTML"));
            page.locator("a[data-page=media]").click();
          }
        });
  }

  @Test
  public void originalImagesOpenCaptionAndDisposeDocumentHandlers() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!media",
        page -> {
          for (int pass = 0; pass < 2; pass++) {
            page.waitForSelector("body[data-showcase-page=media][data-showcase-state=rendered]");
            page.waitForFunction(
                "Array.from(document.querySelectorAll('#catalogue-content img')).every(i => i.complete && i.naturalWidth > 0)");
            if (pass == 0)
              page.screenshot(
                  new Page.ScreenshotOptions()
                      .setPath(java.nio.file.Path.of("target/media-showcase.png"))
                      .setFullPage(true));
            page.locator("#captions img").click();
            page.waitForSelector(".materialbox-caption:has-text('I love Material Design')");
            LightboxAssertions.awaitOpen(page, "#captions img");
            page.keyboard().press("Escape");
            page.waitForFunction("!document.querySelector('#materialbox-overlay')");
            page.locator("#material_box img").click();
            page.waitForSelector("#materialbox-overlay");
            page.evaluate(
                "window.oldImages=Array.from(document.querySelectorAll('img.materialboxed')); location.hash='!button'");
            page.waitForSelector("body[data-showcase-page=button]");
            assertEquals(0, page.locator("#materialbox-overlay,.materialbox-caption").count());
            assertEquals(
                true,
                page.evaluate(
                    "oldImages.every(e => !jQuery(e).data('instantoMaterialBoxDispose'))"));
            assertEquals(
                0,
                ((Number)
                        page.evaluate(
                            "[window,document].reduce((total,target) => total + Object.values(jQuery._data(target,'events') || {}).flat().filter(h => h.namespace.startsWith('instantoMaterialBox')).length,0)"))
                    .intValue());
            page.locator("a[data-page=media]").click();
          }
        });
  }

  @Test
  public void embeddedSamplePlaysPausesSeeksAndFitsSmallScreens() throws Exception {
    BaselineTest.verify(
        "showcase-teavm",
        "/#!media",
        page -> {
          page.waitForSelector("body[data-showcase-page=media][data-showcase-state=rendered]");
          Locator iframe = page.locator("#responsive_videos iframe");
          Locator video = page.frameLocator("#responsive_videos iframe").locator("video");
          video.evaluate("v => { v.preload='auto'; v.load(); }");
          video.evaluate(
              "v => new Promise(resolve => { if(v.readyState >= 1) resolve(); else v.addEventListener('loadedmetadata',resolve,{once:true}); })");
          assertTrue(((Number) video.evaluate("v => v.duration")).doubleValue() >= 3.9);
          video.evaluate("v => v.play()");
          page.waitForFunction(
              "document.querySelector('#responsive_videos iframe').contentDocument.querySelector('video').currentTime > 0.2");
          video.evaluate("v => v.pause()");
          assertEquals(true, video.evaluate("v => v.paused"));
          video.evaluate(
              "v => new Promise(resolve => { v.addEventListener('seeked',resolve,{once:true}); v.currentTime=2; })");
          assertEquals(2.0, ((Number) video.evaluate("v => v.currentTime")).doubleValue(), 0.15);
          for (int width : new int[] {1280, 390}) {
            page.setViewportSize(width, 844);
            assertEquals(
                true,
                iframe.evaluate(
                    "e => { const b=e.getBoundingClientRect(); return b.width>0 && b.height>0 && b.right<=innerWidth+1 && b.width/b.height>1.7 && b.width/b.height<1.9; }"));
            assertEquals(true, page.evaluate("document.documentElement.scrollWidth <= innerWidth"));
          }
          page.evaluate("location.hash='!button'");
          page.waitForSelector("body[data-showcase-page=button]");
          assertEquals(0, page.locator("#responsive_videos iframe").count());
        });
  }

  private static Locator button(Page page, String name) {
    return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name));
  }
}
