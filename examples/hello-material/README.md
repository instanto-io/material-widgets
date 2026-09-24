# Hello Material

A standalone TeaVM application using the Material widget and asset JARs. It has
no parent POM, reactor dependencies or source paths into another checkout. Copy
this directory to start an application.

Use JDK 21 and configure Maven to read development snapshots from
packages.instanto.io using the [Instanto parent instructions](https://github.com/instanto-io/instanto-poms#use-a-parent).
The Material and compatibility snapshots are published there. From this directory:

```sh
mvn clean verify
jwebserver -b 127.0.0.1 -p 8098 -d "$PWD/target/site"
```

Open [the example](http://127.0.0.1:8098/), enter a name and press **Greet**.
The build compiles optimised JavaScript and extracts the pinned browser assets
from their JAR. The host loads jQuery before the application; `MaterialDesign`
initialises the original Material resources before widgets are constructed.

This example uses Java construction. Applications using UiBinder should also
configure the shared `gwt-uibinder-processor` annotation processor as described
in the compatibility repository.

The repository's existing widget contract can also check this application:

```sh
# From the Material repository root, after building this example
mvn -f browser-tests/pom.xml -Dtest=TeaVmWidgetsTest \
  -Dmaterial.widget.module=examples/hello-material test
```
