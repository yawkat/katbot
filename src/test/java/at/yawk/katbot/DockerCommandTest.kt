package at.yawk.katbot

import at.yawk.docker.DockerClient
import at.yawk.paste.client.PasteClient
import org.mockito.Mockito

fun main(args: Array<String>) {
    val command = DockerCommand(
            EventBus(),
            DockerClient.builder()
                    .url("http://46.101.227.212:2375")
                    .build(),
            Mockito.mock(PasteClient::class.java)
    )
    command.createContainer()

    Thread.sleep(10000)

    //println(command.repl!!("echo test").toCompletableFuture().get())
}