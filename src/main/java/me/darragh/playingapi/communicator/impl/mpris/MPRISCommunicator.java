package me.darragh.playingapi.communicator.impl.mpris;

import lombok.RequiredArgsConstructor;
import me.darragh.playingapi.communicator.Communicator;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A communicator implementation that interacts with the D-Bus (MPRIS spec) on Linux.
 * <b>Note:</b> Only tested on Linux Mint (Virtual Machine).
 *
 * @see <a href="https://specifications.freedesktop.org/mpris/latest/">specification</a>
 * @author darraghd493
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class MPRISCommunicator implements Communicator {
    private static final String PLAYING_STATUS = "Playing";
    private static final String MPRIS_PREFIX = "org.mpris.MediaPlayer2.";
    private static final String MPRIS_PLAYER_PREFIX = "org.mpris.MediaPlayer2.Player";

    private DBusConnection connection;
    private Properties playerProperties;
    private String currentPlayer = null;

    private final @Nullable String mprisBusName;

    @Override
    public void start() {
        try {
            this.connection = DBusConnectionBuilder.forSessionBus().build();

            if (this.mprisBusName != null) {
                this.switchPlayer(this.mprisBusName);
                return;
            }

            DBus dbus = this.connection.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);
            this.selectInitialPlayer(dbus);

            this.connection.addSigHandler(DBus.NameOwnerChanged.class, sig -> {
                if (sig.getName().startsWith(MPRIS_PREFIX)) {
                    this.refreshPlayerList(dbus);
                }
            });

            this.connection.addSigHandler(Properties.PropertiesChanged.class, sig -> {
                if (!MPRIS_PLAYER_PREFIX.equals(sig.getInterfaceName())) return;

                Map<String, Variant<?>> changed = sig.getPropertiesChanged();
                if (changed.containsKey("PlaybackStatus")) {
                    String status = unwrap(changed.get("PlaybackStatus")).toString();

                    if (PLAYING_STATUS.equalsIgnoreCase(status)) {
                        if (!this.isSameSender(sig.getSource(), this.currentPlayer)) {
                            this.switchPlayer(sig.getSource());
                        }
                    }
                }
            });
        } catch (DBusException e) {
            throw new RuntimeException("Failed to init MPRIS", e);
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
        return this.getMetadataString("xesam:artist");
    }

    @Override
    public @NotNull String getAlbum() {
        return this.getMetadataString("xesam:album");
    }

    @Override
    public int getDurationSeconds() {
        Object dur = this.getMetadata("mpris:length");
        if (dur instanceof Number n) {
            return (int) (n.longValue() / 1_000_000);
        }
        return 0;
    }

    @Override
    public int getPlayedSeconds() {
        try {
            Object pos = this.playerProperties.Get("org.mpris.MediaPlayer2.Player", "Position");
            Object unwrapped = this.unwrap(pos);
            if (unwrapped instanceof Number n) {
                return (int) (n.longValue() / 1_000_000);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    @Override
    public boolean isPaused() {
        if (this.currentPlayer == null || this.playerProperties == null) return false;
        try {
            Object status = this.unwrap(this.playerProperties.Get(MPRIS_PLAYER_PREFIX, "PlaybackStatus"));
            return status != null && !PLAYING_STATUS.equalsIgnoreCase(status.toString());
        } catch (Exception ignored) {}
        return false;
    }

    @Override
    public @Nullable BufferedImage getArtistImageData() {
        return null;
    }

    @Override
    public boolean isArtistImageDataAvailable() {
        return false;
    }

    @Override
    public @Nullable BufferedImage getAlbumImageData() {
        return getImageFromUrl(this.getMetadataString("mpris:artUrl"));
    }

    @Override
    public boolean isAlbumImageDataAvailable() {
        return !this.getMetadataString("mpris:artUrl").isEmpty();
    }

    //region Metadata Handling
    /**
     * Recursively unwraps D-Bus Variants to get the underlying value.
     *
     * @return The unwrapped object.
     */
    private Object unwrap(Object obj) {
        while (obj instanceof Variant<?> v) {
            obj = v.getValue();
        }
        return obj;
    }

    /**
     * Finds a key in the player metadata map.
     */
    private Object getMetadata(String key) {
        if (this.playerProperties == null) return null;
        try {
            Object rawMetadata = this.playerProperties.Get(MPRIS_PLAYER_PREFIX, "Metadata");
            Object unwrappedMetadata = this.unwrap(rawMetadata);

            if (!(unwrappedMetadata instanceof Map<?, ?> map)) return null;

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String entryKey = String.valueOf(this.unwrap(entry.getKey()));
                if (entryKey.equals(key)) {
                    return this.unwrap(entry.getValue());
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Converts a metadata key to a string representation.
     * <ul>
     *     <li>unmodified for individual strings</li>
     *     <li>comma deliminated for lists of strings</li>
     * </ul>
     *
     * @param key The metadata key.
     * @return The string representation of the metadata value.
     */
    private @NotNull String getMetadataString(String key) {
        Object value = this.getMetadata(key);
        if (value == null) return "";

        if (value instanceof List<?> list) {
            return list.stream()
                    .map(this::unwrap)
                    .map(String::valueOf)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(", "));
        }

        if (value.getClass().isArray()) {
            return Arrays.stream((Object[]) value)
                    .map(this::unwrap)
                    .map(String::valueOf)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(", "));
        }

        return String.valueOf(value);
    }
    //endregion

    //region Player Handling
    /**
     * Selects the initial MPRIS player to connect to.
     *
     * @param dbus The D-Bus interface.
     */
    private void selectInitialPlayer(DBus dbus) {
        try {
            String selected = null;
            for (String name : dbus.ListNames()) {
                if (!name.startsWith(MPRIS_PREFIX)) continue;
                if (this.isPlaying(name)) {
                    selected = name;
                    break;
                }
                if (selected == null) selected = name;
            }
            if (selected != null && !selected.equals(this.currentPlayer)) {
                this.switchPlayer(selected);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Refreshes the list of available MPRIS players and switches to a new one if necessary.
     *
     * @param dbus The D-Bus interface.
     */
    private void refreshPlayerList(DBus dbus) {
        try {
            String playingPlayer = null,
                    firstFound = null;

            for (String name : dbus.ListNames()) {
                if (!name.startsWith(MPRIS_PREFIX)) continue;

                if (this.isPlaying(name)) {
                    playingPlayer = name;
                    break; // priority: find anyone currently playing
                }
                if (firstFound == null) firstFound = name;
            }

            String toSelect = (playingPlayer != null) ? playingPlayer : firstFound;
            if (toSelect != null && !toSelect.equals(currentPlayer)) {
                this.switchPlayer(toSelect);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Switches the current player to the specified bus name.
     *
     * @param bus The bus name of the new player.
     */
    private void switchPlayer(String bus) {
        try {
            this.playerProperties = this.connection.getRemoteObject(bus, "/org/mpris/MediaPlayer2", Properties.class);
            this.currentPlayer = bus;
        } catch (Exception ignored) {
        }
    }

    /**
     * Checks if the specified player is currently playing.
     *
     * @param bus The bus name of the player.
     * @return True if the player is playing, false otherwise.
     */
    private boolean isPlaying(String bus) {
        try {
            Properties properties = this.connection.getRemoteObject(bus, "/org/mpris/MediaPlayer2", Properties.class);
            Object status = this.unwrap(properties.Get(MPRIS_PLAYER_PREFIX, "PlaybackStatus"));
            return status != null && PLAYING_STATUS.equalsIgnoreCase(status.toString());
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Compares a signal source (Unique Name) with the current player (Well-Known Name).
     *
     * @param signalSource The signal source unique name.
     * @param currentBusName The current player well-known name.
     * @return True if they refer to the same sender, false otherwise.
     */
    private boolean isSameSender(String signalSource, String currentBusName) {
        if (signalSource == null || currentBusName == null) return false;
        if (signalSource.equals(currentBusName)) return true;

        try {
            DBus dbus = this.connection.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);
            String uniqueName = dbus.GetNameOwner(currentBusName);
            return signalSource.equals(uniqueName);
        } catch (Exception e) {
            return false;
        }
    }
    //endregion

    /**
     * Retrieves an image from a URL or file path.
     *
     * @param url The URL or file path.
     * @return The image, or null if not found.
     */
    private static @Nullable BufferedImage getImageFromUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            if (url.startsWith("file://")) {
                return ImageIO.read(new File(url.substring(7)));
            }
            //noinspection deprecation
            return ImageIO.read(new URL(url));
        } catch (IOException e) { return null; }
    }
}