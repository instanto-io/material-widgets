package io.instanto.material.build;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import java.io.File;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

/** Retains original examples, replacing only the GWTP/Gin construction boundary. */
@Mojo(name = "showcase-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public final class ShowcaseSourcesMojo extends AbstractMojo {
  @Parameter(required = true)
  private File input;

  @Parameter(defaultValue = "${project.build.directory}/generated-sources/showcase")
  private File output;

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  private static final Set<String> PAGES =
      Set.of(
          "animation",
          "badge",
          "breadcrumb",
          "button",
          "cards",
          "checkbox",
          "chips",
          "collapsible",
          "collection",
          "color",
          "datepicker",
          "dialogs",
          "dropdown",
          "errors",
          "fab",
          "footer",
          "icon",
          "listbox",
          "layout",
          "loaders",
          "media",
          "navbar",
          "pushpin",
          "radiobutton",
          "range",
          "scrollspy",
          "search",
          "security",
          "shadow",
          "sidenavs",
          "tabs",
          "textfields",
          "switches");

  static String adaptView(String source) {
    var cu = StaticJavaParser.parse(source);
    var type = cu.getType(0).asClassOrInterfaceDeclaration();
    if (!type.getExtendedTypes().toString().equals("[ViewImpl]")
        || type.getImplementedTypes().size() != 1
        || !type.getImplementedTypes().get(0).toString().endsWith("Presenter.MyView"))
      throw new IllegalArgumentException(
          "Unrecognised showcase view shell: " + type.getNameAsString());
    cu.getImports()
        .removeIf(
            i ->
                Set.of("com.gwtplatform.mvp.client.ViewImpl", "javax.inject.Inject")
                    .contains(i.getNameAsString()));
    cu.addImport("com.google.gwt.user.client.ui.Composite");
    cu.addImport("com.google.gwt.core.client.GWT");
    type.getExtendedTypes().clear();
    type.addExtendedType("Composite");
    type.getImplementedTypes().clear();
    if (Set.of("NavBarView", "SideNavView").contains(type.getNameAsString())) {
      var setup = type.getMethodsByName("buildPanel");
      var root = type.getMethodsByName("asWidget");
      if (setup.size() != 1
          || !setup.get(0).getParameters().isEmpty()
          || root.size() != 1
          || !root.get(0)
              .getBody()
              .orElseThrow()
              .equals(
                  StaticJavaParser.parseBlock("{ return (MaterialPanel) super.asWidget(); }"))) {
        throw new IllegalArgumentException("Changed gallery construction boundary");
      }
      setup.get(0).getAnnotationByName("Override").orElseThrow().remove();
      // GWTP ViewImpl exposes its bound root; Composite.asWidget() exposes itself.
      root.get(0).setBody(StaticJavaParser.parseBlock("{ return (MaterialPanel) getWidget(); }"));
    }
    // This annotation referred to the removed presenter interface. Keep its method
    // body; the maintained launcher invokes it on reveal and layout changes.
    if (type.getNameAsString().equals("TabsView")) {
      var methods = type.getMethodsByName("recalculateTabs");
      if (methods.size() != 1 || !methods.get(0).getParameters().isEmpty())
        throw new IllegalArgumentException("Changed tabs presenter hook");
      methods.get(0).getAnnotationByName("Override").orElseThrow().remove();
    }
    if (type.getNameAsString().equals("BadgeView")) {
      // The original view imports the application resource bundle without using it.
      cu.getImports()
          .removeIf(i -> i.getNameAsString().equals("gmd.core.demo.client.resources.AppResources"));
    }
    if (type.getConstructors().size() != 1)
      throw new IllegalArgumentException("Expected one injected constructor");
    var ctor = type.getConstructors().get(0);
    if (ctor.getParameters().size() != 1
        || !ctor.getParameter(0).getTypeAsString().equals("Binder")
        || ctor.getAnnotationByName("Inject").isEmpty())
      throw new IllegalArgumentException("Unrecognised view constructor");
    ctor.getAnnotations().clear();
    ctor.setPublic(true);
    type.addConstructor(Modifier.Keyword.PUBLIC)
        .setBody(StaticJavaParser.parseBlock("{ this((Binder) GWT.create(Binder.class)); }"));
    return cu.toString();
  }

  static void verifyGalleryPresenter(String source) {
    var presenter = StaticJavaParser.parse(source).getType(0).asClassOrInterfaceDeclaration();
    var bind = presenter.getMethodsByName("onBind");
    if (bind.size() != 1
        || !bind.get(0)
            .getBody()
            .orElseThrow()
            .equals(StaticJavaParser.parseBlock("{ super.onBind(); getView().buildPanel(); }"))) {
      throw new IllegalArgumentException("Changed gallery presenter setup");
    }
  }

  @Override
  public void execute() throws MojoExecutionException {
    try {
      Path root = input.toPath().resolve("src/main/java");
      Path out = output.toPath().toAbsolutePath().normalize();
      Path build = Path.of(project.getBuild().getDirectory()).toAbsolutePath().normalize();
      if (!out.startsWith(build) || out.equals(build)) {
        throw new IllegalArgumentException(
            "Generated sources must be in a subdirectory of the build directory");
      }
      if (out.toAbsolutePath().startsWith(root.toAbsolutePath()))
        throw new IllegalArgumentException("Output must be separate from source intake");
      Files.createDirectories(out);
      // A changed catalogue must not leave previously generated views in an incremental build.
      try (var files = Files.walk(out)) {
        for (Path p : files.sorted(Comparator.reverseOrder()).toList())
          if (!p.equals(out)) Files.delete(p);
      }
      StringBuilder hashes = new StringBuilder("# SHA-256 of original pinned showcase inputs\n");
      StringBuilder cases = new StringBuilder();
      StringBuilder paths = new StringBuilder();
      try (var files = Files.walk(root)) {
        for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
          String rel = root.relativize(file).toString();
          String base = "gmd/core/demo/client/application/";
          boolean page =
              rel.startsWith(base + "page/")
                  && PAGES.contains(file.getParent().getFileName().toString())
                  && (rel.endsWith("View.java") || rel.endsWith("View.ui.xml"));
          boolean helper =
              Set.of(
                      base + "widget/CodeSection.java",
                      base + "navigation/Component.java",
                      base + "navigation/NavigationService.java",
                      base + "navigation/DataService.java",
                      base + "model/DataHelper.java",
                      base + "model/Hero.java",
                      base + "model/FieldState.java",
                      base + "model/User.java",
                      base + "model/UserOracle.java",
                      base + "model/UserSuggestion.java",
                      base + "model/DemoImageDTO.java",
                      base + "model/DemoImagePanel.java",
                      base + "page/errors/EmailValidator.java",
                      "gmd/core/demo/client/place/NameTokens.java")
                  .contains(rel);
          if (!page && !helper) continue;
          byte[] original = Files.readAllBytes(file);
          hashes
              .append(
                  HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original)))
              .append("  ")
              .append(rel)
              .append('\n');
          Path destination = out.resolve(rel);
          Files.createDirectories(destination.getParent());
          String source = Files.readString(file);
          if (page && rel.endsWith("View.java")) {
            source = adaptView(source);
            String qualified = rel.substring(0, rel.length() - 5).replace('/', '.');
            String token = file.getParent().getFileName().toString();
            if (token.equals("datepicker")) token = "datePicker";
            if (token.equals("radiobutton")) token = "radioButton";
            cases.append("case \"").append(token).append("\": ");
            if (Set.of("navbar", "sidenavs").contains(token)) {
              Path presenter =
                  file.resolveSibling(
                      file.getFileName().toString().replace("View.java", "Presenter.java"));
              String originalPresenter = Files.readString(presenter);
              verifyGalleryPresenter(originalPresenter);
              hashes
                  .append(
                      HexFormat.of()
                          .formatHex(
                              MessageDigest.getInstance("SHA-256")
                                  .digest(Files.readAllBytes(presenter))))
                  .append("  ")
                  .append(root.relativize(presenter))
                  .append('\n');
              cases
                  .append("{ var view = new ")
                  .append(qualified)
                  .append("(); view.buildPanel(); return view; }\n");
              source =
                  source
                      .replace(
                          "https://gwtmaterialdesign.github.io/gwt-material-patterns/snapshot/#",
                          "?pattern=")
                      .replace(
                          "https://github.com/GwtMaterialDesign/gwt-material-patterns/tree/release_2.0/src/main/java/com/github/gwtmaterialdesign/client/application/",
                          "pattern-sources/")
                      .replace("navbartab/TabNavBarView.ui.xml", "navbartabs/TabNavBarView.ui.xml");
            } else {
              cases.append("return new ").append(qualified).append("();\n");
            }
            paths
                .append("case \"")
                .append(token)
                .append("\": return \"")
                .append(rel.substring(0, rel.length() - 5))
                .append("\";\n");
          }
          if (rel.endsWith("model/DemoImagePanel.java")) {
            source = source.replace("image.setUrl(dto.getUrl());", "image.setVisible(false);");
          }
          if (rel.endsWith("animation/AnimationView.ui.xml")) {
            source =
                source.replace(
                    "https://www.topofandroid.com/wp-content/uploads/2015/05/Android-L-Material-Design-Wallpapers-5.png",
                    "images/animations-banner.png");
          }
          if (rel.endsWith("media/MediaView.ui.xml")) {
            source =
                source
                    .replace(
                        "https://gwtmaterialdesign.github.io/gwt-material-demo/images/",
                        "media/images/")
                    .replace(
                        "https://www.youtube.com/embed/Q8TXgCzxEnw?rel=0", "media/player.html");
          }
          if (rel.endsWith("tabs/TabsView.ui.xml")) {
            for (String fragment :
                List.of(
                    "<m:MaterialAnchorButton text=\"Source\" textColor=\"BLACK\" backgroundColor=\"WHITE\" target=\"_blank\" href=\"https://github.com/GwtMaterialDesign/gwt-material-patterns/tree/release_2.0/src/main/java/com/github/gwtmaterialdesign/client/application/navbartabspush\" />",
                    "<m:MaterialAnchorButton text=\"Demo\" target=\"_blank\" href=\"https://gwtmaterialdesign.github.io/gwt-material-patterns/snapshot/#navbar_tab_push\" />",
                    "<m:MaterialImage marginTop=\"20\" url=\"https://gwtmaterialdesign.github.io/gwt-material-demo/images/tab.gif\" />")) {
              if (!source.contains(fragment))
                throw new IllegalArgumentException("Changed external tabs pattern preview");
              source =
                  source.replace(
                      fragment,
                      fragment.startsWith("<m:MaterialImage")
                          ? ""
                          : fragment
                              .replace(
                                  "https://gwtmaterialdesign.github.io/gwt-material-patterns/snapshot/#navbar_tab_push",
                                  "?pattern=navbar_tab_push")
                              .replace(
                                  "https://github.com/GwtMaterialDesign/gwt-material-patterns/tree/release_2.0/src/main/java/com/github/gwtmaterialdesign/client/application/navbartabspush",
                                  "pattern-sources/navbartabspush/TabPushNavBarView.ui.xml"));
            }
          }
          Files.writeString(destination, source);
        }
      }
      String generated =
          "package io.instanto.material.showcase;\n"
              + "import com.google.gwt.user.client.ui.Widget;\npublic final class OriginalPages {\n"
              + "public static Widget create(String token) {switch(token) {"
              + cases
              + "default:return null;}}\n"
              + "public static String source(String token) {switch(token) {"
              + paths
              + "default:return null;}}\n}";
      Path catalog = out.resolve("io/instanto/material/showcase/OriginalPages.java");
      Files.createDirectories(catalog.getParent());
      Files.writeString(catalog, generated);
      Path manifest = Path.of(project.getBuild().getDirectory(), "showcase-inputs.sha256");
      Files.writeString(manifest, hashes);
      project.addCompileSourceRoot(out.toString());
      getLog().info("Retained " + PAGES.size() + " original showcase views and templates");
    } catch (Exception e) {
      throw new MojoExecutionException("Unable to prepare original showcase sources", e);
    }
  }
}
