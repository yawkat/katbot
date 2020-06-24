package at.yawk.katbot.exposure

import java.time.LocalDate
import java.time.LocalDateTime

interface ExposureApi {
    fun listDates(): List<LocalDate>
    fun listTimes(date: LocalDate): List<LocalDateTime>
    fun loadKeys(date: LocalDate): TemporaryExposureKeyExport
    fun loadKeys(date: LocalDateTime): TemporaryExposureKeyExport
}