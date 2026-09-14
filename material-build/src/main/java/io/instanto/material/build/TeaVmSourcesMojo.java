package io.instanto.material.build;

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

/** Applies shared compatibility generators to the checksum-verified upstream source intake. */
@Mojo(name = "teavm-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public final class TeaVmSourcesMojo extends AbstractMojo {
  @Parameter(required = true)
  private File input;

  @Parameter(defaultValue = "${project.build.directory}/generated-sources/teavm")
  private File output;

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  @Override
  public void execute() throws MojoExecutionException {
    try {
      Path root = input.toPath().toAbsolutePath().normalize(),
          out = output.toPath().toAbsolutePath().normalize();
      Path build = Path.of(project.getBuild().getDirectory()).toAbsolutePath().normalize();
      if (!out.startsWith(build) || out.equals(build) || out.startsWith(root))
        throw new IllegalArgumentException("Output must be a separate generated build directory");
      Files.createDirectories(out);
      try (var files = Files.walk(out)) {
        for (Path p : files.sorted(Comparator.reverseOrder()).toList())
          if (!p.equals(out)) Files.delete(p);
      }
      StringBuilder hashes = new StringBuilder();
      Path callbackSource =
          project
              .getBasedir()
              .toPath()
              .getParent()
              .resolve(
                  "target/intake/jquery/src/main/java/gwt/material/design/jquery/client/api/Functions.java");
      var callbacks =
          new io.instanto.compat.NativeCallbackBindings(List.of(Files.readString(callbackSource)));
      try (var files = Files.walk(root)) {
        for (Path file : files.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
          String original = Files.readString(file), source = original;
          String relative = root.relativize(file).toString();
          if (relative.equals("gwt/material/design/client/ui/MaterialDatePicker.java")) {
            String before = "picker.off(\"close\");";
            if (!source.contains(before) || source.indexOf(before) != source.lastIndexOf(before))
              throw new IllegalArgumentException("Review changed date picker disposal lifecycle");
            // Release body-attached popups, document listeners and scroll locks on detach/reload.
            source = source.replace(before, before + "\n            picker.stop();");
            String selected = "getPicker().get(\"select\").obj";
            if (!source.contains(selected))
              throw new IllegalArgumentException("Review changed date picker value conversion");
            source = source.replace(selected, "getPicker().get(\"select\").getDateObject()");
          }
          if (relative.equals("gwt/material/design/client/js/JsMaterialElement.java")) {
            String before = "@JsProperty\n    public JsDate obj;";
            if (!source.contains(before))
              throw new IllegalArgumentException("Review changed native date picker property");
            // The shared native method boundary wraps the browser Date in GWT's JsDate.
            source =
                source.replace(
                    before, "@JsProperty(name = \"obj\") public native JsDate getDateObject();");
            String setter = "public native JsMaterialElement set(String key, Object value);";
            if (!source.contains(setter))
              throw new IllegalArgumentException("Review changed native date picker setter");
            // Keep date arguments typed so the same shared boundary unwraps them before jQuery.
            source =
                source.replace(
                    setter,
                    setter
                        + """

                @JsMethod(name = "set")
                public native JsMaterialElement set(String key, JsDate value);
                @JsMethod(name = "set")
                public native JsMaterialElement set(String key, JsDate value, Functions.Func function);
                """);
          }
          if (relative.equals("gwt/material/design/client/ui/table/MaterialDataTable.java")) {
            String before = "((Double) index + colOffset)";
            if (!source.contains(before))
              throw new IllegalArgumentException("Changed table column index conversion");
            source = source.replace(before, "(((Number) index).intValue() + colOffset)");
            if (source.contains("void onUnload()"))
              throw new IllegalArgumentException("Review changed table disposal lifecycle");
            int end = source.lastIndexOf('}');
            source =
                source.substring(0, end)
                    + """
                @Override
                protected void onUnload() {
                  if ($this().hasClass(TableCssName.STRETCH)) {
                    $this().removeClass(TableCssName.STRETCH);
                    body().removeClass(TableCssName.OVERFLOW_HIDDEN);
                  }
                  super.onUnload();
                }
                """
                    + source.substring(end);
          }
          if (relative.equals("gwt/material/design/client/js/StickyTableOptions.java")) {
            if (!source.contains("extends JavaScriptObject")
                || !source.contains("JavaScriptObject.createObject().cast()"))
              throw new IllegalArgumentException("Changed sticky table options overlay");
            source =
                source
                    .replace(
                        "import com.google.gwt.core.client.JavaScriptObject;",
                        "import jsinterop.annotations.*;")
                    .replace(
                        "public class StickyTableOptions extends JavaScriptObject",
                        "@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = \"Object\") public class StickyTableOptions")
                    .replace("JavaScriptObject.createObject().cast()", "new StickyTableOptions()")
                    .replace(
                        "public native final void setScrollableArea(JQueryElement scrollableArea) /*-{\n        this.scrollableArea = scrollableArea;\n    }-*/;",
                        "@JsProperty public native void setScrollableArea(JQueryElement scrollableArea);")
                    .replace(
                        "public native final void setMarginTop(int marginTop) /*-{\n        this.marginTop = marginTop;\n    }-*/;",
                        "@JsProperty public native void setMarginTop(int marginTop);");
          }
          if (relative.equals("gwt/material/design/client/js/Js.java")) {
            String before =
                "public static native <T> List<T> asList(JavaScriptObject o) /*-{\n        var l = @java.util.ArrayList::new()();\n        l.@java.util.ArrayList::array = o;\n        return l;\n    }-*/;";
            if (!source.contains(before))
              throw new IllegalArgumentException("Changed table ArrayList internals bridge");
            source =
                source.replace(
                    before,
                    "public static <T> List<T> asList(JavaScriptObject o) { return (List<T>) (List<?>) jsinterop.base.Js.asArrayLike(o.unwrap()).asList(); }");
          }
          if (relative.equals("gwt/material/design/addins/client/combobox/MaterialComboBox.java")) {
            String before = "return Integer.parseInt(o.toString());";
            if (!source.contains(before))
              throw new IllegalArgumentException("Changed combo box index conversion");
            source =
                source.replace(
                    before,
                    "return o instanceof Number ? ((Number) o).intValue() : Integer.parseInt(o.toString());");
            String array = "Object[] curVal = (Object[]) getJsComboBox().val();";
            if (!source.contains(array))
              throw new IllegalArgumentException("Changed combo box selection array");
            source =
                source.replace(
                    array,
                    "Object rawValues = getJsComboBox().val(); Object[] curVal = rawValues == null ? null : jsinterop.base.Js.asArrayLike(rawValues).asList().toArray();");
          }
          if (relative.equals("gwt/material/design/client/ui/MaterialSlider.java")) {
            var cu = com.github.javaparser.StaticJavaParser.parse(source);
            var method =
                cu.getType(0).asClassOrInterfaceDeclaration().getMethodsByName("unload").get(0);
            if (!method.getBody().orElseThrow().getStatements().isEmpty())
              throw new IllegalArgumentException("Changed slider unload lifecycle");
            method.setBody(
                com.github.javaparser.StaticJavaParser.parseBlock(
                    "{ io.instanto.material.client.MediaLifecycle.unloadSlider(getElement()); }"));
            source = cu.toString();
          }
          if (relative.equals("gwt/material/design/client/ui/MaterialImage.java")) {
            var cu = com.github.javaparser.StaticJavaParser.parse(source);
            var type = cu.getType(0).asClassOrInterfaceDeclaration();
            if (!type.getMethodsByName("onUnload").isEmpty())
              throw new IllegalArgumentException("Changed image unload lifecycle");
            type.addMember(
                com.github.javaparser.StaticJavaParser.parseBodyDeclaration(
                    "@Override protected void onUnload() { io.instanto.material.client.MediaLifecycle.unloadImage(getElement()); super.onUnload(); }"));
            source = cu.toString();
          }
          if (relative.equals("gwt/material/design/client/ui/MaterialTab.java")) {
            String before = "public void unload() {\n        clearAllIndicators();\n    }";
            if (!source.contains(before))
              throw new IllegalArgumentException("Changed tab unload lifecycle");
            source =
                source.replace(
                    before,
                    "public void unload() { io.instanto.material.client.TabLifecycle.unload(getElement()); }");
          }
          if (relative.equals(
              "gwt/material/design/addins/client/dark/AddinsDarkThemeLoader.java")) {
            var cu = com.github.javaparser.StaticJavaParser.parse(source);
            var retained =
                Set.of(
                    "MaterialAutoCompleteDarkTheme",
                    "MaterialComboBoxDarkTheme",
                    "MaterialTimePickerDarkTheme");
            cu.getImports()
                .removeIf(
                    i ->
                        i.getNameAsString().startsWith("gwt.material.design.addins.client.")
                            && !retained.contains(i.getName().getIdentifier()));
            var calls =
                cu.findAll(com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt.class);
            if (calls.size() != 1 || calls.get(0).getArguments().size() != 15)
              throw new IllegalArgumentException("Changed addins dark theme registry");
            calls
                .get(0)
                .getArguments()
                .removeIf(arg -> !retained.contains(arg.asObjectCreationExpr().getTypeAsString()));
            source = cu.toString();
          }
          // GWT overlays allow window to be typed as Element. This view is passed straight
          // back to native jQuery; the shared boundary unwraps it to the actual window.
          if (relative.equals("gwt/material/design/jquery/client/api/JQuery.java")) {
            String before = "return ScriptInjector.TOP_WINDOW.cast();";
            if (!source.contains(before))
              throw new IllegalArgumentException("Changed JQuery.window overlay");
            source =
                source.replace(
                    before,
                    "return new Element((org.teavm.jso.dom.html.HTMLElement) ScriptInjector.TOP_WINDOW.unwrap());");
          }
          if (relative.equals(
              "gwt/material/design/client/pwa/serviceworker/ServiceWorkerLifecycle.java")) {
            source =
                source.replace("import com.google.web.bindery.requestfactory.shared.Service;", "");
          }
          // Legacy GWT Element and dom.Element denote the same native element. The
          // compatibility API exposes the current dom package consistently.
          source =
              source.replace(
                  "import com.google.gwt.user.client.Element;",
                  "import com.google.gwt.dom.client.Element;");
          source =
              callbacks.transform(GenerateBindings.transform(LegacyNativeBodies.transform(source)));
          Path dest = out.resolve(relative);
          Files.createDirectories(dest.getParent());
          Files.writeString(dest, source);
          hashes
              .append(
                  HexFormat.of()
                      .formatHex(
                          MessageDigest.getInstance("SHA-256")
                              .digest(original.getBytes(java.nio.charset.StandardCharsets.UTF_8))))
              .append("  ")
              .append(relative)
              .append('\n');
        }
      }
      Files.writeString(Path.of(project.getBuild().getDirectory(), "teavm-inputs.sha256"), hashes);
      project.addCompileSourceRoot(out.toString());
    } catch (Exception e) {
      throw new MojoExecutionException("Unable to adapt original sources for TeaVM", e);
    }
  }
}
