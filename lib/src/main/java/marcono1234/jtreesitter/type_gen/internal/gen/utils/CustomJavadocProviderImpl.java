package marcono1234.jtreesitter.type_gen.internal.gen.utils;

import marcono1234.jtreesitter.type_gen.CustomJavadocProvider;
import marcono1234.jtreesitter.type_gen.TypeName;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Internal representation of a user-provided {@link CustomJavadocProvider}.
 */
public class CustomJavadocProviderImpl {
    /**
     * Provides the custom Javadoc for a specific Java element.
     */
    public interface SpecificCustomJavadocProvider {
        Optional<String> getJavadoc(CustomJavadocProviderImpl javadocProvider);
    }

    private final CustomJavadocProvider customJavadocProvider;
    private final CustomJavadocProvider.TypeNameLookup typeNameLookup;
    
    public CustomJavadocProviderImpl(@Nullable CustomJavadocProvider customJavadocProvider, NodeTypeLookup nodeTypeLookup) {
        this.customJavadocProvider = customJavadocProvider != null ? customJavadocProvider : emptyProvider;
        this.typeNameLookup = typeNameLookup(nodeTypeLookup);
    }
    
    private static final CustomJavadocProvider emptyProvider = new CustomJavadocProvider() {
        // For all other methods use the default implementation which returns an empty Optional

        @Override
        public String toString() {
            return "EmptyProvider";
        }
    };
    
    private static CustomJavadocProvider.TypeNameLookup typeNameLookup(NodeTypeLookup nodeTypeLookup) {
        return typeName -> Optional.ofNullable(nodeTypeLookup.getNodeTypeNullable(typeName))
            .map(t -> {
                var className = t.getJavaTypeName();
                String simpleName = String.join("$", className.simpleNames());
                return new TypeName(className.packageName(), simpleName);
            });
    }

    private static String addJavadocSeparator(String javadoc) {
        return "\n\n<hr>\n\n" + javadoc;
    }

    // Uses Optional here (despite internal implementation normally using @Nullable) to allow callers to simply do `Optional.ifPresent(...)`
    
    public Optional<String> forTypedTree() {
        return customJavadocProvider.forTypedTree(typeNameLookup)
            .map(CustomJavadocProviderImpl::addJavadocSeparator);
    }

    public Optional<String> forTypedNode() {
        return customJavadocProvider.forTypedNode(typeNameLookup)
            .map(CustomJavadocProviderImpl::addJavadocSeparator);
    }

    public Optional<String> forNodeType(String nodeType) {
        return customJavadocProvider.forNodeType(nodeType, typeNameLookup)
            .map(CustomJavadocProviderImpl::addJavadocSeparator);
    }

    public Optional<String> forNodeChildrenGetter(String parentNodeType, List<String> childrenNodeTypes) {
        return customJavadocProvider.forNodeChildrenGetter(parentNodeType, childrenNodeTypes, typeNameLookup)
            .map(CustomJavadocProviderImpl::addJavadocSeparator);
    }

    public Optional<String> forNodeChildrenInterface(String parentNodeType, List<String> childrenNodeTypes) {
        return customJavadocProvider.forNodeChildrenInterface(parentNodeType, childrenNodeTypes, typeNameLookup)
            .map(CustomJavadocProviderImpl::addJavadocSeparator);
    }

    public Optional<String> forNodeChildrenTokenClass(String parentNodeType, List<String> tokenTypesNames) {
        return customJavadocProvider.forNodeChildrenTokenClass(parentNodeType, tokenTypesNames, typeNameLookup)
            .map(CustomJavadocProviderImpl::addJavadocSeparator);
    }

    public Optional<String> forNodeChildrenToken(String parentNodeType, String tokenType) {
        return customJavadocProvider.forNodeChildrenToken(parentNodeType, tokenType, typeNameLookup)
            .map(CustomJavadocProviderImpl::addJavadocSeparator);
    }

    public Optional<String> forNodeFieldGetter(String parentNodeType, String fieldName) {
        return customJavadocProvider.forNodeFieldGetter(parentNodeType, fieldName, typeNameLookup)
            .map(CustomJavadocProviderImpl::addJavadocSeparator);
    }

    public Optional<String> forNodeFieldInterface(String parentNodeType, String fieldName) {
        return customJavadocProvider.forNodeFieldInterface(parentNodeType, fieldName, typeNameLookup)
            .map(CustomJavadocProviderImpl::addJavadocSeparator);
    }

    public Optional<String> forNodeFieldTokenClass(String parentNodeType, String fieldName, List<String> tokenTypesNames) {
        return customJavadocProvider.forNodeFieldTokenClass(parentNodeType, fieldName, tokenTypesNames, typeNameLookup)
            .map(CustomJavadocProviderImpl::addJavadocSeparator);
    }

    public Optional<String> forNodeFieldToken(String parentNodeType, String fieldName, String tokenType) {
        return customJavadocProvider.forNodeFieldToken(parentNodeType, fieldName, tokenType, typeNameLookup)
            .map(CustomJavadocProviderImpl::addJavadocSeparator);
    }
}
