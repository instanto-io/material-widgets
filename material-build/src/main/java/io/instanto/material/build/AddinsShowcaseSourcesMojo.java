package io.instanto.material.build;

import com.github.javaparser.StaticJavaParser;
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

/** Retains the original addins views and UiBinder templates in a local TeaVM catalogue. */
@Mojo(
    name = "addins-showcase-sources",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    threadSafe = true)
public final class AddinsShowcaseSourcesMojo extends AbstractMojo {
  @Parameter(required = true)
  private File input;

  @Parameter(defaultValue = "${project.build.directory}/generated-sources/addins-showcase")
  private File output;

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  @Override
  public void execute() throws MojoExecutionException {
    try {
      Path root = input.toPath().resolve("src/main/java"),
          out = output.toPath().toAbsolutePath().normalize();
      Path build = Path.of(project.getBuild().getDirectory()).toAbsolutePath().normalize();
      if (!out.startsWith(build) || out.equals(build) || out.startsWith(root.toAbsolutePath()))
        throw new IllegalArgumentException("Unsafe addins output");
      Files.createDirectories(out);
      try (var files = Files.walk(out)) {
        for (Path p : files.sorted(Comparator.reverseOrder()).toList())
          if (!p.equals(out)) Files.delete(p);
      }
      String base = "gwt/material/design/demo/client/";
      StringBuilder cases = new StringBuilder(),
          sources = new StringBuilder(),
          components = new StringBuilder(),
          hashes = new StringBuilder();
      try (var files = Files.walk(root)) {
        for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
          String rel = root.relativize(file).toString();
          boolean page =
              rel.startsWith(base + "application/addins/")
                  && (rel.endsWith("View.java") || rel.endsWith("View.ui.xml"));
          boolean helper =
              (rel.startsWith(base + "application/dto/")
                  || rel.startsWith(base + "application/addins/autocomplete/base/")
                  || rel.startsWith(base + "application/addins/masonry/cards/")
                  || rel.equals(base + "ui/PrettyCode.java")
                  || rel.equals(base + "ui/PrettyPre.java")
                  || rel.equals(base + "ui/ExternalLibrary.java")
                  || rel.equals(base + "ui/constants/Styles.java")
                  || rel.equals(base + "resources/MaterialResources.java"));
          if (!page && !helper) continue;
          String original = Files.readString(file), source = original;
          if (page && rel.endsWith("View.java")) {
            source =
                ShowcaseSourcesMojo.adaptView(
                    source.replace(
                        "import com.google.inject.Inject;", "import javax.inject.Inject;"));
            var cu = StaticJavaParser.parse(source);
            // Presenter-interface annotations no longer apply to the Composite shell.
            cu.getType(0)
                .asClassOrInterfaceDeclaration()
                .getMethods()
                .forEach(m -> m.getAnnotationByName("Override").ifPresent(a -> a.remove()));
            source = cu.toString();
            String name = file.getFileName().toString().replace("View.java", ""),
                route = "addins-" + file.getParent().getFileName();
            String qualified = rel.substring(0, rel.length() - 5).replace('/', '.');
            Path presenter =
                file.resolveSibling(
                    file.getFileName().toString().replace("View.java", "Presenter.java"));
            if (name.equals("Tree")) presenter = file.resolveSibling("TreeViewPresenter.java");
            var presenterCode = StaticJavaParser.parse(Files.readString(presenter));
            hashes
                .append(
                    HexFormat.of()
                        .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                .digest(Files.readAllBytes(presenter))))
                .append("  ")
                .append(root.relativize(presenter))
                .append('\n');
            var heading =
                presenterCode.findAll(com.github.javaparser.ast.expr.MethodCallExpr.class).stream()
                    .filter(
                        m ->
                            m.getScope()
                                    .map(Object::toString)
                                    .orElse("")
                                    .equals("SetPageTitleEvent")
                                && m.getNameAsString().equals("fire"))
                    .findFirst()
                    .orElseThrow();
            cases
                .append("case \"")
                .append(route)
                .append("\": return new ")
                .append(qualified)
                .append("();\n");
            sources
                .append("case \"")
                .append(route)
                .append("\": return \"")
                .append(rel)
                .append("\";\n");
            components
                .append("new gmd.core.demo.client.application.navigation.Component(")
                .append(heading.getArgument(0))
                .append(",")
                .append(
                    route.equals("addins-cropper")
                        ? "\"Crop, rotate and export a sample image.\""
                        : heading.getArgument(1))
                .append(",\"")
                .append(route)
                .append("\"),\n");
          }
          if (rel.endsWith(".java")) {
            if (rel.endsWith("docviewer/DocViewerView.java")) {
              var doc = StaticJavaParser.parse(source);
              var type = doc.getType(0).asClassOrInterfaceDeclaration();
              type.addMember(
                  StaticJavaParser.parseBodyDeclaration(
                      "@com.google.gwt.uibinder.client.UiField gwt.material.design.client.ui.MaterialPanel viewerHost;"));
              type.addMember(
                  StaticJavaParser.parseBodyDeclaration(
                      "@com.google.gwt.uibinder.client.UiField gwt.material.design.client.ui.MaterialTextBox documentUrl;"));
              type.addMember(
                  StaticJavaParser.parseBodyDeclaration(
                      "                  @com.google.gwt.uibinder.client.UiHandler(\"loadDocument\")\n                  void loadDocument(com.google.gwt.event.dom.client.ClickEvent event) {\n                    String url = documentUrl.getValue();\n                    if (url == null || !url.startsWith(\"https://\")) {\n                      documentUrl.setErrorText(\"Enter a public HTTPS document URL\");\n                      return;\n                    }\n                    documentUrl.clearErrorText();\n                    viewerHost.clear();\n                    gwt.material.design.addins.client.docviewer.MaterialDocViewer viewer =\n                        new gwt.material.design.addins.client.docviewer.MaterialDocViewer(url);\n                    viewer.setWidth(\"100%\");\n                    viewer.setHeight(\"600px\");\n                    viewerHost.add(viewer);\n                  }\n                  "));
              source = doc.toString();
            }
            source =
                source
                    .replace(
                        "gwt.material.design.addins.client.richeditor.events.PasteEvent",
                        "gwt.material.design.client.events.PasteEvent")
                    .replace("MaterialTextInputMask", "MaterialInputMask")
                    .replace(
                        "gwt.material.design.incubator.client.alert.Alert",
                        "gwt.material.design.client.ui.MaterialLabel")
                    .replace("Alert browserNotSupported", "MaterialLabel browserNotSupported")
                    .replace("browserNotSupported.close()", "browserNotSupported.setVisible(false)")
                    .replace("browserNotSupported.open()", "browserNotSupported.setVisible(true)")
                    .replace("uploader.addCancelHandler(", "uploader.addCanceledHandler(")
                    .replace(
                        "for (Widget w : dpMode.getItems())", "for (Object w : dpMode.getItems())");
            source = GenerateBindings.transform(LegacyNativeBodies.transform(source));
          } else if (rel.endsWith(".ui.xml")) {
            if (rel.endsWith("cropper/ImageCropperView.ui.xml"))
              source = source.replace("https://i.imgur.com/CiPPh6h.jpg", "addins/images/image.png");
            source =
                source
                    .replaceAll("(?s)<demo:ExternalLibrary\\b[^>]*/>", "")
                    .replace("ma:inputmask.MaterialTextInputMask", "ma:inputmask.MaterialInputMask")
                    .replace("<in:alert.Alert", "<m:MaterialLabel")
                    .replace(" type=\"ERROR\"", " textColor=\"RED\"");
            if (rel.endsWith("camera/CameraView.ui.xml")) {
              source =
                  source.replace(
                      "<ma:camera.MaterialCameraCapture ",
                      "<ma:camera.MaterialCameraCapture autoPlay=\"false\" ");
              source =
                  source.replace(
                      "This is called when the component is loaded.",
                      "In this showcase, press Play to start the camera.");
            }
            if (rel.endsWith("fileuploader/FileUploaderView.ui.xml")) {
              source =
                  source.replace(
                      "<ma:fileuploader.MaterialFileUploader ",
                      "<ma:fileuploader.MaterialFileUploader autoProcessQueue=\"false\" ");
              source =
                  source
                      .replace("title=\"Note\"", "title=\"Local preview only\"")
                      .replace(
                          "The File Upload Addin component is just a Client UI,",
                          "Files stay in your browser in this showcase. Automatic uploading is disabled. To upload in your application,");
            }
            if (rel.endsWith("docviewer/DocViewerView.ui.xml")) {
              source =
                  source
                      .replace(
                          "<ma:docviewer.MaterialDocViewer width=\"100%\" height=\"600px\" url=\"http://www.africau.edu/images/default/sample.pdf\" />",
                          "                  <m:MaterialLabel text=\"This widget uses Google Docs Viewer. Enter a public HTTPS document URL, then press Open document. Google must be able to fetch the file; local or private files will not work.\" />\n                  <m:MaterialTextBox ui:field=\"documentUrl\" label=\"Public document URL\" />\n                  <m:MaterialButton ui:field=\"loadDocument\" text=\"Open document\" />\n                  <m:MaterialPanel ui:field=\"viewerHost\" />\n                  ")
                      .replace("text=\"File1.doc\"", "text=\"Public document\"");
            }
          }
          source = localAssets(source, input.toPath());
          hashes
              .append(
                  HexFormat.of()
                      .formatHex(
                          MessageDigest.getInstance("SHA-256")
                              .digest(original.getBytes(java.nio.charset.StandardCharsets.UTF_8))))
              .append("  ")
              .append(rel)
              .append('\n');
          Path dest = out.resolve(rel);
          Files.createDirectories(dest.getParent());
          Files.writeString(dest, source);
        }
      }
      Path imageInput =
          input
              .toPath()
              .resolve("src/main/resources/gwt/material/design/demo/client/resources/img");
      Path imageOutput = build.resolve("addins-site/addins/images");
      Files.createDirectories(imageOutput);
      try (var images = Files.list(imageInput)) {
        for (Path image : images.filter(Files::isRegularFile).toList())
          Files.copy(
              image, imageOutput.resolve(image.getFileName()), StandardCopyOption.REPLACE_EXISTING);
      }
      Path theme = out.resolve(base + "ThemeManager.java");
      Files.writeString(
          theme,
          "package gwt.material.design.demo.client; import gwt.material.design.client.base.MaterialWidget; import gwt.material.design.client.constants.Color; public final class ThemeManager { public static final int REGULAR_SHADE=0,DARKER_SHADE=1,LIGHTER_SHADE=2; public static void register(MaterialWidget w){register(w,REGULAR_SHADE);} public static void register(MaterialWidget w,int shade){w.setBackgroundColor(shade==DARKER_SHADE?Color.BLUE_DARKEN_3:shade==LIGHTER_SHADE?Color.BLUE_LIGHTEN_2:Color.BLUE);} }");
      var originals = new org.apache.maven.model.Resource();
      originals.setDirectory(input.toPath().resolve("src/main/resources").toString());
      originals.addInclude("gwt/material/design/demo/client/resources/**");
      project.addResource(originals);
      Path catalogue = out.resolve("io/instanto/material/showcase/OriginalAddins.java");
      Files.createDirectories(catalogue.getParent());
      Files.writeString(
          catalogue,
          "package io.instanto.material.showcase;\npublic final class OriginalAddins {\n"
              + "public static com.google.gwt.user.client.ui.Widget create(String route){switch(route){"
              + cases
              + "default:return null;}}\n"
              + "public static String source(String route){switch(route){"
              + sources
              + "default:return null;}}\n"
              + "public static gmd.core.demo.client.application.navigation.Component[] components(){return new gmd.core.demo.client.application.navigation.Component[]{"
              + components
              + "};}\n}");
      Files.writeString(build.resolve("addins-showcase-inputs.sha256"), hashes);
      project.addCompileSourceRoot(out.toString());
      var resource = new org.apache.maven.model.Resource();
      resource.setDirectory(out.toString());
      resource.addInclude("**/*.ui.xml");
      project.addResource(resource);
    } catch (Exception e) {
      throw new MojoExecutionException("Cannot adapt original addins showcase", e);
    }
  }

  static String localAssets(String source, Path input) {
    var urls =
        java.util.regex.Pattern.compile("https?://[a-zA-Z0-9:/._%-]+?[.](?:png|jpe?g|gif|webp)")
            .matcher(source);
    var result = new StringBuffer();
    Path images = input.resolve("src/main/resources/gwt/material/design/demo/client/resources/img");
    while (urls.find()) {
      String url = urls.group(), name = url.substring(url.lastIndexOf('/') + 1);
      if (!Files.isRegularFile(images.resolve(name)) && name.endsWith(".webp")) {
        String stem = name.substring(0, name.length() - 5);
        name = Files.isRegularFile(images.resolve(stem + ".png")) ? stem + ".png" : stem + ".jpg";
      }
      if (!Files.isRegularFile(images.resolve(name)))
        name =
            url.contains("webp/gallery")
                ? "logo" + (url.endsWith(".webp") ? ".webp" : ".png")
                : "profile.jpg";
      urls.appendReplacement(
          result, java.util.regex.Matcher.quoteReplacement("addins/images/" + name));
    }
    return urls.appendTail(result).toString();
  }
}
