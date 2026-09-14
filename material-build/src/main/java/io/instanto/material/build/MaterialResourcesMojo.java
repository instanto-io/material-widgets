package io.instanto.material.build;

import java.io.File;
import java.nio.file.*;
import java.util.Comparator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;

/** Adapts pinned widget resources without changing their source archives. */
@Mojo(
    name = "material-resources",
    defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
    threadSafe = true)
public final class MaterialResourcesMojo extends AbstractMojo {
  @Parameter(required = true)
  private File resourceInput;

  @Parameter(defaultValue = "${project.build.directory}/generated-resources/material")
  private File resourceOutput;

  @Parameter(defaultValue = "${project.build.directory}", readonly = true)
  private File buildDirectory;

  @Override
  public void execute() throws MojoExecutionException {
    try {
      Path root = resourceInput.toPath().toAbsolutePath().normalize();
      Path out = resourceOutput.toPath().toAbsolutePath().normalize();
      Path build = buildDirectory.toPath().toAbsolutePath().normalize();
      if (!out.startsWith(build) || out.equals(build) || out.startsWith(root))
        throw new IllegalArgumentException("Output must be a separate generated build directory");
      Files.createDirectories(out);
      try (var files = Files.walk(out)) {
        for (Path p : files.sorted(Comparator.reverseOrder()).toList())
          if (!p.equals(out)) Files.delete(p);
      }
      try (var files = Files.walk(root)) {
        for (Path file : files.filter(Files::isRegularFile).toList()) {
          Path dest = out.resolve(root.relativize(file));
          Files.createDirectories(dest.getParent());
          String name = file.getFileName().toString();
          if (name.equals("materialize-0.97.5.js") || name.equals("materialize-0.97.5.min.js")) {
            boolean minified = name.endsWith(".min.js");
            Files.writeString(
                dest,
                adaptDatePicker(
                    adaptMedia(adapt(Files.readString(file), minified), minified), minified));
          } else if (name.equals("timepicker.js") || name.equals("timepicker.min.js")) {
            Files.writeString(dest, adaptClock(Files.readString(file), name.endsWith(".min.js")));
          } else if (root.relativize(file)
              .toString()
              .matches("gwt/material/design/client/resources/css/style(\\.min)?\\.css")) {
            // The table's expand action needs viewport positioning independently of its host
            // layout.
            Files.writeString(
                dest,
                Files.readString(file)
                    + """

                .table-container.stretch {
                  position: fixed; inset: 0; z-index: 10000; margin: 0;
                  display: flex; flex-direction: column; background: #fff;
                }
                .table-container.stretch > .table-body {
                  flex: 1; min-height: 0; height: auto !important; overflow: auto;
                }
                """);
          } else {
            Files.copy(file, dest);
          }
        }
      }
    } catch (Exception e) {
      throw new MojoExecutionException("Unable to adapt Materialize resources", e);
    }
  }

  static String adaptClock(String source, boolean minified) {
    String before =
        minified
            ? "P.on(\"focus.lolliclock click.lolliclock\",t.proxy(this.show,this));"
            : "input.on('focus.lolliclock click.lolliclock', $.proxy(this.show, this));";
    // Opening over a pressed input retargets WebKit's eventual click to the body.
    // Pointer activation waits for click; keyboard focus still opens immediately.
    String after =
        """
        (function(input, open) {
          var pressed = false;
          input.on('pointerdown.lolliclock mousedown.lolliclock', function() { pressed = true; })
            .on('pointerup.lolliclock pointercancel.lolliclock', function() { pressed = false; })
            .on('click.lolliclock', function() { pressed = false; open(); })
            .on('focus.lolliclock', function() { if (!pressed) open(); });
        })(%s.proxy(this.show,this));
        """
            .formatted(minified ? "P,t" : "input,$");
    String adapted = replaceOnce(source, before, after);
    return replaceOnce(
        adapted,
        minified
            ? "this.input.off(\"focus.lolliclock click.lolliclock\")"
            : "this.input.off('focus.lolliclock click.lolliclock')",
        "this.input.off('.lolliclock')");
  }

  static String adaptDatePicker(String source, boolean minified) {
    String before =
        minified
            ? "on(\"focus.\"+p.id+\" click.\"+p.id,function(t){t.preventDefault(),m.$root[0].focus()})"
            : "on( 'focus.' + STATE.id + ' click.' + STATE.id, function( event ) {\n                    event.preventDefault()\n                    P.$root[0].focus()\n                })";
    String state = minified ? "p" : "STATE";
    String picker = minified ? "m" : "P";
    String jq = minified ? "t" : "$";
    // Opening on pointer-induced focus changes the eventual click target to the overlay.
    // Wait for that click, while preserving immediate opening from keyboard/programmatic focus.
    String after =
        """
        on('pointerdown.' + %1$s.id + ' mousedown.' + %1$s.id, function() {
          %1$s.instantoPointerPressed = true;
          var release = 'pointerup.' + %1$s.id + ' pointercancel.' + %1$s.id + ' mouseup.' + %1$s.id;
          %3$s(document).off(release).one(release, function() {
            %1$s.instantoPointerPressed = false;
            %3$s(document).off(release);
          });
        }).on('focus.' + %1$s.id + ' click.' + %1$s.id, function(event) {
          if (event.type === 'focus' && %1$s.instantoPointerPressed) return;
          %1$s.instantoPointerPressed = false;
          event.preventDefault();
          %2$s.$root[0].focus();
        })
        """
            .formatted(state, picker, jq)
            .strip();
    return replaceOnce(source, before, after);
  }

  static String adaptMedia(String source, boolean minified) {
    String slider = minified ? "o" : "$this";
    for (String event : new String[] {"sliderPause", "sliderStart", "sliderNext", "sliderPrev"}) {
      String quote = minified ? "\"" : "'";
      source =
          replaceOnce(
              source,
              slider + ".on(" + quote + event + quote,
              slider + ".on('" + event + ".instantoMaterialSlider'");
    }
    String panEnd =
        minified
            ? ".bind(\"panend\",function(t){\"touch\"===t.gesture.pointerType&&($curr_slide=r.find"
            : ".bind('panend', function(e) {\n          if (e.gesture.pointerType === \"touch\") {\n\n            $curr_slide";
    source = replaceOnce(source, panEnd, panEnd.replace("panend", "panend.instantoMaterialSlider"));
    source =
        replaceOnce(
            source,
            minified
                ? "o.hammer({prevent_default:!1}).bind(\"pan\",function(t)"
                : "$this.hammer({\n            prevent_default: false\n        }).bind('pan', function(e)",
            slider
                + ".hammer({prevent_default:false}).bind('pan.instantoMaterialSlider',function("
                + (minified ? "t" : "e")
                + ")");

    String image = minified ? "r" : "origin";
    String jq = minified ? "t" : "$";
    String ancestors = minified ? "e" : "ancestorsChanged";
    String active = minified ? "n" : "overlayActive";
    String wrap = minified ? "r.wrap(s),r.on(\"click\"," : "origin.wrap(placeholder);";
    String setup =
        """
        var ns='.instantoMaterialBox'+Materialize.guid();
        var originalStyle=%s.attr('style');
        %s.data('instantoMaterialBoxDispose',function(){
          %s(window).off(ns); %s(document).off(ns);
          %s.off(ns).velocity('stop',true);
          if(%s){
            %s('#materialbox-overlay,.materialbox-caption').velocity('stop',true).detach();
            if(%s) %s.css('overflow','');
          }
          %s.removeClass('active initialized');
          if(originalStyle === undefined) %s.removeAttr('style'); else %s.attr('style',originalStyle);
          var holder=%s.parent('.material-placeholder');
          if(holder.length) { %s.insertBefore(holder); holder.remove(); }
          %s.removeData('instantoMaterialBoxDispose');
        });
        """
            .formatted(
                image, image, jq, jq, image, active, jq, ancestors, ancestors, image, image, image,
                image, image, image);
    source =
        replaceOnce(
            source, wrap, minified ? "r.wrap(s);" + setup + "r.on('click'+ns," : wrap + setup);
    if (!minified) source = replaceOnce(source, "origin.on('click',", "origin.on('click'+ns,");
    source =
        replaceOnce(
            source,
            minified
                ? "t(window).scroll(function(){n&&l()})"
                : "$(window).scroll(function() {\n        if (overlayActive )",
            minified
                ? "t(window).on('scroll'+ns,function(){n&&l()})"
                : "$(window).on('scroll'+ns,function() {\n        if (overlayActive )");
    source =
        replaceOnce(
            source,
            minified
                ? "t(document).keyup(function(t){27===t.keyCode"
                : "$(document).keyup(function(e) {\n\n        if (e.keyCode === 27",
            minified
                ? "t(document).on('keyup'+ns,function(t){27===t.keyCode"
                : "$(document).on('keyup'+ns,function(e) {\n\n        if (e.keyCode === 27");
    return replaceOnce(
        source,
        minified ? "e.css(\"overflow\",\"\")" : "ancestorsChanged.css('overflow', '');",
        minified
            ? "e&&e.css(\"overflow\",\"\")"
            : "if (ancestorsChanged) ancestorsChanged.css('overflow', '');");
  }

  static String adapt(String source, boolean minified) {
    String tab = minified ? "e" : "$this";
    String jq = minified ? "t" : "$";
    String prepare =
        "var ns="
            + tab
            + ".data('instantoMaterialTabsNamespace');"
            + "if(!ns){ns='.instantoMaterialTabs'+Materialize.guid();"
            + tab
            + ".data('instantoMaterialTabsNamespace',ns);}"
            + jq
            + "(window).off('resize'+ns);"
            + tab
            + ".find('.indicator').velocity('stop',true).detach();";
    String append = tab + ".append('<div class=\"indicator\"></div>');";
    source =
        minified
            ? replaceOnce(source, "n=t(i[0].hash)," + append, "n=t(i[0].hash);" + prepare + append)
            : replaceOnce(source, append, prepare + append);
    String resize =
        minified
            ? "t(window).resize(function(){o=e.width(),r=e.find(\"li\").first().outerWidth()"
            : "$(window).resize(function () {\n        $tabs_width = $this.width();";
    String resized = resize.replace(".resize(function", ".on('resize'+ns,function");
    source = replaceOnce(source, resize, resized);
    String click =
        minified ? "e.on(\"click\",\"a\",function(u)" : "$this.on('click', 'a', function(e)";
    return replaceOnce(
        source,
        click,
        tab
            + ".off('click.instantoMaterialTabs','a').on('click.instantoMaterialTabs','a',function("
            + (minified ? "u" : "e")
            + ")");
  }

  private static String replaceOnce(String source, String before, String after) {
    int at = source.indexOf(before);
    if (at < 0 || source.indexOf(before, at + before.length()) >= 0)
      throw new IllegalArgumentException("Changed pinned Materialize resource: " + before);
    return source.substring(0, at) + after + source.substring(at + before.length());
  }
}
