/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.action

import at.yawk.katbot.CancelEvent
import at.yawk.katbot.EventBus
import at.yawk.katbot.Subscribe
import at.yawk.katbot.command.Command
import at.yawk.katbot.paste.Paste
import at.yawk.katbot.paste.PasteProvider
import at.yawk.katbot.sendMessageSafe
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.annotations.VisibleForTesting
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import org.slf4j.LoggerFactory
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private val log = LoggerFactory.getLogger(GuildWars2Item::class.java)
private const val BOLD = "\u0002"
private const val RESET = "\u000f"

/**
 * as requested by nakami
 *
 * @author yawkat
 */
class GuildWars2Item @Inject constructor(
        val objectMapper: ObjectMapper,
        val eventBus: EventBus,
        val pasteProvider: PasteProvider
) {
    fun start() {
        eventBus.subscribe(this)
    }

    private val idsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(1024)
            .softValues()
            .build(CacheLoader.from { key: String? ->
                searchIdsUncached(key!!)
            })

    private fun searchIds(query: String): List<ItemAndName> = idsCache[query]

    private fun searchIdsUncached(query: String): List<ItemAndName> {
        if (query.contains("/")) return emptyList()
        val uri = "https://www.gw2shinies.com/api/json/idbyname/" + URLEncoder.encode(query, "UTF-8")
        val array = objectMapper.readValue(URL(uri), Array<ItemAndName>::class.java)
        return array?.asList() ?: emptyList()
    }

    private fun fetchPrices(id: Int): ItemPrices {
        val uri = "https://api.guildwars2.com/v2/commerce/prices/$id"
        return objectMapper.readValue(URL(uri), ItemPrices::class.java)
    }

    @Subscribe
    fun command(command: Command) {
        if (!command.line.startsWith("gw2item")) return

        val query = command.line.tailParameterString(1)
        val reply = try {
            runForQuery(query, (command.target ?: command.actor).nick)
        } catch (e: Exception) {
            log.error("Failed to fetch gw2item data for '$query'", e)
            e.toString()
        }
        command.channel.sendMessageSafe(reply)
        throw CancelEvent
    }

    @VisibleForTesting
    internal fun runForQuery(query: String, nick: String): String {
        val candidates = searchIds(query).sortedBy { it.name.length }
        if (candidates.isEmpty()) {
            return "No results."
        }
        val firstResult = fetchPrices(candidates[0].id)

        val message = StringBuilder()
        message.append(nick).append(": ")

        message.append(candidates[0].name)
                .append(" buys $BOLD${formatPrice(firstResult.buys.unitPrice)}$RESET")
                .append(", sells $BOLD${formatPrice(firstResult.sells.unitPrice)}$RESET")
        if (candidates.size > 1) {
            val others = candidates.subList(1, Math.min(candidates.size, 100))
            message.append(" | Other matches: ")
                    .append(pasteProvider.createPaste(Paste(Paste.Type.TEXT, others.joinToString("\n") { it.name })))
        }
        return message.toString()
    }

    private fun formatPrice(price: Int): String {
        if (price == 0) return "0"

        var s = ""
        val gold = price / 10000
        if (gold != 0) s += "${gold}g"
        val silver = (price / 100) % 100
        if (silver != 0) s += "${silver}s"
        val copper = price % 100
        if (copper != 0) s += "${copper}c"
        return s
    }

    private data class ItemAndName(
            @JsonProperty("item_id") val id: Int,
            val name: String
    )

    private data class ItemPrices(
            val id: Int,
            val whitelisted: Boolean,
            val buys: PriceAndQuantity,
            val sells: PriceAndQuantity
    )

    private data class PriceAndQuantity(
            val quantity: Int,
            @JsonProperty("unit_price") val unitPrice: Int
    )
}