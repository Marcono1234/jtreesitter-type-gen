package marcono1234.jtreesitter.type_gen;

import java.util.Objects;

/**
 * A Java literal value to be emitted in the generated code.
 */
public sealed interface JavaLiteral {
    // For now only cover the most common value types

    record Boolean(boolean value) implements JavaLiteral {
    }

    record Integer(int value) implements JavaLiteral {
    }

    record Long(long value) implements JavaLiteral {
    }

    record Double(double value) implements JavaLiteral {
    }

    record String(java.lang.String value) implements JavaLiteral {
        public String {
            Objects.requireNonNull(value);
        }
    }
}
