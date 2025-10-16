import java.io.ByteArrayOutputStream

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

val COMMIT_PROPERTY = "git-commit"
// TODO: Use https://github.com/palantir/gradle-git-version; check how it is implemented
// Could consider using plugin such as https://github.com/n0mer/gradle-git-properties in the future; however that plugin
// provides too much (not needed) functionality, always creating a `git.properties` file
val getGitCommit by tasks.registering(Exec::class) {
    commandLine("git", "rev-parse", "--verify", "HEAD")
    standardOutput = ByteArrayOutputStream()
    doLast {
        val gitCommit = standardOutput.toString().trim()
        extra.set(COMMIT_PROPERTY, gitCommit)
    }
}

val generatedResourcesDir = layout.buildDirectory.dir("generated-resources/main")
sourceSets.main {
    output.dir(generatedResourcesDir)
}
val createVersionProperties by tasks.registering(WriteProperties::class) {
    dependsOn(getGitCommit)

    val gitCommit = getGitCommit.map { task -> task.extra.get(COMMIT_PROPERTY).toString() }
    // Place property file under own package to avoid clashes or issues when library is packaged as JAR with dependencies
    destinationFile = generatedResourcesDir.map { it.file("marcono1234/jtreesitter/type_gen/version.properties") }

    property("version", project.version.toString())
    property("repository", "https://github.com/Marcono1234/jtreesitter-type-gen")
    property("commit", gitCommit)
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
