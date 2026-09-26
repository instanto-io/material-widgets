# Port design

For the table library, original table examples and their compatibility boundaries,
see [Adapting Material Table](TABLE-DESIGN.md).

This port retains GWT Material's widget implementations and Java packages while
adapting their GWT and JavaScript dependencies for TeaVM. The showcase has a
separate application shell so the original examples can run without GWTP or Gin.

This document explains those changes. [Port status](COMPATIBILITY.md) records
verified behaviour and limitations; the [showcase guide](SHOWCASE.md) links to
available examples and their upstream equivalents.

## What changes from upstream

| Element | TeaVM adaptation | Where it belongs |
|---|---|---|
| Material widgets | Preserve `gwt.material.design.*` packages and widget logic; generate adapted native calls | This repository's core and jQuery builds |
| GWT browser APIs | Use browser-backed implementations of the APIs the widgets call | Shared `gwt-user-compat` |
| JsInterop and JSNI | Generate TeaVM JSO bindings, including conversion of Java DOM wrappers | Shared binding generator and compatibility libraries |
| UiBinder and deferred binding | Generate ordinary Java implementations and select explicit providers | Shared `gwt-uibinder-processor`, with Material provider declarations here |
| Showcase GWTP presenters and routing | Construct selected original views directly and navigate with GWT `History` | Showcase source adapter and launcher |
| Showcase Gin injection | Replace injected Binder construction with generated `GWT.create` support | Showcase source adapter |
| Browser resources | Load the matching upstream jQuery, Materialize, plugins and fonts | Asset JAR, resource providers and host page |
| Maven coordinates | Publish the adaptation under `io.instanto` | This repository's POMs |

The coordinate change distinguishes our adaptation from upstream's work and avoids
confusion about ownership or endorsement. It does not rename upstream Java packages.

## Replacing GWTP and Gin in the showcase

GWTP supplies the upstream showcase's presenter/view framework and navigation;
Gin supplies dependency injection. Neither is part of the Material widget API.
The port replaces their use in the showcase so its examples can demonstrate the
widgets without requiring ports of both application frameworks.

The [showcase source adapter](../material-build/src/main/java/io/instanto/material/build/ShowcaseSourcesMojo.java)
changes each selected view's construction boundary:

- `extends ViewImpl implements …Presenter.MyView` becomes `extends Composite`.
- The `ViewImpl` and `Inject` imports and presenter interface are removed.
- The existing injected `Binder` constructor becomes public without its annotation.
- A public no-argument constructor delegates to that constructor through
  `GWT.create(Binder.class)`.

The adapter retains the view's fields, `@UiField` and `@UiHandler` declarations,
methods, lifecycle methods and original constructor body. It checks for the
expected superclass, presenter interface and injected constructor, and fails the
build if that shape changes. The original UiBinder templates are retained, apart
from the explicit animation image substitution described below.

The Badges adapter removes an unused `AppResources` import. Tabs retains the
original `recalculateTabs()` method but removes its presenter-interface `@Override`.
The launcher invokes it after mounting and after layout changes, using a deferred
callback guarded against navigation to a different view.

A generated `OriginalPages` factory constructs the adapted views. The maintained
[showcase launcher](../showcase-shared/src/main/java/io/instanto/material/showcase/MaterialShowcase.java)
uses the upstream navigation names and descriptions, GWT `History` and fragment
links such as `#!button`. On navigation it clears the previous widget from its
content panel and constructs the selected view. It supplies component search
and the responsive menu. The original `CodeSection` helper supplies
example headings, descriptions and code snippets.

This preserves the selected view code, **not the upstream presenters' behaviour**.
Any setup or resize logic owned by a presenter must be moved explicitly before its
page can be included. Replacing `ViewImpl` with `Composite` is not a general GWTP
migration tool. Applications using GWTP or Gin must decide separately how to adapt
their routing, injection and presenter lifecycle.

The launcher also replaces the upstream dashboard, incubator search, addins logo
wrapper and PWA setup. A plain image displays the upstream logo, and Buttons is
the initial page. These are showcase shell decisions, not implementations of those
upstream frameworks or addins.

## Adapting native calls without rewriting widgets

The [TeaVM source adapter](../material-build/src/main/java/io/instanto/material/build/TeaVmSourcesMojo.java)
applies the shared legacy-JSNI translator and JsInterop binding generator to the
pinned sources. They emit TeaVM JSO code while retaining the Java call sites.
Unsupported JSNI Java-member references or Java receiver access fail explicitly.

GWT DOM objects are Java wrappers; jQuery expects native JavaScript objects. The
shared boundary unwraps arguments and wraps return values, including element
arrays, so both sides refer to the same browser nodes. Namespaced calls retain
their JavaScript owner. Generated parameter names avoid JavaScript reserved words,
and namespaced varargs use array application supported by TeaVM's JSBody parser.

The Material adapter also makes these specific source adjustments:

- `JQuery.window()` constructs a compatibility `Element` wrapper around the native
  window, preserving the original pass-through to jQuery.
- An unused RequestFactory `Service` import is removed from
  `ServiceWorkerLifecycle`; this does not provide RequestFactory support.
- `MaterialTab.unload()` releases the tabs plugin’s owned handlers and indicators,
  as described below.
- Imports of legacy `com.google.gwt.user.client.Element` use
  `com.google.gwt.dom.client.Element` consistently.

Generic compatibility fixes belong in
[Instanto-io/teavm-compat](https://github.com/instanto-io/teavm-compat). This repository
owns Material-specific source adaptations and the showcase. Native GWT and TeaVM
implementations of the same Java packages must remain on separate classpaths.

## UiBinder, resources and initialisation

The shared `gwt-uibinder-processor` compiles UiBinder templates into Java and
generates text-only ClientBundle implementations. The Material templates use its
support for inherited generic setters, preformatted code and default-locale string
constants. Subpackage-qualified widget names such as `webp.MaterialWebpImage`
resolve to Java types rather than being treated as XML directives. Explicit application providers take precedence over generated defaults.

Material's [provider declarations](../core-teavm/src/main/resources/META-INF/services)
select the original `JQueryProvider.NoJQuery` and `DefaultValidatorMessageMixin`
implementations in place of the corresponding GWT module rules. `NoJQuery` means
the host supplies jQuery; it does not remove the JavaScript dependency.

The [host page](../showcase-teavm/src/site/index.html) loads the pinned jQuery asset.
The [TeaVM entry point](../showcase-teavm/src/main/java/io/instanto/material/client/MaterialTeaVm.java)
then calls upstream `MaterialDesign.onModuleLoad()` to initialise Materialize,
animation, clipboard and focus-visible resources before constructing the showcase.
The matching styles, plugins and fonts are part of the asset build.

The shared `gwt-user-compat` supplies image load/error events used by the original
WebP addin. Panel attachment loads children before the parent’s `onLoad`, matching
GWT; this lets Material tabs initialise after their children have their CSS classes.
Unloading runs the parent before its children. The shared API tests exercise both
TeaVM and native GWT implementations.

Two showcase asset changes are deliberate:

- The icon stylesheet uses the upstream TTF directly because the EOT/WOFF/WOFF2
  files named by its original stylesheet are absent from the pinned archive.
  Packaging applies the stylesheet replacement on clean builds too; font bytes
  and licences are preserved.
- The animation template replaces its unavailable remote image with
  `images/animations-banner.png` from the pinned showcase. The displayed XML
  snippet reflects that substitution; the animation controls remain unchanged.

## Tabs plugin lifecycle

The pinned Materialize 0.97.5 tabs plugin adds click and window-resize handlers on
every initialisation. MaterialTab reloads it when tabs change, leaving earlier
handlers referring to obsolete indicators. Its bundled Velocity version also has
delayed animations that are not cancelled by `stop` when `queue: false` is used.
Removing an indicator with jQuery discards data those callbacks still need.

The [resource adapter](../material-build/src/main/java/io/instanto/material/build/MaterialResourcesMojo.java)
adapts both the debug and minified resources. It gives the plugin click handler
its own namespace and each tab bar its own resize namespace, replacing only those
handlers on reinitialisation. It stops and detaches old indicators before creating
one replacement. Detaching preserves data until outstanding animation callbacks
finish, after which the obsolete nodes can be collected.

The [unload bridge](../core-teavm/src/main/java/io/instanto/material/client/TabLifecycle.java)
performs the same cleanup when a MaterialTab leaves the document. Application
click handlers are preserved. The adapter checks each expected source fragment
and fails if the pinned upstream shape changes. The asset JAR and ClientBundle
resources receive the same adaptation; the original archive remains unchanged.

## Addins and their original examples

`gwt-material-addins-teavm` includes all Java sources and browser resources from
Material Addins revision `568eb3f92aa4317504887994cfb5dacebf850931`. Maven fetches
that revision, adapts native calls and packages the original source hashes
and licences. Including the library does not establish every public API's behaviour.

The separate addins showcase comes from `gwt-material-demo` revision
`56687222967f45368ae5008357581d5b485cb0af`. Its 34 original views, UiBinder templates,
models, titles and descriptions are brought in by Maven. The catalogue replaces
GWTP routing and injection with direct view construction. Signature-pad resizing
and carousel reloading are restored from presenter-owned setup. A small demo theme
helper applies the catalogue's blue shades without retaining detached widgets.
The incubator warning component in the camera example becomes a core label.

Decorative images are copied from the pinned demo. Missing remote images use its
local profile or logo images, so these are functional examples rather than exact
visual copies. External library credits belong in the documentation, not among the
live example controls.

Native SignaturePad and Dropzone constructors receive the underlying DOM element.
The rich editor's toolbar allocates its eight rows explicitly; Java cannot grow a
zero-length array as the original GWT JavaScript did. Croppie discards a pending
image result if its widget has already been destroyed, preventing errors after
navigation. Both bundled script variants receive the same guarded change.

Camera support uses `navigator.mediaDevices.getUserMedia`. The widget releases
streams on detach, including streams returned after a permission request outlives
the page. The demo waits for Play before requesting access. File-upload examples
keep files in the local queue with automatic upload disabled; an application must
supply an upload service. The document example accepts a public HTTPS URL and only
constructs the Google Docs viewer when requested. That service must be able to
retrieve the document; local and private files cannot be previewed this way.

## Text fields and addins


The original `TextFieldView`, UiBinder XML, `FieldState` and contact model/oracle
classes are retained. Its presenter contains no extra page setup. The selected
addins load their own pinned Select2, mask and clock resources through generated
ClientBundles. An explicit provider selects the original production `StartupState`.
The full addins dark-theme registry is retained. Complete visual parity still needs
checks across each widget and colour combination.

Two explicit combo-box adaptations handle native values: the selected option index
uses `Number.intValue()` when the browser returns a number, and multiple selection
converts the native array through `JsArrayLike` before using it as a Java array.
Other widget logic and event handlers are retained.

The pinned clock plugin normally opens on focus during a mouse press. Its popup
and the original blur callback can retarget WebKit’s eventual click to the body,
causing immediate dismissal. The resource adapter keeps keyboard focus activation
but waits for a completed click when the input is being pressed. Both debug and
minified resources receive this change; the original Java callbacks are retained.
Browser tests cover mouse, touch and keyboard opening, removal, and time selection.

The date picker has the same focus-versus-click problem. Its input waits for the
completed click during a pointer press; keyboard focus still opens it immediately.
This change is applied to both Materialize resource variants. The Java widget
stops its native picker when unloaded, removing BODY popups, document listeners
and scroll locks before a later mount creates a new picker.

Pickadate returns the selected date in a native `obj` property. The build exposes
that property through a getter, so the existing shared compatibility generator
wraps the browser Date as GWT's `JsDate`. The original widget can then return a
Java `Date` to its value-change handlers. Direct field access would bypass that
conversion and return null after the widget catches the resulting exception.
Typed date setter overloads use the same shared generator to unwrap dates going
to the browser, including preset values and minimum/maximum limits. The original
general-purpose setters remain available.

Shared changes belong in `teavm-compat`: initialisation events, the shared/client
date-formatter hierarchy, suggestion selection/factory hooks, focus, and synthetic
key events with their relative element. The binding generator now converts native
method results declared as `Object` through the shared JsInterop boundary, so
strings, numbers and booleans become Java values. Opaque objects keep their native
identity; arbitrary native arrays and property fields are not automatically made
into Java arrays or fields.

The shared tests compare the added API behaviour with native GWT. The downstream
jQuery probe separately checks string use, numeric/boolean boxing, undefined and
opaque-object identity. Recompile consumers after updating the date-formatter
hierarchy; the predefined enum now belongs to the shared package, as in GWT.

## Error labels and validation

The Errors page uses the same construction adapter, retaining its original
`ErrorsView`, UiBinder template and `EmailValidator`. Its presenter has no extra
setup. Error, success and helper labels use upstream's existing status mixins;
`clearStatusText()` restores configured helper text; ComboBox uses a separate
helper description. The email validator retains upstream's priority over blank
validation. No separate implementation of the showcase's validators is introduced.

## Navigation examples

The navbar and sidebar galleries retain the original titles and descriptions.
Their `buildPanel()` setup runs in the TeaVM launcher, with Demo and Source links
pointing to this site. Upstream screenshot previews are omitted so they cannot
be mistaken for output from this port.

`pattern-sources` imports 18 original views and UiBinder templates from the pinned
GWT Material Patterns archive. Maven downloads and verifies that archive before
source generation. The generated `pattern-inputs.sha256` records the input files.
The adapter replaces GWTP view construction with the same Composite/UiBinder
boundary used by the catalogue, preserving constructor logic and handlers.
Each pattern opens in its own document using `?pattern=…`, because the original
navigation widgets change the page header, main content, body scrolling and
window handlers. A separate document gives those behaviours their intended scope.

The templates contain plain CSS marked as GSS; the adapter checks that exact
stylesheet and removes the unnecessary marker. The older mini-sidebar template's
`expandable` attribute is removed because expansion is already the default in the
pinned core API. The Side Profile addin is included for the content sidebar.
Remote decorative pictures are replaced by the local `patterns/files.svg`
illustration, embedded in the generated templates. Demo and Source links stay
local; comparisons and provenance are in the [showcase guide](SHOWCASE.md).

The navbar toast check exposed boxed durations crossing a native `Object`
parameter. The shared generator now converts scalar `Object` arguments through
`Js.asAny`, unboxing strings, numbers and booleans while retaining native object
identity. Existing varargs still convert each element separately. Property fields
and arbitrary Java object graphs are outside this change. A real jQuery probe
checks native types, null and primitive values; toast expiry checks the user-visible
effect.

## Source provenance and generated output

The [source lock](../upstream/assessment-lock.json) records assessed upstream
revisions. Maven fetches each source archive at its pinned commit, extracts it
under `target/intake`, and generates adaptations under each module's `target`.
The adapters do not edit the original archives.

`teavm-inputs.sha256` and `showcase-inputs.sha256` record original source inputs and
are packaged with the corresponding libraries, licences and provenance. Changes
belong in the adapters or maintained launcher, not in generated files.

The [source-preservation tests](../material-build/src/test/java/io/instanto/material/build/ShowcaseSourcesTest.java)
compare selected original and adapted view members. Browser tests separately check
rendering and interaction; preserving source alone does not establish runtime parity.

## Media resource and lifecycle boundaries

The Media view retains upstream's slider start, pause and fullscreen handlers.
Its six images are served locally. The responsive video iframe uses a small
local sample; see [asset provenance](MEDIA-ASSETS.md).

Upstream `MaterialSlider.unload()` is empty. The generated TeaVM source calls
`MediaLifecycle` to pause its interval, remove its namespaced plugin handlers,
stop animations, detach generated indicators and dispose its Hammer instance.
This also lets reload initialise a single set of indicators and handlers.

Materialboxed images install window scroll and document keyboard listeners.
The resource adapter gives each image its own namespace and disposer. The
TeaVM image unload hook removes these listeners, closes any owned overlay,
restores styles and removes the generated placeholder. Closing a lightbox also
handles the case where no ancestor needed an overflow override. Both development
and minified resources use guarded source adaptations; the archives stay intact.

These are Material-specific lifecycle changes and remain in this repository.

The original fullscreen exit uses four shared GWT style-clearing methods. These
live in `teavm-compat`, as does the panel insertion correction discovered here:
GWT inserts at the container's element index, even after a plugin wraps a logical
child. Shared tests exercise this against TeaVM and original GWT.
`Element.getInnerText()` and `setInnerText()` also follow GWT's `textContent`
semantics. CSS uppercase styling must not change the value returned to the
fullscreen handler, and hidden child text and literal newlines remain intact.

Tabs also links to the separate `navbar_tab_push` example. Its Demo and Source
buttons open the TeaVM pattern and its adapted template; the upstream GIF preview
is omitted. The original resize guidance and inline tab examples remain.

## Showcase theme

`ShowcaseTheme` registers upstream's core, addin and table dark-theme
loaders with `DarkThemeManager`. Switching off dark mode removes those injected
stylesheets. The catalogue stylesheet supplies matching dark colours for our
sidebar, toolbar, descriptions and code examples, and corrects the upstream dark
stylesheet's black input-prefix icons. Typography and layout are unchanged.

The native button exposes its state through `aria-pressed` and works with mouse,
touch and keyboard activation. Light is the default; a choice is saved under
`instanto-material-theme` in browser local storage and read by each new catalogue
or full-page demo. Storage errors are ignored so switching still works when
persistence is unavailable. This is showcase behaviour and does not change how
applications using the widget library choose their own theme.

Upload events read file size as a number and modification time as milliseconds.
The original declarations treated those browser properties as strings, which failed
when a real file reached the Java event handler. The conversion accepts both native
files and the addin's generated blobs without changing the event model.
