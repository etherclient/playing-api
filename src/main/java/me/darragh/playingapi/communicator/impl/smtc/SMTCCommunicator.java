package me.darragh.playingapi.communicator.impl.smtc;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import me.darragh.playingapi.communicator.Communicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * A communicator implementation that interacts with the System Media Transport Controls (SMTC) on Windows.
 *
 * @see <a href="https://learn.microsoft.com/en-us/uwp/api/windows.media.systemmediatransportcontrols?view=winrt-26100">WinRT docs</a>
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
    public @Nullable BufferedImage getArtistImageData() {
        // hardcoded - unsupported
        return null;
    }

    @Override
    public boolean isArtistImageDataAvailable() {
        // hardcoded - unsupported
        return false;
    }

    @Override
    public @Nullable BufferedImage getAlbumImageData() {
        IntByReference sizeRef = new IntByReference();
        Pointer pointer = this.bridge.getAlbumImage(sizeRef);
        return this.getBufferedImage(sizeRef, pointer);
    }

    @Override
    public boolean isAlbumImageDataAvailable() {
        return this.bridge.isAlbumImageAvailable();
    }

    @Override
    public void start() {
        // no-op
    }

    @Override
    public void stop() {
        // no-op
    }

    @Nullable
    private BufferedImage getBufferedImage(IntByReference sizeRef, Pointer p) {
        if (p == null) return null;

        int size = sizeRef.getValue();
        byte[] bytes = p.getByteArray(0, size);
        bridge.freeMemory(p);

        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            return null;
        }
    }
}
