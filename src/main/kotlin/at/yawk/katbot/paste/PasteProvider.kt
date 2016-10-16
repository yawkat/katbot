/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.paste

import at.yawk.katbot.Config
import at.yawk.katbot.randomChoice
import at.yawk.katbot.web.WebProvider
import io.undertow.server.HttpHandler
import io.undertow.util.Headers
import org.skife.jdbi.v2.DBI
import org.skife.jdbi.v2.StatementContext
import org.skife.jdbi.v2.sqlobject.Bind
import org.skife.jdbi.v2.sqlobject.BindBean
import org.skife.jdbi.v2.sqlobject.SqlQuery
import org.skife.jdbi.v2.sqlobject.SqlUpdate
import org.skife.jdbi.v2.sqlobject.customizers.Mapper
import org.skife.jdbi.v2.tweak.ResultSetMapper
import java.net.URI
import java.sql.ResultSet
import javax.inject.Inject
import javax.inject.Singleton
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * @author yawkat
 */
@Singleton
class PasteProvider @Inject constructor(
        private val config: Config,
        dbi: DBI,
        private val webProvider: WebProvider
) {
    private val dao = dbi.onDemand(Dao::class.java)

    fun start() {
        webProvider.addResource(this)
        webProvider.addRootHandler("/p", HttpHandler { xhg ->
            var path = xhg.relativePath
            if (path.startsWith('/')) path = path.substring(1)

            val paste = dao.getPaste(path)
            if (paste == null) {
                xhg.statusCode = Response.Status.NOT_FOUND.statusCode
                xhg.responseHeaders.put(Headers.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                xhg.responseSender.send("Not Found")

            } else when (paste.type) {
                Paste.Type.URL -> {
                    xhg.statusCode = Response.Status.MOVED_PERMANENTLY.statusCode
                    xhg.responseHeaders.put(Headers.LOCATION, paste.data)
                }
                Paste.Type.TEXT -> {
                    xhg.statusCode = Response.Status.OK.statusCode
                    xhg.responseHeaders.put(Headers.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                    xhg.responseSender.send(paste.data)
                }
            }
        })
    }

    private fun generateId(): String {
        val value = CharArray(4)
        for (i in value.indices) {
            val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            value[i] = randomChoice(chars.toList())
        }
        return String(value)
    }

    @Synchronized // sucky but simple solution to avoid PK constraint issues
    fun createPaste(paste: Paste): URI {
        repeat(20) {
            val id = generateId()
            if (!dao.hasPaste(id)) {
                dao.createPaste(id, paste)
                return URI(config.web.externalHostWithTrailingSlash + "p/" + id)
            }
        }
        throw Exception("Failed to generate unique ID for paste")
    }

    private interface Dao {
        @SqlQuery("SELECT count(id) > 0 FROM paste WHERE id = :id")
        fun hasPaste(@Bind("id") id: String): Boolean

        @SqlUpdate("INSERT INTO paste (id, type, data) VALUES (:id, :type, :data)")
        fun createPaste(@Bind("id") id: String, @BindBean paste: Paste)

        @SqlQuery("SELECT type, data FROM paste WHERE id = :id")
        @Mapper(PasteMapper::class)
        fun getPaste(@Bind("id") id: String): Paste?

        class PasteMapper : ResultSetMapper<Paste> {
            override fun map(index: Int, r: ResultSet, ctx: StatementContext) =
                    Paste(Paste.Type.valueOf(r.getString("type")), r.getString("data"))
        }
    }
}