/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.template

import at.yawk.katbot.sendMessageSafe
import com.fasterxml.jackson.annotation.JsonCreator
import org.kitteh.irc.client.library.element.MessageReceiver

/**
 * @author yawkat
 */
class Template private constructor(private val expressions: List<Expression>, private val vm: VM) {
    companion object {
        private val baseVm = SimpleVM()
    }

    @JsonCreator
    constructor(template: String) : this(Parser.parse(template), baseVm)

    fun with(key: String, value: String) = with(ConstantFunction(key.split(' '), listOf(value)))
    fun with(key: String, value: () -> String) = with(ConstantFunction(key.split(' '), { listOf(value()) }))
    fun with(function: Function) = Template(expressions, vm.plusFunctions(listOf(function)))

    fun evaluate() = expressions.flatMap { it.computeValue(vm) }.joinToString(" ")

    fun sendTo(vararg messageReceiver: MessageReceiver) {
        sendTo(messageReceiver.asList())
    }

    fun sendTo(messageReceivers: List<MessageReceiver>) {
        var msg = evaluate()
        if (msg.startsWith("/me ")) {
            val ctcp = "ACTION ${msg.substring(4)}"
            messageReceivers.forEach { it.sendCTCPMessage(ctcp) }
        } else {
            // escape /me and ~ prefix with /send
            if (msg.startsWith("/send ")) msg = msg.substring("/send ".length)
            messageReceivers.forEach { it.sendMessageSafe(msg) }
        }
    }
}