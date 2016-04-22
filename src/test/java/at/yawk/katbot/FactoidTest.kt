package at.yawk.katbot

import org.mockito.Mockito
import org.testng.Assert.*
import org.testng.annotations.Test
import javax.sql.DataSource

/**
 * @author yawkat
 */
class FactoidTest {
    val factoid = Factoid(
            EventBus(),
            Mockito.mock(CatDb::class.java),
            Mockito.mock(DataSource::class.java),
            Mockito.mock(RoleManager::class.java),
            Mockito.mock(CommandBus::class.java)
    )

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

    @Test
    fun `upper`() {
        assertEquals(factoid.evaluateFactoidSubExpression(CommandLine("upper abc def")), listOf("ABC", "DEF"))
    }

    @Test
    fun `lower`() {
        assertEquals(factoid.evaluateFactoidSubExpression(CommandLine("lower AbC dEf")), listOf("abc", "def"))
    }

    @Test
    fun `if`() {
        assertEquals(factoid.evaluateFactoidSubExpression(CommandLine("if true 1")), listOf("1"))
        assertEquals(factoid.evaluateFactoidSubExpression(CommandLine("if true 1 2")), listOf("1"))
        assertEquals(factoid.evaluateFactoidSubExpression(CommandLine("if false 1 2")), listOf("2"))
    }

    @Test
    fun `equals`() {
        assertEquals(factoid.evaluateFactoidSubExpression(CommandLine("equal 1 2 3")), listOf("false"))
        assertEquals(factoid.evaluateFactoidSubExpression(CommandLine("equal 1 1 3")), listOf("false"))
        assertEquals(factoid.evaluateFactoidSubExpression(CommandLine("equal 1 1 1")), listOf("true"))
    }

    @Test
    fun `sum`() {
        assertEquals(factoid.evaluateFactoidSubExpression(CommandLine("sum 1 2 3")), listOf("6"))
        assertEquals(factoid.evaluateFactoidSubExpression(CommandLine("sum 1 2.5 3")), listOf("6.5"))
        assertEquals(factoid.evaluateFactoidSubExpression(CommandLine("sum 1 2.5 -3.5")), listOf("0"))
    }
}