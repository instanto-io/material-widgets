# Initialise suppressed exceptions in Throwable constructors

We encountered this when working with an `UmbrellaException` that retained multiple
exceptions.

We think `TThrowable.suppressed` is left uninitialised when the replacement
constructors annotated with `@Rename("<init>")` run: its field initialiser appears
to belong to the constructors renamed to `fakeInit`. Consequently,
`addSuppressed()` and even `getSuppressed()` on a new exception can throw a
`NullPointerException`.

Reproduce with:

```java
Throwable failure = new Throwable("primary");
failure.addSuppressed(new Throwable("secondary"));
assertEquals(1, failure.getSuppressed().length);
```

This change moves the array initialisation into every replacement
constructor. Regression tests cover the constructor variants, enabled/disabled
suppression, retained exception identity, defensive copies and failures from
`try`-with-resources.

Our downstream workaround is a small TeaVM compiler plugin that inserts the
missing initialisation during compilation. Fixing this in the class library would
let us remove that workaround when adopting a TeaVM release containing the fix.

Validation: the five new regression tests fail on the unmodified JavaScript
backend. With the fix, all seven `ThrowableTest` tests pass on JavaScript and
Wasm GC, including optimised runs. Class-library and test Checkstyle checks pass.

Thanks again for your work on TeaVM and for reviewing our contributions.

---

Submitted as [TeaVM PR #1252](https://github.com/konsoletyper/teavm/pull/1252),
commit `559ec1a`.
