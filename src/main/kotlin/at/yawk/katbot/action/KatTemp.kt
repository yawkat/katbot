/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.action

import at.yawk.katbot.CancelEvent
import at.yawk.katbot.Config
import at.yawk.katbot.EventBus
import at.yawk.katbot.Subscribe
import at.yawk.katbot.command.Command
import at.yawk.katbot.sendMessageSafe
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import java.net.URL
import java.time.Instant
import javax.inject.Inject

/**
 * @author yawkat
 */
class KatTemp @Inject constructor(val eventBus: EventBus, config: Config, val objectMapper: ObjectMapper) {
    private val config = config.temperature

    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(command: Command) {
        if (command.line.messageIs("kattemp")) {

            var components = emptyList<String>()

            for (group in config.groups) {
                val url = config.url.removeSuffix("/") + "/render?target=${group.wildcard}&format=json&from=-5min"
                val graphs = objectMapper.readValue<List<Graph>>(URL(url), object : TypeReference<List<Graph>>() {})
                components += graphs.map {
                    val name = group.aliases[it.name] ?: it.name.removePrefix(group.prefix)
                    val value = it.points.filter { it.value != null }.maxBy { it.timestamp }?.value
                    "$name: ${value?.toString()?.plus("°C") ?: "N/A"}"
                }
            }

            command.channel.sendMessageSafe(components.joinToString(" | "))

            throw CancelEvent
        }
    }

    data class TemperatureConfig(
            val url: String,
            val groups: List<Group>
    ) {
        data class Group(
                val wildcard: String,
                val prefix: String,
                val aliases: Map<String, String>
        )
    }

    data class Graph(
            @JsonProperty("target") val name: String,
            @JsonProperty("datapoints") val points: List<Point>
    )

    data class Point(
            val value: Double?,
            val timestamp: Instant
    ) {
        @JsonCreator constructor(array: ArrayNode) : this(
                array.get(0).let { if (it.isNull) null else it.asDouble() },
                Instant.ofEpochSecond(array.get(1).asLong())
        ) {
            if (array.size() != 2) throw IllegalArgumentException()
        }
    }
}