package io.instanto.material.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.RootPanel;
import gwt.material.design.client.ui.MaterialButton;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialTextBox;

/** Original Material widgets running on the TeaVM compatibility runtime. */
public final class MaterialTeaVm {
  public static void main(String[] args) {
    new gwt.material.design.client.MaterialDesign().onModuleLoad();
    new gwt.material.design.client.MaterialTable().onModuleLoad();
    String query = com.google.gwt.user.client.Window.Location.getQueryString();
    if (query.startsWith("?pattern=")) {
      io.instanto.material.showcase.NavigationPatterns.start(query.substring(9).split("&")[0]);
      return;
    }
    if (!com.google.gwt.user.client.Window.Location.getQueryString().contains("fixture")) {
      new io.instanto.material.showcase.MaterialShowcase("TeaVM").start();
      return;
    }
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
