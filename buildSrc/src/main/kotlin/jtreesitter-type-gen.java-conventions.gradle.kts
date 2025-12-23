import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

// Conventions plugin to share common configuration for all sub-projects,
// see https://docs.gradle.org/8.11/samples/sample_convention_plugins.html

plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
}

group = "marcono1234.jtreesitter.type_gen"
version = property("project.version") as String

java {
    toolchain {
        // Should match JDK version specified in GitHub workflow, to avoid downloading separate JDK
        languageVersion = JavaLanguageVersion.of(23) // jtreesitter requires at least JDK 23
    }
}

// Additionally set desired release version to allow building with newer JDK but still targeting older Java version
tasks.compileJava {
    // For now use Java 21 (latest LTS) to make it easier to use code gen CLI; even though generated code
    // requires Java >= 23 due to jtreesitter
    options.release = 21
}

tasks.withType<Test>().configureEach {
    // TODO: Does this conflict with `useJUnitJupiter` usage for test suites?
    useJUnitPlatform()

    testLogging {
        events = setOf(TestLogEvent.SKIPPED, TestLogEvent.FAILED)

        showExceptions = true
        showStackTraces = true
        showCauses = true
        exceptionFormat = TestExceptionFormat.FULL

        // TODO: Simplify once https://github.com/gradle/gradle/issues/5431 is fixed
        afterSuite(KotlinClosure2({ descriptor: TestDescriptor, result: TestResult ->
            // Only handle root test suite
            if (descriptor.parent == null) {
                logger.lifecycle("${result.testCount} tests (${result.successfulTestCount} successful, ${result.skippedTestCount} skipped, ${result.failedTestCount} failed)")
            }
        }))
    }
}
