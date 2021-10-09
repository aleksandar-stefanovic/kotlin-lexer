package com.github.aleksandar_stefanovic.kotlin_lexer.automata

import com.github.aleksandar_stefanovic.kotlin_lexer.automata.Automaton.Edge
import com.github.aleksandar_stefanovic.kotlin_lexer.regex.RegExpr
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class AutomatonTest {

    // TODO Find a way to compare automata
/*    @Test
    fun cloneTest() {
        val automaton = RegExpr("a|b*").compile().toAutomaton()

        val clone = automaton.clone()

    }*/

    @Test
    fun matchTrivialStringTest() {
        val automaton = RegExpr("a").compile().toAutomaton().toDFA()
        assertTrue(automaton.matchInput("a"))
    }

    @Test
    fun matchStringTest() {
        val automaton = RegExpr("(a{2,3}|b)b*").compile().toAutomaton().toDFA()
        println(automaton)
        assertTrue(automaton.matchInput("b"))
    }


    @Test
    fun toDfaTest() {
        val nfa = Automaton(
            startState = 0,
            endState = 10,
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
            )
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

        assertEquals(expectedEdges, relativeEdges)
        assertEquals(1, dfa.endStates.size)
        assertEquals(4, dfa.endStates.first() - indexDifference)
    }

}