/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import at.yawk.katbot.action.*
import at.yawk.katbot.command.CommandManager
import at.yawk.katbot.exposure.ExposureFactoid
import at.yawk.katbot.markov.Markov
import at.yawk.katbot.nina.NinaListener
import at.yawk.katbot.passive.EventManager
import at.yawk.katbot.passive.ForumListener
import at.yawk.katbot.passive.Ignore
import at.yawk.katbot.passive.RssFeedListener
import at.yawk.katbot.passive.UrlTitleLoader
import at.yawk.katbot.paste.PasteProvider
import at.yawk.katbot.security.Security
import at.yawk.katbot.security.SecurityModule
import at.yawk.katbot.security.WebSecurityEditor
import at.yawk.katbot.web.WebBootstrap
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.inject.Binder
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.binder.AnnotatedBindingBuilder
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.shiro.mgt.SecurityManager
import org.apache.shiro.util.ThreadContext
import org.flywaydb.core.Flyway
import org.h2.jdbcx.JdbcDataSource
import org.kitteh.irc.client.library.Client
import org.kitteh.irc.client.library.element.MessageReceiver
import org.kitteh.irc.client.library.element.User
import org.kitteh.irc.client.library.event.helper.ActorMessageEvent
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

private val log = LoggerFactory.getLogger("at.yawk.katbot.Main")

/**
 * @author yawkat
 */
fun main(args: Array<String>) {
    val jsonMapper = ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    val yamlMapper = ObjectMapper(YAMLFactory())
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    val config: Config = Files.newInputStream(Paths.get("config.yml"))
            .closed { yamlMapper.readValue<Config>(it) }

    val dataSource = JdbcDataSource()
    dataSource.setUrl("jdbc:h2:./db/db")

    val flyway = Flyway()
    flyway.dataSource = dataSource
    flyway.migrate()

    val dbi = DBI(dataSource)

    val client = connect(config)

    val eventBus = EventBus()

    val injector = Guice.createInjector(SecurityModule(dbi), Module {
        it.bind<ObjectMapper>().toInstance(jsonMapper)
        it.bind<EventBus>().toInstance(eventBus)
        it.bind<Config>().toInstance(config)
        it.bind<DataSource>().toInstance(dataSource)
        it.bind<DBI>().toInstance(dbi)
        it.bind<ScheduledExecutorService>().toInstance(Executors.newSingleThreadScheduledExecutor(
                ThreadFactoryBuilder()
                        .setNameFormat("katbot-scheduler-%s")
                        .build()
        ))
        it.bind<HttpClient>().toInstance(HttpClientBuilder.create().disableCookieManagement().build())
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
    })

    val securityManager = injector.getInstance(SecurityManager::class.java)

    client.eventManager.registerEventListener(object {
        @Handler
        fun handle(o: Any): Boolean {
            var hasSubject = false
            if (o is ActorMessageEvent<*>) {
                val actor = o.actor
                if (actor is User) {
                    val subject = Security.getSubjectForUser(securityManager, actor)
                    ThreadContext.bind(securityManager)
                    ThreadContext.bind(subject)
                    hasSubject = true
                }
            }
            try {
                return eventBus.post(o)
            } finally {
                if (hasSubject) {
                    ThreadContext.unbindSubject()
                    ThreadContext.unbindSecurityManager()
                }
            }
        }
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
    injector.getInstance<NCov>().start()
    injector.getInstance<ExposureFactoid>().start()
    injector.getInstance<Fortune>().start()
    injector.getInstance<Seen>().start()
    injector.getInstance<Cip>().start()
    injector.getInstance<EventManager>().start()
    injector.getInstance<Sql>().start()
    injector.getInstance<UrlTitleLoader>().start()
    injector.getInstance<Wosch>().start()
    injector.getInstance<Markov>().start()
    injector.getInstance<GuildWars2Item>().start()
    injector.getInstance<UnicodeInfo>().start()
    injector.getInstance<Invite>().start()
    injector.getInstance<KatTemp>().start()
    injector.getInstance<NinaListener>().start()
    injector.getInstance<WebSecurityEditor>().start()
    injector.getInstance<PasteProvider>().start()

    injector.getInstance<WebBootstrap>().start()
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