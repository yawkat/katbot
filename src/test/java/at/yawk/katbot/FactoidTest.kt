package at.yawk.katbot

import org.testng.Assert.*
import org.testng.annotations.Test

/**
 * @author yawkat
 */
class FactoidTest {
    @Test
    fun testCanonical() {
        assertEquals(Factoid.startsWithCanonical("a'b'c", "a''"), 2)
        assertEquals(Factoid.startsWithCanonical("a'b'c", "a'"), 2)
        assertEquals(Factoid.startsWithCanonical("a'b'c", "a"), 2)
        assertEquals(Factoid.startsWithCanonical("a'b'c", ""), 0)
        assertNull(Factoid.startsWithCanonical("a'b'c", "x"))
        assertTrue(Factoid.equalsCanonical("a'b'c", "abc"))
        assertTrue(Factoid.equalsCanonical("a'b'c", "'a'b'''''''c''"))
        assertFalse(Factoid.equalsCanonical("a'b'c", "'a'bd'''''''c''"))
    }
}