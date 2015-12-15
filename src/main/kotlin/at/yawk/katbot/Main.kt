package at.yawk.katbot

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.inject.Binder
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.binder.AnnotatedBindingBuilder
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.kitteh.irc.client.library.Client
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

internal inline fun <reified T : Any> Injector.getInstance(): T = getInstance(T::class.java)
internal inline fun <reified T : Any> Binder.bind(): AnnotatedBindingBuilder<T> = bind(T::class.java)

val log = LoggerFactory.getLogger("at.yawk.katbot.Main")

/**
 * @author yawkat
 */
fun main(args: Array<String>) {
    val jsonMapper = ObjectMapper()
            .registerKotlinModule()
    val yamlMapper = ObjectMapper(YAMLFactory())
            .registerKotlinModule()

    var config: Config = Files.newInputStream(Paths.get("config.yml"))
            .use { yamlMapper.readValue<Config>(it) }

    val client = connect(config)

    val injector = Guice.createInjector(Module {
        it.bind<ObjectMapper>().toInstance(jsonMapper)
        it.bind<Config>().toInstance(config)
        it.bind<Client>().toInstance(client)
        it.bind<ScheduledExecutorService>().toInstance(Executors.newSingleThreadScheduledExecutor())
        it.bind<HttpClient>().toInstance(HttpClientBuilder.create().build())
    })

    injector.getInstance<Karma>().start()
    injector.getInstance<RssFeedListener>().start()
    injector.getInstance<ForumListener>().start()
    injector.getInstance<Uptime>().start()
    injector.getInstance<Factoid>().start()
}

private fun connect(config: Config): Client {
    val server = config.server
    return Client.builder()
            .listenException { log.error("Error in IRC handler", it) }
            .nick(config.nick)
            .serverHost(server.host)
            .serverPort(server.port)
            .secure(server.secure)
            .serverPassword(server.password)
            .build()
}