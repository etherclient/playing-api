package me.darragh.playingapi.communicator.impl.smtc;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import me.darragh.playingapi.communicator.Communicator;

/**
 * A bridge interface for interacting with the System Media Transport Controls (SMTC) on Windows.
 *
 * @see me.darragh.playingapi.communicator.Communicator
 * @author darraghd493
 * @see 1.0.0
 */
interface SMTCBridge extends Library {
    SMTCBridge INSTANCE = Native.load("SMTCBridge", SMTCBridge.class);

    /**
     * @see Communicator#getTitle()
     */
    Pointer getTitle();

    /**
     * @see Communicator#getArtist()
     */
    Pointer getArtist();

    /**
     * @see Communicator#getAlbum()
     */
    Pointer getAlbum();

    /**
     * @see Communicator#getDurationSeconds()
     */
    int getDurationSeconds();

    /**
     * @see Communicator#getPlayedSeconds()
     */
    int getPlayedSeconds();

    /**
     * @see Communicator#getAlbumImageData()
     */
    Pointer getAlbumImage(IntByReference size);

    /**
     * @see Communicator#isAlbumImageDataAvailable()
     */
    boolean isAlbumImageAvailable();

    /**
     * @see Communicator#getArtistImageData()
     */
    Pointer getArtistImage(IntByReference size);

    /**
     * @see Communicator#isArtistImageDataAvailable()
     */
    boolean isArtistImageAvailable();

    /**
     * @see Communicator#isPaused()
     */
    boolean isPaused();

    /**
     * @see Communicator#playMedia()
     */
    boolean playMedia();

    /**
     * @see Communicator#pauseMedia()
     */
    boolean pauseMedia();

    /**
     * @see Communicator#nextMedia()
     */
    boolean nextMedia();

    /**
     * @see Communicator#previousMedia()
     */
    boolean previousMedia();

    /**
     * Frees memory allocated for a string or image.
     *
     * @param pointer The pointer to the memory to free.
     */
    void freeMemory(Pointer pointer);

    //region Default String Helpers
    default String getTitleString() {
        Pointer p = getTitle();
        if (p == null) return "";
        String s = p.getWideString(0);
        freeMemory(p);
        return s;
    }

    default String getArtistString() {
        Pointer p = getArtist();
        if (p == null) return "";
        return p.getWideString(0);
    }

    default String getAlbumString() {
        Pointer p = getAlbum();
        if (p == null) return "";
        return p.getWideString(0);
    }
    //endregion
}
