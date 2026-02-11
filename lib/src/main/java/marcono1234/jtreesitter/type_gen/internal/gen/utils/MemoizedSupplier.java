package marcono1234.jtreesitter.type_gen.internal.gen.utils;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Supplier which wraps a delegate supplier and caches its result.
 */
public class MemoizedSupplier<T> implements Supplier<T> {
    private final Supplier<T> delegate;
    private volatile @Nullable T value;

    public MemoizedSupplier(Supplier<T> delegate) {
        this.delegate = delegate;
        this.value = null;
    }

    @Override
    public T get() {
        // Double-checked locking
        T value = this.value;
        if (value == null) {
            synchronized (this) {
                value = this.value;
                if (value == null) {
                    this.value = value = Objects.requireNonNull(delegate.get());
                }
            }
        }
        return value;
    }
}
