package io.instanto.material.showcase;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.*;

/** Each full-page pattern has its own document so layout and window handlers remain isolated. */
public final class NavigationPatterns {
  public static void start(String token) {
    Widget view = OriginalPatterns.create(token);
    RootPanel root = RootPanel.get("showcase");
    if (view == null) {
      root.add(new Label("Unknown navigation example."));
      return;
    }
    Document.get().getBody().addClassName("navigation-pattern");
    root.add(view);
    Anchor back =
        new Anchor(
            "Back to showcase",
            "./#!"
                + (token.startsWith("sidenav_")
                    ? "sidenavs"
                    : token.equals("navbar_tab_push") ? "tabs" : "navbar"));
    back.setStyleName("pattern-back");
    RootPanel.get().add(back);
    Button theme = ShowcaseTheme.toggle();
    theme.addStyleName("pattern-theme");
    RootPanel.get().add(theme);
    Document.get().getBody().setAttribute("data-pattern", token);
    Document.get().getBody().setAttribute("data-showcase-state", "rendered");
    Document.get().setTitle(token.replace('_', ' ') + " · Material on TeaVM");
  }
}
