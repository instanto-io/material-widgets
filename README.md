# Material Widgets for TeaVM

An independent TeaVM adaptation of [GWT Material Design](https://github.com/GwtMaterialDesign/gwt-material), preserving upstream Java packages and widget implementations.

> **Early stage.** This is an early port of GWT Material Design to TeaVM. Many widgets work, but
> coverage is incomplete and the API and packaging may still change. Check the
> [port status](docs/COMPATIBILITY.md) before relying on a widget.

Original Material widgets now compile and run on TeaVM. The showcase uses upstream views, UiBinder templates, descriptions and event handlers. See [port status](docs/COMPATIBILITY.md) for coverage and limitations, and the [showcase guide](docs/SHOWCASE.md) for examples. Development snapshots are published to [packages.instanto.io](https://packages.instanto.io); there is no fixed release yet.

We use `io.instanto` coordinates to distinguish this adaptation from upstream and avoid confusion about ownership or endorsement. The build produces the TeaVM widget port.

## Try the showcase

[Explore the TeaVM showcase](https://instanto-io.github.io/material-widgets/), with original widget examples running on TeaVM. Upstream comparisons are in the [showcase guide](docs/SHOWCASE.md). To run it locally:

Use JDK 21 and Maven, with access to the published development snapshots as
described in [building locally](docs/BUILDING.md):

```sh
mvn clean verify
jwebserver -b 127.0.0.1 -p 8098 -d "$PWD/showcase-teavm/target/site"
```

Open [the local showcase](http://127.0.0.1:8098/). Examples run locally on TeaVM.
The navbar, sidebar and combined-tab galleries open separate full-page TeaVM
examples, with their adapted source available alongside each demo.

## Use the widgets

The [standalone Hello Material example](examples/hello-material) has a complete
Maven build and host page. It consumes the widget and asset JARs without inheriting
this repository's parent POM or reading any sibling source checkout.

Start with JDK 21, Maven and a browser. Configure Maven to read development
snapshots from packages.instanto.io using the [Instanto parent instructions](https://github.com/instanto-io/instanto-poms#use-a-parent).
Copy [`examples/hello-material`](examples/hello-material) for a standalone
application: its POM configures TeaVM 0.15.0 and extracts the matching browser
assets. Install a local `teavm-compat` checkout only when testing changes that
have not yet been published.

### 1. Add the widget and asset dependencies

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>gwt-material-widgets-teavm</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>gwt-material-widgets-assets</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

The compatibility dependencies arrive transitively. Keep upstream `gwt-user` off
the TeaVM classpath. The example POM also adds `teavm-classlib` and the TeaVM
Maven compiler plugin, with `io.instanto.material.example.HelloMaterial` as its `mainClass`.

### 2. Provide the application harness

Serve the extracted `gwt-material/` asset directory beside `app.js` and this host
page. The example build creates this layout under `target/site`:

```html
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Hello Material</title>
  <link rel="stylesheet" href="gwt-material/css/materialize.min.css">
</head>
<body>
  <script src="gwt-material/js/jquery-3.5.1.min.js"></script>
  <script src="app.js"></script>
  <script>main();</script>
</body>
</html>
```

jQuery loads first. In Java, initialise Material before creating widgets, then
attach them through `RootPanel` so their lifecycle handlers run:

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

### 3. Add input and an event handler

Replace the single button in `main`, after initialisation, with the following.
Also import `gwt.material.design.client.ui.MaterialTextBox` and `MaterialLabel`:

```java
MaterialTextBox name = new MaterialTextBox("Your name");
MaterialButton greet = new MaterialButton("Greet");
MaterialLabel result = new MaterialLabel("Ready");
greet.addClickHandler(event -> result.setText("Hello " + name.getValue()));
RootPanel.get().add(name);
RootPanel.get().add(greet);
RootPanel.get().add(result);
```

### 4. Compose a screen

Group those widgets in a `gwt.material.design.client.ui.MaterialPanel`. Replace
the three `RootPanel` additions above with:

```java
MaterialPanel form = new MaterialPanel();
form.add(name);
form.add(greet);
form.add(result);
RootPanel.get().add(form);
```

From the copied example directory, run `mvn clean verify`, then
`jwebserver -b 127.0.0.1 -p 8098 -d target/site` and open
[the application](http://127.0.0.1:8098/). Keep the complete asset directory when
deploying. The [showcase launcher](showcase-teavm/src/main/java/io/instanto/material/client/MaterialTeaVm.java)
and [host page](showcase-teavm/src/site/index.html) demonstrate the additional
fonts and resources used by richer examples.

### 5. Add a table when you need one

Add `io.instanto:gwt-material-table-teavm:0.1.0-SNAPSHOT` alongside the core
dependency. After `MaterialDesign` initialisation, initialise the table resources
and build a typed column:

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

Continue with the [table showcase examples](docs/SHOWCASE.md#table-examples) for
paging, grouped rows, custom rendering and infinite loading. Their original
Java views are built directly from the pinned table demo.

### 6. Add optional widgets

For rich editing, signatures, image cropping and the other addins, add:

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>gwt-material-addins-teavm</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Keep the application harness and core assets from the earlier steps. Addin widgets
load their own bundled scripts and styles when used. For example:

```java
MaterialRating rating = new MaterialRating();
rating.setValue(4);
RootPanel.get().add(rating);
```

Import `gwt.material.design.addins.client.rating.MaterialRating`. Explore the
[addins examples](docs/SHOWCASE.md#addins) for larger compositions. Camera access,
uploads and external document viewers have additional requirements described there.

## Documentation

- [Build and test](docs/BUILDING.md): dependencies, local showcase and browser checks.
- [Port design](docs/DESIGN.md): changed elements, including native bindings,
  UiBinder, resource loading and the showcase's replacement of GWTP and Gin.
- [Table design](docs/TABLE-DESIGN.md): retained views, local fixtures and shared compatibility changes.
- [Port status](docs/COMPATIBILITY.md): verified behaviour and remaining limitations.
- [Showcase guide](docs/SHOWCASE.md): example links and upstream comparisons.
- [Roadmap](PLAN.md): remaining implementation and release work.
- [Migration](docs/MIGRATION.md): replacing the existing Verrai Material wrappers.
- [Publishing](docs/PUBLISHING.md): local builds and the GitHub Pages deploy.

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

## Shared build parent

For local builds, install the shared parent from a sibling `instanto-poms`
checkout with `mvn -f ../instanto-poms/pom.xml install`. Release instructions
are in `instanto-poms/RELEASING.md`.
