package at.yawk.katbot

import at.yawk.docker.DockerClient
import at.yawk.docker.http.HttpException
import at.yawk.docker.http.ReadCallback
import at.yawk.docker.model.ContainerConfig
import at.yawk.docker.model.HostConfig
import at.yawk.docker.model.LogLine
import at.yawk.paste.client.PasteClient
import at.yawk.paste.model.TextPasteData
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executors
import javax.inject.Inject

private const val CONTAINER_NAME = "katbot_repl"
private const val OUTPUT_PREFIX = " OUTPUT "
private const val EOF = "EOF"
private const val OUTPUT_LIMIT = 1000

fun isPrintableAsciiChar(it: Char) = it >= ' ' && it <= '~'

/**
 * @author yawkat
 */
class DockerCommand @Inject constructor(val eventBus: EventBus, val dockerClient: DockerClient, val pasteClient: PasteClient) {
    companion object {
        private val log = LoggerFactory.getLogger(DockerCommand::class.java)
    }

    /**
     * REPL function. Accepts the command and returns a future containing the output.
     *
     * Visible for testing
     */
    internal var repl: ((String) -> CompletionStage<String>)? = null

    fun start() {
        createContainer()
        eventBus.subscribe(this)
    }

    @Subscribe(priority = 1000) // lower than factoids
    fun command(command: Command) {
        if (!command.public) return

        val repl = this.repl
        if (repl == null) {
            command.channel.sendMessage("REPL unavailable.")
            return
        }
        repl(command.message).thenAccept {
            val output = String(it
                    .map { if (it == '\t') ' ' else it }
                    .filter { isPrintableAsciiChar(it) || it == '\n' }
                    .toCharArray()).trim()
            if (!output.isEmpty()) {
                // max 2 lines raw, otherwise pastebin
                if (output.count { it == '\n' } >= 2) {
                    val data = TextPasteData()
                    data.text = output
                    command.channel.sendMessage(pasteClient.save(data))
                } else {
                    output.split('\n').forEach { command.channel.sendMessage(it) }
                }
            } else {
                command.channel.sendMessage("No output")
            }
        }
    }

    fun createContainer() {
        val containerConfig = ContainerConfig()
        containerConfig.image = "ubuntu"
        containerConfig.memory = 100 * 1024 * 1024 // 100mb
        containerConfig.tty = true
        containerConfig.attachStderr = true
        containerConfig.attachStdout = true
        containerConfig.attachStdin = true
        containerConfig.openStdin = true
        containerConfig.networkDisabled = true
        containerConfig.cpuSet = "0" // only first cpu
        containerConfig.hostConfig = HostConfig()

        log.debug("Creating container...")
        dockerClient.createContainer().name(CONTAINER_NAME).config(containerConfig).send().whenComplete create@ { it, error ->
            if (error == null ||
                    // HACK: 409 = container exists
                    (error is HttpException && error.message?.startsWith("409") ?: false)) {
                startContainer()
            } else {
                log.error("Failed to create container", error)
                return@create
            }
        }
    }

    private fun startContainer() {
        dockerClient.stopContainer().id(CONTAINER_NAME).send().whenComplete { it, error ->
            // ignore error

            log.debug("Starting container...")
            dockerClient.startContainer().id(CONTAINER_NAME).send().whenComplete start@ { it, error ->
                if (error != null) {
                    log.error("Failed to start container", error)
                    return@start
                }

                initScript()
            }
        }
    }

    private fun initScript() {
        log.debug("Attaching container...")

        val stdinStrings = ArrayBlockingQueue<String>(1024)

        val logsPromise = dockerClient.containerLogs()
                .id(CONTAINER_NAME)
                .stderr(true).stdout(true).stdin(false).follow(true).timestamps(true).tail(0)
                .send()
        logsPromise.whenComplete(attach@ { stdout, error ->
            if (error != null) {
                log.error("Failed to attach to container", error)
                return@attach
            }

            stdout.readCallback(object : ReadCallback<LogLine> {
                override fun read(line: LogLine) {
                    log.trace("LINE {}", line)
                    while (!stdinStrings.offer(line.line)) {
                        // clear old input
                        stdinStrings.poll()
                    }
                }
            })
        })

        fun writeInput(string: String) {
            dockerClient.attachContainer()
                    .id(CONTAINER_NAME).stdin(true).stream(true).tty(true)
                    .send().thenAccept({
                log.trace("WRITING {}", string.replace("\n", "\\n"))
                it.writeAndFlush(string)
                it.close()
            })
        }

        var initCommand = "cat > /repl.sh << \"EOF\"\n"

        BufferedReader(
                InputStreamReader(DockerCommand::class.java.getResourceAsStream("repl.sh"), StandardCharsets.UTF_8)
        ).use {
            initCommand += it.lineSequence().toList().joinToString("\n")
        }

        initCommand += "\nEOF\n"
        initCommand += "chmod 500 /repl.sh\n"
        initCommand += "/repl.sh || exit\n"

        writeInput(initCommand)

        val executor = Executors.newSingleThreadExecutor()

        val allowedCharacters = BitSet()
        for (c in 0x70..0x7e) {
            allowedCharacters.set(c)
        }

        repl = { command ->
            val sanitized = String(command.filter { isPrintableAsciiChar(it) }.toCharArray())

            val future = CompletableFuture<String>()
            executor.execute {
                try {
                    writeInput("$sanitized\n")

                    var output = ""
                    while (true) {
                        val line = stdinStrings.take()
                        if (line.startsWith(OUTPUT_PREFIX) && output.length < OUTPUT_LIMIT) {
                            output += line.substring(OUTPUT_PREFIX.length) + "\n"
                            if (output.length > OUTPUT_LIMIT) {
                                output = output.substring(0, OUTPUT_LIMIT)
                            }
                        } else if (line == EOF) {
                            break
                        } else {
                            log.trace("Discarded output: {}", line)
                        }
                    }
                    // clear leftover input
                    while (!stdinStrings.isEmpty()) {
                        val line = stdinStrings.poll()
                        log.trace("Discarded output: {}", line)
                    }

                    future.complete(output)
                } catch (t: Throwable) {
                    t.printStackTrace()
                    future.completeExceptionally(t)
                }
            }
            future
        }

        log.info("REPL ready")
    }
}