package at.yawk.katbot.exposure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.ByteStreams
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.zip.ZipInputStream

object OnlineExposureApi : ExposureApi {
    private const val BASE_URL = "https://svc90.main.px.t-online.de/version/v1/diagnosis-keys/country/DE/date"
    private val MAGIC = "EK Export v1".padEnd(16).toByteArray()

    private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())

    override fun listDates(): List<LocalDate> = objectMapper.readValue(URL(BASE_URL))

    override fun listTimes(date: LocalDate): List<LocalDateTime> = try {
        objectMapper.readValue<IntArray>(URL("$BASE_URL/$date/hour")).map { date.atTime(it, 0) }
    } catch (e: FileNotFoundException) {
        emptyList()
    }

    private fun loadKeys(url: URL): TemporaryExposureKeyExport {
        ZipInputStream(url.openStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == "export.bin") {
                    val magic = ByteArray(16)
                    @Suppress("UnstableApiUsage")
                    ByteStreams.readFully(zip, magic)
                    if (!magic.contentEquals(MAGIC)) {
                        throw IOException("Invalid magic")
                    }
                    return TemporaryExposureKeyExport.ADAPTER.decode(zip)
                }
            }
        }
        throw NoSuchElementException()
    }

    override fun loadKeys(date: LocalDate) = loadKeys(URL("$BASE_URL/$date"))

    override fun loadKeys(date: LocalDateTime): TemporaryExposureKeyExport {
        require(date.truncatedTo(ChronoUnit.HOURS) == date)
        return loadKeys(URL("$BASE_URL/${date.toLocalDate()}/hour/${date.hour}"))
    }
}