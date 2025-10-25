rootProject.name = "jtreesitter-type-gen"
include("lib", "cli")

plugins {
    // For resolving JDK toolchains, see https://docs.gradle.org/8.11/userguide/toolchains.html#sec:provisioning
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}
