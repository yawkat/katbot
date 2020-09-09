package at.yawk.katbot.nina

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.net.URL

object Nina {
    private val objectMapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())

    fun fetchWarnings() = objectMapper.readValue<List<Announcement>>(
            URL("https://warnung.bund.de/bbk.mowas/gefahrendurchsagen.json"))
}