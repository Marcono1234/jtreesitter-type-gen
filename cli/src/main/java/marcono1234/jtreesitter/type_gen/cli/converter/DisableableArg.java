package marcono1234.jtreesitter.type_gen.cli.converter;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * A command line argument which can be disabled. There are three states:
 * <ul>
 *     <li>not specified on command line ({@code null}): Use default value</li>
 *     <li>{@link #enabled(Object)}: Explicitly enabled, with custom value</li>
 *     <li>{@link #disabled()}: Explicitly disabled</li>
 * </ul>
 *
 * <p>This class is pretty similar to the standard {@link Optional} class, except that unlike {@code Optional}
 * it is not treated in a special way by picocli.
 */
public class DisableableArg<T> {
    private static final DisableableArg<Object> DISABLED = new DisableableArg<>(null);

    @Nullable
    private final T value;

    private DisableableArg(@Nullable T value) {
        this.value = value;
    }
    
    public boolean isDisabled() {
        return value == null;
    }
    
    public T getEnabledValue() {
        if (isDisabled()) {
            throw new IllegalStateException("Not enabled");
        }
        assert value != null;
        return value;
    }

    /**
     * If <i>enabled</i> returns an {@link Optional} containing the value, otherwise returns an empty {@code Optional}.
     */
    public Optional<T> asOptional() {
        return Optional.ofNullable(value);
    }

    /**
     * If <i>enabled</i> returns the value, otherwise returns {@code null}.
     */
    public @Nullable T asNullable() {
        return value;
    }

    /**
     * Returns an <i>enabled</i> instance with the given value.
     */
    public static <T> DisableableArg<T> enabled(T value) {
        Objects.requireNonNull(value);
        return new DisableableArg<>(value);
    }

    /**
     * Returns a <i>disabled</i> instance.
     */
    @SuppressWarnings("unchecked")
    public static <T> DisableableArg<T> disabled() {
        return (DisableableArg<T>) DISABLED;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DisableableArg<?> other) {
            return value == other.value;
        }
        return false;
    }

    @Override
    public String toString() {
        return isDisabled() ? "DisableableArg[disabled]" : "DisableableArg[value=" + value + "]";
    }
}
