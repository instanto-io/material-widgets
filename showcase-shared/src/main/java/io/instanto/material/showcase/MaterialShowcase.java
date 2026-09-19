package io.instanto.material.showcase;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import gmd.core.demo.client.application.navigation.Component;
import gmd.core.demo.client.application.navigation.NavigationService;

/** Shared routing shell; example views, templates and descriptions come from gmd-core-demo. */
public final class MaterialShowcase {
  private final FlowPanel content = new FlowPanel();
  private final Label title = new Label();
  private final Label description = new Label();
  private final FlowPanel navigation = new FlowPanel();
  private final String backend;
  private final Button menu = new Button("☰");
  private Widget currentPage;

  public MaterialShowcase(String backend) {
    this.backend = backend;
  }

  public void start() {
    RootPanel root = RootPanel.get("showcase");
    HTMLPanel shell =
        new HTMLPanel(
            "<aside id='catalogue-sidebar' aria-label='Widget catalogue'></aside><header id='catalogue-toolbar'></header><main id='catalogue-main'><div id='catalogue-header'></div><div id='catalogue-content'></div><footer>Original widgets and examples © GWT Material Design contributors. Independent TeaVM adaptation by Instanto.</footer></main>");
    root.add(shell);
    FlowPanel sidebar = new FlowPanel();
    HTML brand =
        new HTML(
            "<a class='catalogue-brand' href='#!button'><img src='images/gmd-logo.webp' alt=''><span>GWT Material<small>Widget showcase</small></span></a>");
    sidebar.add(brand);
    TextBox search = new TextBox();
    search.getElement().setAttribute("placeholder", "Find a component…");
    search.getElement().setAttribute("aria-label", "Find a component");
    search.addStyleName("catalogue-search");
    search.addDomHandler(e -> filter(search.getValue()), InputEvent.TYPE);
    sidebar.add(search);
    for (Component component : NavigationService.getSideNavLinks()) {
      if (component.getHref().equals("/")) continue;
      Anchor link = new Anchor(component.getName(), "#!" + component.getHref());
      link.getElement().setAttribute("data-page", component.getHref());
      if (OriginalPages.source(component.getHref()) == null) link.addStyleName("pending-page");
      navigation.add(link);
    }
    for (Component table : OriginalTables.components()) {
      Anchor link = new Anchor("Table · " + table.getName(), "#!" + table.getHref());
      link.getElement().setAttribute("data-page", table.getHref());
      navigation.add(link);
    }
    sidebar.add(navigation);
    for (Component addin : OriginalAddins.components()) {
      Anchor link = new Anchor("Addins · " + addin.getName(), "#!" + addin.getHref());
      link.getElement().setAttribute("data-page", addin.getHref());
      navigation.add(link);
    }

    shell.add(sidebar, "catalogue-sidebar");
    FlowPanel toolbar = new FlowPanel();

    menu.getElement().setAttribute("aria-label", "Toggle component menu");
    menu.getElement().setAttribute("aria-expanded", "false");
    menu.addStyleName("catalogue-menu");
    menu.addClickHandler(
        e -> {
          Element body = Document.get().getBody();
          boolean open = !body.hasClassName("menu-open");
          if (open) body.addClassName("menu-open");
          else body.removeClassName("menu-open");
          menu.getElement().setAttribute("aria-expanded", Boolean.toString(open));
          refreshPageLayout();
        });
    toolbar.add(menu);
    Label name = new Label("Components");
    toolbar.add(name);
    Label status =
        new Label(
            backend.equals("TeaVM")
                ? "TeaVM · compatibility preview"
                : "GWT · upstream comparison");
    status.addStyleName("catalogue-backend");
    toolbar.add(status);
    toolbar.add(ShowcaseTheme.toggle());
    shell.add(toolbar, "catalogue-toolbar");
    FlowPanel header = new FlowPanel();
    title.addStyleName("catalogue-title");
    title.getElement().setAttribute("role", "heading");
    title.getElement().setAttribute("aria-level", "1");
    description.addStyleName("catalogue-description");
    header.add(title);
    header.add(description);
    shell.add(header, "catalogue-header");
    shell.add(content, "catalogue-content");
    History.addValueChangeHandler(e -> show(e.getValue()));
    Window.addResizeHandler(e -> refreshPageLayout());
    show(History.getToken());
  }

  private void filter(String query) {
    String search = query.trim().toLowerCase();
    for (Widget widget : navigation) {
      widget.setVisible(((Anchor) widget).getText().toLowerCase().contains(search));
    }
  }

  private void show(String token) {
    String page = token.startsWith("!") ? token.substring(1) : token;
    if (page.isEmpty() || page.equals("/")) page = "button";
    Component component = NavigationService.get(page);
    if (component == null) {
      for (Component table : OriginalTables.components()) {
        if (table.getHref().equals(page)) {
          component = table;
          break;
        }
      }
    }
    if (component == null) {
      for (Component addin : OriginalAddins.components())
        if (addin.getHref().equals(page)) {
          component = addin;
          break;
        }
    }
    // Some original widget examples
    // have their own fragment links.
    if (component == null) return;
    content.clear();
    title.setText(component.getName());
    description.setText(component.getDescription());
    String path =
        page.startsWith("table-")
            ? page
            : page.startsWith("addins-") ? OriginalAddins.source(page) : OriginalPages.source(page);
    if (path != null) {
      currentPage =
          page.startsWith("table-")
              ? OriginalTables.create(page)
              : page.startsWith("addins-")
                  ? OriginalAddins.create(page)
                  : OriginalPages.create(page);
      if (page.equals("navbar") || page.equals("sidenavs")) {
        Label note =
            new Label(
                "Open a demo to try the original full-page navigation example running on TeaVM.");
        note.addStyleName("catalogue-pattern-note");
        content.add(note);
      }
      content.add(currentPage);
    } else {
      currentPage = null;
      FlowPanel pending = new FlowPanel();
      pending.addStyleName("catalogue-pending");
      pending.add(new Label("This example is awaiting migration."));
      pending.add(new Label("This example will appear here once it runs on TeaVM."));
      content.add(pending);
    }
    for (Widget widget : navigation) {
      String key = widget.getElement().getAttribute("data-page");
      if (key.equals(page)) {
        widget.addStyleName("active-page");
        widget.getElement().setAttribute("aria-current", "page");
      } else {
        widget.removeStyleName("active-page");
        widget.getElement().removeAttribute("aria-current");
      }
    }
    Document.get().getBody().removeClassName("menu-open");
    menu.getElement().setAttribute("aria-expanded", "false");
    Window.scrollTo(0, 0);
    refreshPageLayout();
    Document.get().setTitle(component.getName() + " · GWT Material showcase");
    Document.get().getBody().setAttribute("data-showcase-page", page);
    Document.get()
        .getBody()
        .setAttribute("data-showcase-state", path == null ? "pending" : "rendered");
  }

  private void refreshPageLayout() {
    if (currentPage
        instanceof gwt.material.design.demo.client.application.addins.signature.SignaturePadView) {
      ((gwt.material.design.demo.client.application.addins.signature.SignaturePadView) currentPage)
          .resizeSignaturePad();
    }
    if (currentPage
        instanceof gwt.material.design.demo.client.application.addins.carousel.CarouselView) {
      ((gwt.material.design.demo.client.application.addins.carousel.CarouselView) currentPage)
          .reloadCarousels();
    }
    if (currentPage instanceof gmd.core.demo.client.application.page.tabs.TabsView) {
      gmd.core.demo.client.application.page.tabs.TabsView tabs =
          (gmd.core.demo.client.application.page.tabs.TabsView) currentPage;
      com.google.gwt.core.client.Scheduler.get()
          .scheduleDeferred(
              () -> {
                if (currentPage == tabs && tabs.isAttached()) {
                  tabs.recalculateTabs();
                }
              });
    }
  }
}
