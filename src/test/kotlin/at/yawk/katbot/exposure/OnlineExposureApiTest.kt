package at.yawk.katbot.exposure

import org.testng.Assert
import org.testng.annotations.Test
import java.time.LocalDate
import java.time.LocalDateTime

class OnlineExposureApiTest {
    @Test
    fun test() {
        val dates = OnlineExposureApi.listDates()
        Assert.assertTrue(dates.isNotEmpty())
        val keys = OnlineExposureApi.loadKeys(dates[0])
        Assert.assertTrue(keys.keys.size > 10)
        val times = OnlineExposureApi.listTimes(dates[0])
        Assert.assertTrue(times.isNotEmpty())
        val timeKeys = OnlineExposureApi.loadKeys(times[0])
        Assert.assertTrue(timeKeys.keys.size > 10)
    }

    @Test
    fun missingTime() {
        Assert.assertEquals(OnlineExposureApi.listTimes(LocalDate.of(2000, 0, 0)), emptyList<LocalDateTime>())
    }
}