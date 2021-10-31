package com.github.aleksandar_stefanovic.kotlin_lexer.automata

import com.github.aleksandar_stefanovic.kotlin_lexer.automata.Automaton.Edge
import com.github.aleksandar_stefanovic.kotlin_lexer.regex.RegExpr
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AutomatonTest {

    @Test
    fun matchString1() {
        val automaton = RegExpr("a").compile().toAutomaton().toDFA()
        assertEquals("a", automaton.matchInput("a").first)
    }

    @Test
    fun matchString2() {
        val ast = RegExpr("a{2,3}").compile()
        val automaton = ast.toAutomaton()
        val dfa = automaton.toDFA()
        assertEquals("aaa", dfa.matchInput("aaa").first)
    }

    @Test
    fun matchString3() {
        val ast = RegExpr("a{2,3}|b|c").compile()
        val automaton = ast.toAutomaton()
        val dfa = automaton.toDFA()
        assertEquals("c", dfa.matchInput("c").first)
    }

    @Test
    fun matchString4() {
        val ast = RegExpr("(a{2,3}|b|c)b*").compile()
        val automaton = ast.toAutomaton()
        val dfa = automaton.toDFA()
        assertEquals("b", dfa.matchInput("b").first)
    }

    @Test
    fun matchConcatenation() {
        val ast = RegExpr("abc").compile()
        println(ast)
        val automaton = ast.toAutomaton()
        println(automaton)
        val dfa = automaton.toDFA()
        println(dfa)
        assertEquals("abc", dfa.matchInput("abc").first)
    }

    @Test
    fun toDfaTest() {
        val nfa = Automaton(
            startState = 0,
            endStates = setOf(10),
            edges = setOf(
                Edge(0, 1, null),
                Edge(0, 7, null),
                Edge(1, 2, null),
                Edge(1, 4, null),
                Edge(2, 3, 'a'),
                Edge(3, 6, null),
                Edge(4, 5, 'b'),
                Edge(5, 6, null),
                Edge(6, 1, null),
                Edge(6, 7, null),
                Edge(7, 8, 'a'),
                Edge(8, 9, 'b'),
                Edge(9, 10, 'b')
            ),
            tokens = mapOf(10 to "FINAL")
        )

        val dfa = nfa.toDFA()

        val expectedEdges = setOf(
            DFA.Edge(0, 2, 'a'),
            DFA.Edge(0, 1, 'b'),
            DFA.Edge(1, 2, 'a'),
            DFA.Edge(1, 1, 'b'),
            DFA.Edge(2, 2, 'a'),
            DFA.Edge(2, 3, 'b'),
            DFA.Edge(3, 2, 'a'),
            DFA.Edge(3, 4, 'b'),
            DFA.Edge(4, 2, 'a'),
            DFA.Edge(4, 1, 'b')
        )

        val indexDifference = dfa.startState // This amount will be subtracted from each index

        // Make indices relative amongst each other, not unique
        val relativeEdges = dfa.edges.map { (from, to, symbol) ->
            DFA.Edge(from - indexDifference, to - indexDifference, symbol)
        }.toSet()

        val relativeEndStates = dfa.endStates.mapKeys { (key, _) -> key - indexDifference }

        assertEquals(expectedEdges, relativeEdges)
        assertEquals(1, dfa.endStates.size)
        assertEquals(4, dfa.endStates.keys.first() - indexDifference)
        assertEquals(mapOf(Pair(4, listOf("FINAL"))), relativeEndStates)
    }

}