package me.darragh.playingapi.communicator.impl.spotify;

import org.jetbrains.annotations.NotNull;

/**
 * A simple {@link SpotifyServerPageHandler} that displays a message.
 *
 * @see SpotifyServerPageHandler
 * @author darraghd493
 * @since 1.0.0
 */
public final class SimpleSpotifyPageHandler implements SpotifyServerPageHandler {
    public static final String PAGE = "<!doctype html><html lang=en><meta charset=UTF-8><meta content=\"IE=edge\" http-equiv=X-UA-Compatible><meta content=\"width=device-width,initial-scale=1\" name=viewport><title>OAuth2</title><h1>%s</h1>";

    @Override
    public @NotNull String generatePage(@NotNull String message) {
        return String.format(PAGE, message);
    }
}
