package me.darragh.playingapi.communicator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

/**
 * Represents a communicator that provides information about the currently playing media.
 *
 * @author darraghd493
 * @since 1.0.0
 */
public interface Communicator {
    String UNKNOWN_STRING = "Unknown";
    int UNKNOWN_DURATION = -1;

    /**
     * Gets the title of the currently playing media.
     *
     * @return The title of the media.
     */
    @NotNull String getTitle();

    /**
     * Gets the author of the currently playing media.
     *
     * @return The author of the media.
     */
    @NotNull String getAuthor();

    /**
     * Gets the album of the currently playing media.
     *
     * @return The album of the media.
     */
    @NotNull String getAlbum();

    /**
     * Gets the total duration of the currently playing media in seconds.
     *
     * @return The duration in seconds.
     */
    int getDurationSeconds();

    /**
     * Gets the number of seconds that have been played of the currently playing media.
     *
     * @return The played seconds.
     */
    int getPlayedSeconds();

    /**
     * Gets the number of remaining seconds of the currently playing media.
     *
     * @return The remaining seconds.
     */
    default int getRemainingSeconds() {
        return getDurationSeconds() - getPlayedSeconds();
    }

    /**
     * Gets the image data of the author of the currently playing media.
     *
     * @return The author image data, or null if not available.
     */
    @Nullable BufferedImage getAuthorImageData();

    /**
     * Checks if author image data is available.
     *
     * @return True if author image data is available, false otherwise.
     */
    boolean isAuthorImageDataAvailable();

    /**
     * Gets the image data of the album of the currently playing media.
     *
     * @return The album image data, or null if not available.
     */
    @Nullable BufferedImage getAlbumImageData();

    /**
     * Checks if album image data is available.
     *
     * @return True if album image data is available, false otherwise.
     */
    boolean  isAlbumImageDataAvailable();

    /**
     * Starts the communicator.
     */
    void start();

    /**
     * Stops the communicator.
     */
    void stop();
}
