package demo.sherlock.container;

import java.util.function.Consumer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;

public class MainVerticle extends AbstractVerticle {

  // static {
  // System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
  // }

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
          .onSuccess(result -> System.out.println("Open for business"))
          .onFailure(err -> err.printStackTrace());
    };


    // Define the metrics options for prometeus
    final int port = allPorts.getInteger("prometeus");
    final HttpServerOptions serverMetricsOptions = new HttpServerOptions().setPort(port);
    final VertxPrometheusOptions prometheusOptions = new VertxPrometheusOptions()
        .setEmbeddedServerOptions(serverMetricsOptions)
        .setStartEmbeddedServer(true)
        .setPublishQuantiles(true)
        .setEnabled(true)
        .setEmbeddedServerEndpoint("/");

    final MicrometerMetricsOptions metricsOptions = new MicrometerMetricsOptions()
        .setPrometheusOptions(prometheusOptions)
        .setJvmMetricsEnabled(true)
        .setEnabled(true);

    System.out.format("Metrics is configured for port %s\n", port);

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
  public void start(Promise<Void> startPromise) {

    this.startServer("main")
        .compose(v -> this.startServer("admin"))
        .onSuccess(v -> System.out.println("Ready to go!"))
        .onFailure(err -> err.printStackTrace());

  }

  private Future<Void> startServer(final String serverId) {
    final int port = this.config().getInteger(serverId);
    final HttpServerOptions options = new HttpServerOptions()
        .setPort(port);

    return Future.future(promise -> {
      Router router = Router.router(this.getVertx());
      router.route().handler(ctx -> {
        ctx.response().end("You reached server " + serverId + ", port " + port);
      });
      this.getVertx().createHttpServer(options)
          .requestHandler(router)
          .listen()
          .onSuccess(v -> {
            System.out.format("Server %s up and running at port %s\n", serverId, port);
            promise.complete();
          })
          .onFailure(err -> promise.fail(err));
    });
  }

}
