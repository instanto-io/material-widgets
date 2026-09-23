package io.instanto.material.testing;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Native WebKit checks for the two preliminary fixtures, not widget port coverage. */
public class BaselineTest {

  @Test
  public void teavmWrappersPreserveNativeIdentityAndListenerDisposal() throws Exception {
    verify(
        "interop-teavm",
        page -> {
          page.waitForSelector("body[data-probe=passed],body[data-probe-error]");
          assertNull(page.locator("body").getAttribute("data-probe-error"));
          assertEquals("Shared DOM identity", page.locator("#interop-node").textContent());
        });
  }

  private void verify(String module, java.util.function.Consumer<Page> assertion) throws Exception {
    verify(module, "/", assertion);
  }

  static void verify(String module, String path, java.util.function.Consumer<Page> assertion)
      throws Exception {
    verify(module, path, false, assertion);
  }

  static void verify(
      String module, String path, boolean hasTouch, java.util.function.Consumer<Page> assertion)
      throws Exception {
    Path root = Path.of("..", module, "target", "site").toAbsolutePath().normalize();
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
                switch (name.substring(name.lastIndexOf('.') + 1)) {
                  case "js" -> "text/javascript";
                  case "css" -> "text/css";
                  case "html" -> "text/html";
                  case "mp4" -> "video/mp4";
                  case "svg" -> "image/svg+xml";
                  case "png" -> "image/png";
                  case "gif" -> "image/gif";
                  case "jpg", "jpeg" -> "image/jpeg";
                  default -> "application/octet-stream";
                };
            exchange.getResponseHeaders().set("Content-Type", type);
            byte[] body = Files.readAllBytes(file);
            exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
            String range = exchange.getRequestHeaders().getFirst("Range");
            if (range != null && range.matches("bytes=\\d+-\\d*")) {
              String[] bounds = range.substring(6).split("-", -1);
              long from = Long.parseLong(bounds[0]);
              long to =
                  bounds[1].isEmpty()
                      ? body.length - 1
                      : Math.min(Long.parseLong(bounds[1]), body.length - 1);
              if (from > to || from >= body.length) {
                exchange.getResponseHeaders().set("Content-Range", "bytes */" + body.length);
                exchange.sendResponseHeaders(416, -1);
              } else {
                int length = (int) (to - from + 1);
                exchange
                    .getResponseHeaders()
                    .set("Content-Range", "bytes " + from + "-" + to + "/" + body.length);
                exchange.sendResponseHeaders(206, length);
                exchange.getResponseBody().write(body, (int) from, length);
              }
            } else {
              exchange.sendResponseHeaders(200, body.length);
              exchange.getResponseBody().write(body);
            }
          }
          exchange.close();
        });
    server.start();
    try (Playwright playwright = Playwright.create();
        Browser browser = playwright.webkit().launch()) {
      Page page =
          browser.newPage(
              new Browser.NewPageOptions().setViewportSize(1280, 900).setHasTouch(hasTouch));
      List<String> errors = new ArrayList<>();
      page.onPageError(
          error -> {
            errors.add(error);
            System.err.println("Browser error: " + error);
          });
      page.onResponse(
          response -> {
            if (response.status() >= 400) errors.add(response.status() + " " + response.url());
          });
      page.onRequestFailed(request -> errors.add(request.url() + " " + request.failure()));
      String baseUrl =
          System.getProperty(
              "material.browser.baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
      page.navigate(baseUrl.replaceAll("/$", "") + path);
      try {
        assertion.accept(page);
      } catch (RuntimeException | AssertionError failure) {
        if (!errors.isEmpty())
          failure.addSuppressed(new AssertionError("Browser errors: " + errors));
        throw failure;
      }
      assertEquals("Browser errors", List.of(), errors);
    } finally {
      server.stop(0);
    }
  }
}
