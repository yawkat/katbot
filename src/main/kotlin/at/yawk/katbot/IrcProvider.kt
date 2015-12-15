package at.yawk.katbot

/**
 * @author yawkat
 */
interface IrcProvider {
    fun sendToChannels(channels: List<String>, message: String)

    fun registerEventListener(listener: Any)
}