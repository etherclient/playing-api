package me.darragh.playingapi.communicator;

import lombok.RequiredArgsConstructor;

/**
 * Predicts the current playback position for communicators that only update their state every few seconds.
 * 
 * @author darraghd493
 * @since 1.0.5
 */
@RequiredArgsConstructor
public final class PlaybackPredictor {
    private final Communicator communicator;

    private int lastReportedSeconds = -1;
    private long lastUpdateMillis = 0;
    private String lastReportedTitle = null;

    /**
     * Predicts the current played seconds.
     *
     * @return The predicted played seconds, or the exact played seconds if paused or unavailable.
     */
    public int getPredictedPlayedSeconds() {
        int currentReported = this.communicator.getPlayedSeconds();
        boolean paused = this.communicator.isPaused();
        String currentTitle = this.communicator.getTitle();

        boolean trackChanged = this.lastReportedTitle == null || !this.lastReportedTitle.equals(currentTitle);

        if (currentReported != this.lastReportedSeconds || trackChanged) {
            this.lastReportedSeconds = currentReported;
            this.lastUpdateMillis = System.currentTimeMillis();
            this.lastReportedTitle = currentTitle;
            return currentReported;
        }

        if (paused || this.lastUpdateMillis == 0 || currentReported == Communicator.UNKNOWN_DURATION) {
            this.lastUpdateMillis = System.currentTimeMillis();
            return currentReported;
        }

        long sinceUpdate = System.currentTimeMillis() - this.lastUpdateMillis;
        int predicted = currentReported + (int) (sinceUpdate / 1000);
        int duration = this.communicator.getDurationSeconds();

        if (duration != Communicator.UNKNOWN_DURATION) {
            return Math.min(predicted, duration);
        }

        return predicted;
    }

    /**
     * Gets the predicted remaining seconds based on the predicted played seconds.
     *
     * @return The predicted remaining seconds.
     */
    public int getPredictedRemainingSeconds() {
        int duration = this.communicator.getDurationSeconds();
        if (duration == Communicator.UNKNOWN_DURATION) return Communicator.UNKNOWN_DURATION;
        return duration - this.getPredictedPlayedSeconds();
    }
}
