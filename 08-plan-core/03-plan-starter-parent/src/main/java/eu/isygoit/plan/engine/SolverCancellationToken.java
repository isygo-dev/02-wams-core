package eu.isygoit.plan.engine;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SolverCancellationToken {
    private final AtomicBoolean cancellationRequested = new AtomicBoolean();
    private final Object lock = new Object();
    private Runnable cancellationAction;

    public boolean isCancellationRequested() {
        return cancellationRequested.get();
    }

    public Registration onCancellation(Runnable action) {
        AtomicBoolean invoked = new AtomicBoolean();
        Runnable once = () -> {
            if (invoked.compareAndSet(false, true)) {
                action.run();
            }
        };
        synchronized (lock) {
            cancellationAction = once;
        }
        if (isCancellationRequested()) {
            once.run();
        }
        return () -> {
            synchronized (lock) {
                if (cancellationAction == once) {
                    cancellationAction = null;
                }
            }
        };
    }

    public void cancel() {
        if (!cancellationRequested.compareAndSet(false, true)) {
            return;
        }
        Runnable action;
        synchronized (lock) {
            action = cancellationAction;
        }
        if (action != null) {
            action.run();
        }
    }

    @FunctionalInterface
    public interface Registration extends AutoCloseable {
        @Override
        void close();
    }
}
