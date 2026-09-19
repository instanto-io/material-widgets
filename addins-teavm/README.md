# Selected Material addins for TeaVM

This module provides selected original WebP image, autocomplete, combo-box,
input-mask and time-picker implementations. These support the core showcase’s
images and text-field examples; other addins are not included yet.

Add `io.instanto:gwt-material-addins-teavm:0.1.0-SNAPSHOT` alongside the core
widgets. The upstream Java package is unchanged:

```java
import gwt.material.design.addins.client.webp.MaterialWebpImage;

MaterialWebpImage image = new MaterialWebpImage();
image.setUrl("images/card.webp");
image.setFallbackUrl("images/card.jpg");
```

The build downloads upstream sources at their pinned revision and adapts them
for TeaVM. See [port design](../docs/DESIGN.md#selected-addins) and
[build instructions](../docs/BUILDING.md).

Credit belongs to [GWT Material](https://github.com/GwtMaterialDesign/gwt-material-addins).
To support its maintainers, use **Support Us** in the
[upstream demo footer](https://gwtmaterialdesign.github.io/gwt-material-demo/).
