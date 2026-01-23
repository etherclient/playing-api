package me.darragh.playingapi.communicator;

import lombok.experimental.UtilityClass;
import me.darragh.playingapi.communicator.impl.SpotifyCommunicator;
import org.jetbrains.annotations.NotNull;

/**
 * A factory for creating {@link Communicator} instances.
 *
 * @author darraghd493
 * @since 1.0.0
 */
@UtilityClass
public final class CommunicatorFactory {
    /**
     * Creates a new {@link SpotifyCommunicator} instance.
     *
     * @param clientId The Spotify client ID.
     * @param redirectUri The redirect URI for Spotify authentication.
     * @return A new {@link SpotifyCommunicator} instance.
     */
    public static SpotifyCommunicator createSpotifyCommunicator(@NotNull String clientId, @NotNull String redirectUri) {
        return new SpotifyCommunicator(clientId, redirectUri);
    }
}
