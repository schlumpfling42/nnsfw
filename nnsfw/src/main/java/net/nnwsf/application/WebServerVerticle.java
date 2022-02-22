package net.nnwsf.application;

import java.util.logging.Logger;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.mutiny.core.http.HttpServer;
import net.nnwsf.handler.HttpHandlerImpl;

public class WebServerVerticle extends AbstractVerticle {

    private static final Logger log = Logger.getLogger(WebServerVerticle.class.getName());

    @Override
    public Uni<Void> asyncStart() {

        final HttpServerOptions options = new HttpServerOptions()
            .setTcpNoDelay(true);

        log.info("Starting server at " + HttpHandlerImpl.getHostname() + " port " + HttpHandlerImpl.getPort());
        Uni<HttpServer> startHttpServer = vertx.createHttpServer(options)
                .requestHandler(HttpHandlerImpl.getRouter()::handle)
                .listen(HttpHandlerImpl.getPort(), HttpHandlerImpl.getHostname())
                .onItem().invoke(() -> log.info("✅ HTTP server listening on port " + HttpHandlerImpl.getPort()));

        return startHttpServer.replaceWithVoid(); // <1>
    }

}