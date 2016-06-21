/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import com.google.common.annotations.VisibleForTesting
import java.util.*
import javax.inject.Inject
import javax.sql.DataSource

/**
 * @author yawkat
 */
private val MAGIC_WORD = "wosch"

class Wosch @Inject constructor(val eventBus: EventBus, val dataSource: DataSource, val roleManager: RoleManager) {
    private val substitutions = ArrayList<Substitution>()

    private fun sortSubstitutions() {
        substitutions.sortBy { -it.english.length }
    }

    @Synchronized
    fun start() {
        eventBus.subscribe(this)

        dataSource.connection.closed {
            val results = it.prepareStatement("select * from wosch").executeQuery()

            while (results.next()) {
                substitutions.add(Substitution(
                        results.getString("key"),
                        results.getString("value"),
                        results.getBoolean("wordBoundary")
                ))
            }
        }
        sortSubstitutions()
    }

    @Subscribe
    fun command(command: Command) {
        if (!command.line.startsWith(MAGIC_WORD)) return
        if (command.line.parameters.size <= 1) {
            command.channel.sendMessageSafe("Usage: $MAGIC_WORD <message>")
            throw CancelEvent
        }

        val action = command.line.parameters[1]
        if (command.public && (action == "+=" || action == "-=") && command.line.parameters.size > 2) {
            if (!roleManager.hasRole(command.actor, Role.EDIT_WOSCH)) {
                command.channel.sendMessageSafe("You aren't allowed to do that.")
                throw CancelEvent
            }

            val key = command.line.parameters[2]

            if (action == "-=") {
                if (substitutions.find { it.english == key } == null) {
                    command.channel.sendMessageSafe("No such substitution")
                    throw CancelEvent
                }

                synchronized(this) {
                    dataSource.connection.closed {
                        val statement = it.prepareStatement("delete from wosch where key = ?")
                        statement.setString(1, key)
                        statement.executeUpdate()
                    }
                    substitutions.removeAll { it.english == key }
                }
                command.channel.sendMessageSafe("Substitution removed")
                throw CancelEvent
            } else if (command.line.parameters.size > 3) {
                val value = command.line.parameters[3]
                val wordBoundary = command.line.parameters.getOrNull(4) == "@wordBoundary"

                if (substitutions.find { it.english == key } != null) {
                    command.channel.sendMessageSafe("Substitution already present")
                    throw CancelEvent
                }

                synchronized(this) {
                    dataSource.connection.closed {
                        val statement = it.prepareStatement("insert into wosch (key, value, wordBoundary) values (?, ?, ?)")
                        statement.setString(1, key)
                        statement.setString(2, value)
                        statement.setBoolean(3, wordBoundary)
                        statement.executeUpdate()
                    }
                    substitutions.add(Substitution(key, value, wordBoundary))
                    sortSubstitutions()
                }
                command.channel.sendMessageSafe("Substitution added")
                throw CancelEvent
            }
        }

        val toWosch = command.line.tailParameterString(1)
        synchronized(this) {
            command.channel.sendMessageSafe(woschinize(substitutions, toWosch))
        }

        throw CancelEvent
    }
}

@VisibleForTesting
internal fun woschinize(substitutions: List<Substitution>, msg: String): String {
    @Suppress("NAME_SHADOWING")
    var msg = msg
    var lower = msg.toLowerCase()
    for (substitution in substitutions) {
        var newMessage = msg

        var index = -1
        var offset = 0
        while (true) {
            index = lower.indexOf(substitution.english, index + 1)
            if (index == -1) break
            if (substitution.wordBoundary) {
                if (index > 0 && lower[index - 1].isLetter()) continue
                val end = index + substitution.english.length
                if (end < lower.length && lower[end].isLetter()) continue
            }

            val sub = if (newMessage[index + offset].isUpperCase()) {
                substitution.wosch.capitalize()
            } else {
                substitution.wosch
            }
            newMessage = newMessage.substring(0, index + offset) +
                    sub +
                    newMessage.substring(index + offset + substitution.english.length)
            offset += substitution.wosch.length - substitution.english.length
        }
        if (newMessage != msg) {
            msg = newMessage
            lower = msg.toLowerCase()
        }
    }
    return msg
}

internal data class Substitution(val english: String, val wosch: String, val wordBoundary: Boolean = false)