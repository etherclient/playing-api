package me.darragh.playingapi.communicator;

import lombok.experimental.UtilityClass;
import me.darragh.playingapi.communicator.impl.mpris.MPRISCommunicator;
import me.darragh.playingapi.communicator.impl.spotify.SpotifyCommunicator;
import me.darragh.playingapi.communicator.impl.smtc.SMTCCommunicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A factory for creating {@link Communicator} instances.
 *
 * @author darraghd493
 * @since 1.0.0
 */
@UtilityClass
public final class CommunicatorFactory {
    /**
     * Creates a new {@link MPRISCommunicator} instance.
     *
     * @return A new {@link MPRISCommunicator} instance.
     */
    public static MPRISCommunicator createMPRISCommunicator(@Nullable String mprisBusName) {
        return new MPRISCommunicator(mprisBusName);
    }

    /**
     * Creates a new {@link SMTCCommunicator} instance.
     *
     * @return A new {@link SMTCCommunicator} instance.
     */
    public static SMTCCommunicator createSMTCCommunicator() {
        return new SMTCCommunicator();
    }

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
