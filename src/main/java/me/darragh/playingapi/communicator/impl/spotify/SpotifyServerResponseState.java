package me.darragh.playingapi.communicator.impl.spotify;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the state of the Spotify OAuth response.
 *
 * @author darraghd493
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum SpotifyServerResponseState {
    SUCCESS("You have successfully authenticated.", true),
    FAILURE("Authentication failed.", false),
    INVALID("Invalid response.", false);

    private final String defaultMessage;
    private final boolean success;
}
