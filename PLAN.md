# Material port roadmap

The goal is a standalone TeaVM adaptation of GWT Material that preserves upstream
widget packages and implementations. Generic runtime and generation work belongs
in [Instanto-io/teavm-compat](https://github.com/instanto-io/teavm-compat).

This is the remaining work plan. [Port status](docs/COMPATIBILITY.md) records what
is established, [design](docs/DESIGN.md) explains the adaptations, and the
[showcase guide](docs/SHOWCASE.md) links to working examples and identifies further showcase work.

## Simplify the repository

Extend the Maven download and checksum approach used for addins to the remaining
core, jQuery and showcase source archives, then remove those ZIPs from Git.
Keep downloads cached for subsequent offline builds.
Preserve source provenance, licences and the maintained TeaVM showcase shell.

## Complete the core showcase

Port the original GWT Material Patterns views next: the six navbar gallery
patterns, eleven side-navigation patterns and the combined navbar/push example
linked from Tabs. Pin their sources, adapt presenter-owned initialisation and
keep original handlers and templates. Their demo destinations must run locally
on TeaVM before they appear as working examples in the showcase.

Keep upstream comparisons in documentation. Distinguish successful compilation,
rendering and tested interactions for each addition.

## Expand behaviour coverage

Use Webapp Testkit and TeaVMTestRunner for application and widget scenarios,
with Java Playwright retained for WebKit. The table showcase now uses this harness;
Gherkin/Cucumber Tea integration and migration of the older browser suites remain.
Cover keyboard/focus, values,
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
