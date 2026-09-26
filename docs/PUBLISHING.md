# Publish the TeaVM showcase

Versioned library releases follow `instanto-poms/RELEASING.md`: create the version
branch, publish its artifacts, then advance main to the next snapshot. The showcase
procedure below remains separate from Maven artifact publication.

Build and test locally with JDK 21 after installing the shared compatibility
integration described in [the build notes](BUILDING.md):

```sh
mvn -Pproduction clean verify
mvn -f showcase-tests/pom.xml test
mvn -f browser-tests/pom.xml test
```

The static site is `showcase-teavm/target/site`. Its JavaScript, styles, fonts and
images are ready for a normal static host. Navigation uses URL fragments, so
the server does not need application routes or rewrite rules.

GitHub Actions publishes the showcase; the built site is not committed. On each
push to `main`, the production Chrome run in CI uploads the site it tested, and the
Showcase Pages workflow deploys that site with `LICENSE` and `NOTICE` added. If CI
fails, the previous showcase stays online.

Only `main` is used during development. There is no separate showcase branch;
release branches will be created when releases are made.

GitHub Actions runs the Java build on local infrastructure and publishes
development snapshots to packages.instanto.io after CI passes. Maven can run
offline once its declared dependencies are cached.

The public [Material Widgets repository](https://github.com/instanto-io/material-widgets)
hosts the [TeaVM showcase](https://instanto-io.github.io/material-widgets/) through
GitHub Actions, with HTTPS enforced.

Verify the deployed catalogue and original widget fixture with the same browser
checks used locally:

```sh
mvn -f browser-tests/pom.xml -Dtest=ShowcaseTest,TeaVmWidgetsTest \
  -Dmaterial.browser.baseUrl=https://instanto-io.github.io/material-widgets/ test
```
