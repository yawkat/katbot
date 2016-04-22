package at.yawk.katbot

import org.testng.Assert.*
import org.testng.annotations.Test

/**
 * @author yawkat
 */
class FactoidTest {
    @Test
    fun testCanonical() {
        assertTrue(Factoid.equalsCanonical("a'b'c", "abc"))
        assertTrue(Factoid.equalsCanonical("a'b'c", "'a'b'''''''c''"))
        assertFalse(Factoid.equalsCanonical("a'b'c", "'a'bd'''''''c''"))
    }

    @Test
    fun testEntry() {
        fun findMatch(command: String, vararg entries: Entry): Template? {
            val parameters = CommandLine(command).parameters
            for (pass in PASSES) {
                for (entry in entries) {
                    val match = entry.match(parameters, pass)
                    if (match != null) return match
                }
            }
            return null
        }

        assertNotNull(findMatch("a", Entry("a", "")))
        assertNull(findMatch("ab", Entry("a", "")))
        assertNotNull(findMatch("a", Entry("'a'", "")))
        assertNotNull(findMatch("a'", Entry("'a'", "")))
        assertNull(findMatch("a", Entry("a $", "")))
        assertNotNull(findMatch("a '", Entry("a $", "")))
        assertNotNull(findMatch("a b", Entry("a $", "")))

        assertEquals(findMatch("a b #", Entry("a b $", "1"), Entry("a $", "2"), Entry("a b ", "3"))?.finish(), "1")
        assertEquals(findMatch("a b #", Entry("a $", "2"), Entry("a b ", "3"))?.finish(), "2")
        assertEquals(findMatch("a b ", Entry("a b ", "3"))?.finish(), "3")
    }
}