package me.darragh.playingapi.communicator.impl.spotify;

import com.sun.net.httpserver.HttpExchange;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the response of a Spotify OAuth response.
 *
 * @author darraghd493
 * @since 1.0.0
 */
public interface SpotifyServerResponseHandler {
    /**
     * Handles the response from the Spotify OAuth page.
     *
     * @param exchange The HTTP exchange.
     * @return Whether the response was handled successfully.
     */
    boolean handleResponse(@NotNull HttpExchange exchange);
}
