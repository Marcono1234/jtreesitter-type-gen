import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Runs the codegen CLI.
 */
@CacheableTask
abstract class CodegenCliTask : DefaultTask() {
    @get:Inject
    protected abstract val javaToolchainService: JavaToolchainService

    @get:Nested
    abstract val launcher: Property<JavaLauncher>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val codeGenCliJar: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val nodeTypesJson: RegularFileProperty

    @get:Nested
    abstract val codeGenConfig: Property<CodeGenConfig>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    data class CodeGenConfig(
        @get:Input
        val packageName: String,
        @get:Input
        val useOptional: Boolean,
        @get:Input
        @get:Optional
        val rootNodeType: String? = null,
        @get:Input
        @get:Optional
        val languageProvider: String? = null,
        @get:Input
        @get:Optional
        val languageVersion: String? = null,
    )

    init {
        // Use same toolchain as project,
        // see https://docs.gradle.org/8.11/userguide/toolchains.html#sec:plugins_toolchains
        val toolchain = project.extensions.getByType(JavaPluginExtension::class.java).toolchain

        @Suppress("LeakingThis")
        val defaultLauncher = javaToolchainService.launcherFor(toolchain)
        @Suppress("LeakingThis")
        launcher.convention(defaultLauncher)
    }

    @TaskAction
    fun runCodeGen() {
        val javaPath = launcher.get().executablePath
        val nodeTypesJson = nodeTypesJson.get().asFile
        val codeGenConfig = codeGenConfig.get()

        // TODO: Could maybe use `JavaExec` task instead? However, does that really match `java -jar` behavior?
        //   (which is desired for integration test), and does it print console output on failure? (to make troubleshooting easier)
        val command = mutableListOf(
            javaPath.asFile.toString(),
            "-jar", codeGenCliJar.get().toString(),
            // Arguments:
            "--node-types", nodeTypesJson.absolutePath,
            "--output-dir", outputDir.get().asFile.absolutePath,
            "--package", codeGenConfig.packageName,
            // Use fixed time, otherwise Gradle will cache the output even though the generated time would
            // actually differ
            "--generated-time", Instant.EPOCH.toString(),
        )
        if (!codeGenConfig.useOptional) {
            command.add("--nullable-annotation")
            command.add("org.jspecify.annotations.Nullable")
        }
        codeGenConfig.rootNodeType?.let {
            command.add("--root-node")
            command.add(it)
        }
        codeGenConfig.languageProvider?.let {
            command.add("--language-provider")
            command.add(it)
        }
        codeGenConfig.languageVersion?.let {
            command.add("--expected-language-version")
            command.add(it)
        }

        val process = ProcessBuilder(command)
            .start()

        // Close std-in; not needed
        process.outputStream.close()
        val stdOutCollector = ProcessOutputCollector("std-out", process.inputReader())
        val stdErrCollector = ProcessOutputCollector("std-out", process.errorReader())

        if (!process.waitFor(20, TimeUnit.SECONDS)) {
            process.destroy()
            // Note: Might have to run `./gradlew ... --info` to see full message
            throw Exception(
                "Code generation process for '${nodeTypesJson}' took too long\n" +
                "std-out:\n${stdOutCollector.getIndentedOutput()}\nstd-err:\n${stdErrCollector.getIndentedOutput()}"
            )
        }
        val exitCode = process.exitValue()
        if (exitCode != 0) {
            // Note: Might have to run `./gradlew ... --info` to see full message
            throw Exception(
                "Code generation for '${nodeTypesJson}' exited with code $exitCode\n" +
                "std-out:\n${stdOutCollector.getIndentedOutput()}\nstd-err:\n${stdErrCollector.getIndentedOutput()}"
            )
        }
    }
}
