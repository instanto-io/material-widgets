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
mvn -Pproduction,stage-pages clean verify
mvn -f showcase-tests/pom.xml test
mvn -f browser-tests/pom.xml test
```

The static site is `showcase-teavm/target/site`. Its JavaScript, styles, fonts and
images are ready for a normal static host. Navigation uses URL fragments, so
the server does not need application routes or rewrite rules.

The Maven `stage-pages` profile copies the generated site, `.nojekyll`, `LICENSE`
and `NOTICE` into `docs/`, alongside the maintained documentation. Review and
commit the generated changes after the browser checks pass. GitHub Pages serves
`main` from `/docs`; it does not compile the Java project.

Only `main` is used during development. There is no separate showcase branch;
release branches will be created when releases are made.

No Java build workflow is installed on GitHub. The production build and browser
checks can run on local infrastructure; Maven can run offline once its declared
dependencies are cached.

The public [Material Widgets repository](https://github.com/instanto-io/material-widgets)
hosts the [TeaVM showcase](https://instanto-io.github.io/material-widgets/) from
`main` under `/docs`, with HTTPS enforced.

Verify the deployed catalogue and original widget fixture with the same browser
checks used locally:

```sh
mvn -f browser-tests/pom.xml -Dtest=ShowcaseTest,TeaVmWidgetsTest \
  -Dmaterial.browser.baseUrl=https://instanto-io.github.io/material-widgets/ test
```

## Working history and release snapshots

Development history belongs in the private `cstainton/material-widgets`
repository. In the working checkout, `origin` points there and `release` points
to `instanto-io/material-widgets`.

Commit and push the working history first. Publish the tested file tree to the
Instanto repository as a single parentless commit, created with `git commit-tree`
without a parent. Update release `main` using `--force-with-lease` against its
previous exact commit. Verify that the private working repository contains the
history before replacing the release snapshot. Never push development `main`
directly to the release remote.

The local `refs/releases/instanto-main` ref holds the public snapshot; the release
remote's default push refspec points to that ref. Both repositories use `main`;
release branches are reserved for future releases.
