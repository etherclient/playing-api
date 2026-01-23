package me.darragh.playingapi;

import me.darragh.playingapi.communicator.Communicator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A bootstrap class for handling {@link me.darragh.playingapi.communicator.Communicator}s.
 *
 * @author darraghd493
 * @since 1.0.0
 */
public final class CommunicatorBootstrap {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private final AtomicReference<Communicator> communicator;

    public CommunicatorBootstrap(Communicator communicator) {
        this.communicator = new AtomicReference<>(communicator);
    }

    /**
     * Starts the communicator.
     */
    public void start() {
        EXECUTOR.execute(() -> this.communicator.get().start());
    }

    /**
     * Stops the communicator.
     */
    public void stop() {
        EXECUTOR.execute(() -> this.communicator.get().stop());
    }

    /**
     * Gets the current communicator.
     *
     * @return The communicator.
     */
    public Communicator getCommunicator() {
        return this.communicator.get();
    }

    /**
     * Gets the current communicator asynchronously.
     *
     * @return A future that will complete with the communicator.
     */
    public CompletableFuture<Communicator> getCommunicatorAsync() {
        return CompletableFuture.supplyAsync(this.communicator::get, EXECUTOR);
    }

    /**
     * Shuts down the communicator executor.
     */
    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }
}
