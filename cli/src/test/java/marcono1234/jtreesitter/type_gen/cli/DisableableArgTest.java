package marcono1234.jtreesitter.type_gen.cli;

import marcono1234.jtreesitter.type_gen.cli.converter.DisableableArg;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DisableableArgTest {
    @Test
    void testEnabled() {
        var arg = DisableableArg.enabled("value");
        assertFalse(arg.isDisabled());
        assertEquals("value", arg.getEnabledValue());
        assertEquals(Optional.of("value"), arg.asOptional());

        assertThrows(NullPointerException.class, () -> DisableableArg.enabled(null));
    }

    @Test
    void testDisabled() {
        var arg = DisableableArg.disabled();
        assertTrue(arg.isDisabled());
        var e = assertThrows(IllegalStateException.class, arg::getEnabledValue);
        assertEquals("Not enabled", e.getMessage());
    }

    @Test
    void testHashCode() {
        assertEquals(DisableableArg.enabled("test").hashCode(), DisableableArg.enabled("test").hashCode());

        // This assumes that there is no hash collision
        assertNotEquals(DisableableArg.enabled(1).hashCode(), DisableableArg.enabled(2).hashCode());

        assertEquals(DisableableArg.disabled().hashCode(), DisableableArg.disabled().hashCode());
    }

    // Suppress warnings because this intentionally tests the `equals` implementation
    @SuppressWarnings({"SimplifiableAssertion", "EqualsWithItself", "EqualsBetweenInconvertibleTypes", "ConstantValue"})
    @Test
    void testEquals() {
        assertTrue(DisableableArg.enabled("test").equals(DisableableArg.enabled("test")));
        assertFalse(DisableableArg.enabled("test").equals(DisableableArg.disabled()));
        assertFalse(DisableableArg.enabled("test").equals("test"));
        assertFalse(DisableableArg.enabled("test").equals(null));

        assertTrue(DisableableArg.disabled().equals(DisableableArg.disabled()));
        assertFalse(DisableableArg.disabled().equals("test"));
        assertFalse(DisableableArg.disabled().equals(null));
    }

    @Test
    void testToString() {
        assertEquals("DisableableArg[value=test]", DisableableArg.enabled("test").toString());
        assertEquals("DisableableArg[disabled]", DisableableArg.disabled().toString());
    }
}
