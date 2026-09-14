# Replacing Verrai Material wrappers

GWT Material applications can retain their `gwt.material.design.*` widget APIs
when adopting this TeaVM port, within its documented compatibility coverage.
Applications using Verrai's Material wrappers need a separate migration: those
wrappers use MDC Web and `io.instanto.verrai.material`, with different Java APIs,
DOM structure and styles. Changing the Maven dependency alone is insufficient.

## Separate widgets from application integration

Use original GWT Material widgets for widget behaviour. Keep template adoption,
dependency injection and binding in thin framework adapters. Preserve template
ownership, existing attributes, enabled state and listener disposal when replacing
a wrapper. Map differences explicitly instead of introducing aliases that hide
changed semantics.

The showcase's [GWTP and Gin replacement](DESIGN.md#replacing-gwtp-and-gin-in-the-showcase)
is specific to its launcher. It does not migrate an application's presenters,
routing or injection automatically.

## Adoption sequence

Start with button, text input, label and card to establish construction, template
integration, value changes and event disposal. Follow with drawer, dialog, menu,
floating action button, progress, chip, list and alert, checking the required
upstream equivalents against the [port's coverage](COMPATIBILITY.md).

The initial assessment identified `verrai/verrai-demo` and the wrapper entries in
Verrai's reactor, dependency management and BOM. It also found a dependency in
`sarto/demos/sarto-tms/tms-client-material`. Recheck those applications, their
templates and generated bindings when beginning adoption; this repository does
not establish their current migration status.

Run the application's existing behaviour tests before removing its old wrapper
dependency. Retire the Verrai module and BOM entries only once its users have
migrated. The port's showcase tests provide evidence about the widgets and shared
runtime, not about a downstream application's framework integration.
