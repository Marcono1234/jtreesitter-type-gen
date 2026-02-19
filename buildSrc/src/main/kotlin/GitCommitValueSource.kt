import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

// Based on https://docs.gradle.org/9.3.0/userguide/configuration_cache_requirements.html#config_cache:requirements:external_processes
// TODO: Use https://github.com/palantir/gradle-git-version; check how it is implemented
// Could consider using plugin such as https://github.com/n0mer/gradle-git-properties in the future; however that plugin
// provides too much (not needed) functionality, always creating a `git.properties` file
abstract class GitCommitValueSource : ValueSource<String, ValueSourceParameters.None> {
    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            commandLine("git", "rev-parse", "--verify", "HEAD")
            standardOutput = output
        }
        return output.toString().trim()
    }
}
