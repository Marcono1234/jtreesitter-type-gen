package marcono1234.jtreesitter.type_gen.internal.gen.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class MemoizedSupplierTest {
    @Test
    void get() {
        var atomicInt = new AtomicInteger(0);
        var supplier = new MemoizedSupplier<>(atomicInt::incrementAndGet);

        assertEquals(0, atomicInt.get());
        assertEquals(1, supplier.get());
        assertEquals(1, atomicInt.get());

        // Value should remain the same, and not call delegate again
        assertEquals(1, supplier.get());
        assertEquals(1, atomicInt.get());
    }

    @Test
    void get_Concurrent() throws Exception {
        var atomicInt = new AtomicInteger(0);
        var supplier = new MemoizedSupplier<>(() -> {
            try {
                // Add some delay to make it more likely that multiple threads wait for the result
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return atomicInt.incrementAndGet();
        });

        var exception = new AtomicReference<>(null);
        var threads = IntStream.range(0, 100).mapToObj(_ -> Thread.startVirtualThread(() -> {
            try {
                assertEquals(1, supplier.get());
            } catch (Throwable t) {
                exception.set(t);
            }
        })).toList();
        for (Thread thread : threads) {
            thread.join();
        }

        assertNull(exception.get());
        assertEquals(1, atomicInt.get());

        // Value should remain the same, and not call delegate again
        assertEquals(1, supplier.get());
        assertEquals(1, atomicInt.get());
    }
}
