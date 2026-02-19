plugins {
    id("jtreesitter-type-gen.java-conventions")
    `java-library`
}

dependencies {
    implementation(libs.jspecify)
    implementation(libs.jackson.databind)
    implementation(libs.javapoet)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.junit)
    testRuntimeOnly(libs.junitPlatformLauncher)
    testImplementation(libs.assertj)

    // Used to compile generated code
    testImplementation(libs.compilerTesting)
    testImplementation(libs.slf4j.simple)
    testRuntimeOnly(libs.jtreesitter)
}

java {
    // Publish sources and javadoc
    withSourcesJar()
    withJavadocJar()
}

val gitCommitProvider = providers.of(GitCommitValueSource::class) {}

val generatedResourcesDir = layout.buildDirectory.dir("generated-resources/main")
sourceSets.main {
    output.dir(generatedResourcesDir)
}
// Note: Including the Git commit ref in a resource file defeats the build cache to some extent; though at least during
//   development when not committing changes yet the build cache helps
val createVersionProperties by tasks.registering(WriteProperties::class) {
    // Place property file under own package to avoid clashes or issues when library is packaged as JAR with dependencies
    destinationFile = generatedResourcesDir.map { it.file("marcono1234/jtreesitter/type_gen/version.properties") }

    property("version", project.version.toString())
    property("repository", "https://github.com/Marcono1234/jtreesitter-type-gen")
    property("commit", gitCommitProvider)
    property("jtreesitter", libs.versions.jtreesitter)
}
tasks.classes {
    dependsOn(createVersionProperties)
}


tasks.test {
    // System property for updating the expected output, see test class
    // Only propagate it if it is actually set (!= null)
    System.getProperty("test-update-expected")?.let { systemProperty("test-update-expected", it) }
}

tasks.javadoc {
    options {
        // Cast to standard doclet options, see https://github.com/gradle/gradle/issues/7038#issuecomment-448294937
        this as StandardJavadocDocletOptions

        encoding = "UTF-8"

        // Enable doclint, but ignore warnings for missing tags, see
        // https://docs.oracle.com/en/java/javase/21/docs/specs/man/javadoc.html#extra-options-for-the-standard-doclet
        // The Gradle option methods are rather misleading, but a boolean `true` value just makes sure the flag
        // is passed to javadoc, see https://github.com/gradle/gradle/issues/2354
        addBooleanOption("Xdoclint:all,-missing", true)


        // Workaround because Gradle does not properly set the source path itself, see https://github.com/gradle/gradle/issues/19726
        val sourceSetDirectories = sourceSets.main.get().java.sourceDirectories
        addPathOption("-source-path").value = sourceSetDirectories.files.toList()
        exclude("marcono1234/jtreesitter/type_gen/internal")
    }
}
