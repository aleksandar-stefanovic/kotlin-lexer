package com.github.aleksandar_stefanovic.kotlin_lexer.regex

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

        val escapedAST = (ast as AST.Concatenation).asts[3]

        assertIs<AST.Literal>(escapedAST)
        assertEquals('\\', escapedAST.char)
    }

    @Test
    fun escapeAsLastChar() {
        assertFails { RegExpr("asd\\").compile() }
    }

    @Test
    fun customRepeatPass() {
        val ast = RegExpr("a{2,5}").compile()

        assertIs<AST.Repeat>(ast)

        val innerAST = ast.ast

        assertIs<AST.SingleCharacter>(innerAST)
        assertEquals('a', innerAST.char)
        assertEquals(2, ast.from)
        assertEquals(5, ast.to)
    }

    @Test
    fun groupingPass() {
        val ast = RegExpr("(a)").compile()

        assertIs<AST.Grouping>(ast)
    }

    @Test
    fun customRepeatPassParsesGroupings() {

        val ast = RegExpr("(a{2,3}){2,5}").compile()

        assertIs<AST.Repeat>(ast)

        val innerAST = ast.ast

        assertIs<AST.Grouping>(innerAST)

        assertEquals(1, innerAST.asts.size)

        val innerRepeatAST = innerAST.asts[0] as AST.Repeat

        assertEquals('a', (innerRepeatAST.ast as AST.SingleCharacter).char)

        assertEquals(2, innerRepeatAST.from)

        assertEquals(3, innerRepeatAST.to)

        assertEquals(2, ast.from)

        assertEquals(5, ast.to)
    }

    @Test
    fun concatenationPassTest() {

        val ast = RegExpr("ab").compile()

        assertIs<AST.Concatenation>(ast)
        assertEquals(2, ast.asts.size)
        assertEquals('a', (ast.asts[0] as AST.SingleCharacter).char)
        assertEquals('b', (ast.asts[1] as AST.SingleCharacter).char)
    }

    @Test
    fun concatenationParsesGroupingsTest() {

        val ast = RegExpr("(ab)cd").compile()

        assertIs<AST.Concatenation>(ast)

        val children = ast.asts

        assertEquals(3, children.size)

        val firstChild = children[0]

        assertIs<AST.Grouping>(firstChild)
        assertEquals(1, firstChild.asts.size)
        assertIs<AST.Concatenation>(firstChild.asts[0])
    }

    @Test
    fun alternationPassTest() {
        val ast = RegExpr("ab|cd|eg").compile()

        assertIs<AST.Alternation>(ast)
        assertEquals(3, ast.asts.size)
    }

    @Test
    fun alternationParsesGroupings() {

        val ast = RegExpr("(ab|cd)").compile()

        assertIs<AST.Grouping>(ast)

        assertEquals(1, ast.asts.size)

        val child = ast.asts[0]

        assertIs<AST.Alternation>(child)
        assertEquals(2, child.asts.size)

    }
}