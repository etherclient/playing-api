package me.darragh.playingapi.communicator.impl.spotify;

import org.jetbrains.annotations.NotNull;

/**
 * Handles the generation of a response page.
 *
 * @author darraghd493
 * @since 1.0.0
 */
@FunctionalInterface
public interface SpotifyServerPageHandler {
    /**
     * Generates an HTML page with the given message.
     *
     * @param message The message to display on the page.
     * @return The generated HTML page.
     */
    @NotNull String generatePage(@NotNull String message);
}
