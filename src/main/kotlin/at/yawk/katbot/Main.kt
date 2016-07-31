/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import at.yawk.docker.DockerClient
import at.yawk.katbot.action.*
import at.yawk.katbot.command.CommandManager
import at.yawk.katbot.markov.Markov
import at.yawk.katbot.passive.*
import at.yawk.paste.client.PasteClient
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
import org.flywaydb.core.Flyway
import org.h2.jdbcx.JdbcDataSource
import org.kitteh.irc.client.library.Client
import org.kitteh.irc.client.library.element.MessageReceiver
import org.kitteh.irc.lib.net.engio.mbassy.listener.Handler
import org.skife.jdbi.v2.DBI
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import javax.sql.DataSource

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
            .closed { yamlMapper.readValue<Config>(it) }

    val dataSource = JdbcDataSource()
    dataSource.setUrl("jdbc:h2:./db/db")

    val flyway = Flyway()
    flyway.dataSource = dataSource
    flyway.migrate()

    val client = connect(config)

    val eventBus = EventBus()
    client.eventManager.registerEventListener(object {
        @Handler
        fun handle(o: Any) = eventBus.post(o)
    })

    val injector = Guice.createInjector(Module {
        it.bind<ObjectMapper>().toInstance(jsonMapper)
        it.bind<EventBus>().toInstance(eventBus)
        it.bind<Config>().toInstance(config)
        it.bind<DataSource>().toInstance(dataSource)
        it.bind<DBI>().toInstance(DBI(dataSource))
        it.bind<ScheduledExecutorService>().toInstance(Executors.newSingleThreadScheduledExecutor())
        it.bind<HttpClient>().toInstance(HttpClientBuilder.create().build())
        it.bind<IrcProvider>().toInstance(object : IrcProvider {
            override fun findChannels(channelNames: Collection<String>): List<MessageReceiver> {
                return channelNames.map {
                    val channelOptional = client.getChannel(it)
                    if (!channelOptional.isPresent) {
                        ForumListener.log.warn("Could not find channel {}", it)
                        null
                    } else {
                        channelOptional.get()
                    }
                }.filterNotNull()
            }
        })
        it.bind<PasteClient>().toInstance(PasteClient(config.paste, jsonMapper))
        it.bind<DockerClient>().toInstance(DockerClient.builder().url(config.docker.url).build())
    })

    injector.getInstance<CommandManager>().start()

    injector.getInstance<Restrict>().start()
    injector.getInstance<Karma>().start()
    injector.getInstance<RssFeedListener>().start()
    injector.getInstance<ForumListener>().start()
    injector.getInstance<Uptime>().start()
    injector.getInstance<Factoid>().start()
    injector.getInstance<Decide>().start()
    injector.getInstance<Interact>().start()
    injector.getInstance<Ignore>().start()
    injector.getInstance<UrbanDictionary>().start()
    injector.getInstance<RoleManagerImpl>().start()
    injector.getInstance<Fortune>().start()
    injector.getInstance<Seen>().start()
    injector.getInstance<Cip>().start()
    injector.getInstance<EventManager>().start()
    injector.getInstance<Sql>().start()
    injector.getInstance<UrlTitleLoader>().start()
    injector.getInstance<Wosch>().start()
    injector.getInstance<Markov>().start()
    injector.getInstance<DockerCommand>().start()
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