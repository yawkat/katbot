package at.yawk.katbot.passive

import org.testng.Assert
import org.testng.annotations.Test
import java.net.URL

class UrlTitleLoaderTest {
    @Test
    fun youtube() {
        val title = UrlTitleLoader.getTitle(URL("https://www.youtube.com/watch?v=rzgITwK7GdM"))
        Assert.assertNotNull(title)
        Assert.assertTrue(title!!.contains("Pink Cloud"), title)
    }
}