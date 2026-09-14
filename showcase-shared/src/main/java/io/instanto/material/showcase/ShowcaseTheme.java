package io.instanto.material.showcase;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Button;
import gwt.material.design.addins.client.dark.AddinsDarkThemeLoader;
import gwt.material.design.client.base.TableDarkThemeLoader;
import gwt.material.design.client.theme.dark.CoreDarkThemeLoader;
import gwt.material.design.client.theme.dark.DarkThemeManager;
import org.teavm.jso.JSBody;

/** Applies upstream widget themes alongside the catalogue's own colours. */
public final class ShowcaseTheme {
  private static boolean initialised;

  public static Button toggle() {
    DarkThemeManager themes = DarkThemeManager.get();
    if (!initialised) {
      themes
          .register(new CoreDarkThemeLoader())
          .register(new AddinsDarkThemeLoader())
          .register(new TableDarkThemeLoader());
      apply(readPreference());
      initialised = true;
    }
    Button button = new Button("Dark mode");
    button.getElement().setId("showcase-theme");
    button.addStyleName("catalogue-theme");
    update(button);
    button.addClickHandler(
        event -> {
          boolean dark = !themes.isDarkMode();
          apply(dark);
          savePreference(dark);
          update(button);
        });
    return button;
  }

  private static void apply(boolean dark) {
    DarkThemeManager.get().setDarkMode(dark);
    String name = dark ? "dark" : "light";
    Document.get().getDocumentElement().setAttribute("data-theme", name);
    Document.get().getBody().setAttribute("data-theme", name);
  }

  private static void update(Button button) {
    boolean dark = DarkThemeManager.get().isDarkMode();
    button.getElement().setAttribute("aria-pressed", Boolean.toString(dark));
    button.setTitle(dark ? "Switch to light mode" : "Switch to dark mode");
  }

  @JSBody(
      script =
          "try { return localStorage.getItem('instanto-material-theme') === 'dark'; } catch (e) { return false; }")
  private static native boolean readPreference();

  @JSBody(
      params = "dark",
      script =
          "try { localStorage.setItem('instanto-material-theme', dark ? 'dark' : 'light'); } catch (e) { /* The toggle still works when storage is unavailable. */ }")
  private static native void savePreference(boolean dark);
}
