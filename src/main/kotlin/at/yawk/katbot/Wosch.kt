/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import com.google.common.annotations.VisibleForTesting
import javax.inject.Inject

/**
 * @author yawkat
 */
private val MAGIC_WORD = "wosch"

class Wosch @Inject constructor(val eventBus: EventBus) {
    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(command: Command) {
        if (command.message.startsWith(MAGIC_WORD)) {
            if (command.message.length <= MAGIC_WORD.length) {
                command.channel.sendMessage("Usage: $MAGIC_WORD <message>")
            } else {
                val toWosch = command.message.substring(MAGIC_WORD.length + 1)
                command.channel.sendMessage(woschinize(toWosch))
            }

            throw CancelEvent
        }
    }
}

@VisibleForTesting
internal fun woschinize(msg: String): String {
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

private data class Substitution(val english: String, val wosch: String, val wordBoundary: Boolean = false)

private val substitutions = listOf(
        Substitution("internet", "zwischennetz"),
        Substitution("cip", "rechnerschwimmbecken"),
        Substitution("thread", "faden"),
        Substitution("include", "einbinde"),
        Substitution("loop", "schleife"),
        Substitution("dependency", "abhängigkeit"),
        Substitution("dependencies", "abhängigkeiten"),
        Substitution("config", "konfig"),
        Substitution("dev", "entw", wordBoundary = true),
        Substitution("software", "programm"),
        Substitution("computer", "rechner"),
        Substitution("bot", "maschine", wordBoundary = true),
        Substitution("katbot", "katzenmaschine"),
        Substitution("ircbox", "unterhaltungskiste"),
        Substitution("ping", "pling"),
        Substitution("webchat", "netzunterhalter"),
        Substitution("client", "klient"),
        Substitution("server", "dienstleister"),
        Substitution("worker", "arbeiter"),
        Substitution("slave", "sklave"),
        Substitution("master", "boss"),

        // http://www.mirko-hansen.de/downloads/wosch.h
        Substitution("*", "zeiger", wordBoundary = true),
        Substitution("abort", "abbrechen"),
        Substitution("accept", "akzeptiere"),
        Substitution("bind", "binde", wordBoundary = true),
        Substitution("break", "breche"),
        Substitution("calloc", "lreser"),
        Substitution("case", "fall"),
        Substitution("char", "buch", wordBoundary = true),
        Substitution("closedir", "schliesseverz"),
        Substitution("const", "konst"),
        Substitution("continue", "fortsetzen"),
        Substitution("do", "tue", wordBoundary = true),
        Substitution("double", "doppel"),
        Substitution("else", "sonst"),
        Substitution("errno", "fehnu"),
        Substitution("exit", "verlasse"),
        Substitution("float", "flies"),
        Substitution("for", "fuer", wordBoundary = true),
        Substitution("fprintf", "fdruckef"),
        Substitution("free", "befreie"),
        Substitution("if", "falls", wordBoundary = true),
        Substitution("int", "gan", wordBoundary = true),
        Substitution("listen", "zuhoeren"),
        Substitution("main", "haupt"),
        Substitution("malloc", "preser"),
        Substitution("memcpy", "speikop"),
        Substitution("memset", "speischrei"),
        Substitution("opendir", "oeffneverz"),
        Substitution("perror", "dfehler"),
        Substitution("printf", "druckef"),
        Substitution("read", "lese"),
        Substitution("readdir", "leseverz"),
        Substitution("readlink", "leseverk"),
        Substitution("realloc", "frreser"),
        Substitution("return", "antworte"),
        Substitution("sizeof", "groessevon"),
        Substitution("socket", "buchse"),
        Substitution("sprintf", "sdruckef"),
        Substitution("static", "statisch"),
        Substitution("strcat", "zeihin"),
        Substitution("strcmp", "zeiver"),
        Substitution("strcpy", "zeikop"),
        Substitution("strlen", "zeilae"),
        Substitution("struct", "struktur"),
        Substitution("switch", "schalter"),
        Substitution("void", "nix"),
        Substitution("while", "waehrend"),
        Substitution("write", "schreibe")
)