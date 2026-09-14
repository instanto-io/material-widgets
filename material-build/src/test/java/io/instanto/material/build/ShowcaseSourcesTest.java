package io.instanto.material.build;

import static org.junit.Assert.*;

import com.github.javaparser.StaticJavaParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class ShowcaseSourcesTest {
  @Test
  public void navigationAdaptationKeepsOriginalConstructionAndHandlers() throws Exception {
    Path root =
        Path.of(
            "../target/intake/patterns/src/main/java/com/github/gwtmaterialdesign/client/application");
    assertEquals(18, PatternSourcesMojo.PAGES.size());
    for (String folder : PatternSourcesMojo.PAGES.values()) {
      try (var files = Files.list(root.resolve(folder))) {
        for (Path file : files.filter(p -> p.toString().endsWith("View.java")).toList()) {
          String source = Files.readString(file);
          var before = StaticJavaParser.parse(source).getType(0).asClassOrInterfaceDeclaration();
          var after =
              StaticJavaParser.parse(ShowcaseSourcesMojo.adaptView(source))
                  .getType(0)
                  .asClassOrInterfaceDeclaration();
          assertEquals(folder, before.getFields(), after.getFields());
          assertEquals(folder, before.getMethods(), after.getMethods());
          assertEquals(
              folder,
              before.getConstructors().get(0).getBody(),
              after.getConstructors().get(0).getBody());
        }
      }
    }
  }

  @Test
  public void tableAdaptationRetainsOriginalConstructionAndEventHandlers() throws Exception {
    Path pages =
        Path.of("../target/intake/table-demo/src/main/java/gmd/datatable/demo/client/application");
    for (String page : TableShowcaseSourcesMojo.PAGES) {
      String type = Character.toUpperCase(page.charAt(0)) + page.substring(1) + "View";
      String source = Files.readString(pages.resolve(page + "/" + type + ".java"));
      var before =
          StaticJavaParser.parse(
                  source.replace("GWT.create(FakeUserService.class)", "new FakeUserService()"))
              .getType(0)
              .asClassOrInterfaceDeclaration();
      var after =
          StaticJavaParser.parse(TableShowcaseSourcesMojo.adaptView(source))
              .getType(0)
              .asClassOrInterfaceDeclaration();
      for (var method : before.getMethods()) {
        if (java.util.Set.of("setupTable", "setupOptions", "setData", "getSideContent", "loadData")
            .contains(method.getNameAsString()))
          method.getAnnotationByName("Override").ifPresent(annotation -> annotation.remove());
      }
      assertEquals(
          type + " fields, including provided widgets", before.getFields(), after.getFields());
      assertEquals(
          type + " methods and handlers",
          before.getMethods().stream().map(Object::toString).toList(),
          after.getMethods().stream().map(Object::toString).toList());
      assertEquals(
          type + " constructor",
          before.getConstructors().get(0).getBody(),
          after.getConstructors().get(0).getBody());
    }
  }

  @Test
  public void adaptationPreservesOriginalFieldsHandlersAndLifecycleMethods() throws Exception {
    Path pages =
        Path.of("../target/intake/showcase/src/main/java/gmd/core/demo/client/application/page");
    for (String path :
        new String[] {
          "button/ButtonView.java",
          "checkbox/CheckboxView.java",
          "animation/AnimationView.java",
          "badge/BadgeView.java",
          "cards/CardView.java",
          "collection/CollectionView.java",
          "layout/LayoutView.java",
          "loaders/LoadersView.java",
          "media/MediaView.java",
          "shadow/ShadowView.java",
          "tabs/TabsView.java",
          "textfields/TextFieldView.java",
          "errors/ErrorsView.java",
          "navbar/NavBarView.java",
          "sidenavs/SideNavView.java"
        }) {
      String original = Files.readString(pages.resolve(path));
      var before = StaticJavaParser.parse(original).getType(0).asClassOrInterfaceDeclaration();
      var after =
          StaticJavaParser.parse(ShowcaseSourcesMojo.adaptView(original))
              .getType(0)
              .asClassOrInterfaceDeclaration();
      if (path.equals("tabs/TabsView.java")) {
        // Only the annotation on the retained presenter method changes.
        before
            .getMethodsByName("recalculateTabs")
            .get(0)
            .getAnnotationByName("Override")
            .orElseThrow()
            .remove();
      }
      if (path.startsWith("navbar/") || path.startsWith("sidenavs/")) {
        before
            .getMethodsByName("buildPanel")
            .get(0)
            .getAnnotationByName("Override")
            .orElseThrow()
            .remove();
        before
            .getMethodsByName("asWidget")
            .get(0)
            .setBody(StaticJavaParser.parseBlock("{ return (MaterialPanel) getWidget(); }"));
        String presenter =
            Files.readString(pages.resolve(path.replace("View.java", "Presenter.java")));
        ShowcaseSourcesMojo.verifyGalleryPresenter(presenter);
        assertThrows(
            IllegalArgumentException.class,
            () ->
                ShowcaseSourcesMojo.verifyGalleryPresenter(
                    presenter.replace(
                        "getView().buildPanel();", "getView().buildPanel(); initialiseMore();")));
      }
      assertEquals(path + " fields", before.getFields(), after.getFields());
      assertEquals(path + " methods and UiHandlers", before.getMethods(), after.getMethods());
      assertEquals(
          path + " constructor body",
          before.getConstructors().get(0).getBody(),
          after.getConstructors().get(0).getBody());
      assertEquals(
          path + " UiBinder declaration",
          before.getMembers().stream().filter(m -> m.isClassOrInterfaceDeclaration()).toList(),
          after.getMembers().stream().filter(m -> m.isClassOrInterfaceDeclaration()).toList());
    }
  }

  @Test
  public void unexpectedInjectionContractsFailInsteadOfLosingBehaviour() {
    String source =
        "class ChangedView extends ViewImpl implements ChangedPresenter.MyView { @Inject ChangedView(Binder binder, Service service) { initWidget(binder.createAndBindUi(this)); service.start(); } }";
    assertThrows(IllegalArgumentException.class, () -> ShowcaseSourcesMojo.adaptView(source));
  }
}
