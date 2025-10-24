plugins {
    id("jtreesitter-type-gen.java-conventions")
    java
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":lib"))
    implementation(libs.picocli)
    implementation(libs.jspecify)
    // Jackson for reading token name mapping file
    implementation(libs.jackson.databind)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.junit)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

java {
    // Publish sources, but not Javadoc since this is a CLI tool
    withSourcesJar()
}

// Create JAR with dependencies
// Note: Don't try to replace original JAR, that can prevent Gradle from caching results, see https://github.com/GradleUp/shadow/issues/174
tasks.shadowJar {
    isEnableRelocation = false
    duplicatesStrategy = DuplicatesStrategy.FAIL

    manifest {
        attributes(
            "Main-Class" to "marcono1234.jtreesitter.type_gen.cli.Main",

            // Note: Depending on the dependencies, might have to set `Multi-Release: true`, see https://github.com/johnrengelman/shadow/issues/449
        )
    }

    // Exclude `module-info` from dependencies, see also https://github.com/johnrengelman/shadow/issues/729
    exclude("META-INF/versions/*/module-info.class")
}
// Run shadow task by default
tasks.assemble {
    dependsOn(tasks.shadowJar)
}

@Suppress("UnstableApiUsage") // for Test Suites
testing {
    suites {
        val testName = "integrationTest"
        val testNameUpper = testName.replaceFirstChar(Char::uppercaseChar)
        val testSuite by register<JvmTestSuite>(testName) {
            useJUnitJupiter(libs.versions.junit)

            dependencies {
                implementation(libs.jtreesitter)
                // For @Nullable
                implementation(libs.jspecify)
            }

            targets {
                all {
                    testTask.configure {
                        // System property for updating the expected output, see test class
                        // Only propagate it if it is actually set (!= null)
                        System.getProperty("test-update-expected")?.let { systemProperty("test-update-expected", it) }

                        jvmArgs("--enable-native-access=ALL-UNNAMED")
                    }
                }
            }
        }

        data class CodeGenTaskConfig(
            val language: String,
            val useOptional: Boolean = false,
            val rootNodeTypeName: String? = null,
            val languageProvider: String? = null,
            val languageVersion: String? = null,
            val fallbackNodeTypeMapping: Map<String, String> = emptyMap(),
        )

        val codeGenTaskConfigs = listOf(
            CodeGenTaskConfig("java"),
            CodeGenTaskConfig("java", useOptional = true),
            CodeGenTaskConfig("json"),
            // Note: `languageVersion` here can differ slightly from actual language version as long as version check
            //   in generated code still considers them compatible
            CodeGenTaskConfig("json", languageProvider = "languageField", languageVersion = "0.24.8"),
            CodeGenTaskConfig("json", languageProvider = "languageMethod()", languageVersion = "0.24.8"),
            // Manually map type name due to missing / incorrect type information for alias, see https://github.com/tree-sitter/tree-sitter/issues/1654
            CodeGenTaskConfig("python", fallbackNodeTypeMapping = mapOf("as_pattern_target" to "expression")),
        )
        val nodeTypesDir = layout.projectDirectory.dir("src").dir(testName).dir("node-types-data").asFile
        // Generate unique indices per language name to make sure task names are unique
        val codegenTaskIndices = mutableMapOf<String, Int>()

        val compileTask = tasks.named("compile${testNameUpper}Java")
        tasks.check {
            // By default only compile the generated source, but don't run it; that requires native libraries
            // to be present, so test is only run manually (e.g. by CI)
            dependsOn(compileTask)
        }

        for (taskConfig in codeGenTaskConfigs) {
            val langName = taskConfig.language
            val nodeTypesFile = nodeTypesDir.resolve("node-types-${langName}.json")

            var packageName = "com.example.${langName}"
            if (taskConfig.useOptional) {
                packageName += "_optional"
            }
            val languageProvider = taskConfig.languageProvider?.let {
                val transformedName = it.removeSuffix("()").replace(Regex("[a-z][A-Z]")) { matchResult ->
                    val match = matchResult.value
                    // Convert camelcase to snakecase
                    match[0] + "_" + match[1].lowercaseChar()
                }
                packageName += "_lang_${transformedName}"

                // Class in the non-generated test sources
                "language.${langName}.LanguageProvider#${it}"
            }

            val codegenTaskIndex = codegenTaskIndices.merge(langName, 1, Integer::sum)

            // Use individual output dirs, otherwise when using a single output dir for all tests Gradle build cache
            // cannot reliably cache the outputs
            val generatedSourcesDir = layout.buildDirectory.dir("${testName}/generated-sources-${langName}-${codegenTaskIndex}")
            testSuite.sources.java.srcDir(generatedSourcesDir)

            val langNameUpper = langName.replaceFirstChar(Char::uppercaseChar)
            val generateSourcesTask by tasks.register<CodegenCliTask>("generate${testNameUpper}Sources${langNameUpper}${codegenTaskIndex}") {
                description = "Integration test code generation with config: ${taskConfig}"

                codeGenCliJar.set(tasks.shadowJar.map { it.archiveFile.get() })
                nodeTypesJson.set(nodeTypesFile)
                codeGenConfig.set(
                    CodegenCliTask.CodeGenConfig(
                        packageName,
                        taskConfig.useOptional,
                        taskConfig.rootNodeTypeName,
                        languageProvider,
                        taskConfig.languageVersion,
                        taskConfig.fallbackNodeTypeMapping,
                    )
                )

                outputDir.set(generatedSourcesDir)
            }
            compileTask.configure {
                dependsOn(generateSourcesTask)
            }
        }
    }
}
