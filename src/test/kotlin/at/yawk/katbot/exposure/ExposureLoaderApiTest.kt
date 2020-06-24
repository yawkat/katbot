package at.yawk.katbot.exposure

import org.testng.Assert
import org.testng.annotations.Test

class ExposureLoaderApiTest {
    @Test
    fun test() {
        val dates = ExposureLoaderApi.listDates()
        Assert.assertTrue(dates.isNotEmpty())
        val keys = ExposureLoaderApi.loadKeys(dates[0])
        Assert.assertTrue(keys.keys.size > 10)
    }
}