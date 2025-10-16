// TODO Rename, maybe "jtreesitter-lang-nodes-gen", "jtreesitter-static-bindings-gen", ...?
//   maybe name based on terminology of https://tree-sitter.github.io/tree-sitter/using-parsers/6-static-node-types.html
rootProject.name = "jtreesitter-type-gen"
include("lib", "cli")

plugins {
    // For resolving JDK toolchains, see https://docs.gradle.org/8.11/userguide/toolchains.html#sec:provisioning
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}
