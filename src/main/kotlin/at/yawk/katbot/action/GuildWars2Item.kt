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

/**
 * as requested by nakami
 *
 * @author yawkat
 */
class GuildWars2Item @Inject constructor(
        val objectMapper: ObjectMapper,
        val eventBus: EventBus
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
                .append(" buys ${firstResult.buys.unitPrice}")
                .append(" sells ${firstResult.sells.unitPrice}")
        if (candidates.size > 1) {
            val others = candidates.subList(1, Math.min(candidates.size, 5))
            message.append(" | Other matches: ").append(others.joinToString(", ") { "'${it.name}'" })
            if (candidates.size > 5) message.append(" ...")
        }
        return message.toString()
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