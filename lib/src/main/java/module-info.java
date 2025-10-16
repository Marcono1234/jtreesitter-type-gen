/**
 * Module for code generation.
 *
 * <p>The main class is {@link marcono1234.jtreesitter.type_gen.CodeGenerator}.
 */
@SuppressWarnings({"module", "JavaModuleNaming"}) // suppress warnings about module name, see https://bugs.openjdk.org/browse/JDK-8264488
module marcono1234.jtreesitter.type_gen {
    requires com.fasterxml.jackson.databind;
    requires org.jspecify;

    requires com.palantir.javapoet;
    // Needed for JavaPoet usage
    requires java.compiler;

    exports marcono1234.jtreesitter.type_gen;
}
