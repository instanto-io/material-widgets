package io.instanto.material.build;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class CameraSourceAdapterTest {
  @Test
  public void changedUpstreamCameraSetupRequiresReview() throws Exception {
    String source =
        Files.readString(
            Path.of(
                "../target/intake/addins/src/main/java/gwt/material/design/addins/client/camera/MaterialCameraCapture.java"));
    String adapted = CameraSourceAdapter.adapt(source);
    assertThrows(IllegalArgumentException.class, () -> CameraSourceAdapter.adapt(adapted));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            CameraSourceAdapter.adapt(
                source.replace("Navigator.getMedia = stream", "changedSetup()")));
    assertThrows(
        IllegalArgumentException.class,
        () -> CameraSourceAdapter.adapt(source.replace("track.stop()", "track.changedStop()")));
  }
}
