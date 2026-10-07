package eu.isygoit.async;

import com.vaadin.flow.server.VaadinSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public final class ContextAwareAsync {

    private static volatile Executor defaultExecutor;

    private ContextAwareAsync() {
    }

    static void setDefaultExecutor(Executor executor) {
        defaultExecutor = Objects.requireNonNull(executor, "executor");
    }

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return CompletableFuture.supplyAsync(supplier, requireDefaultExecutor());
    }

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier, Executor executor) {
        Objects.requireNonNull(supplier, "supplier");
        Objects.requireNonNull(executor, "executor");
        return CompletableFuture.supplyAsync(contextualize(supplier), executor);
    }

    public static CompletableFuture<Void> runAsync(Runnable task) {
        Objects.requireNonNull(task, "task");
        return CompletableFuture.runAsync(task, requireDefaultExecutor());
    }

    public static CompletableFuture<Void> runAsync(Runnable task, Executor executor) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(executor, "executor");
        return CompletableFuture.runAsync(contextualize(task), executor);
    }

    private static <T> Supplier<T> contextualize(Supplier<T> supplier) {
        ContextSnapshot context = ContextSnapshot.capture();
        return () -> context.call(supplier);
    }

    static Runnable contextualize(Runnable task) {
        ContextSnapshot context = ContextSnapshot.capture();
        return () -> {
            context.run(task);
        };
    }

    private static Executor requireDefaultExecutor() {
        Executor executor = defaultExecutor;
        if (executor == null) {
            throw new IllegalStateException("ContextAwareAsync has not been configured");
        }
        return executor;
    }

    private static final class ContextSnapshot {

        private final SecurityContext securityContext = SecurityContextHolder.getContext();
        private final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        private final VaadinSession vaadinSession = VaadinSession.getCurrent();

        private static ContextSnapshot capture() {
            return new ContextSnapshot();
        }

        private <T> T call(Supplier<T> supplier) {
            try {
                bind();
                return supplier.get();
            } finally {
                clear();
            }
        }

        private void run(Runnable task) {
            try {
                bind();
                task.run();
            } finally {
                clear();
            }
        }

        private void bind() {
            SecurityContextHolder.setContext(securityContext);
            if (requestAttributes != null) {
                RequestContextHolder.setRequestAttributes(requestAttributes);
            }
            VaadinSession.setCurrent(vaadinSession);
        }

        private void clear() {
            VaadinSession.setCurrent(null);
            RequestContextHolder.resetRequestAttributes();
            SecurityContextHolder.clearContext();
        }
    }
}
