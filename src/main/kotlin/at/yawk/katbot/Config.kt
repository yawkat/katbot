package at.yawk.katbot

import java.net.URI

/**
 * @author yawkat
 */
data class Config(
        val nick: String,
        val server: Config.Server,
        val feeds: Map<URI, RssFeedListener.FeedConfiguration>,
        val forums: Map<URI, ForumListener.ForumConfiguration>,
        val paste: at.yawk.paste.client.Config,
        val interactions: Map<String, List<String>>,
        val ignore: Set<String>,
        val eventChannels: Set<String>,
        val dockerUrl: String
) {
    data class Server(
            val host: String,
            val port: Int,
            val secure: Boolean,
            val password: String
    )
}
