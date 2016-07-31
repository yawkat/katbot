/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.web

import at.yawk.katbot.Config
import at.yawk.katbot.log
import io.undertow.Undertow
import io.undertow.server.handlers.PathHandler
import io.undertow.server.handlers.resource.ClassPathResourceManager
import io.undertow.server.handlers.resource.ResourceHandler
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer
import java.util.*
import javax.inject.Inject
import javax.ws.rs.Path
import javax.ws.rs.core.Application

/**
 * @author yawkat
 */
class WebBootstrap @Inject constructor(val config: Config) : WebProvider {
    private val resources = HashSet<Any>()
    private var started = false

    fun start() {
        val application = object : Application() {
            override fun getSingletons() = resources
        }

        val server = UndertowJaxrsServer()

        val rootField = server.javaClass.getDeclaredField("root")
        rootField.isAccessible = true
        val root = rootField.get(server) as PathHandler
        val classLoader = WebBootstrap::class.java.classLoader
        root.addPrefixPath("/", ResourceHandler(ClassPathResourceManager(classLoader, "at/yawk/katbot/web/static")))
        root.addPrefixPath("/webjars", ResourceHandler(ClassPathResourceManager(classLoader, "META-INF/resources/webjars")))

        server.deploy(application, "/api")
                .start(Undertow.builder().addHttpListener(config.web.port, config.web.bindHost))
    }

    override fun addResource(resource: Any) {
        if (started) throw IllegalStateException()
        if (!resource.javaClass.isAnnotationPresent(Path::class.java)) {
            log.warn("Missing @Path annotation on ${resource.javaClass.name}")
        }
        resources.add(resource)
    }
}