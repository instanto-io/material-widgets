package io.instanto.gwt.testing.api;

import static io.instanto.gwt.testing.api.ContractAssertions.*;

import com.google.gwt.dom.client.*;
import com.google.gwt.event.logical.shared.HasAttachHandlers;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import java.util.ArrayList;
import java.util.List;

/** Typed DOM views must operate on the same browser nodes as generic widget elements. */
public final class MaterialDomContracts {
  private MaterialDomContracts() {}

  public static void elementTextIgnoresPresentationAndPreservesLineBreaks() {
    Label label = new Label();
    RootPanel.get().add(label);
    try {
      Element element = label.getElement();
      element.getStyle().setProperty("textTransform", "uppercase");
      element.setInnerHTML("Fullscreen <span style='display:none'>Slider</span>");
      equal("Fullscreen Slider", element.getInnerText(), "text ignores CSS case and visibility");
      element.setInnerText("First\nSecond <plain>");
      equal("First\nSecond <plain>", element.getInnerText(), "plain text roundtrip");
      equal(1, element.getChildCount(), "line breaks do not create br elements");
      equal("First\nSecond &lt;plain&gt;", element.getInnerHTML(), "plain text has no br elements");
      element.setInnerText(null);
      equal("", element.getInnerText(), "null clears text");
    } finally {
      label.removeFromParent();
    }
  }

  public static void panelInsertionUsesContainerElementsAfterPluginWrapping() {
    com.google.gwt.user.client.ui.FlowPanel panel = new com.google.gwt.user.client.ui.FlowPanel();
    Label image = new Label("wrapped child");
    Label last = new Label("last");
    panel.add(image);
    panel.add(last);
    Element wrapper = Document.get().createDivElement();
    panel.getElement().insertBefore(wrapper, image.getElement());
    wrapper.appendChild(image.getElement());
    panel.getElement().insertBefore(Document.get().createTextNode("spacing"), wrapper);
    Label heading = new Label("heading");
    panel.insert(heading, 0);
    isTrue(
        panel.getElement().getFirstChildElement().getInnerText().equals("heading"),
        "heading inserted before plugin wrapper");
    isTrue(
        wrapper.getFirstChildElement().getInnerText().equals("wrapped child"),
        "plugin wrapper preserved");
    equal(3, panel.getWidgetCount(), "logical children retained");
    isTrue(panel.getWidget(1) == image, "logical image index retained");
    panel.insert(last, 0);
    isTrue(
        panel.getElement().getFirstChildElement().getInnerText().equals("last"),
        "existing child reordered");
    panel.remove(image);
    equal(0, wrapper.getChildCount(), "wrapped widget removed from its physical parent");
    equal(2, panel.getWidgetCount(), "logical removal follows physical removal");
  }

  public static void panelsLoadAfterTheirChildrenAndUnloadBeforeThem() {
    List<String> calls = new ArrayList<>();
    com.google.gwt.user.client.ui.FlowPanel parent =
        new com.google.gwt.user.client.ui.FlowPanel() {
          @Override
          protected void onLoad() {
            isTrue(getWidget(0).isAttached(), "child is attached during parent load");
            calls.add("parent-load");
          }

          @Override
          protected void onUnload() {
            isTrue(getWidget(0).isAttached(), "child remains attached during parent unload");
            calls.add("parent-unload");
          }
        };
    Label child =
        new Label("child") {
          @Override
          protected void onLoad() {
            isTrue(getParent().isAttached(), "parent is marked attached during child load");
            getElement().addClassName("child-ready");
            calls.add("child-load");
          }

          @Override
          protected void onUnload() {
            isTrue(getParent().isAttached(), "parent remains attached during child unload");
            calls.add("child-unload");
          }
        };
    parent.add(child);
    try {
      for (int pass = 0; pass < 2; pass++) {
        RootPanel.get().add(parent);
        parent.removeFromParent();
      }
      equal(
          "[child-load, parent-load, parent-unload, child-unload, child-load, parent-load, parent-unload, child-unload]",
          calls.toString(),
          "panel lifecycle order over repeated mounts");
    } finally {
      parent.removeFromParent();
    }
  }

  public static void typedResourceElementsRetainNativeProperties() {
    Document document = Document.get();
    ImageElement image = document.createImageElement();
    image.setWidth(143);
    image.setHeight(92);
    image.setAlt("A material illustration");
    ImageElement view = ImageElement.as(image);
    equal(143, view.getWidth(), "image width");
    equal(92, view.getHeight(), "image height");
    view.setAlt("Changed through second view");
    equal("Changed through second view", image.getAlt(), "image views share native state");
    equal("143", image.getAttribute("width"), "image width reflected as attribute");
    LinkElement link = document.createLinkElement();
    link.setRel("stylesheet");
    link.setMedia("print");
    equal("stylesheet", link.getAttribute("rel"), "stylesheet relation");
    equal("print", LinkElement.as(link).getMedia(), "stylesheet media");
    StyleElement style = document.createStyleElement();
    style.setType("text/css");
    style.setMedia("screen");
    style.setInnerText(".material-contract { color: red; }");
    equal("screen", StyleElement.as(style).getMedia(), "style media");
    equal(".material-contract { color: red; }", style.getInnerText(), "stylesheet content");
    SourceElement source = document.createSourceElement();
    source.setType("video/mp4");
    equal("video/mp4", source.getAttribute("type"), "media source type");
    IFrameElement frame = document.createIFrameElement();
    frame.setName("material-preview");
    equal("material-preview", frame.getAttribute("name"), "frame name");
  }

  public static void optionViewsTrackInsertionSelectionAndDisabledState() {
    Document document = Document.get();
    SelectElement select = document.createSelectElement();
    OptionElement first = document.createOptionElement();
    first.setText("First option");
    first.setValue("first");
    first.setLabel("First label");
    first.setDefaultSelected(true);
    select.add(first, null);
    OptionElement second = document.createOptionElement();
    second.setText("Second option");
    second.setValue("second");
    select.add(second, first);
    equal(1, first.getIndex(), "insertion updates original option index");
    equal(0, second.getIndex(), "inserted option index");
    equal("First option", select.getOptions().getItem(1).getText(), "live option view text");
    equal(
        "First label",
        select.getOptions().getItem(1).getLabel(),
        "option label distinct from text");
    isTrue(first.isDefaultSelected(), "default selection retained");
    second.setSelected(true);
    isTrue(second.isSelected(), "live selection changes");
    isTrue(!first.isSelected(), "single selection clears prior selection");
    isTrue(first.isDefaultSelected(), "live selection preserves default");
    select.getOptions().getItem(0).setDisabled(true);
    isTrue(second.isDisabled(), "disabled state shared across wrappers");
    second.removeFromParent();
    equal(0, first.getIndex(), "removal updates remaining index");
    OptGroupElement group = document.createOptGroupElement();
    group.setLabel("Choices");
    group.setDisabled(true);
    group.appendChild(first);
    select.appendChild(group);
    equal("Choices", OptGroupElement.as(group).getLabel(), "group label");
    isTrue(group.isDisabled(), "group disabled state");
    equal(1, select.getOptions().getLength(), "options include grouped descendants");
  }

  public static void attachHandlerInterfaceReportsWidgetLifecycle() {
    Label widget = new Label("lifecycle");
    HasAttachHandlers source = widget;
    List<Boolean> calls = new ArrayList<>();
    var registration = source.addAttachHandler(event -> calls.add(event.isAttached()));
    RootPanel.get().add(widget);
    widget.removeFromParent();
    registration.removeHandler();
    RootPanel.get().add(widget);
    widget.removeFromParent();
    equal("[true, false]", calls.toString(), "attach interface and listener removal");
  }
}
