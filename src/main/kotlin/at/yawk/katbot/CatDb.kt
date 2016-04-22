/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.inject.ImplementedBy
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import java.net.URLEncoder
import java.util.concurrent.ThreadLocalRandom
import javax.inject.Inject
import javax.inject.Singleton

fun <E> randomChoice(list: List<E>): E {
    return list[ThreadLocalRandom.current().nextInt(list.size)]
}

/**
 * @author yawkat
 */
@ImplementedBy(CatDbImpl::class)
interface CatDb {
    fun getImage(vararg tags: String): CatDb.Image

    fun getImages(vararg tags: String): List<CatDb.Image>

    data class Image(val id: Int, val url: String, val tags: List<String>)
}

@Singleton
class CatDbImpl @Inject constructor(val httpClient: HttpClient, val objectMapper: ObjectMapper) : CatDb {
    override fun getImage(vararg tags: String): CatDb.Image = randomChoice(getImages(*tags))

    override fun getImages(vararg tags: String): List<CatDb.Image> {
        var uri = "https://catdb.yawk.at/images?"
        for (tag in tags) {
            uri += "&tag=${URLEncoder.encode(tag, "UTF-8")}"
        }
        val response = httpClient.execute(HttpGet(uri))
        return response.entity.content.use { objectMapper.readValue<List<CatDb.Image>>(it) }
    }
}