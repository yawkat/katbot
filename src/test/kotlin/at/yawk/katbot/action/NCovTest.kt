package at.yawk.katbot.action

import org.testng.Assert
import org.testng.annotations.Test

class NCovTest {
    @Test
    fun testLoad() {
        val regions = NCov.load()
        Assert.assertTrue(regions.size >= 22)
        for (region in regions) {
            Assert.assertTrue(region.deaths <= region.cases)
        }
        val china = regions.first { it.name.contains("China") }
        Assert.assertTrue(china.cases >= 7804)
        Assert.assertTrue(china.deaths >= 170)
    }
}