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
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import java.net.URL
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
        return objectMapper.readValue(dataJs, Hosts::class.java).hosts
    }

    private fun fetchFromProxy(path: String): String {
        return URL("https://cipmap.cs.fau.de/$path").openStream().use {
            it.readAllBytes().toString(Charsets.UTF_8)
        }
    }

    @JsonIgnoreProperties(value = ["exercises_today", "temperatures", "roomname", "hostname"])
    class Hosts {
        val hosts: MutableMap<String, ComputerState> = HashMap()

        @JsonAnySetter
        fun addHost(key: String, state: ComputerState) {
            hosts[key] = state
        }
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
        var mapJs = fetchFromProxy("map.js")

        // leading assignment
        mapJs = mapJs.substring(mapJs.indexOf("map = ") + "map = ".length)
        // trailing commas in objects
        mapJs = mapJs.replace(",\\s*}".toRegex(RegexOption.MULTILINE), "}")

        val rooms = HashMap<String, String>()

        val tree = objectMapper.reader()
                .withFeatures(JsonParser.Feature.ALLOW_COMMENTS)
                .readTree(mapJs)
        tree.fields().forEach { room ->
            room.value["computer"].elements().forEach {
                rooms[it["id"].asText()] = room.key
            }
        }

        return rooms
    }
}