package me.darragh.playingapi.communicator.impl.spotify;

import com.sun.net.httpserver.HttpExchange;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the response of a Spotify OAuth response.
 *
 * @author darraghd493
 * @since 1.0.0
 */
@FunctionalInterface
public interface SpotifyServerResponseHandler {
    /**
     * Handles the response from the Spotify OAuth page.
     *
     * @param exchange The HTTP exchange.
     * @return The state of the response.
     */
    SpotifyServerResponseState handleResponse(@NotNull HttpExchange exchange);
}
