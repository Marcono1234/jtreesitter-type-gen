package marcono1234.jtreesitter.type_gen.internal.gen.utils;

import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * Performs topological sorting.
 *
 * @param <T> element type
 */
public class TopoSorter<T> {
    private final Function<T, List<? extends T>> referencesGetter;

    /**
     * @param referencesGetter
     *      For a node <var>n</var> gets all nodes which it refers to. That is, for all edges from <var>n</var> to
     *      <var>m</var>, given <var>n</var> this function returns all <var>m</var> nodes.
     */
    public TopoSorter(Function<T, List<? extends T>> referencesGetter) {
        this.referencesGetter = referencesGetter;
    }

    /**
     * How to handle the case when {@code referencesGetter} returns an element which was not in the given
     * input collection to sort.
     */
    public enum UnknownReferenceStrategy {
        /** Add the new element to the results. */
        ADD,
        /** Ignore the new element. */
        IGNORE,
        /** Throw an exception. */
        THROW,
    }

    /**
     * Sorts the given elements and returns the sorted set.
     */
    public SequencedSet<T> sort(SequencedCollection<? extends T> elements, UnknownReferenceStrategy unknownReferenceStrategy) {
        Set<T> originalElements = unknownReferenceStrategy != UnknownReferenceStrategy.ADD ? Set.copyOf(elements) : null;

        var sorted = new LinkedHashSet<T>();
        for (T element : elements) {
            visit(element, sorted, unknownReferenceStrategy, originalElements);
        }


        return sorted;
    }

    public SequencedSet<T> sort(SequencedCollection<? extends T> elements) {
        return sort(elements, UnknownReferenceStrategy.THROW);
    }

    /**
     * Depth-first-search implementation, see https://en.wikipedia.org/wiki/Topological_sorting#Depth-first_search
     * (except without 'temporary mark' tracking)
     */
    private void visit(T element, LinkedHashSet<T> sorted, UnknownReferenceStrategy unknownReferenceStrategy, @Nullable Set<T> originalElements) {
        // checking if `sorted` already contains the element acts as 'permanent mark' check
        if (sorted.contains(element)) {
            return;
        }

        for (T referencedElement : referencesGetter.apply(element)) {
            if (originalElements != null && !originalElements.contains(referencedElement)) {
                switch (unknownReferenceStrategy) {
                    case IGNORE -> {
                        continue;
                    }
                    case THROW -> throw new IllegalStateException("References getter introduced for '" + element + "' unknown element '" + referencedElement + "'");
                    case ADD -> throw new AssertionError("unreachable because `originalElements` should be null, see `sort` implementation");
                }
            }

            visit(referencedElement, sorted, unknownReferenceStrategy, originalElements);
        }

        sorted.addFirst(element);
    }
}
