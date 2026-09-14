package io.instanto.material.build;

import com.github.javaparser.StaticJavaParser;
import java.io.File;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

/** Original full-page navigation examples with the GWTP construction boundary replaced. */
@Mojo(name = "pattern-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public final class PatternSourcesMojo extends AbstractMojo {
  static final Map<String, String> PAGES =
      Map.ofEntries(
          Map.entry("navbar_default", "navbardefault"),
          Map.entry("navbar_fixed", "navbarfixed"),
          Map.entry("navbar_tall", "navbartall"),
          Map.entry("navbar_extend", "navbarextend"),
          Map.entry("navbar_tab", "navbartabs"),
          Map.entry("navbar_shrink", "navbarshrink"),
          Map.entry("navbar_tab_push", "navbartabspush"),
          Map.entry("sidenav_fixed", "sidenavfixed"),
          Map.entry("sidenav_drawer", "sidenavdrawer"),
          Map.entry("sidenav_drawer_header", "sidenavdrawerheader"),
          Map.entry("sidenav_push", "sidenavpush"),
          Map.entry("sidenav_push_header", "sidenavpushheader"),
          Map.entry("sidenav_card", "sidenavcard"),
          Map.entry("sidenav_mini", "sidenavmini"),
          Map.entry("sidenav_mini_expandable", "sidenavminiexpand"),
          Map.entry("sidenav_edge", "sidenavedge"),
          Map.entry("sidenav_colaps", "sidenavcollapsible"),
          Map.entry("sidenav_content", "sidenavcontent"));

  @Parameter(required = true)
  private File input;

  @Parameter(defaultValue = "${project.build.directory}/generated-sources/patterns")
  private File output;

  @Parameter(defaultValue = "${project.build.directory}/pattern-site")
  private File siteOutput;

  @Parameter(defaultValue = "${project.basedir}/../showcase-teavm/src/site/patterns/files.svg")
  private File illustration;

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  @Override
  public void execute() throws MojoExecutionException {
    try {
      Path root =
          input.toPath().resolve("src/main/java/com/github/gwtmaterialdesign/client/application");
      Path out = output.toPath().toAbsolutePath().normalize();
      Path site = siteOutput.toPath().toAbsolutePath().normalize();
      Path build = Path.of(project.getBuild().getDirectory()).toAbsolutePath().normalize();
      for (Path directory : List.of(out, site)) {
        if (!directory.startsWith(build) || directory.equals(build))
          throw new IllegalArgumentException("Output must be below the build directory");
        Files.createDirectories(directory);
        try (var files = Files.walk(directory)) {
          for (Path file : files.sorted(Comparator.reverseOrder()).toList())
            if (!file.equals(directory)) Files.delete(file);
        }
      }
      StringBuilder cases = new StringBuilder(), hashes = new StringBuilder();
      String imageUrl =
          "data:image/svg+xml;base64,"
              + Base64.getEncoder().encodeToString(Files.readAllBytes(illustration.toPath()));
      for (var page : new TreeMap<>(PAGES).entrySet()) {
        Path folder = root.resolve(page.getValue());
        try (var files = Files.list(folder)) {
          for (Path file : files.sorted().toList()) {
            String name = file.getFileName().toString();
            if (!name.endsWith("View.java") && !name.endsWith("View.ui.xml")) continue;
            String original = Files.readString(file);
            hashes
                .append(
                    HexFormat.of()
                        .formatHex(
                            MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))))
                .append("  ")
                .append(root.relativize(file))
                .append('\n');
            String source =
                name.endsWith(".java")
                    ? ShowcaseSourcesMojo.adaptView(original)
                    : adaptTemplate(original, page.getValue(), name)
                        .replace("patterns/files.svg", imageUrl);
            Path destination =
                out.resolve(
                    "com/github/gwtmaterialdesign/client/application/"
                        + page.getValue()
                        + "/"
                        + name);
            Files.createDirectories(destination.getParent());
            Files.writeString(destination, source);
            Path publishedSource = site.resolve("pattern-sources/" + page.getValue() + "/" + name);
            Files.createDirectories(publishedSource.getParent());
            Files.writeString(publishedSource, source);
            if (name.endsWith(".java")) {
              cases
                  .append("case \"")
                  .append(page.getKey())
                  .append("\": return new com.github.gwtmaterialdesign.client.application.")
                  .append(page.getValue())
                  .append('.')
                  .append(name.substring(0, name.length() - 5))
                  .append("();\n");
            }
          }
        }
      }
      Path registry = out.resolve("io/instanto/material/showcase/OriginalPatterns.java");
      Files.createDirectories(registry.getParent());
      Files.writeString(
          registry,
          StaticJavaParser.parse(
                  "package io.instanto.material.showcase; import com.google.gwt.user.client.ui.Widget; public final class OriginalPatterns { public static Widget create(String token) { switch(token) {"
                      + cases
                      + "default: return null;}}}")
              .toString());
      Files.writeString(build.resolve("pattern-inputs.sha256"), hashes);
      project.addCompileSourceRoot(out.toString());
    } catch (Exception e) {
      throw new MojoExecutionException("Unable to adapt navigation patterns", e);
    }
  }

  static String adaptTemplate(String source, String folder, String name) {
    // These pinned examples use ordinary CSS, with no GSS expressions or transformations.
    String background =
        "<ui:style gss=\"true\">\n        body {\n            background: #e9e9e9;\n        }\n    </ui:style>";
    if (!source.contains(background))
      throw new IllegalArgumentException("Review changed pattern stylesheet: " + name);
    source = source.replace(background, background.replace(" gss=\"true\"", ""));
    // Expansion is the default in the pinned core; the older template's setter was removed.
    if (folder.equals("sidenavminiexpand")) source = source.replace(" expandable=\"true\"", "");
    // Original layout and handlers are retained; demonstrations and source links stay local.
    source =
        source.replaceAll(
            "href=\"https://github.com/GwtMaterialDesign/gwt-material-patterns/[^\"]*\"",
            "href=\"pattern-sources/" + folder + "/" + name + "\"");
    return source.replaceAll("url=\"https?://[^\"]*\"", "url=\"patterns/files.svg\"");
  }
}
