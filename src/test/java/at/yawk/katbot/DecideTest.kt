package at.yawk.katbot

import org.testng.Assert.*
import org.testng.annotations.Test

/**
 * @author yawkat
 */
class DecideTest {
    @Test
    fun testParsePossibilities() {
        assertEquals(parseParameters("\"Kaffee?\""), listOf("Kaffee?"))
        assertEquals(parseParameters("Kaffee?"), listOf("Kaffee?"))
        assertEquals(parseParameters("Ka\"f f\"ee?"), listOf("Kaf fee?"))
        assertEquals(parseParameters("Kaf\\ fee?"), listOf("Kaf fee?"))
        assertEquals(parseParameters("Kaf fee?"), listOf("Kaf", "fee?"))
    }
}