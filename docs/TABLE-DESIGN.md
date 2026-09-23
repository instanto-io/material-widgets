# Adapting Material Table

The table artifact compiles the original Material Table Java implementation
against the same compatibility libraries as the core widgets. It is a separate
dependency, `io.instanto:gwt-material-table-teavm`, so an application using only
core widgets need not include it. `MaterialTable.onModuleLoad()` loads its original
table CSS and JavaScript plugins after core Material initialisation.

Maven downloads and verifies pinned archives for Material Table and
`gmd-table-demo`. Revisions are in the [source lock](../upstream/assessment-lock.json);
archive SHA-256 values are in the root POM. The generated artifacts retain source
licences and input manifests. Upstream source trees and their GWT builds are not
checked into this repository.

## The original showcase views

The six original views, UiBinder templates, descriptions, columns and handlers
are retained. The generated launcher substitutes a `Composite` construction
shell for GWTP and Gin. It runs the presenter's original `onBind` setup before
attachment and its `onReveal` data setup after attachment. Compiler tests compare
the retained view fields, methods and constructor bodies with the pinned input.
`@UiField(provided = true)` keeps the infinite view's configured table and data source.

The original option panels appear below their tables in the shared catalogue.
The original local generators supply user and product data. The infinite example
uses upstream's asynchronous `FakeUserService` and its 125 generated users;
it does not require an RPC server. Faker's random source is seeded for repeatable
names and values. The product tax generator and simulated service delays retain
their original Java randomness, so these examples do not promise identical totals
or timing between visits.

The external portrait is replaced by an original Instanto SVG fixture, embedded
as a data URI. This removes the external image request and also works through
TeaVM's test resource server, which does not set an SVG content type. The fixture
source is [avatar.svg](../showcase-teavm/src/site/tables/avatar.svg). There are no
upstream screenshots or external demo destinations in the running table examples;
comparison links are in the [showcase guide](SHOWCASE.md).

The Faker bundle comes unchanged from the pinned table demo. Its
[MIT notice](../showcase-teavm/src/site/licenses/faker-MIT.txt), crediting Matthew
Bergman, Marak Squires and the original data contributors, is reproduced from the
[historical Faker distribution](https://www.npmjs.com/package/faker/v/4.1.0).
The demo does not declare its bundled Faker version; the source archive and
generated input manifest identify the actual code used here.

## Compatibility boundaries

Reusable changes live in `teavm-compat`:

- Typed table rows, cells and sections, live cell collections, table layout,
  number-format operations and the small scheduling/event API surface used by tables.
- Native DOM identity for `JavaScriptObject.equals` and `hashCode`, allowing
  a row obtained through jQuery to recover the Java model held by the table.
- Conversion of generic native callback arguments and results, including boxed
  numbers and GWT DOM wrappers. Weakly cached adapters preserve callback identity
  when a listener is removed. Native DOM property accessors use the same boundary.
- UiBinder provided fields and generic handler resolution using the owner's
  declared field type.

Table-specific source adaptations stay here. `StickyTableOptions` becomes a native
object with property setters; it no longer subclasses GWT's JavaScript overlay.
The legacy `Js.asList` helper uses the compatibility array view instead of writing
GWT's private `ArrayList` storage. Column toggle identifiers use integer indices,
avoiding a TeaVM `0.0` suffix where the original code relies on GWT's `0` formatting.

The shared API suite runs against TeaVM and native GWT. It tests the number
patterns used by the views, BigDecimal precision, parse positions, native row
identity and live table collections. Number formatting currently follows the
compatibility runtime's default US locale; these checks do not establish all
currencies, locales or time-zone behaviours.

## Browser verification

`showcase-tests` runs Cucumber Tea scenarios through TeaVMTestRunner and Webapp
Testkit. Each scenario opens the application assembled by Maven, then uses DOM
actions and layout checks against its original handlers. The local table fixture
needs no mock service. Java Playwright provides a separate WebKit check, including
screenshot capture.

See [build commands](BUILDING.md) and [coverage and remaining limits](COMPATIBILITY.md).

## Table expansion

The toolbar's square icon toggles the original table `stretch` class. The pinned
stylesheet gives that class full width and height but no viewport positioning.
The resource adapter adds fixed positioning and a scrolling table body, so the
table can expand out of the surrounding page layout. Removing the class restores
the normal layout. Detaching an expanded table also clears the body's scroll lock.
