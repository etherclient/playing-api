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
    public static final String PAGE = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta http-equiv="X-UA-Compatible" content="IE=edge">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>OAuth2</title>
        </head>
        <body>
            <h1>%s</h1>
        </body>
        </html>
        """;

    @Override
    public @NotNull String generatePage(@NotNull String message) {
        return String.format(PAGE, message);
    }
}
