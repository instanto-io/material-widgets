package io.instanto.material.testing;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Real pointer and keyboard events against both resource variants used by consumers. */
public class DatePickerResourcesTest {
  @Test
  public void mouseTouchAndKeyboardKeepYearPickerOpen() {
    Path assets = Path.of("../material-assets/target/classes/META-INF/resources/gwt-material");
    try (Playwright playwright = Playwright.create();
        Browser browser = playwright.webkit().launch()) {
      for (String suffix : new String[] {".js", ".min.js"}) {
        try (Page page = browser.newPage(new Browser.NewPageOptions().setHasTouch(true))) {
          page.setDefaultTimeout(5000);
          List<String> errors = new ArrayList<>();
          page.onPageError(errors::add);
          page.setContent(
              "<button id='before'>Before</button><div style='padding:100px'><input id='date' style='width:240px'></div>");
          page.addStyleTag(
              new Page.AddStyleTagOptions().setPath(assets.resolve("css/materialize.min.css")));
          page.addScriptTag(
              new Page.AddScriptTagOptions().setPath(assets.resolve("js/jquery-3.5.1.min.js")));
          page.addScriptTag(
              new Page.AddScriptTagOptions()
                  .setPath(assets.resolve("js/materialize-0.97.5" + suffix)));
          page.evaluate(
              "$('#date').pickadate({selectYears:10}).pickadate('picker').on('open',()=>document.getElementById('date').focus()).on('close',()=>document.getElementById('date').blur())");
          for (String activation : new String[] {"mouse", "touch", "keyboard"}) {
            if (activation.equals("mouse")) page.locator("#date").click();
            else if (activation.equals("touch")) page.locator("#date").tap();
            else {
              page.locator("#before").focus();
              page.keyboard().press("Tab");
            }
            page.waitForSelector(".picker--opened .picker__select--year");
            String year =
                page.locator(".picker__select--year option").first().getAttribute("value");
            page.locator(".picker--opened .picker__select--year").selectOption(year);
            page.locator(".picker--opened .picker__day--infocus:not(.picker__day--disabled)")
                .first()
                .click();
            assertTrue(activation + suffix, page.locator("#date").inputValue().contains(year));
            if (page.locator(".picker--opened").count() > 0)
              page.locator(".picker--opened .picker__close").click();
            page.waitForFunction(
                "!document.querySelector('.picker--opened') && document.querySelector('.picker__holder').getBoundingClientRect().height < 1");
          }
          page.evaluate("$('#date').pickadate('picker').stop()");
          page.locator("#date").click();
          assertEquals(0, page.locator(".picker").count());
          assertEquals(suffix, List.of(), errors);
        }
      }
    }
  }
}
