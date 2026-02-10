package marcono1234.jtreesitter.type_gen.internal.gen.utils;

import marcono1234.jtreesitter.type_gen.CustomMethodsProvider;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Internal representation of a user-provided {@link CustomMethodsProvider}.
 */
public class CustomMethodsProviderImpl {
    @Nullable
    private final CustomMethodsProvider customMethodsProvider;

    public CustomMethodsProviderImpl(@Nullable CustomMethodsProvider customMethodsProvider) {
        this.customMethodsProvider = customMethodsProvider;
    }

    public List<CustomMethodData> customMethodsForTypedTree() {
        if (customMethodsProvider == null) {
            return List.of();
        }

        return customMethodsProvider.forTypedTree().stream()
            .map(CustomMethodData::fromUserConfig)
            .toList();
    }

    public List<CustomMethodData> customMethodsForTypedNode() {
        if (customMethodsProvider == null) {
            return List.of();
        }

        return customMethodsProvider.forTypedNode().stream()
            .map(CustomMethodData::fromUserConfig)
            .toList();
    }

    public List<CustomMethodData> customMethodsForNodeType(String nodeType) {
        if (customMethodsProvider == null) {
            return List.of();
        }

        return customMethodsProvider.forNodeType(nodeType).stream()
            .map(CustomMethodData::fromUserConfig)
            .toList();
    }

    public List<CustomMethodData> customMethodsForNodeChildrenType(String parentNodeType, List<String> childrenNodeTypes) {
        if (customMethodsProvider == null) {
            return List.of();
        }

        return customMethodsProvider.forNodeChildrenType(parentNodeType, childrenNodeTypes).stream()
            .map(CustomMethodData::fromUserConfig)
            .toList();
    }

    public List<CustomMethodData> customMethodsForNodeFieldType(String parentNodeType, String fieldName) {
        if (customMethodsProvider == null) {
            return List.of();
        }

        return customMethodsProvider.forNodeFieldType(parentNodeType, fieldName).stream()
            .map(CustomMethodData::fromUserConfig)
            .toList();
    }
}
