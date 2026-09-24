# Material port roadmap

The goal is a standalone TeaVM adaptation of GWT Material that preserves upstream
widget packages and implementations. Generic runtime and generation work belongs
in [Instanto-io/teavm-compat](https://github.com/instanto-io/teavm-compat).

This is the remaining work plan. [Port status](docs/COMPATIBILITY.md) records what
is established, [design](docs/DESIGN.md) explains the adaptations, and the
[showcase guide](docs/SHOWCASE.md) links to working examples and identifies further showcase work.

## Complete application behaviour

The core catalogue, all eighteen navigation patterns, six table views and the
thirty-four original addins views are available. Extend their interaction coverage
and check examples in real applications. Keep upstream comparisons in documentation
and distinguish successful compilation, rendering and tested interactions.

## Expand behaviour coverage


Use Webapp Testkit, Cucumber Tea and TeaVMTestRunner for application and widget
scenarios, with Java Playwright retained for WebKit. The showcase browser suite
now describes addins, tables, navigation, date pickers and the theme in feature
files. Extend those scenarios to cover keyboard/focus, values,
validation, disabled state, event disposal and repeated mounting. Run development
and optimised builds across Chromium, Firefox and WebKit. Keep shared GWT API
comparisons in the compatibility repository.

## Extend addins and table coverage

Use the assessed revisions in the [source lock](upstream/assessment-lock.json) as
starting points, then pin the selected sources and plugin assets. Addins need
focused selection, autocomplete, editor and upload checks. The six original table
views now use local data and have interaction tests. Extend them with keyboard
navigation, native touch input, frozen-column geometry, remote filtering/sorting,
service failures and broader lifecycle coverage.
Record external-service requirements separately from local fixtures.

## Release and adoption

Publish independently usable widget and asset artifacts, and verify an application
from an empty Maven cache. Build and test the showcase on local infrastructure;
GitHub hosts the repository and generated site. Keep only `main` during development
and create release branches when making releases.

Migrate framework-specific template, injection and binding code separately. The
[Verrai migration notes](docs/MIGRATION.md) describe the decisions required before
retiring its existing wrappers.

## Verification and execution backlog (verify first, implement if missing)

- [ ] Verify 33 catalogue views and all 18 navbar/sidebar patterns are still rendered on TeaVM and cross-link correctly to upstream references.
- [ ] Verify text fields, errors, date pickers, dark mode and all six table views remain green in the browser contracts.
- [ ] Extend the existing addins checks (including rating, signature, steps, cropper, carousel and window controls) to media upload, external document viewing, keyboard and touch interactions.
- [ ] Verify table interactions for frozen columns, keyboard navigation, touch interaction, remote sorting/filtering, and service-failure flows.
- [ ] Verify Chromium, Firefox and WebKit full matrix in shared TeaVMTestRunner and Webapp Testkit scenarios, including optimized-build checks.
- [ ] Verify locale variants and less-used native callbacks/browser API paths that are still untested.
- [ ] Verify first-release dependency chain publishing and a standalone application build from an empty Maven cache.
