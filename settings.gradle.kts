rootProject.name = "jtreesitter-type-gen"
include("lib", "cli")

plugins {
    // For resolving JDK toolchains, see https://docs.gradle.org/9.1.0/userguide/toolchains.html#sec:provisioning
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}
