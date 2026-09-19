package io.instanto.material.testing;

import static org.junit.Assert.*;

import io.instanto.webapp.testkit.app.ApplicationRule;
import io.instanto.webapp.testkit.dom.Dom;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class AddinsInteractionsTest {
  @Rule
  public ApplicationRule app =
      new ApplicationRule("/resources/applications/material/index.html#!button")
          .readyWhen(p -> "button".equals(p.root().getAttribute("data-showcase-page")))
          .sized(1280, 900);

  private void open(String name) {
    Dom.click(Dom.find("a[data-page=addins-" + name + "]"));
    Dom.waitFor(
        () ->
            assertEquals(
                "addins-" + name,
                app.application().page().root().getAttribute("data-showcase-page")));
  }

  @Test
  public void cropperExportsItsLocalImage() {
    open("cropper");
    Dom.waitFor(() -> assertTrue(cropperReady(Dom.find(".croppie-container"))));
    Dom.click(Dom.findByText("Crop"));
    Dom.waitFor(() -> assertFalse(Dom.findAll(".modal img[src^='data:image/']").isEmpty()));
    Dom.click(Dom.findByText("Close"));
  }

  @JSBody(
      params = "element",
      script =
          "var image=element.querySelector('.cr-image');return !!(image && image.style.opacity==='1' && image.width>0 && image.height>0);")
  private static native boolean cropperReady(HTMLElement element);

  @Test
  public void filesRemainInTheLocalQueue() {
    open("fileuploader");
    queueFile(app.application().page().root());
    Dom.waitFor(
        () -> assertTrue(Dom.find("#catalogue-content").getTextContent().contains("sample.txt")));
    Dom.waitFor(() -> assertTrue(localQueue(app.application().page().root())));
  }

  @JSBody(
      params = "body",
      script =
          """
      var w=body.ownerDocument.defaultView,transfer=new w.DataTransfer();
      transfer.items.add(new w.File(['Local showcase sample'],'sample.txt',{type:'text/plain'}));
      var input=body.querySelector('input.dz-hidden-input');
      input.files=transfer.files; input.dispatchEvent(new w.Event('change',{bubbles:true}));
      """)
  private static native void queueFile(HTMLElement body);

  @JSBody(
      params = "body",
      script =
          """
      var instances=body.ownerDocument.defaultView.Dropzone.instances;
      return instances.some(function(d){return d.files.some(function(f){return f.name==='sample.txt' && f.status==='queued';}) && d.options.autoProcessQueue===false;});
      """)
  private static native boolean localQueue(HTMLElement body);

  @Test
  public void cameraStartsOnRequestAndDisposesALateStream() {
    var body = app.application().page().root();
    fakeCamera(body);
    open("camera");
    assertEquals("0", body.getAttribute("data-camera-requests"));
    Dom.click(Dom.findByText("play_arrow"));
    Dom.waitFor(() -> assertEquals("1", body.getAttribute("data-camera-requests")));
    Dom.click(Dom.find("a[data-page=button]"));
    finishCamera(body);
    Dom.waitFor(() -> assertEquals("1", body.getAttribute("data-camera-stopped")));
  }

  @JSBody(
      params = "body",
      script =
          """
      var w=body.ownerDocument.defaultView;
      body.setAttribute('data-camera-requests','0');
      Object.defineProperty(w.navigator,'mediaDevices',{configurable:true,value:{
        getUserMedia:function(){
          body.setAttribute('data-camera-requests',String(+body.getAttribute('data-camera-requests')+1));
          return new w.Promise(function(resolve){w.finishTestCamera=function(){resolve({getTracks:function(){return [{stop:function(){body.setAttribute('data-camera-stopped','1');}}];}});};});
        }
      }});
      """)
  private static native void fakeCamera(HTMLElement body);

  @JSBody(params = "body", script = "body.ownerDocument.defaultView.finishTestCamera();")
  private static native void finishCamera(HTMLElement body);

  @Test
  public void richEditorSetsAndReadsHtmlThroughOriginalHandlers() {
    open("richeditor");
    Dom.type(Dom.find("input[placeholder='Any HTML']"), "<p>TeaVM editor sample</p>");
    Dom.click(Dom.findByText("Set HTML"));
    Dom.waitFor(
        () ->
            assertTrue(
                Dom.findAll(".note-editable").stream()
                    .anyMatch(e -> e.getTextContent().contains("TeaVM editor sample"))));
    Dom.click(Dom.findByText("Get HTML"));
    Dom.waitFor(
        () ->
            assertTrue(
                Dom.findAll(".toast").stream()
                    .anyMatch(e -> e.getTextContent().contains("TeaVM editor sample"))));
  }

  @Test
  public void ratingPublishesTheOriginalValueEvent() {
    open("rating");
    Dom.click(Dom.findByText("Set Value with Event"));
    Dom.waitFor(
        () ->
            assertTrue(
                Dom.findAll(".toast").stream()
                    .anyMatch(e -> e.getTextContent().contains("Value : 4"))));
  }

  @Test
  public void signatureCapturesExportsAndClears() {
    open("signature");
    var canvas = Dom.find("#catalogue-content canvas");
    stroke(canvas);
    Dom.waitFor(
        () ->
            assertTrue(
                Dom.find("#catalogue-content")
                    .getTextContent()
                    .contains("End Signature Event fired")));
    Dom.click(Dom.findByText("Get Image Data"));
    Dom.waitFor(
        () -> assertFalse(Dom.findAll(".modal img[src^='data:image/png;base64,']").isEmpty()));
    Dom.click(Dom.findByText("Close"));
    Dom.click(Dom.findByText("Clear"));
    Dom.waitFor(
        () ->
            assertTrue(
                Dom.find("#catalogue-content")
                    .getTextContent()
                    .contains("Clear Signature Event fired")));
  }

  @Test
  public void stepperCompletesAndResets() {
    open("steppers");
    Dom.click(Dom.findAllByText("Continue to Step 2").get(0));
    Dom.click(Dom.findAllByText("Continue to Step 3").get(0));
    Dom.click(Dom.findAllByText("Finish").get(0));
    Dom.waitFor(
        () ->
            assertTrue(
                Dom.findAll(".toast").stream()
                    .anyMatch(e -> e.getTextContent().contains("All done"))));
  }

  @JSBody(
      params = "canvas",
      script =
          """
          var w=canvas.ownerDocument.defaultView,r=canvas.getBoundingClientRect();
          var Event=w.PointerEvent || w.MouseEvent, prefix=w.PointerEvent ? 'pointer' : 'mouse';
          function send(target,type,x,y,buttons) {
            target.dispatchEvent(new Event(prefix+type,{bubbles:true,
              clientX:r.left+x,clientY:r.top+y,button:0,buttons:buttons}));
          }
          send(canvas,'down',25,25,1);
          send(canvas,'move',90,60,1);
          send(canvas.ownerDocument,'up',90,60,0);
          """)
  private static native void stroke(HTMLElement canvas);
}
