/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import org.kitteh.irc.client.library.element.MessageReceiver

/**
 * @author yawkat
 */
private const val MAX_MESSAGE_LENGTH = 500

fun MessageReceiver.sendMessageSafe(msg: String) {
    this.sendMessage(if (msg.length > MAX_MESSAGE_LENGTH) msg.substring(0, MAX_MESSAGE_LENGTH) else msg)
}