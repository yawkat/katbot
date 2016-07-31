/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.markov

import at.yawk.katbot.*
import at.yawk.katbot.command.Command
import org.skife.jdbi.v2.DBI
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.inject.Inject

/**
 * @author yawkat
 */
private const val PREFIX_LENGTH = 2
private const val MAX_MESSAGE_LENGTH = 20 // max 20 words

private fun splitMessage(message: String) = message.split(' ').filter { it.isNotBlank() }
private fun joinMessage(message: List<String>) = message.joinToString(" ")

internal fun generateMessage(chainDao: ChainDao, chainName: String): String? {
    val message = splitMessage(chainDao.selectStart(chainName) ?: return null).toMutableList()
    while (message.size < MAX_MESSAGE_LENGTH) {
        val prefix = joinMessage(message.takeLast(PREFIX_LENGTH))
        message.add(chainDao.selectSuffix(chainName, prefix) ?: break)
    }
    return joinMessage(message)
}

internal fun learnFromMessage(chainDao: ChainDao, chainName: String, message: List<String>) {
    for (start in 0..(message.size - PREFIX_LENGTH - 1)) {
        val prefix = joinMessage(message.subList(start, start + PREFIX_LENGTH))
        val suffix = message[start + PREFIX_LENGTH]
        if (start == 0) { chainDao.insertStart(chainName, prefix) }
        chainDao.insertSuffix(chainName, prefix, suffix)
    }
}

class Markov @Inject constructor(val eventBus: EventBus, val roleManager: RoleManager, dbi: DBI) {
    private val dao = dbi.open(ChainDao::class.java)

    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(event: Command) {
        if (event.line.startsWith("markov")) {
            val chainName = event.line.parameters.getOrNull(1)
            if (chainName == null) {
                event.channel.sendMessage("Usage: ~markov <chain>")
                throw CancelEvent
            }
            when (event.line.parameters.getOrNull(2)) {
                "+=" -> {
                    if (!roleManager.hasRole(event.actor, Role.EDIT_MARKOV)) {
                        event.channel.sendMessageSafe("You aren't allowed to do that.")
                        throw CancelEvent
                    }
                    learnFromMessage(dao, chainName, event.line.parameterRange(3))
                    event.channel.sendMessageSafe("Learned a little!")
                }
                "loadlocal" -> {
                    if (!roleManager.hasRole(event.actor, Role.ADMIN)) {
                        event.channel.sendMessageSafe("You aren't allowed to do that.")
                        throw CancelEvent
                    }
                    var count = 0
                    Files.lines(Paths.get("markovin")).forEach {
                        val parts = it.split(' ').filter { it.isNotBlank() }
                        if (parts.size > 6) {
                            count++
                            learnFromMessage(dao, chainName, parts)
                        }
                    }
                    event.channel.sendMessageSafe("Learned a lot ($count lines)")
                }
                null -> event.channel.sendMessageSafe(generateMessage(dao, chainName) ?: "Chain is empty!")
                else -> event.channel.sendMessageSafe("Unknown command")
            }
            throw CancelEvent
        }
    }
}