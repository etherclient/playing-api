package me.darragh.playingapi.communicator.impl.spotify;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple HTTP server handler for Spotify OAuth authentication.
 *
 * @author darraghd493
 * @since 1.0.0
 */
public class SpotifyServerHandler implements HttpHandler {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private final @NotNull SpotifyCommunicator communicator;
    private final @NotNull SpotifyServerPageHandler pageHandler;
    private final @NotNull SpotifyServerResponseHandler responseHandler;

    private final int port;
    private HttpServer server;

    public SpotifyServerHandler(@NotNull SpotifyCommunicator communicator, @NotNull SpotifyServerPageHandler pageHandler, @NotNull SpotifyServerResponseHandler responseHandler, int port) {
        this.communicator = communicator;
        this.pageHandler = pageHandler;
        this.responseHandler = responseHandler;
        this.port = port;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        SpotifyServerResponseState state = SpotifyServerResponseState.INVALID;

        try {
            state = this.responseHandler.handleResponse(exchange);
        } catch (Exception ignored) {
        }

        this.standardWrite(exchange, this.pageHandler.generatePage(state.getDefaultMessage()));
        this.stop();
    }

    /**
     * Starts the server.
     */
    public void start() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        this.server.createContext("/", this);
        this.server.setExecutor(EXECUTOR);
        this.server.start();
    }

    /**
     * Stops the server.
     */
    public void stop() {
        if (this.server == null) {
            return;
        }
        this.server.stop(0);
        this.server = null;
    }

    /**
     * Checks if the server is running.
     *
     * @return True if the server is running, false otherwise.
     */
    public boolean isRunning() {
        return this.server != null && this.server.getAddress() != null;
    }

    /**
     * Writes the response to the given request in a standard way.
     *
     * @param request The request to write to.
     * @param string The string to write.
     * @throws IOException If an I/O error occurs.
     */
    private void standardWrite(@NotNull HttpExchange request, @NotNull String string) throws IOException {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        request.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        request.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = request.getResponseBody()) {
            outputStream.write(bytes);
            outputStream.flush();
        }
    }
}
