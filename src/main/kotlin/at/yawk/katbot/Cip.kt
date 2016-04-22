/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author yawkat
 */
@Singleton
class Cip @Inject constructor(
        val objectMapper: ObjectMapper,
        val httpClient: HttpClient,
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
                command.channel.sendMessage("$nick, could not fetch data")
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
            command.channel.sendMessage("$nick, $rooms")
            throw CancelEvent
        }
    }

    /**
     * @return A map of `pc id -> state`
     */
    fun loadState(): Map<String, ComputerState> {
        var dataJs = httpClient.execute(HttpGet("http://${System.getProperty("cipHost")}/?callback=x")).entity.content.use {
            it.reader(StandardCharsets.UTF_8).buffered().readText()
        }

        // x({..data..})
        dataJs = dataJs.substring(2, dataJs.length - 1)

        return objectMapper.readValue(dataJs, object : TypeReference<Map<String, ComputerState>>() {})
    }

    data class ComputerState(
            @JsonProperty("idletime")
            val idleTime: Int,
            val information: String,
            val occupied: Boolean,
            @JsonProperty("persongroup")
            val personGroup: String,
            @JsonProperty("personname")
            val personName: String
    )

    /**
     * @return A map of `pc id -> room id`
     */
    fun loadMap(): Map<String, String> {
        var mapJs = httpClient.execute(HttpGet("http://cipmap.t-animal.de/js/map.js")).entity.content.use {
            it.reader(StandardCharsets.UTF_8).buffered().readText()
        }

        // leading assignment
        mapJs = mapJs.replace("map = ", "")
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