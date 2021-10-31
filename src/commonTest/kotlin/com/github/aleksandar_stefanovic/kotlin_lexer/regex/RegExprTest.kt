package com.github.aleksandar_stefanovic.kotlin_lexer.regex

import com.github.aleksandar_stefanovic.kotlin_lexer.regex.Repeat.Range
import kotlin.test.*

internal class RegExprTest {

    @Test
    fun emptyExprError() {
        assertFails { RegExpr("") }
    }

    @Test
    fun escapePass() {

        val regExpr = RegExpr("""asd\\asd""")

        val ast = regExpr.compile()

        val escapedAST = (ast as Concatenation).asts[3]

        assertIs<Literal>(escapedAST)
        assertEquals('\\', escapedAST.char)
    }

    @Test
    fun escapeAsLastChar() {
        assertFails { RegExpr("asd\\").compile() }
    }

    @Test
    fun customRepeatPass() {
        val ast = RegExpr("a{2,5}").compile()

        assertIs<Range>(ast)

        val innerAST = ast.ast

        assertIs<SingleCharacter>(innerAST)
        assertEquals('a', innerAST.char)
        assertEquals(2, ast.range.first)
        assertEquals(5, ast.range.last)
    }

    @Test
    fun groupingPass() {
        val ast = RegExpr("(a)").compile()

        assertIs<Grouping>(ast)
    }

    @Test
    fun customRepeatPassParsesGroupings() {

        val ast = RegExpr("(a{2,3}){2,5}").compile()

        assertIs<Range>(ast)

        val innerAST = ast.ast

        assertIs<Grouping>(innerAST)

        val range = innerAST.ast

        assertIs<Range>(range)

        assertEquals('a', (range.ast as SingleCharacter).char)

        assertEquals(2, range.range.first)

        assertEquals(3, range.range.last)

        assertEquals(2, ast.range.first)

        assertEquals(5, ast.range.last)
    }

    @Test
    fun concatenationPassTest() {

        val ast = RegExpr("ab").compile()

        assertIs<Concatenation>(ast)
        assertEquals(2, ast.asts.size)
        assertEquals('a', (ast.asts[0] as SingleCharacter).char)
        assertEquals('b', (ast.asts[1] as SingleCharacter).char)
    }

    @Test
    fun concatenationParsesGroupingsTest() {

        val ast = RegExpr("(ab)cd").compile()

        assertIs<Concatenation>(ast)

        val children = ast.asts

        assertEquals(3, children.size)

        val firstChild = children[0]

        assertIs<Grouping>(firstChild)
        assertIs<Concatenation>(firstChild.ast)
    }

    @Test
    fun alternationPassTest() {
        val ast = RegExpr("ab|cd|eg").compile()

        assertIs<Alternation>(ast)
        assertEquals(3, ast.asts.size)
    }

    @Test
    fun alternationParsesGroupings() {

        val grouping = RegExpr("(ab|cd)").compile()

        assertIs<Grouping>(grouping)

        val child = grouping.ast

        assertIs<Alternation>(child)
        assertEquals(2, child.asts.size)

    }
}