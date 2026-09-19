# Port status and compatibility

**Compatibility preview.** The pinned Material core and jQuery/JSCore sources
compile against the TeaVM compatibility libraries, and selected original
showcase views run on TeaVM. This is not yet a complete port or a published Maven
release.

For implementation decisions, including the showcase's GWTP replacement, see
[port design](DESIGN.md). For commands and dependencies, see
[building locally](BUILDING.md).

## What is established

| Area | Evidence | What it does not establish |
|---|---|---|
| Core and jQuery source compatibility | All 604 core and 72 jQuery Java sources compile | TeaVM compiles reachable code; uncalled public APIs still need runtime coverage |
| Original showcase | 33 original catalogue views and UiBinder templates render on TeaVM; WebKit opens each twice. The navbar, sidebar and combined-tab galleries also open 18 full-page TeaVM patterns | Complete behaviour of every widget on those pages |
| Addins | All original addins sources compile; 34 original views render, load local images and survive navigation away and back in Chrome and Firefox through Webapp Testkit, and in WebKit | Every configuration, real camera hardware, external document rendering or an upload backend |
| Tables | All 189 Material Table Java sources compile. Six original table views render and remount under Webapp Testkit in Chrome and supplementary WebKit checks | Every table API, native touch gesture or frozen-column interaction |
| Table interactions | Testkit scenarios cover 52-row paging, sorting events, dataset replacement and clearing, paged column visibility, selection with its original model, density changes, viewport expansion/restoration and scroll cleanup, category opening/closing and loading another bounded window of infinite rows | Remote sorting/filtering, server failure/retry, every locale, keyboard navigation and all configuration combinations |
| Selected interactions | Original button click/double-click and disabled state, checkbox check-all, dialog opening/closing and overlay removal, collection clicks, timed panel loaders, tab selection/addition/resize/reset, and text-field value events, validation, reset, sensitivity, masks, combo boxes and time picking | Every value, validation, keyboard or lifecycle path |
| Media | Original slider controls/fullscreen, image lightboxes and disposal; local iframe video playback, pause, seek and responsive sizing in WebKit | External video services, physical swipe gestures and complete media API parity |
| Showcase theme | Light/dark switching, keyboard activation, saved preference after reload and full-page navigation, operation without browser storage, and dark table expansion | Every widget colour combination or complete upstream dark-theme parity |
| Showcase navigation | Search, history, Media navigation and narrow-screen navigation/scrolling checks | Physical touchscreen gestures; the scrolling check uses wheel input |
| Native jQuery boundary | Original binding probe checks DOM identity, native arrays, proxy callbacks, listener removal, detach/reattach and primitive/opaque `Object` results | Every native field, callback payload or opaque object conversion |
| Build modes | The same Java WebKit suite has passed for development and optimised production builds | A complete Chromium/Firefox/WebKit widget matrix |
| Standalone use | The Hello Material example builds from widget and asset JARs | A published release verified from an empty Maven cache |

The [showcase guide](SHOWCASE.md) lists available examples with links to the port
and upstream. It also distinguishes available addins and deeper interaction checks.
Source-preservation checks cover selected original view members separately from
browser behaviour. Historical lexical inventories under `reports/` are assessment
records, not current runtime evidence.

The shared API suites also check image load/error events and handler removal,
plus child-before-parent loading and parent-before-child unloading over repeated
mounts. The original WebP images are checked for successful decoding on the new
pages. Unsupported-WebP browser fallback is not yet covered.

Text-field checks also cover numeric values, blank-as-null behaviour, read-only and
disabled states, remounting, and the original contact autocomplete after revealing
its intentionally hidden control. They do not yet establish real clipboard access,
all validation messages, locales, every input-mask variant or dark-theme parity.

Date-picker checks select values in all 15 enabled examples, in light and dark
mode in WebKit, and after remounting in Chrome through Webapp Testkit. They also
check preset dates, date limits, the original value-change/close handlers and removal of an open BODY popup
when leaving the page. Both native resource variants are checked for mouse,
touch and keyboard opening and year selection. This does not establish every
locale, date format or physical-device gesture.

The empty date-picker path can log an exception caught by upstream’s own
`getPickerDate()` implementation; it returns null. This is distinct from the
uncaught browser errors rejected by the suite.

The Errors page checks error/success/helper transitions and clearing across all
nine original control examples, including remounts. Its original email validator
is exercised on blur, explicit validation and correction. Required-field checks
cover all eight empty controls and recovery of text and numeric values; they do
not yet establish every date/time validation or locale path.

Navigation checks cover original navbar selection events and expiring toasts,
repeated mounting, and the absence of upstream demo links and screenshot previews.
All 18 full-page patterns render under TeaVMTestRunner with Webapp Testkit and
in WebKit. WebKit also exercises drawer opening/overlay dismissal and expansion
and collapse of the mini sidebar. These checks do not establish every responsive
layout, keyboard path or physical touch gesture.

Addins interaction checks exercise rich-editor HTML set/read handlers, rating value
events, step completion, signature drawing/export/clear, cropper export and local file queuing with the original added-file event. Page
checks include navigation away and back, local image decoding and browser errors.
Camera stream cleanup uses a controlled fixture; it does not certify physical
camera access. File uploads are deliberately queued locally in the static showcase,
and Google Docs rendering depends on a public document and an external service.

## Remaining compatibility work


- Extend addins interaction coverage, including real uploads, camera capture, keyboard and touch input. Verify external document rendering separately from local page checks.
- Expand widget behaviour coverage with Gherkin/Cucumber Tea, TeaVMTestRunner and
  mockatcha-dom. Retain Java Playwright for WebKit and verify the wider browser
  matrix. The existing checks do not resolve deferred Bootstrap step definitions.
- Exercise GWT-valued native fields, generic callback payloads and opaque `Object`
  arguments beyond the paths used by the current showcase.
- Add locale property bundles and permutations where needed; generated string
  constants currently provide default-locale values.
- Extend typed-array and media coverage beyond unsigned byte arrays and the
  implemented media properties. Controlled fixtures do not establish real
  location permissions, external media services or full document navigation.
- Verify exact GWT scheduler event-turn ordering and physical touch interaction.
- Complete release packaging and downstream framework adoption checks.

## Development dependencies

The Material compatibility contributions are in the shared compatibility
repository. Use the [local installation instructions](BUILDING.md#shared-dependencies)
when the matching packages have not been published. Recompile dependent code when
updating the DOM hierarchy, ImageResource SafeUri signature or date-formatter
hierarchy; previously compiled
JARs are not an ABI compatibility guarantee.

The build currently uses TeaVM 0.15 with a transitive Throwable workaround from
`gwt-user-compat`. The proposed upstream fix is tracked in
[TeaVM PR #1252](https://github.com/konsoletyper/teavm/pull/1252); this document does
not assume that a newer TeaVM release has incorporated it.
