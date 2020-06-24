package at.yawk.katbot.exposure

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

private val INTERVAL_LENGTH = TimeUnit.MINUTES.toSeconds(10)

val TemporaryExposureKey.start: Instant
    get() = Instant.ofEpochSecond(INTERVAL_LENGTH * this.rolling_start_interval_number)

val TemporaryExposureKey.startDate: LocalDate
    get() = start.atOffset(ZoneOffset.UTC).toLocalDate()

val TemporaryExposureKey.length: Duration
    get() = Duration.ofSeconds(this.rolling_period * INTERVAL_LENGTH)
