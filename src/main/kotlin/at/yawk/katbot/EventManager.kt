/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import com.fasterxml.jackson.databind.ObjectMapper
import org.kitteh.irc.client.library.command.TopicCommand
import org.kitteh.irc.client.library.element.Channel
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.Temporal
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * @author yawkat
 */
class EventManager @Inject constructor(
        val ircProvider: IrcProvider,
        val config: Config,
        val objectMapper: ObjectMapper,
        val eventBus: EventBus,
        val executor: ScheduledExecutorService
) {
    companion object {
        private val ZONE = ZoneId.of("Europe/Berlin")

        private val REMINDER_OFFSETS = mapOf(
                Pair(Pair(Period.ofDays(14), Duration.ZERO), "in two weeks"),
                Pair(Pair(Period.ofDays(7), Duration.ZERO), "in a week"),
                Pair(Pair(Period.ofDays(2), Duration.ZERO), "in two days"),
                Pair(Pair(Period.ofDays(1), Duration.ZERO), "tomorrow")
        )

        private const val EVENT_TOPIC_PREFIX = " || Events ## "

        private val log = LoggerFactory.getLogger(EventManager::class.java)
    }

    private var lastUpdate: Instant? = null

    fun start() {
        eventBus.subscribe(this)
        executor.scheduleAtFixedRate({
            try {
                updateEvents()
            } catch(e: Exception) {
                log.error("Failed to update events", e)
            }
        }, 0, 5, TimeUnit.MINUTES)
    }

    @Subscribe
    fun command(command: Command) {
        if (command.line.messageIs("updateEvents")) {
            updateEvents()
            command.channel.sendMessageSafe("Update complete")
            throw CancelEvent
        }
    }

    @Synchronized
    fun updateEvents() {
        val events = fetchEvents()
        @Suppress("UNCHECKED_CAST")
        val channels = ircProvider.findChannels(config.eventChannels) as List<Channel>

        val now = Instant.now()
        val lastUpdateLocal = lastUpdate
        lastUpdate = now

        if (lastUpdateLocal != null) {
            for (event in events) {
                for (reminderOffset in REMINDER_OFFSETS) {
                    val deadline = OffsetDateTime.ofInstant(event.deadline, ZONE)
                            .minus(reminderOffset.key.first)
                            .minus(reminderOffset.key.second)
                            .toInstant()

                    if (deadline.isBefore(now) && deadline.isAfter(lastUpdateLocal)) {
                        channels.forEach { it.sendMessageSafe("[Reminder] ${event.name} is ${reminderOffset.value}") }
                    }
                }
            }
        }

        val topicStartTime = OffsetDateTime.ofInstant(now, ZONE).minusHours(2).toInstant()
        val topicEndTime = OffsetDateTime.ofInstant(now, ZONE).plusDays(14).toInstant()

        val shownEvents = events.filter { it.deadline.isAfter(topicStartTime) && it.deadline.isBefore(topicEndTime) }
        val topicSuffix = if (shownEvents.isNotEmpty()) {
            EVENT_TOPIC_PREFIX + shownEvents
                    .map { it.name + ": " + it.timeString }
                    .joinToString(" | ")
        } else {
            ""
        }


        for (channel in channels) {
            val oldTopic = channel.topic.value
            oldTopic.ifPresent { oldTopic ->
                val newTopic = if (oldTopic.contains(EVENT_TOPIC_PREFIX.trimEnd())) {
                    oldTopic.substring(0, oldTopic.indexOf(EVENT_TOPIC_PREFIX.trimEnd())) + topicSuffix
                } else {
                    oldTopic + topicSuffix
                }

                if (newTopic != oldTopic) {
                    channel.setTopic(newTopic)
                }
            }

            if (!oldTopic.isPresent) {
                log.warn("Channel ${channel.name} has unknown topic, sending /topic to check")
                TopicCommand(channel.client, channel).execute()
            }
        }
    }

    private fun fetchEvents(): List<Event> {
        val events = ArrayList<Event>()
        val tree = objectMapper.readTree(URL("https://faui2k15.de/events.json"))
        for (event in tree) {
            val timeString = event.get("time").asText()
            val time: Temporal = try {
                OffsetDateTime.parse(timeString)
            } catch(e: DateTimeParseException) {
                LocalDate.parse(timeString)
            }
            events.add(Event(event.get("name").asText(), time))
        }
        events.sortBy { it.deadline }
        return events
    }

    data class Event(val name: String, val time: Temporal) {
        val deadline: Instant
            get() = when (time) {
                is Instant -> time
                is OffsetDateTime -> time.toInstant()
                is LocalDate -> time.atStartOfDay(ZONE).toInstant()
                else -> throw UnsupportedOperationException("Unsupported temporal type ${time.javaClass.name}")
            }

        val timeString: String
            get() {
                fun print(time: Temporal): String {
                    return when (time) {
                        is Instant -> print(OffsetDateTime.ofInstant(time, ZONE))
                        is OffsetDateTime -> time.format(DateTimeFormatter.ofPattern("EEE yyyy-MM-dd HH:mm"))
                        is LocalDate -> time.format(DateTimeFormatter.ofPattern("EEE yyyy-MM-dd"))
                        else -> throw UnsupportedOperationException("Unsupported temporal type ${time.javaClass.name}")
                    }
                }

                return print(time)
            }
    }
}