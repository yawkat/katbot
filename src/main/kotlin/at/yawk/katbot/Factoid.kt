package at.yawk.katbot

import com.google.common.base.Ascii
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject
import javax.sql.DataSource

/**
 * @author yawkat
 */
class Factoid @Inject constructor(
        val eventBus: EventBus,
        val catDb: CatDb,
        val dataSource: DataSource,
        val roleManager: RoleManager,
        val commandManager: CommandManager
) {
    companion object {
        private val canonicalChars = BitSet()

        init {
            for (c in '\u0000'..'\uffff') {
                if (c.isLetterOrDigit()) {
                    canonicalChars.set(c.toInt())
                }
            }
            canonicalChars.set(' '.toInt())
        }

        private fun isCanonicalChar(char: Char) = canonicalChars.get(char.toInt())

        /**
         * @return The index of the first canonical character in [string] that is not part of prefix or `null` if the prefix did not apply.
         */
        fun startsWithCanonical(string: String, prefix: String): Int? {
            var i = 0
            var j = 0
            while (true) {
                if (j >= prefix.length) break
                if (!isCanonicalChar(prefix[j])) {
                    j++
                    continue
                }
                if (i >= string.length) break
                if (!isCanonicalChar(string[i])) {
                    i++
                    continue
                }

                if (Ascii.toLowerCase(string[i]) != Ascii.toLowerCase(prefix[j]) ||
                        !string[i].equals(prefix[j], ignoreCase = true)) {
                    return null // mismatch
                }

                i++
                j++
            }

            if (j < prefix.length) return null // haven't used full prefix

            while (i < string.length && !isCanonicalChar(string[i])) i++
            return i
        }

        fun equalsCanonical(a: String, b: String) = startsWithCanonical(a, b) == a.length
    }

    private val factoids = ArrayList<Entry>()

    @Synchronized
    private fun addFactoid(entry: Entry, insertIntoDb: Boolean) {
        val iterator = factoids.iterator()
        while (iterator.hasNext()) {
            val other = iterator.next()
            if (other.components.size == entry.components.size &&
                    equalsCanonical(other.name, entry.name)) {
                dataSource.connection.closed {
                    val statement = it.prepareStatement("delete from factoids where canonicalName = ?")
                    statement.setString(1, other.name)
                    statement.execute()
                }
                iterator.remove()
            }
        }
        if (insertIntoDb) {
            dataSource.connection.closed {
                val statement = it.prepareStatement("insert into factoids (canonicalName, value) values (?, ?)")
                statement.setString(1, entry.name)
                statement.setString(2, entry.value)
                statement.execute()
            }
        }
        factoids.add(entry)
        factoids.sortBy { it.components.size }
    }

    fun start() {
        dataSource.connection.closed {
            val statement = it.prepareStatement("select * from factoids")
            val result = statement.executeQuery()
            while (result.next()) {
                addFactoid(
                        Entry(result.getString("canonicalName"), result.getString("value")),
                        insertIntoDb = false
                )
            }
        }

        eventBus.subscribe(this)
    }

    @Synchronized
    @Subscribe(priority = 100) // low priority
    fun command(event: Command) {
        if (event.message.contains(" = ")) {
            if (!roleManager.hasRole(event.actor, Role.ADD_FACTOIDS)) {
                event.channel.sendMessage("${event.actor.nick}, you are not allowed to do that.")
                throw CancelEvent
            }

            val splitIndex = event.message.indexOf(" = ")
            val name = event.message.substring(0, splitIndex)
            val value = event.message.substring(splitIndex + " = ".length).trimEnd()
            addFactoid(Entry(name, value), insertIntoDb = true)
            event.channel.sendMessage("Factoid added.")
            throw CancelEvent
        }

        val message = event.message.trim()
        if (!equalsCanonical(message, "")) {
            for (factoid in factoids) {
                val match = factoid.match(message)
                if (match != null) {
                    // detect infinite loop
                    if (event.hasCause { it.meta == factoid }) {
                        event.channel.sendMessage("Infinite loop in factoid ${factoid.name}")
                        throw CancelEvent
                    }

                    val finalTemplate = match.set("sender", event.actor.nick)
                            .set("cat", { catDb.getImage().url })
                            .setWithParameter("cat", { tags ->
                                catDb.getImage(*tags.split("|").toTypedArray()).url
                            })
                            .setWithParameter("random", { choices -> randomChoice(choices.split("|")) })
                            .setActorAndTarget(event)
                    if (!commandManager.parseAndFire(
                            event.actor,
                            event.channel,
                            finalTemplate.finish(),
                            event.public,
                            false,
                            event.userLocator,
                            Cause(event, factoid)
                    )) {
                        finalTemplate.sendTo(event.channel)
                    }
                    throw CancelEvent
                }
            }
        }
    }

    private data class Entry(
            val name: String,
            val value: String
    ) {
        companion object {
            private val INNER_PARAMETER_PATTERN: Pattern = NICK_PATTERN.toPattern(Pattern.CASE_INSENSITIVE)
            private val LAST_PARAMETER_PATTERN: Pattern = ".+".toPattern(Pattern.CASE_INSENSITIVE)
        }

        val components = name.split('$')
        var response = Template(value)

        fun match(string: String): Template? {
            var result = response
            var i = 0
            for ((index, component) in components.withIndex()) {
                if (i > string.length) return null

                if (index > 0) {
                    val pattern = if (index == components.size - 1 && component.isEmpty())
                        LAST_PARAMETER_PATTERN else INNER_PARAMETER_PATTERN

                    val matcher = pattern.matcher(string)
                    if (!matcher.find(i)) return null
                    if (matcher.start() != i) return null
                    result = result.set("$index", matcher.group())
                    i = matcher.end()
                }

                i += startsWithCanonical(string.substring(i), component) ?: return null
            }
            if (i < string.length) return null

            return result
        }
    }
}