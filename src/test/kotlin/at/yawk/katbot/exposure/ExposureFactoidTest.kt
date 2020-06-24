package at.yawk.katbot.exposure

import okio.ByteString
import org.testng.Assert
import org.testng.annotations.Test
import java.time.LocalDate

class ExposureFactoidTest {
    @Test
    fun test() {
        val data = mapOf(
                LocalDate.of(1970, 1, 3) to TemporaryExposureKeyExport(0, 0, "", 0, 0, emptyList(), listOf(
                        TemporaryExposureKey(ByteString.EMPTY, 1, 0, 144),
                        TemporaryExposureKey(ByteString.EMPTY, 1, 144, 144),
                        TemporaryExposureKey(ByteString.EMPTY, 2, 0, 144),
                        TemporaryExposureKey(ByteString.EMPTY, 2, 144, 144),
                        TemporaryExposureKey(ByteString.EMPTY, 3, 0, 144),
                        TemporaryExposureKey(ByteString.EMPTY, 3, 144, 144)
                )),
                LocalDate.of(1970, 1, 4) to TemporaryExposureKeyExport(0, 0, "", 0, 0, emptyList(), listOf(
                        TemporaryExposureKey(ByteString.EMPTY, 1, 0, 144),
                        TemporaryExposureKey(ByteString.EMPTY, 1, 144 * 2, 144),
                        TemporaryExposureKey(ByteString.EMPTY, 2, 0, 144),
                        TemporaryExposureKey(ByteString.EMPTY, 2, 144, 144),
                        TemporaryExposureKey(ByteString.EMPTY, 3, 0, 144),
                        TemporaryExposureKey(ByteString.EMPTY, 3, 144, 144)
                ))
        )
        Assert.assertEquals(
                buildMessage(data, today = LocalDate.of(1970, 1, 6)),
                "Latest update: 1970-01-04 | Keys by date: t-3: 1 (+1) t-4: 5 (+2) t-5: 6 (+3)"
        )
    }
}