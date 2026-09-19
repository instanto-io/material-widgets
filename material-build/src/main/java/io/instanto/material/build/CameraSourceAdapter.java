package io.instanto.material.build;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;

/** Uses the current media API and disposes streams obtained after a widget is detached. */
final class CameraSourceAdapter {
  static String adapt(String source) {
    var cu = StaticJavaParser.parse(source);
    var type = cu.getType(0).asClassOrInterfaceDeclaration();
    type.addField("int", "captureRequest", com.github.javaparser.ast.Modifier.Keyword.PRIVATE);
    MethodDeclaration supported = type.getMethodsByName("isSupported").get(0);
    if (!supported.toString().contains("Navigator.webkitGetUserMedia"))
      throw new IllegalArgumentException("Changed camera support check");
    supported.setBody(StaticJavaParser.parseBlock("{ return supportsMediaDevices(); }"));
    type.addMember(
        StaticJavaParser.parseBodyDeclaration(
            """
        @org.teavm.jso.JSBody(script = "return !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia);")
        private static native boolean supportsMediaDevices();
        """));
    var play = type.getMethodsByName("nativePlay").get(0);
    if (!play.toString().contains("Navigator.getMedia = stream"))
      throw new IllegalArgumentException("Changed camera stream setup");
    play.setBody(
        StaticJavaParser.parseBlock(
            """
        {
          if (mediaStream != null) { VideoElement.as(video).play(); return; }
          final int request = ++captureRequest;
          Constraints constraints = new Constraints();
          constraints.audio = false;
          MediaTrackConstraints settings = new MediaTrackConstraints();
          settings.width = width;
          settings.height = height;
          settings.facingMode = facingMode.getName();
          constraints.video = settings;
          Navigator.mediaDevices.getUserMedia(constraints).then((streamObj) -> {
            MediaStream acquired = (MediaStream) streamObj;
            if (request != captureRequest || !isAttached()) {
              for (MediaStreamTrack track : acquired.getTracks()) track.stop();
              return null;
            }
            mediaStream = acquired;
            video.setPropertyObject("srcObject", mediaStream);
            VideoElement.as(video).play();
            onCameraCaptureLoad();
            return null;
          }).fail((error, unused) -> {
            if (request == captureRequest && isAttached()) onCameraCaptureError(String.valueOf(error));
            return null;
          });
        }
        """));
    var stop = type.getMethodsByName("stop").get(0);
    if (!stop.toString().contains("track.stop()"))
      throw new IllegalArgumentException("Changed camera cleanup");
    stop.getBody()
        .orElseThrow()
        .addStatement(0, StaticJavaParser.parseStatement("captureRequest++;"));
    stop.getBody().orElseThrow().addStatement("mediaStream = null;");
    stop.getBody()
        .orElseThrow()
        .addStatement("video.getElement().setPropertyObject(\"srcObject\", null);");
    return cu.toString();
  }
}
