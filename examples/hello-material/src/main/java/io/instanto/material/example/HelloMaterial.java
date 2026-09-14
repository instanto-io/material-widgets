package io.instanto.material.example;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.RootPanel;
import gwt.material.design.client.ui.MaterialButton;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialTextBox;

/** Original Material widgets running on the TeaVM compatibility runtime. */
public final class HelloMaterial {
  public static void main(String[] args) {
    new gwt.material.design.client.MaterialDesign().onModuleLoad();
    MaterialTextBox input = new MaterialTextBox();
    input.setPlaceholder("Your name");
    input.getElement().setId("material-name");
    MaterialButton button = new MaterialButton("Greet");
    button.getElement().setId("material-greet");
    MaterialLabel result = new MaterialLabel("Ready");
    result.getElement().setId("material-result");
    button.addClickHandler(event -> result.setText("Hello " + input.getValue()));
    RootPanel.get().add(input);
    RootPanel.get().add(button);
    RootPanel.get().add(result);
    Document.get().getBody().setAttribute("data-ready", "true");
  }
}
