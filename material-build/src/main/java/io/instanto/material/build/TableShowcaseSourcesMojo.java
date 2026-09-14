package io.instanto.material.build;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.instanto.compat.GenerateBindings;
import io.instanto.compat.LegacyNativeBodies;
import java.io.File;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

/**
 * Retains the table demo's original views and presenter-owned setup using a local routing shell.
 */
@Mojo(
    name = "table-showcase-sources",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    threadSafe = true)
public final class TableShowcaseSourcesMojo extends AbstractMojo {
  @Parameter(required = true)
  private File input;

  @Parameter(defaultValue = "${project.build.directory}/generated-sources/table-showcase")
  private File output;

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  static final List<String> PAGES =
      List.of("standard", "paged", "categorized", "frozen", "customized", "infinite");

  static String adaptView(String source) {
    var cu = StaticJavaParser.parse(source);
    var type = cu.getType(0).asClassOrInterfaceDeclaration();
    for (var method : type.getMethods()) {
      if (Set.of("setupTable", "setupOptions", "setData", "getSideContent", "loadData")
          .contains(method.getNameAsString()))
        method.getAnnotationByName("Override").ifPresent(a -> a.remove());
    }
    String adapted = ShowcaseSourcesMojo.adaptView(cu.toString());
    return adapted.replace("GWT.create(FakeUserService.class)", "new FakeUserService()");
  }

  @Override
  public void execute() throws MojoExecutionException {
    try {
      Path root = input.toPath().resolve("src/main/java"),
          out = output.toPath().toAbsolutePath().normalize();
      Path build = Path.of(project.getBuild().getDirectory()).toAbsolutePath().normalize();
      if (!out.startsWith(build)
          || out.equals(build)
          || out.startsWith(root.toAbsolutePath().normalize()))
        throw new IllegalArgumentException("Output must be a separate generated build directory");
      Files.createDirectories(out);
      try (var files = Files.walk(out)) {
        for (Path p : files.sorted(Comparator.reverseOrder()).toList())
          if (!p.equals(out)) Files.delete(p);
      }
      StringBuilder hashes = new StringBuilder("# Original pinned table showcase inputs\n"),
          cases = new StringBuilder(),
          components = new StringBuilder();
      String base = "gmd/datatable/demo/client/";
      try (var files = Files.walk(root)) {
        for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
          String rel = root.relativize(file).toString();
          boolean page =
              PAGES.stream().anyMatch(p -> rel.startsWith(base + "application/" + p + "/"));
          if (!(page
              || rel.startsWith(base + "generator/")
              || rel.equals(base + "resources/AppResources.java")
              || rel.equals(base + "application/widget/CodeSection.java"))) continue;
          if (!(rel.endsWith(".java") || rel.endsWith(".ui.xml"))) continue;
          if (rel.endsWith("Module.java") || rel.endsWith("/UserService.java")) continue;
          String source = Files.readString(file);
          hashes
              .append(
                  HexFormat.of()
                      .formatHex(
                          MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))))
              .append("  ")
              .append(rel)
              .append('\n');
          if (rel.endsWith("Presenter.java")) {
            var type = StaticJavaParser.parse(source).getType(0).asClassOrInterfaceDeclaration();
            String token = file.getParent().getFileName().toString();
            String viewClass = rel.replace('/', '.').replace("Presenter.java", "View");
            var bind = type.getMethodsByName("onBind").get(0).getBody().orElseThrow();
            var reveal = type.getMethodsByName("onReveal").get(0).getBody().orElseThrow();
            var header =
                reveal.findAll(MethodCallExpr.class).stream()
                    .filter(c -> c.getNameAsString().equals("setHeaderTitle"))
                    .findFirst()
                    .orElseThrow();
            components
                .append("new gmd.core.demo.client.application.navigation.Component(")
                .append(header.getArgument(0))
                .append(",")
                .append(header.getArgument(1))
                .append(",\"table-")
                .append(token)
                .append("\"),\n");
            StringBuilder init = new StringBuilder(), data = new StringBuilder();
            for (var stmt : bind.getStatements()) {
              if (stmt.toString().trim().equals("super.onBind();")) continue;
              if (!stmt.toString().startsWith("getView()."))
                throw new IllegalArgumentException("Changed table bind lifecycle");
              init.append(stmt.toString().replace("getView()", "view"));
            }
            for (var stmt : reveal.getStatements()) {
              if (stmt.toString().trim().equals("super.onReveal();")
                  || stmt.toString().startsWith("setHeaderTitle(")) continue;
              if (!stmt.toString().startsWith("getView()."))
                throw new IllegalArgumentException("Changed table reveal lifecycle");
              data.append(stmt.toString().replace("getView()", "view"));
            }
            cases
                .append("case \"table-")
                .append(token)
                .append("\": { var view=new ")
                .append(viewClass)
                .append("(); ")
                .append(init)
                .append("view.addAttachHandler(event -> { if(event.isAttached()) { ")
                .append(data)
                .append(" } }); return view; }\n");
            continue;
          }
          if (rel.endsWith("View.java")) source = adaptView(source);
          if (rel.endsWith(".ui.xml")) {
            // Keep comparisons in documentation, not in the running TeaVM examples.
            source =
                source.replaceAll(
                    "(?s)<m:MaterialLink\\b[^>]*href=\"https://github.com/GwtMaterialDesign/[^\"]*\"[^>]*/>",
                    "");
            source =
                source.replace(
                    "For the java classes, you can browse theme from the Java Source above",
                    "The original examples below run locally on TeaVM.");
          }
          if (rel.endsWith("UserGenerator.java"))
            source =
                source.replace(
                    "https://i.pinimg.com/originals/7c/c7/a6/7cc7a630624d20f7797cb4c8e93c09c1.png",
                    "data:image/svg+xml;base64,"
                        + java.util.Base64.getEncoder()
                            .encodeToString(
                                Files.readAllBytes(
                                    project
                                        .getBasedir()
                                        .toPath()
                                        .getParent()
                                        .resolve("showcase-teavm/src/site/tables/avatar.svg"))));
          if (rel.endsWith("DataGenerator.java"))
            source =
                source
                    .replace(
                        "MaterialDesign.injectJs(AppResources.INSTANCE.fakerJs());",
                        "MaterialDesign.injectJs(AppResources.INSTANCE.fakerJs()); seed();")
                    .replace(
                        "public class DataGenerator {",
                        "public class DataGenerator { @org.teavm.jso.JSBody(script=\"window.faker.seed(1729);\") private static native void seed();");
          if (rel.endsWith(".java"))
            source = GenerateBindings.transform(LegacyNativeBodies.transform(source));
          Path dest = out.resolve(rel);
          Files.createDirectories(dest.getParent());
          Files.writeString(dest, source);
        }
      }
      Path registry = out.resolve("io/instanto/material/showcase/OriginalTables.java");
      Files.createDirectories(registry.getParent());
      Files.writeString(
          registry,
          StaticJavaParser.parse(
                  "package io.instanto.material.showcase; import com.google.gwt.user.client.ui.Widget; import gmd.datatable.demo.client.generator.DataGenerator; public final class OriginalTables { public static gmd.core.demo.client.application.navigation.Component[] components() { return new gmd.core.demo.client.application.navigation.Component[]{"
                      + components
                      + "}; } public static Widget create(String token) { switch(token) {"
                      + cases
                      + "default: return null; } } }")
              .toString());
      Files.writeString(build.resolve("table-showcase-inputs.sha256"), hashes);
      project.addCompileSourceRoot(out.toString());
    } catch (Exception e) {
      throw new MojoExecutionException("Unable to adapt original table showcase", e);
    }
  }
}
