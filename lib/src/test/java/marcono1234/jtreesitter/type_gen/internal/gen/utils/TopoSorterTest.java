package marcono1234.jtreesitter.type_gen.internal.gen.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TopoSorterTest {
    private static Arguments args(String input, Map<String, List<String>> references, String expectedSorted) {
        return Arguments.of(
            input,
            references,
            expectedSorted
        );
    }

    static Stream<Arguments> dataToSort() {
        return Stream.of(
            args("", Map.of(), ""),
            // No edges between nodes
            args("A,B,C", Map.of(), "C,B,A"),
            args("A,A1,A2", Map.of("A", List.of("A1"), "A1", List.of("A2")), "A,A1,A2"),
            args("A1,A,A2", Map.of("A", List.of("A1"), "A1", List.of("A2")), "A,A1,A2"),
            args("A1,A2,A", Map.of("A", List.of("A1"), "A1", List.of("A2")), "A,A1,A2"),
            args("A,B,C1,D,C2", Map.of("A", List.of("B"), "B", List.of("C1", "C2"), "C1", List.of("D"), "C2", List.of("C1")), "A,B,C2,C1,D"),
            args("C2,B,C1,A,D", Map.of("A", List.of("B"), "B", List.of("C1", "C2"), "C1", List.of("D"), "C2", List.of("C1")), "A,B,C2,C1,D")
        );
    }

    @ParameterizedTest
    @MethodSource("dataToSort")
    void sort(String inputs, Map<String, List<String>> references, String expectedSorted) {
        var inputsList = Arrays.asList(inputs.split(",", -1));
        var expectedSortedList = Arrays.asList(expectedSorted.split(",", -1));

        var sorted = new TopoSorter<String>((s) -> references.getOrDefault(s, List.of())).sort(inputsList);
        // Wrap in List to also assert elements order
        assertEquals(expectedSortedList, List.copyOf(sorted));
    }

    @Test
    void sort_UnknownReference() {
        var sorter = new TopoSorter<String>((s) -> switch (s) {
            case "A" -> List.of("B");
            case "B" -> List.of("X");
            case "X" -> List.of("Y");
            default -> List.of();
        });

        var sorted = sorter.sort(List.of("A", "B"), TopoSorter.UnknownReferenceStrategy.ADD);
        // Wrap in List to also assert elements order
        assertEquals(List.of("A", "B", "X", "Y"), List.copyOf(sorted));

        sorted = sorter.sort(List.of("A", "B"), TopoSorter.UnknownReferenceStrategy.IGNORE);
        // Wrap in List to also assert elements order
        assertEquals(List.of("A", "B"), List.copyOf(sorted));

        var e = assertThrows(IllegalStateException.class, () -> sorter.sort(List.of("A", "B"), TopoSorter.UnknownReferenceStrategy.THROW));
        assertEquals("References getter introduced for 'B' unknown element 'X'", e.getMessage());
    }
}
