import org.jspecify.annotations.NullMarked;

/**
 * Module for the command-line interface (CLI) for the code generation.
 */
@NullMarked
@SuppressWarnings({"module", "JavaModuleNaming"}) // suppress warnings about module name, see https://bugs.openjdk.org/browse/JDK-8264488
module marcono1234.jtreesitter.type_gen.cli {
    requires marcono1234.jtreesitter.type_gen;
    requires info.picocli;
    requires org.jspecify;
}
