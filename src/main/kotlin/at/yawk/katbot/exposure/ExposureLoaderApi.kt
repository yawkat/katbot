package at.yawk.katbot.exposure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.ByteStreams
import java.io.IOException
import java.net.URL
import java.time.LocalDate
import java.util.zip.ZipInputStream

object ExposureLoaderApi {
    private const val BASE_URL = "https://svc90.main.px.t-online.de/version/v1/diagnosis-keys/country/DE/date"
    private val MAGIC = "EK Export v1".padEnd(16).toByteArray()

    private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())

    fun listDates(): List<LocalDate> = objectMapper.readValue(URL(BASE_URL))

    fun loadKeys(date: LocalDate): TemporaryExposureKeyExport {
        ZipInputStream(URL("$BASE_URL/$date").openStream()).use { zip ->
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
}