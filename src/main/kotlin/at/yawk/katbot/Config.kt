/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import at.yawk.katbot.action.KatTemp
import at.yawk.katbot.nina.WarningChannel
import at.yawk.katbot.passive.ForumListener
import at.yawk.katbot.passive.RssFeedListener
import at.yawk.katbot.web.WebConfig
import java.net.URI

/**
 * @author yawkat
 */
data class Config(
        val nick: String,
        val server: Config.Server,
        val feeds: Map<URI, RssFeedListener.FeedConfiguration>,
        val forums: Map<URI, ForumListener.ForumConfiguration>,
        val interactions: List<String>,
        val eventChannels: Set<String>,
        val nina: List<WarningChannel> = emptyList(),
        val web: WebConfig,
        val temperature: KatTemp.TemperatureConfig
) {
    data class Server(
            val host: String,
            val port: Int,
            val secure: Boolean,
            val password: String
    )
}
