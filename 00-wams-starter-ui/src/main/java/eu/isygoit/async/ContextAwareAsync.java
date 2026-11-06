package eu.isygoit.async;

import eu.isygoit.exception.BadArgumentException;
import eu.isygoit.exception.ServiceOperationException;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public final class ContextAwareAsync {

    private static volatile Executor defaultExecutor;

    private ContextAwareAsync() {
    }

    static void setDefaultExecutor(Executor executor) {
        defaultExecutor = requireArgument(executor, "executor");
    }

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        requireArgument(supplier, "supplier");
        return CompletableFuture.supplyAsync(contextualize(supplier), requireDefaultExecutor());
    }

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier, Executor executor) {
        requireArgument(supplier, "supplier");
        requireArgument(executor, "executor");
        return CompletableFuture.supplyAsync(contextualize(supplier), executor);
    }

    public static CompletableFuture<Void> runAsync(Runnable task) {
        requireArgument(task, "task");
        return CompletableFuture.runAsync(contextualize(task), requireDefaultExecutor());
    }

    public static CompletableFuture<Void> runAsync(Runnable task, Executor executor) {
        requireArgument(task, "task");
        requireArgument(executor, "executor");
        return CompletableFuture.runAsync(contextualize(task), executor);
    }

    private static <T> T requireArgument(T value, String name) {
        if (value == null) {
            throw new BadArgumentException(name + " must not be null");
        }
        return value;
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
            throw new ServiceOperationException("ContextAwareAsync has not been configured");
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