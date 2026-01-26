package me.darragh.playingapi.communicator.impl.mpris;

import lombok.RequiredArgsConstructor;
import me.darragh.playingapi.communicator.Communicator;
import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import java.util.Map;

/**
 * A communicator implementation that interacts with the D-Bus (MPRIS spec) on Linux.
 * <b>Note:</b> Untested - I don't use Linux myself.
 *
 * @see <a href="https://specifications.freedesktop.org/mpris/latest/">specification</a>
 * @author darraghd493
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class MPRISCommunicator implements Communicator {
    private DBusConnection connection;
    private DBus.Properties playerProperties;
    private final @NotNull String mprisBusName;

    @Override
    public void start() {
        try {
            this.connection = DBusConnection.getConnection(DBusConnection.SESSION);
            this.playerProperties = this.connection.getRemoteObject(
                    this.mprisBusName,
                    "/org/mpris/MediaPlayer2",
                    DBus.Properties.class
            );
        } catch (DBusException e) {
            throw new RuntimeException("Failed to connect to MPRIS bus: " + e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        if (this.connection != null) {
            this.connection.disconnect();
        }
    }

    @Override
    public @NotNull String getTitle() {
        return this.getMetadataString("xesam:title");
    }

    @Override
    public @NotNull String getArtist() {
        Object artists = this.getMetadata("xesam:artist");
        if (artists instanceof String[] arr) { // TODO: Join with ", " - but I'm not sure if this is even correct
            return arr.length > 0 ? arr[0] : "";
        }
        return "";
    }

    @Override
    public @NotNull String getAlbum() {
        return this.getMetadataString("xesam:album");
    }

    @Override
    public int getDurationSeconds() {
        Object dur = this.getMetadata("mpris:length"); // microseconds
        if (dur instanceof Long) {
            return (int) (((Long) dur) / 1_000_000);
        }
        return 0;
    }

    @Override
    public int getPlayedSeconds() {
        try {
            Object pos = this.playerProperties.Get("org.mpris.MediaPlayer2.Player", "Position");
            if (pos instanceof Long) {
                return (int) (((Long) pos) / 1_000_000);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    @Override
    public @Nullable BufferedImage getArtistImageData() {
        return this.getImageFromUrl(this.getMetadataString("mpris:artUrl"));
    }

    @Override
    public boolean isArtistImageDataAvailable() {
        return this.getArtistImageData() != null;
    }

    @Override
    public @Nullable BufferedImage getAlbumImageData() {
        return null;
    }

    @Override
    public boolean isAlbumImageDataAvailable() {
        return false;
    }

    private Object getMetadata(String key) {
        try {
            Object metadata = this.playerProperties.Get("org.mpris.MediaPlayer2.Player", "Metadata");
            if (metadata instanceof Map<?, ?> map) {
                return map.get(key);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String getMetadataString(String key) {
        Object value = this.getMetadata(key);
        return value != null ? value.toString() : "";
    }

    private BufferedImage getImageFromUrl(String urlString) {
        if (urlString == null || urlString.isEmpty()) return null;
        try {
            if (urlString.startsWith("file://")) {
                urlString = urlString.replace("file://", "");
                return ImageIO.read(new File(urlString));
            } else {
                //noinspection deprecation
                return ImageIO.read(new URL(urlString));
            }
        } catch (IOException e) {
            return null;
        }
    }
}