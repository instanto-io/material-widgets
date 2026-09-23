# Build and test locally

Use JDK 21 and Maven. Spotless formats maintained Java during the build. The
build uses Maven plugins and Java adapters; it does not require Python or npm.

## Shared dependencies

Runtime and generation tools come from
[Instanto-io/teavm-compat](https://github.com/instanto-io/teavm-compat), including
`gwt-user-compat`, `jsinterop-binding-generator` and `gwt-uibinder-processor`.
Configure a token with package-read access for Maven server
`github-teavm-compat` when resolving published artifacts from GitHub Packages.

For unpublished changes, install the matching compatibility checkout first:

```sh
# In teavm-compat, using JDK 21
mvn clean install
# In material-widgets
mvn clean verify
```

There is no dependency on a Bootstrap checkout. Once the dependencies are cached,
Maven can run offline with `-o`.

## Run the showcase

```sh
mvn clean verify
mvn -f showcase-tests/pom.xml test
mvn -f browser-tests/pom.xml test
jwebserver -b 127.0.0.1 -p 8098 -d "$PWD/showcase-teavm/target/site"
```

Open the [local showcase](http://127.0.0.1:8098/). `showcase-tests` uses
Cucumber Tea, Webapp Testkit and TeaVMTestRunner. Install their current snapshot
artifacts locally first. Chrome must be available to the runner; use
`-Dmaterial.test.browser=browser-firefox` for Firefox. The module stages the
complete built application on the runner's resource server and runs the feature
scenarios in `showcase-tests/src/test/resources/features`. The
`showcase-tests` profile also runs it at the end of the main reactor:
`mvn -Pshowcase-tests verify`.

`browser-tests` retains Java Playwright for WebKit and requires that browser
engine to be installed. It serves
the built fixtures itself; the `jwebserver` command is for manual browsing.

For optimised JavaScript, run `mvn -Pproduction verify`, then the same browser
suite. See [port status](COMPATIBILITY.md) for what the checks establish.

## Use the library outside this reactor

Install this build with `mvn install`, then follow the
[Hello Material example](../examples/hello-material/README.md). Its independent
POM consumes the widget and asset JARs without reading sibling sources or
inheriting this repository's parent POM.

## Source inputs and build targets

This repository builds TeaVM widgets and showcases only. Shared comparisons with
the original GWT APIs run in `teavm-compat`.

Maven downloads the pinned core, jQuery, addins, addins-showcase, table and
table-showcase archives and verifies their SHA-256 values before extracting them.
Downloads are cached under Maven's local repository, allowing subsequent
`mvn -o clean verify` builds.

For local production staging and GitHub Pages hosting, use the
[publishing guide](PUBLISHING.md).
