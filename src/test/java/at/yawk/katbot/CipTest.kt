package at.yawk.katbot

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.impl.client.HttpClientBuilder
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * @author yawkat
 */
class CipTest {
    lateinit var cip: Cip

    @BeforeMethod
    fun setUp() {
        cip = Cip(ObjectMapper().findAndRegisterModules(), HttpClientBuilder.create().build(), EventBus())
    }

    @Test
    fun testLoadMap() {
        println(cip.loadMap())
    }

    @Test
    fun testLoadState() {
        println(cip.loadState())
    }
}