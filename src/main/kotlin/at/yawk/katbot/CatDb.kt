package at.yawk.katbot

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
@Singleton
class CatDb @Inject constructor(val httpClient: HttpClient, val objectMapper: ObjectMapper) {
    fun getImage(vararg tags: String): Image = randomChoice(getImages(*tags))

    fun getImages(vararg tags: String): List<Image> {
        var uri = "https://catdb.yawk.at/images?"
        for (tag in tags) {
            uri += "&tag=${URLEncoder.encode(tag, "UTF-8")}"
        }
        val response = httpClient.execute(HttpGet(uri))
        return response.entity.content.use { objectMapper.readValue<List<Image>>(it) }
    }

    data class Image(val id: Int, val url: String, val tags: List<String>)
}