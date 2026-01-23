package me.darragh.playingapi.communicator.impl.smtc;

import com.sun.jna.Pointer;
import me.darragh.playingapi.communicator.Communicator;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * A communicator implementation that interacts with the System Media Transport Controls (SMTC) on Windows.
 *
 * @see <a href="https://learn.microsoft.com/en-us/uwp/api/windows.media.systemmediatransportcontrols?view=winrt-26100">...</a>
 * @see SMTCBridge
 * @author darraghd493
 * @since 1.0.0
 */
public final class SMTCCommunicator implements Communicator {
    private final SMTCBridge bridge;

    public SMTCCommunicator() {
        this.bridge = SMTCBridge.INSTANCE;
    }

    @Override
    public @NotNull String getTitle() {
        return this.bridge.getTitleString();
    }

    @Override
    public @NotNull String getArtist() {
        return this.bridge.getArtistString();
    }

    @Override
    public @NotNull String getAlbum() {
        return this.bridge.getAlbumString();
    }

    @Override
    public int getDurationSeconds() {
        return this.bridge.getDurationSeconds();
    }

    @Override
    public int getPlayedSeconds() {
        return this.bridge.getPlayedSeconds();
    }

    @Override
    public BufferedImage getAlbumImageData() {
        int[] size = new int[2];
        Pointer pointer = this.bridge.getAlbumImage(size);
        return toBufferedImage(pointer, size);
    }

    @Override
    public boolean isAlbumImageDataAvailable() {
        return this.bridge.isAlbumImageAvailable();
    }

    @Override
    public BufferedImage getAuthorImageData() {
        int[] size = new int[2];
        Pointer pointer = this.bridge.getAuthorImage(size);
        return toBufferedImage(pointer, size);
    }

    @Override
    public boolean isAuthorImageDataAvailable() {
        return this.bridge.isAuthorImageAvailable();
    }

    @Override
    public void start() {
        // no-op
    }

    @Override
    public void stop() {
        // no-op
    }

    private BufferedImage toBufferedImage(Pointer pointer, int[] size) {
        if (pointer == null || size == null || size.length < 2 || size[0] <= 0 || size[1] <= 0) return null;
        int width = size[0], height = size[1];
        byte[] bytes = pointer.getByteArray(0, width * height * 4); // RGBA
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = bytes[idx++] & 0xFF;
                int g = bytes[idx++] & 0xFF;
                int b = bytes[idx++] & 0xFF;
                int a = bytes[idx++] & 0xFF;
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                img.setRGB(x, y, argb);
            }
        }
        this.bridge.freeMemory(pointer);
        return img;
    }
}
