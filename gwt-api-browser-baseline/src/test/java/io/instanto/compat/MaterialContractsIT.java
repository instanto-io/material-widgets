package io.instanto.compat;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Executes the portable Material-related API contracts against native GWT. */
@org.junit.runner.RunWith(org.junit.runners.Parameterized.class)
public class MaterialContractsIT {
  @org.junit.runners.Parameterized.Parameters(name = "{0}")
  public static Object[] contracts() {
    return new Object[] {
      "table-numbers",
      "table-dom",
      "textfield-initialisation",
      "textfield-time-format",
      "textfield-keys",
      "textfield-suggestions",
      "panel-order",
      "image-events",
      "browser-location",
      "browser-storage",
      "browser-metadata",
      "browser-geometry",
      "suggestion-constructor",
      "browser-scroll",
      "document-query",
      "stylesheet-order",
      "inline-script",
      "script-nonce",
      "stylesheet-deferred",
      "script-load",
      "script-failure",
      "button-safe-html",
      "list-selection",
      "list-direction",
      "label-direction",
      "radio-labels",
      "styles",
      "children",
      "input",
      "lifecycle",
      "values",
      "direction",
      "selection",
      "dates",
      "suppression",
      "resources",
      "options",
      "attach",
      "event-sources",
      "event-mutation",
      "event-errors",
      "sanitization"
    };
  }

  @org.junit.runners.Parameterized.Parameter public String contract;

  @Test
  public void originalGwtContract() throws Exception {
    verify(contract, page -> page.waitForFunction("document.body.dataset.contract === 'passed'"));
  }

  private void verify(String module, java.util.function.Consumer<Page> assertion) throws Exception {
    Path root = Path.of("target", "site").toAbsolutePath().normalize();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          String request = exchange.getRequestURI().getPath();
          Path file =
              root.resolve(request.equals("/") ? "index.html" : request.substring(1)).normalize();
          if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            exchange.sendResponseHeaders(404, -1);
          } else {
            String name = file.getFileName().toString();
            String type =
                name.endsWith(".js")
                    ? "text/javascript"
                    : name.endsWith(".css")
                        ? "text/css"
                        : name.endsWith(".html") ? "text/html" : "application/octet-stream";
            exchange.getResponseHeaders().set("Content-Type", type);
            byte[] body = Files.readAllBytes(file);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
          }
          exchange.close();
        });
    server.start();
    try (Playwright playwright = Playwright.create();
        Browser browser = playwright.webkit().launch()) {
      Page page = browser.newPage();
      page.setDefaultTimeout(10000);
      List<String> errors = new ArrayList<>();
      page.onPageError(errors::add);
      page.onResponse(
          response -> {
            if (response.status() >= 400) errors.add(response.status() + " " + response.url());
          });
      page.onRequestFailed(
          request -> {
            boolean expected =
                module.equals("script-failure")
                    && request
                        .url()
                        .equals(
                            io.instanto.gwt.testing.api.MaterialResourceContracts
                                .INVALID_SCRIPT_URL);
            if (!expected) errors.add(request.url() + " " + request.failure());
          });

      page.navigate("http://127.0.0.1:" + server.getAddress().getPort() + "/?contract=" + module);
      try {
        assertion.accept(page);
      } catch (RuntimeException failure) {
        throw new AssertionError("Contract " + module + ": " + errors, failure);
      }
      assertEquals("Browser errors", List.of(), errors);
    } finally {
      server.stop(0);
    }
  }
}
