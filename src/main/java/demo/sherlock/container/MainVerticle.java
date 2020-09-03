package demo.sherlock.container;

import java.util.function.Consumer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;

public class MainVerticle extends AbstractVerticle {

  static {
    System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
  }


  public static void main(final String[] args) {

    MainVerticle.runVerticle();

  }

  public static void runVerticle() {

    final JsonObject allPorts = new JsonObject()
        .put("prometeus", 8890)
        .put("main", 8880)
        .put("admin", 8889);

    final Consumer<Vertx> runner = vertx -> {
      final DeploymentOptions depOpt = new DeploymentOptions().setConfig(allPorts);
      vertx.deployVerticle(new MainVerticle(), depOpt)
          .onSuccess(result -> System.out.println("Open for bsuiness"))
          .onFailure(err -> err.printStackTrace());
    };


    // Define the metrics options for prometeus
    final HttpServerOptions serverMetricsOptions = new HttpServerOptions()
        .setPort(allPorts.getInteger("prometeus"));
    final VertxPrometheusOptions prometheusOptions =
        new VertxPrometheusOptions().setEmbeddedServerOptions(serverMetricsOptions);
    final MicrometerMetricsOptions metricsOptions = new MicrometerMetricsOptions()
        .setPrometheusOptions(prometheusOptions)
        .setJvmMetricsEnabled(true)
        .setEnabled(true);

    final VertxOptions options = new VertxOptions();
    // Setting timeout higher
    System.setProperty("jnx.debuginit", "true");
    options.setBlockedThreadCheckInterval(1000 * 60 * 60);

    // Add metrics
    options.setMetricsOptions(metricsOptions);
    final Vertx vertx = Vertx.vertx(options);
    runner.accept(vertx);
  }


  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    // FIXME: 2 http servers to start
    vertx.createHttpServer().requestHandler(req -> {
      req.response()
          .putHeader("content-type", "text/plain")
          .end("Hello from Vert.x!");
    }).listen(8888, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }
}
