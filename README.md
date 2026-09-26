# Material Widgets for TeaVM

Material Widgets runs the original [GWT Material Design](https://github.com/GwtMaterialDesign/gwt-material)
Java widgets on TeaVM. It preserves the upstream `gwt.material.design.*`
packages and adapts the browser integration. This is an independent port;
`io.instanto` identifies its different publisher.

The port is still growing. Check [widget coverage](docs/COMPATIBILITY.md) when
choosing a feature.

**[Explore the TeaVM showcase](https://instanto-io.github.io/material-widgets/).**
It runs the adapted upstream example views, including full-page navigation,
tables and addins. The [showcase guide](docs/SHOWCASE.md) links to the examples,
their Java source and the upstream demos for comparison.

## Start with a button

The [Hello Material application](examples/hello-material) shows the complete
host page and browser assets. Load the Material stylesheet and JavaScript
before starting your TeaVM entry point. Initialise Material before creating
widgets, and attach widgets through `RootPanel` so their lifecycle runs:

```java
package io.instanto.material.example;

import com.google.gwt.user.client.ui.RootPanel;
import gwt.material.design.client.MaterialDesign;
import gwt.material.design.client.ui.MaterialButton;

public final class HelloMaterial {
    public static void main(String[] args) {
        new MaterialDesign().onModuleLoad();
        RootPanel.get().add(new MaterialButton("Hello Material"));
    }
}
```

The host page and asset layout are in the
[example application](examples/hello-material). Keep the extracted
`gwt-material/` directory together so fonts and icons load correctly.

## Add input

Inside `main`, replace the button with a text box, button and result label.
Import `MaterialTextBox` and `MaterialLabel` from
`gwt.material.design.client.ui`:

```java
MaterialTextBox name = new MaterialTextBox();
name.setPlaceholder("Your name");
MaterialButton greet = new MaterialButton("Greet");
MaterialLabel result = new MaterialLabel("Ready");
greet.addClickHandler(event -> result.setText("Hello " + name.getValue()));
RootPanel.get().add(name);
RootPanel.get().add(greet);
RootPanel.get().add(result);
```

To group these widgets, import `MaterialPanel` from the same package and
replace the three root additions:

```java
MaterialPanel form = new MaterialPanel();
form.add(name);
form.add(greet);
form.add(result);
RootPanel.get().add(form);
```

## Explore tables and addins

The table extension uses the same initialisation pattern. After initialising
`MaterialDesign`, initialise the table resources and add a typed column:

```java
new gwt.material.design.client.MaterialTable().onModuleLoad();
gwt.material.design.client.ui.table.MaterialDataTable<String> table =
    new gwt.material.design.client.ui.table.MaterialDataTable<>();
table.addColumn("Name", new gwt.material.design.client.ui.table.cell.TextColumn<String>() {
    @Override public String getValue(String name) { return name; }
});
RootPanel.get().add(table);
java.util.List<String> names = java.util.List.of("Ada", "Grace", "Katherine");
table.setVisibleRange(0, names.size());
table.setRowData(0, names);
```

See the [table showcase](docs/SHOWCASE.md#table-examples) for paging,
grouped rows and custom rendering. The [addins showcase](docs/SHOWCASE.md#addins)
covers editors, ratings, signatures and other optional widgets. Some addins
need browser capabilities or third-party services; their showcase notes
describe those requirements.

## Learn more

- [Widget coverage](docs/COMPATIBILITY.md): verified behaviour and limitations.
- [Showcase guide](docs/SHOWCASE.md): runnable examples and original sources.
- [Port design](docs/DESIGN.md): changes to native bindings, UiBinder, resources
  and the showcase shell.
- [Build and test](docs/BUILDING.md): maintainer instructions.

## Upstream credits

The original widgets and Java API are the work of
[GWT Material Design and its contributors](https://github.com/GwtMaterialDesign/gwt-material).
Its [jQuery/JSCore bindings](https://github.com/GwtMaterialDesign/gwt-material-jquery)
and [core showcase](https://github.com/GwtMaterialDesign/gmd-core-demo) supply the
native declarations, example views, UiBinder templates and descriptions used here.
The additional widgets, including the editor, cropper, signature pad and text-field controls, come from [Material Addins](https://github.com/GwtMaterialDesign/gwt-material-addins).
Tables come from [Material Table](https://github.com/GwtMaterialDesign/gwt-material-table),
with original views and fixtures from its [table showcase](https://github.com/GwtMaterialDesign/gmd-table-demo).
The browser resources include [Materialize](https://github.com/Dogfalo/materialize)
and [jQuery](https://github.com/jquery/jquery).

[GWT](https://github.com/gwtproject/gwt) provides the original Java browser APIs,
and [TeaVM](https://github.com/konsoletyper/teavm), by Alexey Andreev and its
contributors, makes this port possible. Instanto maintains the TeaVM adaptation,
compatibility integration and showcase shell.

Original copyright notices are retained. See [NOTICE](NOTICE), [LICENSE](LICENSE)
and the [source lock](upstream/assessment-lock.json) for attribution, licences and
exact upstream revisions. Browser libraries and fonts retain their own licences.

## Support the projects

Like GWT Material Design? Please [support the upstream project](https://gwtmaterialdesign.github.io/gwt-material-demo/)
using the PayPal Donate button under **Support Us** in the showcase footer.
You can also [contribute fixes and improvements](https://github.com/GwtMaterialDesign/gwt-material/blob/master/CONTRIBUTING.md).

Using the TeaVM port? Please [support TeaVM](https://github.com/sponsors/konsoletyper).

Want to see this port and more TeaVM libraries maintained? Please [sponsor this port](https://github.com/sponsors/instanto-io).
