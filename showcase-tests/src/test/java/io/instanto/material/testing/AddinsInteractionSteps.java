package io.instanto.material.testing;

import static io.instanto.webapp.testkit.dom.Dom.*;
import static org.junit.Assert.*;

import io.instanto.cucumber.tea.CucumberSuite;
import io.instanto.cucumber.tea.Then;
import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLElement;

@CucumberSuite("features/addins-interactions.feature")
public class AddinsInteractionSteps extends MaterialSteps {

  private void open(String name) {
    navigate("addins-" + name);
  }

  @Then("the cropper exports its local image")
  public void cropperExportsItsLocalImage() {
    open("cropper");
    waitFor(() -> assertTrue(cropperReady(find(".croppie-container"))));
    click(findByText("Crop"));
    waitFor(() -> assertFalse(findAll(".modal img[src^='data:image/']").isEmpty()));
    click(findByText("Close"));
  }

  @JSBody(
      params = "element",
      script =
          "var image=element.querySelector('.cr-image');return !!(image && image.style.opacity==='1' && image.width>0 && image.height>0);")
  private static native boolean cropperReady(HTMLElement element);

  @Then("the selected file stays in the local queue")
  public void filesRemainInTheLocalQueue() {
    open("fileuploader");
    queueFile(app.page().root());
    waitFor(() -> assertTrue(find("#catalogue-content").getTextContent().contains("sample.txt")));
    waitFor(() -> assertTrue(localQueue(app.page().root())));
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

  @Then("a late camera stream is stopped after leaving")
  public void cameraStartsOnRequestAndDisposesALateStream() {
    var body = app.page().root();
    fakeCamera(body);
    open("camera");
    assertEquals("0", body.getAttribute("data-camera-requests"));
    click(findByText("play_arrow"));
    waitFor(() -> assertEquals("1", body.getAttribute("data-camera-requests")));
    click(find("a[data-page=button]"));
    finishCamera(body);
    waitFor(() -> assertEquals("1", body.getAttribute("data-camera-stopped")));
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

  @Then("the editor sets and reads the supplied HTML")
  public void richEditorSetsAndReadsHtmlThroughOriginalHandlers() {
    open("richeditor");
    type(find("input[placeholder='Any HTML']"), "<p>TeaVM editor sample</p>");
    click(findByText("Set HTML"));
    waitFor(
        () ->
            assertTrue(
                findAll(".note-editable").stream()
                    .anyMatch(e -> e.getTextContent().contains("TeaVM editor sample"))));
    click(findByText("Get HTML"));
    waitFor(
        () ->
            assertTrue(
                findAll(".toast").stream()
                    .anyMatch(e -> e.getTextContent().contains("TeaVM editor sample"))));
  }

  @Then("the editor clears and inserts text through its original controls")
  public void richEditorClearsAndInsertsTextThroughOriginalHandlers() throws Throwable {
    String stage = "opening the editor";
    try {
      open("richeditor");
      stage = "finding Reset";
      HTMLElement reset = findByRole("button", "Reset");
      stage = "clearing the editor";
      click(reset);
      waitFor(() -> assertTrue(editableForReset(reset).getTextContent().isBlank()));
      stage = "inserting text";
      // A programmatic button click does not place the cursor in the editor.
      placeCaretAtEnd(editableForReset(reset));
      click(findByRole("button", "Insert Material Design"));
      waitFor(
          () -> assertTrue(editableForReset(reset).getTextContent().contains("Material Design")));
      stage = "reading the inserted text";
      click(findByRole("button", "Get Value"));
      waitFor(
          () ->
              assertTrue(
                  findAll(".toast").stream()
                      .anyMatch(e -> e.getTextContent().contains("Material Design"))));
    } catch (Throwable failure) {
      System.out.println("Material rich editor failed while " + stage + ": " + failure);
      throw failure;
    }
  }

  @JSBody(
      params = "resetButton",
      script = "return resetButton.closest('.row').querySelector('.note-editable');")
  private static native HTMLElement editableForReset(HTMLElement resetButton);

  @JSBody(
      params = "editable",
      script =
          """
          editable.focus();
          var document = editable.ownerDocument;
          var range = document.createRange();
          range.selectNodeContents(editable);
          range.collapse(false);
          var selection = document.defaultView.getSelection();
          selection.removeAllRanges();
          selection.addRange(range);
          """)
  private static native void placeCaretAtEnd(HTMLElement editable);

  @Then("the rating publishes its value event")
  public void ratingPublishesTheOriginalValueEvent() {
    open("rating");
    click(findByText("Set Value with Event"));
    waitFor(
        () ->
            assertTrue(
                findAll(".toast").stream()
                    .anyMatch(e -> e.getTextContent().contains("Value : 4"))));
  }

  @Then("a signature can be exported and cleared")
  public void signatureCapturesExportsAndClears() {
    open("signature");
    var canvas = find("#catalogue-content canvas");
    stroke(canvas);
    waitFor(
        () ->
            assertTrue(
                find("#catalogue-content").getTextContent().contains("End Signature Event fired")));
    click(findByText("Get Image Data"));
    waitFor(() -> assertFalse(findAll(".modal img[src^='data:image/png;base64,']").isEmpty()));
    click(findByText("Close"));
    click(findByText("Clear"));
    waitFor(
        () ->
            assertTrue(
                find("#catalogue-content")
                    .getTextContent()
                    .contains("Clear Signature Event fired")));
  }

  @Then("the stepper completes")
  public void stepperCompletesAndResets() {
    open("steppers");
    click(findAllByText("Continue to Step 2").get(0));
    click(findAllByText("Continue to Step 3").get(0));
    click(findAllByText("Finish").get(0));
    waitFor(
        () ->
            assertTrue(
                findAll(".toast").stream().anyMatch(e -> e.getTextContent().contains("All done"))));
  }

  @Then("the window opens and closes through its original controls")
  public void windowOpensAndCloses() {
    open("window");
    click(findByText("Open Window"));
    waitFor(() -> assertFalse(findAll(".window.open").isEmpty()));
    click(findAll(".window.open .window-action").get(0));
    waitFor(() -> assertTrue(findAll(".window.open").isEmpty()));
  }

  @Then("the carousel moves to the requested slide")
  public void carouselMovesToRequestedSlide() {
    open("carousel");
    click(findByText("Go to 2nd slide"));
    click(findByText("Get Current Slide Index"));
    waitFor(
        () ->
            assertTrue(
                findAll(".toast").stream()
                    .anyMatch(e -> e.getTextContent().contains("1 Current Slide Index"))));
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
