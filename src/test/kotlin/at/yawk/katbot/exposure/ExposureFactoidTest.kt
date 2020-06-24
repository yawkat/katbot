package at.yawk.katbot.exposure

import okio.ByteString
import org.testng.Assert
import org.testng.annotations.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ExposureFactoidTest {
    @Test
    fun test() {
        val data = mapOf(
                LocalDateTime.of(1970, 1, 3, 10, 0) to AnalysisResult(listOf(
                        listOf(
                                TemporaryExposureKey(ByteString.EMPTY, 1, 0, 144),
                                TemporaryExposureKey(ByteString.EMPTY, 1, 144, 144)
                        ),
                        listOf(
                                TemporaryExposureKey(ByteString.EMPTY, 2, 0, 144),
                                TemporaryExposureKey(ByteString.EMPTY, 2, 144, 144)
                        ),
                        listOf(
                                TemporaryExposureKey(ByteString.EMPTY, 3, 0, 144),
                                TemporaryExposureKey(ByteString.EMPTY, 3, 144, 144)
                        )
                )),
                LocalDateTime.of(1970, 1, 4, 10, 0) to AnalysisResult(listOf(
                        listOf(
                                TemporaryExposureKey(ByteString.EMPTY, 1, 144, 144),
                                TemporaryExposureKey(ByteString.EMPTY, 1, 144 * 2, 144)
                        ),
                        listOf(
                                TemporaryExposureKey(ByteString.EMPTY, 2, 0, 144),
                                TemporaryExposureKey(ByteString.EMPTY, 2, 144, 144)
                        ),
                        listOf(
                                TemporaryExposureKey(ByteString.EMPTY, 3, 0, 144),
                                TemporaryExposureKey(ByteString.EMPTY, 3, 144, 144)
                        )
                ))
        )
        Assert.assertEquals(
                buildMessage(data),
                "Latest update: 1970-01-04 10:00 | Registered infections by date: t-1d: 1 (+1) t-2d: 5 (+2)"
        )
    }
}