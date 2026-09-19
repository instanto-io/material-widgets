package io.instanto.material.build;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class UploadSourceAdapterTest {
  @Test
  public void changedUpstreamMetadataConversionRequiresReview() throws Exception {
    String source =
        Files.readString(
            Path.of(
                "../target/intake/addins/src/main/java/gwt/material/design/addins/client/fileuploader/MaterialFileUploader.java"));
    String adapted = UploadSourceAdapter.adapt(source);
    assertThrows(IllegalArgumentException.class, () -> UploadSourceAdapter.adapt(adapted));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            UploadSourceAdapter.adapt(
                source.replace("Double.parseDouble(file.size)", "file.getSize()")));
  }
}
