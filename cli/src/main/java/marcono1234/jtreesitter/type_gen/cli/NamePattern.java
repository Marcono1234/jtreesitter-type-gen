package marcono1234.jtreesitter.type_gen.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pattern for name generation. Patterns consists of literal text pieces and placeholders.
 *
 * <p>A pattern is obtained by using {@link Parser#parse(String)} to parse a pattern string. The parser
 * defines which placeholders the pattern supports and from which data {@code <D>} it retrieves the information
 * to resolve the placeholders.
 *
 * <p>The method {@link #createName(D)} is then used to create a name from the pattern.
 *
 * @param <D> type of the data for resolving placeholders in the pattern
 */
public class NamePattern<D> {
    /**
     * Parser for a pattern string.
     *
     * @param <D> type of the data for resolving placeholders in the pattern
     */
    public static class Parser<D> {
        /**
         * Obtains the placeholder value from the data, possibly transforming it first.
         */
        public interface PlaceholderResolver<D> {
            String getValue(D data);
        }

        private final Map<String, PlaceholderResolver<D>> placeholders;

        /**
         * @param placeholders maps from placeholder name to the corresponding resolver
         */
        public Parser(Map<String, PlaceholderResolver<D>> placeholders) {
            if (placeholders.isEmpty()) {
                throw new IllegalArgumentException("Must specify at least one placeholder");
            }
            this.placeholders = placeholders;
        }

        /**
         * Parses the pattern string and creates a pattern object.
         *
         * @throws IllegalArgumentException if the pattern string is malformed
         */
        public NamePattern<D> parse(String pattern) {
            if (pattern.isEmpty()) {
                throw new IllegalArgumentException("Pattern string must not be empty");
            }

            var patternPieces = new ArrayList<PatternPiece<D>>();

            final char START_CHAR = '{';
            final char END_CHAR = '}';
            int searchStart = 0;
            int placeholderStart;
            while ((placeholderStart = pattern.indexOf(START_CHAR, searchStart)) != -1) {
                int placeholderEnd = pattern.indexOf(END_CHAR, placeholderStart + 1);

                if (placeholderEnd == -1) {
                    throw new IllegalArgumentException("Missing closing '" + END_CHAR + "'");
                }
                int otherStartIndex = pattern.indexOf(START_CHAR, placeholderStart + 1, placeholderEnd);
                if (otherStartIndex != -1 && otherStartIndex < placeholderEnd) {
                    throw new IllegalArgumentException("Unexpected '" + START_CHAR + "' in placeholder name");
                }

                // Append leading literal string, if any
                if (searchStart < placeholderStart) {
                    patternPieces.add(new LiteralPiece<>(pattern.substring(searchStart, placeholderStart)));
                }
                searchStart =  placeholderEnd + 1;

                String placeholderName = pattern.substring(placeholderStart + 1, placeholderEnd);
                if (placeholderName.isEmpty()) {
                    throw new IllegalArgumentException("Placeholder name cannot be empty");
                }

                var placeholder = placeholders.get(placeholderName);
                if (placeholder == null) {
                    throw new IllegalArgumentException("Unknown placeholder: " + placeholderName);
                }
                patternPieces.add(new PlaceholderPiece<>(placeholder));
            }

            if (pattern.indexOf(END_CHAR, searchStart) != -1) {
                throw new IllegalArgumentException("Unpaired closing '" + END_CHAR + "'");
            }

            // Append trailing literal string, if any
            if (searchStart < pattern.length()) {
                patternPieces.add(new LiteralPiece<>(pattern.substring(searchStart)));
            }
            return new NamePattern<>(patternPieces);
        }

    }

    private interface PatternPiece<D> {
        String getString(D data);
    }

    private record LiteralPiece<D>(String value) implements PatternPiece<D> {
        @Override
        public String getString(D data) {
            return value;
        }
    }

    private record PlaceholderPiece<D>(Parser.PlaceholderResolver<D> placeholderResolver) implements PatternPiece<D> {
        @Override
        public String getString(D data) {
            return placeholderResolver.getValue(data);
        }
    }

    private final List<PatternPiece<D>> pieces;

    private NamePattern(List<PatternPiece<D>> pieces) {
        this.pieces = pieces;
    }

    /**
     * Creates the name by resolving placeholders in the pattern using the given data.
     */
    public String createName(D data) {
        var builder = new StringBuilder();
        for (var piece : pieces) {
            builder.append(piece.getString(data));
        }
        return builder.toString();
    }
}
