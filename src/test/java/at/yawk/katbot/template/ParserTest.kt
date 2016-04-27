/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.template

import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * @author yawkat
 */
class ParserTest {
    @Test
    fun `simple`() {
        assertEquals(
                Parser.parse("abc def"),
                listOf(Expression.Literal("abc"),
                        Expression.Literal("def"))
        )
    }

    @Test
    fun `simple nested`() {
        assertEquals(
                Parser.parse("abc de\${uio asd}f"),
                listOf(Expression.Literal("abc"),
                        Expression.ConcatNeighbours(listOf(
                                Expression.Literal("de"),
                                Expression.SimpleInvocation(listOf(Expression.Literal("uio"),  Expression.Literal("asd"))),
                                Expression.Literal("f")
                        )))
        )
    }

    @Test
    fun `unclosed nested`() {
        assertEquals(
                Parser.parse("abc de\${uio asdf"),
                listOf(Expression.Literal("abc"),
                        Expression.ConcatNeighbours(listOf(
                                Expression.Literal("de"),
                                Expression.SimpleInvocation(listOf(Expression.Literal("uio"), Expression.Literal("asdf")))
                        )))
        )
    }

    @Test
    fun `exploded nested`() {
        assertEquals(
                Parser.parse("abc de*\${uio asd}f"),
                listOf(Expression.Literal("abc"),
                        Expression.ConcatNeighbours(listOf(
                                Expression.Literal("de"),
                                Expression.ExplodedInvocation(listOf(Expression.Literal("uio"),  Expression.Literal("asd"))),
                                Expression.Literal("f")
                        )))
        )
    }

    @Test
    fun `escape simple`() {
        assertEquals(
                Parser.parse("\\\${abc}"),
                listOf(Expression.Literal("\${abc}"))
        )
    }

    @Test
    fun `escape exploded`() {
        assertEquals(
                Parser.parse("*\\\${abc}"),
                listOf(Expression.Literal("*\${abc}"))
        )
    }

    @Test
    fun `quoting`() {
        assertEquals(
                Parser.parse("ab\"c de\"f ghi"),
                listOf(Expression.Literal("abc def"),
                        Expression.Literal("ghi"))
        )
    }
}