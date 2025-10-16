import java.io.BufferedReader
import java.time.Duration

/**
 * Class for concurrently reading process output.
 * This is necessary to avoid deadlocks when output buffers become full.
 */
internal class ProcessOutputCollector(name: String, private val reader: BufferedReader) {
    private val thread: Thread
    @Volatile
    private var isCancelled: Boolean = false
    private val outputBuilder: StringBuilder = StringBuilder()

    init {
        val thread = Thread({
            while (!isCancelled) {
                val line = reader.readLine() ?: break
                outputBuilder.append(line).append('\n')
            }
        }, "CodeGenProcessOutputCollector[$name]")
        thread.isDaemon = true
        thread.start()
        this.thread = thread
    }

    fun cancel() {
        isCancelled = true
        thread.interrupt()
    }

    fun getOutput(): String {
        if (!thread.join(Duration.ofSeconds(5))) {
            cancel()
            throw Exception("Waiting for thread '${thread.name}' timed out")
        }
        return outputBuilder.toString()
    }

    fun getIndentedOutput(): String {
        val indent = "  "
        val output = getOutput()
        return if (output.isEmpty()) {
            output
        } else {
            indent + output.replace("\n", "\n" + indent).trimEnd()
        }
    }
}
