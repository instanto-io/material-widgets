# Publish the TeaVM showcase

Refined source is transferred from private development into Instanto-io before
a versioned library release begins. Run that release from the Instanto-io
checkout using `instanto-poms/RELEASING.md`: create the version branch, publish
its artifacts, then advance main to the next snapshot. Preserve release branches
and the current development version when transferring later source updates.
The showcase procedure below remains separate from Maven artifact publication.

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

## Working history and release snapshots

Development history belongs in the private `cstainton/material-widgets`
repository. Commit and push there first. Then publish the tested file tree as a
single new commit on top of Instanto-io `main`, preserving its existing public
history. Check for incoming public changes before preparing that commit. Both
repositories use `main` during development; release branches are reserved for
future releases.
