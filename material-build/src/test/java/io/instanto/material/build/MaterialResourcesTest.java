package io.instanto.material.build;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class MaterialResourcesTest {
  @Test
  public void cropperCleanupGuardRejectsChangedPinnedResources() throws Exception {
    for (String suffix : new String[] {".js", ".min.js"}) {
      String original =
          Files.readString(
              Path.of(
                  "../target/intake/addins/src/main/resources/gwt/material/design/addins/client/cropper/resources/js/croppie"
                      + suffix));
      boolean minified = suffix.equals(".min.js");
      String adapted = MaterialResourcesMojo.adaptCropper(original, minified);
      assertThrows(
          IllegalArgumentException.class,
          () -> MaterialResourcesMojo.adaptCropper(adapted, minified));
      assertThrows(
          IllegalArgumentException.class,
          () ->
              MaterialResourcesMojo.adaptCropper(
                  original.replace(".then(", ".changedThen("), minified));
    }
  }

  @Test
  public void datePickerAdaptationRejectsChangedInputInBothResources() throws Exception {
    for (String suffix : new String[] {".js", ".min.js"}) {
      String original =
          Files.readString(
              Path.of(
                  "../target/intake/core/gwt-material/src/main/resources/gwt/material/design/client/resources/js/materialize-0.97.5"
                      + suffix));
      boolean minified = suffix.equals(".min.js");
      String adapted = MaterialResourcesMojo.adaptDatePicker(original, minified);
      assertThrows(
          IllegalArgumentException.class,
          () -> MaterialResourcesMojo.adaptDatePicker(adapted, minified));
      assertThrows(
          IllegalArgumentException.class,
          () ->
              MaterialResourcesMojo.adaptDatePicker(
                  original.replace("focus.", "changedFocus."), minified));
    }
  }

  @Test
  public void adaptsMediaLifecycleInBothPinnedResources() throws Exception {
    for (String suffix : new String[] {".js", ".min.js"}) {
      String original =
          Files.readString(
              Path.of(
                  "../target/intake/core/gwt-material/src/main/resources/gwt/material/design/client/resources/js/materialize-0.97.5"
                      + suffix));
      boolean minified = suffix.equals(".min.js");
      String adapted = MaterialResourcesMojo.adaptMedia(original, minified);
      assertThrows(
          IllegalArgumentException.class,
          () -> MaterialResourcesMojo.adaptMedia(adapted, minified));
      assertThrows(
          IllegalArgumentException.class,
          () ->
              MaterialResourcesMojo.adaptMedia(
                  original.replace("sliderPause", "changedPause"), minified));
    }
  }

  @Test
  public void adaptsBothClockResourcesAndRejectsChangedInputs() throws Exception {
    for (String suffix : new String[] {".js", ".min.js"}) {
      String original =
          Files.readString(
              Path.of(
                  "../target/intake/addins/src/main/resources/gwt/material/design/addins/client/timepicker/resources/js/timepicker"
                      + suffix));
      boolean minified = suffix.equals(".min.js");
      String adapted = MaterialResourcesMojo.adaptClock(original, minified);
      assertThrows(
          IllegalArgumentException.class,
          () -> MaterialResourcesMojo.adaptClock(adapted, minified));
      assertThrows(
          IllegalArgumentException.class,
          () ->
              MaterialResourcesMojo.adaptClock(
                  original.replace("focus.lolliclock", "focus.changed"), minified));
    }
  }

  @Test
  public void adaptsBothPinnedResourcesAndRejectsChangedInputs() throws Exception {
    for (String suffix : new String[] {".js", ".min.js"}) {
      String original =
          Files.readString(
              Path.of(
                  "../target/intake/core/gwt-material/src/main/resources/gwt/material/design/client/resources/js/materialize-0.97.5"
                      + suffix));
      boolean minified = suffix.equals(".min.js");
      String adapted = MaterialResourcesMojo.adapt(original, minified);
      assertTrue(
          adapted.contains(
              ".off('click.instantoMaterialTabs','a').on('click.instantoMaterialTabs','a'"));
      assertThrows(
          IllegalArgumentException.class, () -> MaterialResourcesMojo.adapt(adapted, minified));
      assertThrows(
          IllegalArgumentException.class,
          () ->
              MaterialResourcesMojo.adapt(
                  original.replace("indicator", "changed-indicator"), minified));
    }
  }
}
