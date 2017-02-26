/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.action

import at.yawk.katbot.CancelEvent
import at.yawk.katbot.EventBus
import at.yawk.katbot.Subscribe
import at.yawk.katbot.command.Command
import at.yawk.katbot.sendMessageSafe
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.util.HashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author yawkat
 */
@Singleton
class Cip @Inject constructor(
        val objectMapper: ObjectMapper,
        val eventBus: EventBus
) {
    companion object {
        private val log = LoggerFactory.getLogger(Cip::class.java)
    }

    /**
     * @see loadMap
     */
    private lateinit var roomMappings: Map<String, String>

    fun start() {
        log.info("Loading rooms")
        roomMappings = loadMap()
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(command: Command) {
        if (command.line.messageIs("cip")) {

            val nick = (command.target ?: command.actor).nick

            val state = try {
                loadState()
            } catch(e: Exception) {
                log.warn("Could not load cipmap", e)
                command.channel.sendMessageSafe("$nick, could not fetch data")
                return
            }

            data class Counter(var occupied: Int, var total: Int)
            val roomCounters = HashMap<String, Counter>()
            for (entry in state.entries) {
                val room = roomMappings[entry.key] ?: continue
                val counter = roomCounters.getOrPut(room, { Counter(0, 0) })

                counter.total++
                if (entry.value.occupied) counter.occupied++
            }

            val rooms = roomCounters
                    .entries
                    .sortedBy { -it.value.total }
                    .map { "${it.key}: ${it.value.occupied}/${it.value.total}" }
                    .joinToString(" ")
            command.channel.sendMessageSafe("$nick, $rooms")
            throw CancelEvent
        }
    }

    /**
     * @return A map of `pc id -> state`
     */
    fun loadState(): Map<String, ComputerState> {
        val dataJs = fetchFromProxy("api/hosts")
        return objectMapper.readValue(dataJs, object : TypeReference<Map<String, ComputerState>>() {})
    }

    private fun fetchFromProxy(path: String): String {
        // i really hate my life right now.
        // the old code (which just used java) broke with 'Protocol family unavailable' on a server where everything
        // except java works with ipv6. Apparently, to detect ipv6 support, openjdk reads /proc/net/if_inet6 which
        // under the grsecurity kernel of the server is not readable to users. Then hotspot yields a beautifully
        // undescriptive error message that leads to hours of debugging.

        // because I don't want to reboot this server at the moment, the grsecurity flag stays and I just use curl
        // which does not break.

        val process = ProcessBuilder("curl", "-H", "Host: cipmap.cs.fau.de", "--insecure", "https://${System.getProperty("cipHost")}/$path").start()
        val dataJs = String(process.inputStream.readBytes())
        if (process.waitFor() != 0) throw Exception("return status != 0")
        return dataJs
    }

    data class ComputerState(
            @JsonProperty("idle")
            val idleTime: Int,
            @JsonProperty("update")
            val updateTime: Int,
            val occupied: Boolean,
            @JsonProperty("gecos")
            val personGroup: String,
            @JsonProperty("name")
            val personName: String
    )

    /**
     * @return A map of `pc id -> room id`
     */
    fun loadMap(): Map<String, String> {
        var mapJs = fetchFromProxy("static/server/js/map.js")

        // leading assignment
        mapJs = mapJs.substring(mapJs.indexOf("map = ") + "map = ".length)
        // trailing commas in objects
        mapJs = mapJs.replace(",\\s*}".toRegex(RegexOption.MULTILINE), "}")

        val rooms = HashMap<String, String>()

        val tree = objectMapper.reader()
                .withFeatures(JsonParser.Feature.ALLOW_COMMENTS)
                .readTree(mapJs)
        tree.fields().forEach { room ->
            if (room.key == "diverse") return@forEach
            if (room.key == "doors") return@forEach

            room.value.elements().forEach {
                rooms.put(it.get("id").asText(), room.key)
            }
        }

        return rooms
    }
}